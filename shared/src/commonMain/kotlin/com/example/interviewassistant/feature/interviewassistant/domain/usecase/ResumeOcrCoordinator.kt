package com.example.interviewassistant.feature.interviewassistant.domain.usecase

import com.example.interviewassistant.core.error.AppError
import com.example.interviewassistant.core.error.AppResult
import com.example.interviewassistant.core.file.AppFileStore
import com.example.interviewassistant.core.util.TimeProvider
import com.example.interviewassistant.feature.interviewassistant.data.remote.ocr.PaddleOcrGateway
import com.example.interviewassistant.feature.interviewassistant.data.remote.ocr.RemoteOcrJob
import com.example.interviewassistant.feature.interviewassistant.domain.model.OcrJob
import com.example.interviewassistant.feature.interviewassistant.domain.model.OcrStatus
import com.example.interviewassistant.feature.interviewassistant.domain.model.Resume
import com.example.interviewassistant.feature.interviewassistant.domain.repository.ProviderConfigurationRepository
import com.example.interviewassistant.feature.interviewassistant.domain.repository.ResumeRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Imports resumes and drives the recoverable PaddleOCR job lifecycle.
 */
class ResumeOcrCoordinator(
    private val resumes: ResumeRepository,
    private val files: AppFileStore,
    private val providers: ProviderConfigurationRepository,
    private val gateway: PaddleOcrGateway,
    private val pollIntervalMillis: Long = DEFAULT_POLL_INTERVAL_MILLIS,
    private val maxPollAttempts: Int = DEFAULT_MAX_POLL_ATTEMPTS,
) {
    private val processLocksGuard = Mutex()
    private val processLocks = mutableMapOf<String, Mutex>()

    /**
     * Copies a user-selected document and processes it to completion.
     */
    suspend fun importAndProcess(
        displayName: String,
        originalFileName: String,
        mimeType: String,
        content: ByteArray,
    ): AppResult<Resume> {
        if (!isSupportedDocument(originalFileName, mimeType)) {
            return AppResult.Error(AppError.InvalidData("Only PDF, JPG, and PNG resumes are supported"))
        }
        val imported = runCatching {
            resumes.import(displayName, originalFileName, mimeType, content)
        }.getOrElse { return AppResult.Error(AppError.Unknown(it)) }
        return process(imported.id)
    }

    /**
     * Continues a queued or interrupted OCR job.
     *
     * Concurrent calls for the same [resumeId] share a single in-flight pipeline; waiters
     * receive the latest resume state without re-submitting to PaddleOCR.
     */
    suspend fun process(resumeId: String): AppResult<Resume> {
        val mutex = mutexFor(resumeId)
        if (!mutex.tryLock()) {
            mutex.withLock {
                return resultForExisting(resumeId)
            }
        }
        return try {
            val existing = resumes.get(resumeId)
            if (existing?.ocrStatus == OcrStatus.READY && !existing.ocrText.isNullOrBlank()) {
                AppResult.Success(existing)
            } else {
                processLocked(resumeId)
            }
        } finally {
            mutex.unlock()
        }
    }

    /**
     * Resumes every persisted OCR job after application startup.
     */
    suspend fun recoverPendingJobs() {
        resumes.recoverableOcrJobs().forEach { process(it.resumeId) }
    }

    private suspend fun processLocked(resumeId: String): AppResult<Resume> {
        val token = providers.paddleToken()
            ?: return fail(resumeId, "PaddleOCR token is not configured") { AppError.Configuration(it) }
        val resume = resumes.get(resumeId)
            ?: return AppResult.Error(AppError.InvalidData("Resume not found"))
        val configuration = providers.configuration.value.paddle

        // Surface in-progress state immediately so the UI can show a spinner before the network call.
        updateResume(resume, OcrStatus.QUEUED)

        return try {
            val existing = resumes.getOcrJob(resumeId)
            val recoverableJobId = existing
                ?.takeIf { it.state in RECOVERABLE_STATUSES }
                ?.providerJobId
            val jobId = recoverableJobId ?: gateway.submit(
                endpoint = configuration.endpoint,
                token = token,
                model = configuration.model,
                fileName = resume.originalFileName,
                mimeType = resume.mimeType,
                content = files.read(resume.storedPath),
            )
            saveJob(resumeId, jobId, OcrStatus.PENDING)
            updateResume(resume, OcrStatus.PENDING)
            pollUntilComplete(resume, jobId, token, configuration.endpoint)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: AppError) {
            fail(resumeId, error.message ?: "OCR request failed") { error }
        } catch (error: Throwable) {
            fail(resumeId, error.message ?: "OCR request failed") { AppError.Unknown(error) }
        }
    }

    private suspend fun resultForExisting(resumeId: String): AppResult<Resume> {
        val resume = resumes.get(resumeId)
            ?: return AppResult.Error(AppError.InvalidData("Resume not found"))
        return when (resume.ocrStatus) {
            OcrStatus.READY -> {
                if (resume.ocrText.isNullOrBlank()) {
                    AppResult.Error(AppError.InvalidData("OCR completed without resume text"))
                } else {
                    AppResult.Success(resume)
                }
            }
            OcrStatus.FAILED -> {
                AppResult.Error(AppError.InvalidData(resume.ocrError ?: "OCR request failed"))
            }
            OcrStatus.QUEUED, OcrStatus.PENDING, OcrStatus.RUNNING -> {
                AppResult.Error(AppError.InvalidData("OCR did not finish"))
            }
        }
    }

    private suspend fun mutexFor(resumeId: String): Mutex {
        return processLocksGuard.withLock {
            processLocks.getOrPut(resumeId) { Mutex() }
        }
    }

    private suspend fun pollUntilComplete(
        resume: Resume,
        jobId: String,
        token: String,
        endpoint: String,
    ): AppResult<Resume> {
        repeat(maxPollAttempts) { attempt ->
            val remote = gateway.query(endpoint, token, jobId)
            val status = remote.toStatus()
            saveJob(remote, resume.id)
            when (status) {
                OcrStatus.READY -> {
                    val resultUrl = remote.resultUrl
                        ?: return fail(resume.id, "PaddleOCR completed without a result URL") {
                            AppError.InvalidData(it)
                        }
                    val document = gateway.downloadDocument(resultUrl)
                    val text = document.text.trim()
                    if (text.isEmpty()) {
                        return fail(resume.id, "PaddleOCR returned empty resume text") {
                            AppError.InvalidData(it)
                        }
                    }
                    cacheOcrImages(resume.id, document.images)
                    val completed = updateResume(resume, OcrStatus.READY, text = text)
                    return AppResult.Success(completed)
                }
                OcrStatus.FAILED -> {
                    return fail(
                        resume.id,
                        remote.errorMessage ?: "PaddleOCR job failed",
                        { AppError.InvalidData(it) },
                    )
                }
                OcrStatus.PENDING, OcrStatus.RUNNING, OcrStatus.QUEUED -> {
                    updateResume(resume, status)
                    if (attempt < maxPollAttempts - 1) delay(pollIntervalMillis)
                }
            }
        }
        return fail(resume.id, "PaddleOCR polling timed out") { AppError.Network }
    }

    private suspend fun cacheOcrImages(resumeId: String, images: Map<String, String>) {
        files.deleteOcrAssets(resumeId)
        images.forEach { (relativePath, url) ->
            runCatching {
                val bytes = gateway.downloadBytes(url)
                files.saveOcrAsset(resumeId, relativePath, bytes)
            }
        }
    }

    private suspend fun updateResume(
        original: Resume,
        status: OcrStatus,
        text: String? = original.ocrText,
        error: String? = null,
    ): Resume {
        val updated = original.copy(
            ocrStatus = status,
            ocrText = text,
            ocrError = error,
            updatedAt = TimeProvider.currentTimeMillis(),
        )
        resumes.save(updated)
        return updated
    }

    private suspend fun saveJob(
        resumeId: String,
        jobId: String,
        status: OcrStatus,
    ) {
        resumes.saveOcrJob(
            OcrJob(
                resumeId = resumeId,
                providerJobId = jobId,
                state = status,
                progressCurrent = 0,
                progressTotal = 0,
                resultUrl = null,
                errorMessage = null,
                updatedAt = TimeProvider.currentTimeMillis(),
            ),
        )
    }

    private suspend fun saveJob(remote: RemoteOcrJob, resumeId: String) {
        resumes.saveOcrJob(
            OcrJob(
                resumeId = resumeId,
                providerJobId = remote.jobId,
                state = remote.toStatus(),
                progressCurrent = remote.progressCurrent,
                progressTotal = remote.progressTotal,
                resultUrl = remote.resultUrl,
                errorMessage = remote.errorMessage,
                updatedAt = TimeProvider.currentTimeMillis(),
            ),
        )
    }

    private suspend fun fail(
        resumeId: String,
        message: String,
        errorFactory: (String) -> AppError,
    ): AppResult<Resume> {
        val resume = resumes.get(resumeId)
        if (resume != null) {
            updateResume(resume, OcrStatus.FAILED, error = message)
            val existing = resumes.getOcrJob(resumeId)
            resumes.saveOcrJob(
                OcrJob(
                    resumeId = resumeId,
                    providerJobId = existing?.providerJobId,
                    state = OcrStatus.FAILED,
                    progressCurrent = existing?.progressCurrent ?: 0,
                    progressTotal = existing?.progressTotal ?: 0,
                    resultUrl = existing?.resultUrl,
                    errorMessage = message,
                    updatedAt = TimeProvider.currentTimeMillis(),
                ),
            )
        }
        return AppResult.Error(errorFactory(message))
    }

    private fun RemoteOcrJob.toStatus(): OcrStatus {
        return when (state.lowercase()) {
            "pending" -> OcrStatus.PENDING
            "running" -> OcrStatus.RUNNING
            "done" -> OcrStatus.READY
            "failed" -> OcrStatus.FAILED
            else -> OcrStatus.PENDING
        }
    }

    private fun isSupportedDocument(fileName: String, mimeType: String): Boolean {
        val extension = fileName.substringAfterLast('.', missingDelimiterValue = "").lowercase()
        return mimeType.lowercase() in SUPPORTED_MIME_TYPES || extension in SUPPORTED_EXTENSIONS
    }

    private companion object {
        const val DEFAULT_POLL_INTERVAL_MILLIS = 5_000L
        const val DEFAULT_MAX_POLL_ATTEMPTS = 120
        val SUPPORTED_MIME_TYPES = setOf("application/pdf", "image/jpeg", "image/png")
        val SUPPORTED_EXTENSIONS = setOf("pdf", "jpg", "jpeg", "png")
        val RECOVERABLE_STATUSES = setOf(OcrStatus.QUEUED, OcrStatus.PENDING, OcrStatus.RUNNING)
    }
}
