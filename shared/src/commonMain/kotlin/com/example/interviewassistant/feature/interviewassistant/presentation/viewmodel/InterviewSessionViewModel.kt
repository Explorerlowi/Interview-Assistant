package com.example.interviewassistant.feature.interviewassistant.presentation.viewmodel

import com.example.interviewassistant.core.util.CoroutineDispatcherProvider
import com.example.interviewassistant.core.util.TimeProvider
import com.example.interviewassistant.feature.interviewassistant.data.local.newId
import com.example.interviewassistant.feature.interviewassistant.data.remote.speech.SpeechRecognitionEvent
import com.example.interviewassistant.feature.interviewassistant.domain.model.AnswerTriggerMode
import com.example.interviewassistant.feature.interviewassistant.domain.model.AssistantAnswer
import com.example.interviewassistant.feature.interviewassistant.domain.model.AssistantAnswerStatus
import com.example.interviewassistant.feature.interviewassistant.domain.model.InterviewSession
import com.example.interviewassistant.feature.interviewassistant.domain.model.InterviewSessionStatus
import com.example.interviewassistant.feature.interviewassistant.domain.model.TranscriptSegment
import com.example.interviewassistant.feature.interviewassistant.domain.repository.InterviewSessionRepository
import com.example.interviewassistant.feature.interviewassistant.domain.repository.ResumeRepository
import com.example.interviewassistant.feature.interviewassistant.domain.repository.SpeechRecognizer
import com.example.interviewassistant.feature.interviewassistant.domain.usecase.InterviewAnswerContext
import com.example.interviewassistant.feature.interviewassistant.domain.usecase.InterviewAnswerGenerator
import com.example.interviewassistant.feature.interviewassistant.presentation.state.InterviewSessionUiEffect
import com.example.interviewassistant.feature.interviewassistant.presentation.state.InterviewSessionUiEvent
import com.example.interviewassistant.feature.interviewassistant.presentation.state.InterviewSessionUiState
import com.example.interviewassistant.presentation.viewmodel.BaseViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Owns the live interview state machine, recognition and streamed answer lifecycle.
 */
