package com.example.interviewassistant.feature.interviewassistant

import com.example.interviewassistant.core.util.CoroutineDispatcherProvider
import com.example.interviewassistant.feature.interviewassistant.data.remote.llm.LlmChunk
import com.example.interviewassistant.feature.interviewassistant.data.remote.llm.LlmMessage
import com.example.interviewassistant.feature.interviewassistant.data.remote.llm.OpenAiStreamGateway
import com.example.interviewassistant.feature.interviewassistant.data.remote.speech.SpeechRecognitionEvent
import com.example.interviewassistant.feature.interviewassistant.domain.model.AnswerTriggerMode
import com.example.interviewassistant.feature.interviewassistant.domain.model.AssistantAnswer
import com.example.interviewassistant.feature.interviewassistant.domain.model.InterviewSession
import com.example.interviewassistant.feature.interviewassistant.domain.model.InterviewSessionDetail
import com.example.interviewassistant.feature.interviewassistant.domain.model.InterviewSessionStatus
import com.example.interviewassistant.feature.interviewassistant.domain.model.LlmConfiguration
import com.example.interviewassistant.feature.interviewassistant.domain.model.OcrJob
import com.example.interviewassistant.feature.interviewassistant.domain.model.OcrStatus
import com.example.interviewassistant.feature.interviewassistant.domain.model.ProviderConfiguration
import com.example.interviewassistant.feature.interviewassistant.domain.model.ProviderSecretStatus
import com.example.interviewassistant.feature.interviewassistant.domain.model.ProviderSecretUpdate
import com.example.interviewassistant.feature.interviewassistant.domain.model.ProviderSecrets
import com.example.interviewassistant.feature.interviewassistant.domain.model.Resume
import com.example.interviewassistant.feature.interviewassistant.domain.model.TranscriptSegment
import com.example.interviewassistant.feature.interviewassistant.domain.model.XunfeiCredentials
import com.example.interviewassistant.feature.interviewassistant.domain.repository.InterviewSessionRepository
import com.example.interviewassistant.feature.interviewassistant.domain.repository.ProviderConfigurationRepository
import com.example.interviewassistant.feature.interviewassistant.domain.repository.ResumeRepository
import com.example.interviewassistant.feature.interviewassistant.domain.repository.SpeechRecognizer
import com.example.interviewassistant.feature.interviewassistant.domain.usecase.InterviewAnswerGenerator
import com.example.interviewassistant.feature.interviewassistant.domain.usecase.InterviewPromptBuilder
import com.example.interviewassistant.feature.interviewassistant.presentation.state.InterviewSessionUiEvent
import com.example.interviewassistant.feature.interviewassistant.presentation.viewmodel.InterviewSessionViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class InterviewSessionViewModelTest {
    @Test
    fun `automatic mode deduplicates repeated final transcript and persists answer`() = runTest {
        val resume = sampleResume()
        val gateway = FakeLlmGateway()
        val sessionRepository = FakeSessionRepository(resume)
        val dispatcher = TestDispatchers(testScheduler)
        val viewModel = createViewModel(resume, sessionRepository, FakeSpeechRecognizer(), gateway, dispatcher)

        viewModel.onEvent(
            InterviewSessionUiEvent.StartSession(
                resume.id,
                "Interview",
                AnswerTriggerMode.AUTOMATIC,
            ),
        )
        advanceUntilIdle()
        viewModel.onEvent(InterviewSessionUiEvent.StartListening)
        advanceUntilIdle()

        assertEquals(1, gateway.callCount)
        assertEquals("建议", viewModel.uiState.value.answers.single().content)
        assertEquals(1, sessionRepository.transcripts.size)
    }

    @Test
    fun `switching session does not append stale generation to new session`() = runTest {
        val resume = sampleResume()
        val gateway = SlowLlmGateway(delayMillis = 500)
        val sessionRepository = FakeSessionRepository(resume)
        val viewModel = createViewModel(
            resume,
            sessionRepository,
            FakeSpeechRecognizer(),
            gateway,
            TestDispatchers(testScheduler),
        )

        viewModel.onEvent(
            InterviewSessionUiEvent.StartSession(resume.id, "Session A", AnswerTriggerMode.AUTOMATIC),
        )
        advanceUntilIdle()
        val sessionA = viewModel.uiState.value.session!!.id
        viewModel.onEvent(InterviewSessionUiEvent.StartListening)
        // Let generation start but not finish.
        testScheduler.advanceTimeBy(50)

        viewModel.onEvent(
            InterviewSessionUiEvent.StartSession(resume.id, "Session B", AnswerTriggerMode.MANUAL),
        )
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.session?.id != sessionA)
        assertEquals(emptyList(), state.answers)
        assertFalse(state.isGenerating)
    }

    @Test
    fun `leave workspace cancels in-flight generation`() = runTest {
        val resume = sampleResume()
        val gateway = SlowLlmGateway(delayMillis = 500)
        val sessionRepository = FakeSessionRepository(resume)
        val viewModel = createViewModel(
            resume,
            sessionRepository,
            FakeSpeechRecognizer(emitFinals = false),
            gateway,
            TestDispatchers(testScheduler),
        )

        viewModel.onEvent(
            InterviewSessionUiEvent.StartSession(resume.id, "Interview", AnswerTriggerMode.MANUAL),
        )
        advanceUntilIdle()
        viewModel.onEvent(InterviewSessionUiEvent.UpdateQuestion("请介绍项目经验"))
        viewModel.onEvent(InterviewSessionUiEvent.GenerateAnswer)
        testScheduler.advanceTimeBy(50)
        assertTrue(viewModel.uiState.value.isGenerating)

        viewModel.onEvent(InterviewSessionUiEvent.LeaveWorkspace)
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isGenerating)
        assertEquals(emptyList(), viewModel.uiState.value.answers)
    }

    @Test
    fun `cancel generation then regenerate runs a single completed answer`() = runTest {
        val resume = sampleResume()
        val gateway = SlowLlmGateway(delayMillis = 100)
        val sessionRepository = FakeSessionRepository(resume)
        val viewModel = createViewModel(
            resume,
            sessionRepository,
            FakeSpeechRecognizer(emitFinals = false),
            gateway,
            TestDispatchers(testScheduler),
        )

        viewModel.onEvent(
            InterviewSessionUiEvent.StartSession(resume.id, "Interview", AnswerTriggerMode.MANUAL),
        )
        advanceUntilIdle()
        viewModel.onEvent(InterviewSessionUiEvent.UpdateQuestion("请介绍项目经验"))
        viewModel.onEvent(InterviewSessionUiEvent.GenerateAnswer)
        testScheduler.advanceTimeBy(10)
        viewModel.onEvent(InterviewSessionUiEvent.CancelGeneration)
        advanceUntilIdle()
        viewModel.onEvent(InterviewSessionUiEvent.GenerateAnswer)
        advanceUntilIdle()

        assertEquals(2, gateway.callCount)
        assertEquals(1, viewModel.uiState.value.answers.size)
        assertFalse(viewModel.uiState.value.isGenerating)
    }

    @Test
    fun `failed open clears previous session and ignores complete`() = runTest {
        val resume = sampleResume()
        val sessionRepository = FakeSessionRepository(resume)
        val viewModel = createViewModel(
            resume,
            sessionRepository,
            FakeSpeechRecognizer(emitFinals = false),
            FakeLlmGateway(),
            TestDispatchers(testScheduler),
        )

        viewModel.onEvent(
            InterviewSessionUiEvent.StartSession(resume.id, "Interview", AnswerTriggerMode.MANUAL),
        )
        advanceUntilIdle()
        val sessionId = viewModel.uiState.value.session!!.id

        viewModel.onEvent(InterviewSessionUiEvent.OpenSession("missing-session"))
        advanceUntilIdle()

        assertEquals(null, viewModel.uiState.value.session)
        assertFalse(viewModel.uiState.value.isLoadingSession)
        assertEquals("Interview session not found", viewModel.uiState.value.errorMessage)

        viewModel.onEvent(InterviewSessionUiEvent.CompleteSession)
        advanceUntilIdle()

        val status = sessionRepository.sessions.value.first { it.id == sessionId }.status
        assertEquals(InterviewSessionStatus.PAUSED, status)
    }

    private fun createViewModel(
        resume: Resume,
        sessions: InterviewSessionRepository,
        speechRecognizer: SpeechRecognizer,
        gateway: OpenAiStreamGateway,
        dispatcher: CoroutineDispatcherProvider,
    ): InterviewSessionViewModel {
        return InterviewSessionViewModel(
            resumes = FakeResumeRepository(resume),
            sessions = sessions,
            speechRecognizer = speechRecognizer,
            answerGenerator = InterviewAnswerGenerator(
                providers = FakeProviderRepository(),
                gateway = gateway,
                promptBuilder = InterviewPromptBuilder(),
            ),
            dispatcherProvider = dispatcher,
        )
    }

    private fun sampleResume(): Resume = Resume(
        id = "resume",
        displayName = "Candidate",
        originalFileName = "resume.pdf",
        storedPath = "/resume.pdf",
        mimeType = "application/pdf",
        ocrStatus = OcrStatus.READY,
        ocrText = "Kotlin engineer",
        ocrError = null,
        createdAt = 1,
        updatedAt = 1,
    )

    private class TestDispatchers(scheduler: TestCoroutineScheduler) : CoroutineDispatcherProvider {
        private val dispatcher = StandardTestDispatcher(scheduler)
        override val main: CoroutineDispatcher = dispatcher
        override val io: CoroutineDispatcher = dispatcher
        override val default: CoroutineDispatcher = dispatcher
    }

    private class FakeSpeechRecognizer(
        private val emitFinals: Boolean = true,
    ) : SpeechRecognizer {
        override fun recognize(): Flow<SpeechRecognitionEvent> = flow {
            if (emitFinals) {
                emit(SpeechRecognitionEvent.Final("请介绍项目"))
                emit(SpeechRecognitionEvent.Final("请介绍项目"))
            }
        }

        override suspend fun stop() = Unit
    }

    private class FakeLlmGateway : OpenAiStreamGateway {
        var callCount = 0

        override fun stream(
            configuration: LlmConfiguration,
            apiKey: String,
            messages: List<LlmMessage>,
        ): Flow<LlmChunk> {
            callCount += 1
            return flowOf(LlmChunk(content = "建议"))
        }
    }

    private class SlowLlmGateway(
        private val delayMillis: Long,
    ) : OpenAiStreamGateway {
        var callCount = 0

        override fun stream(
            configuration: LlmConfiguration,
            apiKey: String,
            messages: List<LlmMessage>,
        ): Flow<LlmChunk> {
            callCount += 1
            return flow {
                delay(delayMillis)
                emit(LlmChunk(content = "建议"))
            }
        }
    }

    private class FakeProviderRepository : ProviderConfigurationRepository {
        override val configuration: StateFlow<ProviderConfiguration> = MutableStateFlow(ProviderConfiguration())
        override fun save(configuration: ProviderConfiguration, secrets: ProviderSecretUpdate) = Unit
        override fun secretStatus(): ProviderSecretStatus = ProviderSecretStatus(true, true, true)
        override fun secrets(): ProviderSecrets = ProviderSecrets(
            paddleToken = "token",
            xunfeiAppId = "app",
            xunfeiApiKey = "key",
            xunfeiApiSecret = "secret",
            llmApiKey = "key",
        )
        override fun paddleToken(): String = "token"
        override fun xunfeiCredentials(): XunfeiCredentials = XunfeiCredentials("app", "key", "secret")
        override fun llmApiKey(): String = "key"
        override fun clearSecrets() = Unit
        override fun reset() = Unit
    }

    private class FakeResumeRepository(resume: Resume) : ResumeRepository {
        override val resumes = MutableStateFlow(listOf(resume))
        override suspend fun refresh() = Unit
        override suspend fun import(
            displayName: String,
            originalFileName: String,
            mimeType: String,
            content: ByteArray,
        ): Resume = error("Not used")

        override suspend fun get(id: String): Resume? = resumes.value.firstOrNull { it.id == id }
        override suspend fun save(resume: Resume) = Unit
        override suspend fun delete(id: String) = Unit
        override suspend fun saveOcrJob(job: OcrJob) = Unit
        override suspend fun getOcrJob(resumeId: String): OcrJob? = null
        override suspend fun recoverableOcrJobs(): List<OcrJob> = emptyList()
    }

    private class FakeSessionRepository(
        private val resume: Resume,
    ) : InterviewSessionRepository {
        override val sessions = MutableStateFlow<List<InterviewSession>>(emptyList())
        val transcripts = mutableListOf<TranscriptSegment>()
        private val answers = mutableListOf<AssistantAnswer>()

        override suspend fun initialize() = Unit

        override suspend fun saveSession(session: InterviewSession) {
            sessions.value = sessions.value.filterNot { it.id == session.id } + session
        }

        override suspend fun detail(sessionId: String): InterviewSessionDetail? {
            val session = sessions.value.firstOrNull { it.id == sessionId } ?: return null
            return InterviewSessionDetail(session, resume, transcripts.toList(), answers.toList())
        }

        override suspend fun saveTranscript(segment: TranscriptSegment) {
            transcripts.removeAll { it.id == segment.id }
            transcripts += segment
        }

        override suspend fun saveAnswer(answer: AssistantAnswer) {
            answers.removeAll { it.id == answer.id }
            answers += answer
        }

        override suspend fun deleteSession(sessionId: String) = Unit
        override suspend fun clearAll() = Unit
    }
}
