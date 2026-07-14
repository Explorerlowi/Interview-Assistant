package com.example.interviewassistant.feature.interviewassistant

import com.example.interviewassistant.core.error.AppResult
import com.example.interviewassistant.core.file.AppFileStore
import com.example.interviewassistant.core.file.StoredFile
import com.example.interviewassistant.feature.interviewassistant.data.remote.ocr.OcrDocumentResult
import com.example.interviewassistant.feature.interviewassistant.data.remote.ocr.PaddleOcrGateway
import com.example.interviewassistant.feature.interviewassistant.data.remote.ocr.RemoteOcrJob
import com.example.interviewassistant.feature.interviewassistant.domain.model.OcrJob
import com.example.interviewassistant.feature.interviewassistant.domain.model.OcrStatus
import com.example.interviewassistant.feature.interviewassistant.domain.model.ProviderConfiguration
import com.example.interviewassistant.feature.interviewassistant.domain.model.ProviderSecretStatus
import com.example.interviewassistant.feature.interviewassistant.domain.model.ProviderSecretUpdate
import com.example.interviewassistant.feature.interviewassistant.domain.model.ProviderSecrets
import com.example.interviewassistant.feature.interviewassistant.domain.model.Resume
import com.example.interviewassistant.feature.interviewassistant.domain.model.XunfeiCredentials
import com.example.interviewassistant.feature.interviewassistant.domain.repository.ProviderConfigurationRepository
import com.example.interviewassistant.feature.interviewassistant.domain.repository.ResumeRepository
import com.example.interviewassistant.feature.interviewassistant.domain.usecase.ResumeOcrCoordinator
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class ResumeOcrCoordinatorTest {
    @Test
    fun `import persists progress and completed OCR text`() = runTest {
        val resumes = FakeResumeRepository()
        val gateway = FakeGateway()
        val coordinator = ResumeOcrCoordinator(
            resumes = resumes,
            files = FakeFileStore(),
            providers = FakeProviderRepository(),
            gateway = gateway,
            pollIntervalMillis = 0,
            maxPollAttempts = 5,
        )

        val result = coordinator.importAndProcess(
            displayName = "候选人",
            originalFileName = "resume.pdf",
            mimeType = "application/pdf",
            content = byteArrayOf(1, 2, 3),
        )

        val success = assertIs<AppResult.Success<Resume>>(result)
        assertEquals(OcrStatus.READY, success.data.ocrStatus)
        assertEquals("parsed resume", success.data.ocrText)
        assertEquals(OcrStatus.READY, resumes.jobs.values.single().state)
        assertEquals(3, gateway.queryCount)
    }

    @Test
    fun `unsupported document is rejected before local import`() = runTest {
        val resumes = FakeResumeRepository()
        val coordinator = ResumeOcrCoordinator(
            resumes,
            FakeFileStore(),
            FakeProviderRepository(),
            FakeGateway(),
        )

        val result = coordinator.importAndProcess("x", "resume.docx", "application/docx", byteArrayOf())

        assertIs<AppResult.Error>(result)
        assertEquals(0, resumes.resumes.value.size)
    }

    @Test
    fun `concurrent process for same resume submits once`() = runTest {
        val resumes = FakeResumeRepository()
        val gateway = CountingGateway()
        val coordinator = ResumeOcrCoordinator(
            resumes = resumes,
            files = FakeFileStore(),
            providers = FakeProviderRepository(),
            gateway = gateway,
            pollIntervalMillis = 0,
            maxPollAttempts = 5,
        )
        val imported = resumes.import(
            displayName = "候选人",
            originalFileName = "resume.pdf",
            mimeType = "application/pdf",
            content = byteArrayOf(1, 2, 3),
        )

        val first = async { coordinator.process(imported.id) }
        val second = async { coordinator.process(imported.id) }
        val results = listOf(first.await(), second.await())

        assertEquals(1, gateway.submitCount)
        assertTrue(results.all { it is AppResult.Success<*> })
        assertEquals(OcrStatus.READY, resumes.get(imported.id)?.ocrStatus)
    }

    private class CountingGateway : FakeGateway() {
        var submitCount = 0

        override suspend fun submit(
            endpoint: String,
            token: String,
            model: String,
            fileName: String,
            mimeType: String,
            content: ByteArray,
        ): String {
            submitCount += 1
            delay(20)
            return "job-1"
        }
    }

    private open class FakeGateway : PaddleOcrGateway {
        var queryCount = 0

        override suspend fun submit(
            endpoint: String,
            token: String,
            model: String,
            fileName: String,
            mimeType: String,
            content: ByteArray,
        ): String = "job-1"

        override suspend fun query(endpoint: String, token: String, jobId: String): RemoteOcrJob {
            queryCount += 1
            val state = when (queryCount) {
                1 -> "pending"
                2 -> "running"
                else -> "done"
            }
            return RemoteOcrJob(
                jobId,
                state,
                queryCount.toLong(),
                3,
                if (state == "done") "https://result" else null,
                null,
            )
        }

        override suspend fun downloadDocument(resultUrl: String): OcrDocumentResult =
            OcrDocumentResult(text = "parsed resume", images = emptyMap())

        override suspend fun downloadBytes(url: String): ByteArray = byteArrayOf(9, 9, 9)
    }

    private class FakeResumeRepository : ResumeRepository {
        override val resumes = MutableStateFlow<List<Resume>>(emptyList())
        val jobs = mutableMapOf<String, OcrJob>()

        override suspend fun refresh() = Unit

        override suspend fun import(
            displayName: String,
            originalFileName: String,
            mimeType: String,
            content: ByteArray,
        ): Resume {
            val resume = Resume(
                "resume-1",
                displayName,
                originalFileName,
                "/private/$originalFileName",
                mimeType,
                OcrStatus.QUEUED,
                null,
                null,
                1,
                1,
            )
            resumes.value = listOf(resume)
            return resume
        }

        override suspend fun get(id: String): Resume? = resumes.value.firstOrNull { it.id == id }

        override suspend fun save(resume: Resume) {
            resumes.value = resumes.value.filterNot { it.id == resume.id } + resume
        }

        override suspend fun delete(id: String) {
            resumes.value = resumes.value.filterNot { it.id == id }
        }

        override suspend fun saveOcrJob(job: OcrJob) {
            jobs[job.resumeId] = job
        }

        override suspend fun getOcrJob(resumeId: String): OcrJob? = jobs[resumeId]

        override suspend fun recoverableOcrJobs(): List<OcrJob> = jobs.values.filter {
            it.state in setOf(OcrStatus.QUEUED, OcrStatus.PENDING, OcrStatus.RUNNING)
        }
    }

    private class FakeFileStore : AppFileStore {
        val assets = mutableMapOf<String, ByteArray>()

        override suspend fun importResume(
            originalName: String,
            mimeType: String,
            content: ByteArray,
        ): StoredFile = StoredFile(originalName, "/private/$originalName", mimeType, content.size.toLong())

        override suspend fun read(path: String): ByteArray = byteArrayOf(1, 2, 3)
        override suspend fun delete(path: String) = Unit
        override suspend fun exists(path: String): Boolean = true

        override suspend fun saveOcrAsset(
            resumeId: String,
            relativePath: String,
            content: ByteArray,
        ): String {
            val path = "/ocr/$resumeId/$relativePath"
            assets[path] = content
            return path
        }

        override suspend fun ocrAssetBaseUri(resumeId: String): String = "file:///ocr/$resumeId/"

        override suspend fun deleteOcrAssets(resumeId: String) {
            assets.keys.filter { it.startsWith("/ocr/$resumeId/") }.forEach(assets::remove)
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
}