class InterviewSessionViewModel(
    private val resumes: ResumeRepository,
    private val sessions: InterviewSessionRepository,
    private val speechRecognizer: SpeechRecognizer,
    private val answerGenerator: InterviewAnswerGenerator,
    dispatcherProvider: CoroutineDispatcherProvider,
) : BaseViewModel<InterviewSessionUiState, InterviewSessionUiEvent, InterviewSessionUiEffect>(
    InterviewSessionUiState(),
    dispatcherProvider,
) {
    private val mutableUiState = MutableStateFlow(InterviewSessionUiState())
    private val mutableEffect = MutableSharedFlow<InterviewSessionUiEffect>()
    private val workMutex = Mutex()
    private var recognitionJob: Job? = null
    private var generationJob: Job? = null
    private var activeAnswer: AssistantAnswer? = null
    private var boundSessionId: String? = null
    private var lastSubmittedQuestion: String? = null
    private var lastFinalTranscript: String? = null

    override val uiState: StateFlow<InterviewSessionUiState> = mutableUiState.asStateFlow()
    override val effect: SharedFlow<InterviewSessionUiEffect> = mutableEffect.asSharedFlow()

    override fun onEvent(event: InterviewSessionUiEvent) {
        when (event) {
            is InterviewSessionUiEvent.StartSession -> startSession(event)
            is InterviewSessionUiEvent.OpenSession -> openSession(event.sessionId)
            InterviewSessionUiEvent.StartListening -> startListening()
            InterviewSessionUiEvent.StopListening -> stopListening()
            is InterviewSessionUiEvent.UpdateQuestion -> {
                mutableUiState.value = mutableUiState.value.copy(currentQuestion = event.text)
            }
            InterviewSessionUiEvent.GenerateAnswer -> {
                generateAnswer(mutableUiState.value.currentQuestion, allowDuplicate = true)
            }
            InterviewSessionUiEvent.CancelGeneration -> cancelGeneration()
            is InterviewSessionUiEvent.SetTriggerMode -> setTriggerMode(event.mode)
            InterviewSessionUiEvent.CompleteSession -> completeSession()
            InterviewSessionUiEvent.LeaveWorkspace -> leaveWorkspace()
            InterviewSessionUiEvent.ClearError -> {
                mutableUiState.value = mutableUiState.value.copy(errorMessage = null)
            }
        }
    }

    private fun startSession(event: InterviewSessionUiEvent.StartSession) {
        viewModelScope.launch(dispatcherProvider.io) {
            mutableUiState.value = mutableUiState.value.copy(isLoadingSession = true, errorMessage = null)
            teardownActiveWork(pauseSession = true)
            // Unbind immediately so Complete/SetTriggerMode cannot mutate the previous session.
            boundSessionId = null
            val resume = resumes.get(event.resumeId)
            if (resume == null) {
                clearWorkspaceState("Resume not found")
                return@launch
            }
            val now = TimeProvider.currentTimeMillis()
            val session = InterviewSession(
                id = newId("session"),
                resumeId = resume.id,
                title = event.title.trim().ifBlank { resume.displayName },
                status = InterviewSessionStatus.IDLE,
                triggerMode = event.triggerMode,
                createdAt = now,
                updatedAt = now,
            )
            sessions.saveSession(session)
            lastSubmittedQuestion = null
            lastFinalTranscript = null
            boundSessionId = session.id
            mutableUiState.value = InterviewSessionUiState(
                session = session,
                resume = resume,
                triggerMode = event.triggerMode,
                isLoadingSession = false,
            )
        }
    }

    private fun openSession(sessionId: String) {
        viewModelScope.launch(dispatcherProvider.io) {
            mutableUiState.value = mutableUiState.value.copy(isLoadingSession = true, errorMessage = null)
            teardownActiveWork(pauseSession = true)
            boundSessionId = null
            val detail = sessions.detail(sessionId)
            if (detail == null) {
                clearWorkspaceState("Interview session not found")
                return@launch
            }
            lastSubmittedQuestion = detail.answers.lastOrNull()?.question?.normalizedQuestion()
            lastFinalTranscript = detail.transcripts.lastOrNull()?.text?.normalizedQuestion()
            boundSessionId = detail.session.id
            val missingResumeMessage = if (detail.resume == null) {
                "Resume for this session is no longer available"
            } else {
                null
            }
            mutableUiState.value = InterviewSessionUiState(
                session = detail.session,
                resume = detail.resume,
                transcripts = detail.transcripts,
                answers = detail.answers,
                triggerMode = detail.session.triggerMode,
                isLoadingSession = false,
                errorMessage = missingResumeMessage,
            )
        }
    }

    private fun startListening() {
        val state = mutableUiState.value
        if (state.isLoadingSession || recognitionJob?.isActive == true) return
        val session = state.session ?: run {
            setError("Start or open an interview session first")
            return
        }
        if (state.resume == null) {
            setError("Resume for this session is no longer available")
            return
        }
        recognitionJob = viewModelScope.launch {
            updateSessionStatus(session, InterviewSessionStatus.LISTENING)
            if (!isBoundTo(session.id)) return@launch
            mutableUiState.value = mutableUiState.value.copy(
                isListening = true,
                errorMessage = null,
            )
            try {
                speechRecognizer.recognize().collect(::handleSpeechEvent)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Throwable) {
                if (isBoundTo(session.id)) {
                    setError(error.message ?: "Speech recognition failed")
                }
            } finally {
                if (isBoundTo(session.id)) {
                    mutableUiState.value = mutableUiState.value.copy(isListening = false)
                }
            }
        }
    }

    private fun stopListening() {
        viewModelScope.launch(dispatcherProvider.io) {
            workMutex.withLock {
                val recognition = recognitionJob
                recognitionJob = null
                recognition?.cancelAndJoin()
                speechRecognizer.stop()
                val session = mutableUiState.value.session
                if (session != null && isBoundTo(session.id)) {
                    updateSessionStatus(session, InterviewSessionStatus.PAUSED)
                    mutableUiState.value = mutableUiState.value.copy(isListening = false)
                }
            }
        }
    }

    private suspend fun handleSpeechEvent(event: SpeechRecognitionEvent) {
        val sessionId = boundSessionId ?: return
        when (event) {
            is SpeechRecognitionEvent.Partial -> {
                if (!isBoundTo(sessionId)) return
                mutableUiState.value = mutableUiState.value.copy(
                    liveTranscript = event.text,
                    currentQuestion = event.text,
                )
            }
            is SpeechRecognitionEvent.Final -> {
                val text = event.text.trim()
                if (text.isEmpty() || !isBoundTo(sessionId)) return
                val normalized = text.normalizedQuestion()
                if (normalized == lastFinalTranscript) return
                lastFinalTranscript = normalized
                persistTranscript(text)
                if (!isBoundTo(sessionId)) return
                mutableUiState.value = mutableUiState.value.copy(
                    liveTranscript = "",
                    currentQuestion = text,
                )
                mutableUiState.value.session?.let {
                    updateSessionStatus(it, InterviewSessionStatus.QUESTION_READY)
                }
                if (mutableUiState.value.triggerMode == AnswerTriggerMode.AUTOMATIC) {
                    generateAnswer(text, allowDuplicate = false)
                }
            }
            is SpeechRecognitionEvent.Failure -> {
                if (!isBoundTo(sessionId)) return
                mutableUiState.value = mutableUiState.value.copy(
                    isListening = false,
                    errorMessage = event.message,
                )
                mutableUiState.value.session?.let {
                    updateSessionStatus(it, InterviewSessionStatus.ERROR)
                }
            }
            SpeechRecognitionEvent.SessionRotated -> Unit
        }
    }

    private suspend fun persistTranscript(text: String) {
        val state = mutableUiState.value
        val session = state.session ?: return
        if (!isBoundTo(session.id)) return
        val segment = TranscriptSegment(
            id = newId("transcript"),
            sessionId = session.id,
            sequence = (state.transcripts.maxOfOrNull(TranscriptSegment::sequence) ?: 0) + 1,
            text = text,
            isFinal = true,
            createdAt = TimeProvider.currentTimeMillis(),
        )
        sessions.saveTranscript(segment)
        if (!isBoundTo(session.id)) return
        mutableUiState.value = mutableUiState.value.copy(
            transcripts = mutableUiState.value.transcripts + segment,
        )
    }

    private fun generateAnswer(question: String, allowDuplicate: Boolean) {
        val normalized = question.normalizedQuestion()
        if (normalized.length < MINIMUM_QUESTION_LENGTH) return
        if (!allowDuplicate && normalized == lastSubmittedQuestion) return
        val state = mutableUiState.value
        if (state.isLoadingSession) return
        val session = state.session ?: return
        val resume = state.resume ?: run {
            setError("Resume for this session is no longer available")
            return
        }
        lastSubmittedQuestion = normalized

        viewModelScope.launch(dispatcherProvider.io) {
            workMutex.withLock {
                val previous = generationJob
                generationJob = null
                previous?.cancelAndJoin()
            }
            if (!isBoundTo(session.id)) return@launch

            val job = viewModelScope.launch {
                val now = TimeProvider.currentTimeMillis()
                var answer = AssistantAnswer(
                    id = newId("answer"),
                    sessionId = session.id,
                    question = question.trim(),
                    content = "",
                    model = answerGenerator.model,
                    status = AssistantAnswerStatus.GENERATING,
                    createdAt = now,
                    updatedAt = now,
                )
                val answerId = answer.id
                activeAnswer = answer
                sessions.saveAnswer(answer)
                updateSessionStatus(session, InterviewSessionStatus.GENERATING)
                if (isBoundTo(session.id) && activeAnswer?.id == answerId) {
                    mutableUiState.value = mutableUiState.value.copy(
                        isGenerating = true,
                        streamingAnswer = "",
                        errorMessage = null,
                    )
                }
                try {
                    answerGenerator.generate(
                        InterviewAnswerContext(
                            resumeText = resume.ocrText.orEmpty(),
                            question = question,
                            recentTranscript = state.transcripts.joinToString(separator = "\n") { it.text },
                            recentAnswers = state.answers.takeLast(3).map(AssistantAnswer::content),
                        ),
                    ).collect { chunk ->
                        if (chunk.content.isNotEmpty()) {
                            answer = answer.copy(
                                content = answer.content + chunk.content,
                                updatedAt = TimeProvider.currentTimeMillis(),
                            )
                            if (activeAnswer?.id == answerId) {
                                activeAnswer = answer
                            }
                            if (isBoundTo(session.id) && activeAnswer?.id == answerId) {
                                mutableUiState.value = mutableUiState.value.copy(
                                    streamingAnswer = answer.content,
                                )
                            }
                        }
                    }
                    answer = answer.copy(
                        status = AssistantAnswerStatus.COMPLETED,
                        updatedAt = TimeProvider.currentTimeMillis(),
                    )
                    sessions.saveAnswer(answer)
                    if (isBoundTo(session.id) && activeAnswer?.id == answerId) {
                        mutableUiState.value = mutableUiState.value.copy(
                            answers = mutableUiState.value.answers + answer,
                            streamingAnswer = answer.content,
                        )
                        updateSessionStatus(session, InterviewSessionStatus.QUESTION_READY)
                    }
                } catch (cancelled: CancellationException) {
                    answer = answer.copy(
                        status = AssistantAnswerStatus.INTERRUPTED,
                        updatedAt = TimeProvider.currentTimeMillis(),
                    )
                    sessions.saveAnswer(answer)
                    if (isBoundTo(session.id)) {
                        updateSessionStatus(session, InterviewSessionStatus.QUESTION_READY)
                    }
                    throw cancelled
                } catch (error: Throwable) {
                    answer = answer.copy(
                        status = AssistantAnswerStatus.FAILED,
                        updatedAt = TimeProvider.currentTimeMillis(),
                    )
                    sessions.saveAnswer(answer)
                    if (isBoundTo(session.id) && activeAnswer?.id == answerId) {
                        updateSessionStatus(session, InterviewSessionStatus.ERROR)
                        setError(error.message ?: "Answer generation failed")
                    }
                } finally {
                    if (activeAnswer?.id == answerId) {
                        activeAnswer = null
                        if (isBoundTo(session.id)) {
                            mutableUiState.value = mutableUiState.value.copy(isGenerating = false)
                        }
                    }
                }
            }
            workMutex.withLock {
                generationJob = job
            }
            job.join()
            workMutex.withLock {
                if (generationJob === job) {
                    generationJob = null
                }
            }
        }
    }

    private fun cancelGeneration() {
        viewModelScope.launch(dispatcherProvider.io) {
            workMutex.withLock {
                val job = generationJob
                generationJob = null
                job?.cancelAndJoin()
                if (mutableUiState.value.isGenerating) {
                    mutableUiState.value = mutableUiState.value.copy(isGenerating = false)
                }
            }
        }
    }

    private fun setTriggerMode(mode: AnswerTriggerMode) {
        val state = mutableUiState.value
        if (state.isLoadingSession) return
        val session = state.session ?: return
        if (!isBoundTo(session.id)) return
        mutableUiState.value = state.copy(triggerMode = mode)
        viewModelScope.launch(dispatcherProvider.io) {
            val updated = session.copy(
                triggerMode = mode,
                updatedAt = TimeProvider.currentTimeMillis(),
            )
            sessions.saveSession(updated)
            if (isBoundTo(session.id)) {
                mutableUiState.value = mutableUiState.value.copy(session = updated, triggerMode = mode)
            }
        }
    }

    private fun leaveWorkspace() {
        viewModelScope.launch(dispatcherProvider.io) {
            teardownActiveWork(pauseSession = true)
        }
    }

    private fun completeSession() {
        val state = mutableUiState.value
        if (state.isLoadingSession) return
        val session = state.session ?: return
        if (!isBoundTo(session.id)) return
        viewModelScope.launch(dispatcherProvider.io) {
            if (!isBoundTo(session.id)) return@launch
            teardownActiveWork(pauseSession = false)
            if (!isBoundTo(session.id)) return@launch
            updateSessionStatus(session, InterviewSessionStatus.COMPLETED)
            boundSessionId = null
            mutableEffect.emit(InterviewSessionUiEffect.SessionCompleted)
        }
    }

    private fun clearWorkspaceState(errorMessage: String) {
        boundSessionId = null
        lastSubmittedQuestion = null
        lastFinalTranscript = null
        mutableUiState.value = InterviewSessionUiState(
            isLoadingSession = false,
            errorMessage = errorMessage,
        )
    }

    /**
     * Cancels recognition and generation, stops the speech recognizer, and optionally pauses
     * the currently bound session.
     */
    private suspend fun teardownActiveWork(pauseSession: Boolean) {
        workMutex.withLock {
            val recognition = recognitionJob
            val generation = generationJob
            recognitionJob = null
            generationJob = null
            recognition?.cancelAndJoin()
            generation?.cancelAndJoin()
            speechRecognizer.stop()
            val session = mutableUiState.value.session
            mutableUiState.value = mutableUiState.value.copy(
                isListening = false,
                isGenerating = false,
                streamingAnswer = if (pauseSession) mutableUiState.value.streamingAnswer else "",
            )
            if (pauseSession && session != null && isBoundTo(session.id)) {
                updateSessionStatus(session, InterviewSessionStatus.PAUSED)
            }
        }
    }

    private suspend fun updateSessionStatus(
        session: InterviewSession,
        status: InterviewSessionStatus,
    ) {
        val updated = session.copy(status = status, updatedAt = TimeProvider.currentTimeMillis())
        sessions.saveSession(updated)
        if (isBoundTo(session.id)) {
            mutableUiState.value = mutableUiState.value.copy(session = updated)
        }
    }

    private fun isBoundTo(sessionId: String): Boolean = boundSessionId == sessionId

    private fun setError(message: String) {
        mutableUiState.value = mutableUiState.value.copy(errorMessage = message)
    }

    override fun onCleared() {
        recognitionJob?.cancel()
        generationJob?.cancel()
        super.onCleared()
    }

    private fun String.normalizedQuestion(): String {
        return trim().lowercase().replace(WHITESPACE, " ")
    }

    private companion object {
        const val MINIMUM_QUESTION_LENGTH = 4
        val WHITESPACE = Regex("\\s+")
    }
}
