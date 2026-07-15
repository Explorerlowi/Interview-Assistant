package com.example.interviewassistant.feature.interviewassistant.data.local

import com.example.interviewassistant.core.file.AppFileStore
import com.example.interviewassistant.core.util.CoroutineDispatcherProvider
import com.example.interviewassistant.core.util.TimeProvider
import com.example.interviewassistant.database.AssistantAnswerEntity
import com.example.interviewassistant.database.InterviewDatabase
import com.example.interviewassistant.database.InterviewSessionEntity
import com.example.interviewassistant.database.OcrJobEntity
import com.example.interviewassistant.database.ResumeEntity
import com.example.interviewassistant.database.TranscriptSegmentEntity
import com.example.interviewassistant.feature.interviewassistant.domain.model.AnswerTriggerMode
import com.example.interviewassistant.feature.interviewassistant.domain.model.AssistantAnswer
import com.example.interviewassistant.feature.interviewassistant.domain.model.AssistantAnswerStatus
import com.example.interviewassistant.feature.interviewassistant.domain.model.InterviewSession
import com.example.interviewassistant.feature.interviewassistant.domain.model.InterviewSessionDetail
import com.example.interviewassistant.feature.interviewassistant.domain.model.InterviewSessionStatus
import com.example.interviewassistant.feature.interviewassistant.domain.model.OcrJob
import com.example.interviewassistant.feature.interviewassistant.domain.model.OcrStatus
import com.example.interviewassistant.feature.interviewassistant.domain.model.Resume
import com.example.interviewassistant.feature.interviewassistant.domain.model.TranscriptSegment
import com.example.interviewassistant.feature.interviewassistant.domain.repository.InterviewSessionRepository
import com.example.interviewassistant.feature.interviewassistant.domain.repository.ResumeRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlin.random.Random

/**
 * SQLDelight-backed resume library with application-private source files.
 */
class SqlDelightResumeRepository(
    private val database: InterviewDatabase,
    private val fileStore: AppFileStore,
    private val dispatchers: CoroutineDispatcherProvider,
) : ResumeRepository {
    private val queries = database.interviewAssistantQueries
    private val mutableResumes = MutableStateFlow<List<Resume>>(emptyList())

    override val resumes: StateFlow<List<Resume>> = mutableResumes.asStateFlow()

    override suspend fun refresh() {
        mutableResumes.value = withContext(dispatchers.io) {
            queries.selectAllResumes().executeAsList().map(ResumeEntity::toDomain)
        }
    }

    override suspend fun import(
        displayName: String,
        originalFileName: String,
        mimeType: String,
        content: ByteArray,
    ): Resume {
        val storedFile = fileStore.importResume(originalFileName, mimeType, content)
        val now = TimeProvider.currentTimeMillis()
        val resume = Resume(
            id = newId("resume"),
            displayName = displayName.trim().ifBlank { originalFileName.substringBeforeLast('.') },
            originalFileName = originalFileName,
            storedPath = storedFile.path,
            mimeType = mimeType,
            ocrStatus = OcrStatus.QUEUED,
            ocrText = null,
            ocrError = null,
            createdAt = now,
            updatedAt = now,
            ocrOriginalText = null,
        )
        try {
            save(resume)
        } catch (error: Throwable) {
            fileStore.delete(storedFile.path)
            throw error
        }
        return resume
    }

    override suspend fun get(id: String): Resume? = withContext(dispatchers.io) {
        queries.selectResumeById(id).executeAsOneOrNull()?.toDomain()
    }

    override suspend fun save(resume: Resume) {
        withContext(dispatchers.io) {
            if (queries.selectResumeById(resume.id).executeAsOneOrNull() == null) {
                queries.insertResume(
                    id = resume.id,
                    display_name = resume.displayName,
                    original_file_name = resume.originalFileName,
                    stored_path = resume.storedPath,
                    mime_type = resume.mimeType,
                    ocr_status = resume.ocrStatus.name,
                    ocr_text = resume.ocrText,
                    ocr_original_text = resume.ocrOriginalText,
                    ocr_error = resume.ocrError,
                    created_at = resume.createdAt,
                    updated_at = resume.updatedAt,
                )
            } else {
                queries.updateResume(
                    display_name = resume.displayName,
                    original_file_name = resume.originalFileName,
                    stored_path = resume.storedPath,
                    mime_type = resume.mimeType,
                    ocr_status = resume.ocrStatus.name,
                    ocr_text = resume.ocrText,
                    ocr_original_text = resume.ocrOriginalText,
                    ocr_error = resume.ocrError,
                    created_at = resume.createdAt,
                    updated_at = resume.updatedAt,
                    id = resume.id,
                )
            }
        }
        refresh()
    }

    override suspend fun delete(id: String) {
        val resume = get(id) ?: return
        withContext(dispatchers.io) {
            val sessionIds = queries.selectSessionsByResume(id).executeAsList().map(InterviewSessionEntity::id)
            database.transaction {
                sessionIds.forEach { sessionId ->
                    queries.deleteAnswersBySession(sessionId)
                    queries.deleteTranscriptsBySession(sessionId)
                    queries.deleteSession(sessionId)
                }
                queries.deleteOcrJob(id)
                queries.deleteResume(id)
            }
        }
        fileStore.delete(resume.storedPath)
        fileStore.deleteOcrAssets(id)
        refresh()
    }

    override suspend fun saveOcrJob(job: OcrJob) {
        withContext(dispatchers.io) {
            queries.upsertOcrJob(
                resume_id = job.resumeId,
                provider_job_id = job.providerJobId,
                state = job.state.name,
                progress_current = job.progressCurrent,
                progress_total = job.progressTotal,
                result_url = job.resultUrl,
                error_message = job.errorMessage,
                updated_at = job.updatedAt,
            )
        }
    }

    override suspend fun getOcrJob(resumeId: String): OcrJob? = withContext(dispatchers.io) {
        queries.selectOcrJobByResumeId(resumeId).executeAsOneOrNull()?.toDomain()
    }

    override suspend fun recoverableOcrJobs(): List<OcrJob> = withContext(dispatchers.io) {
        queries.selectRecoverableOcrJobs().executeAsList().map(OcrJobEntity::toDomain)
    }
}

/**
 * SQLDelight-backed interview history repository.
 */
class SqlDelightInterviewSessionRepository(
    private val database: InterviewDatabase,
    private val fileStore: AppFileStore,
    private val dispatchers: CoroutineDispatcherProvider,
) : InterviewSessionRepository {
    private val queries = database.interviewAssistantQueries
    private val mutableSessions = MutableStateFlow<List<InterviewSession>>(emptyList())

    override val sessions: StateFlow<List<InterviewSession>> = mutableSessions.asStateFlow()

    override suspend fun initialize() {
        val now = TimeProvider.currentTimeMillis()
        withContext(dispatchers.io) {
            queries.pauseActiveSessions(now)
            queries.markInterruptedAnswers(now)
        }
        refresh()
    }

    override suspend fun saveSession(session: InterviewSession) {
        withContext(dispatchers.io) {
            if (queries.selectSessionById(session.id).executeAsOneOrNull() == null) {
                queries.insertSession(
                    id = session.id,
                    resume_id = session.resumeId,
                    title = session.title,
                    status = session.status.name,
                    trigger_mode = session.triggerMode.name,
                    created_at = session.createdAt,
                    updated_at = session.updatedAt,
                )
            } else {
                queries.updateSession(
                    resume_id = session.resumeId,
                    title = session.title,
                    status = session.status.name,
                    trigger_mode = session.triggerMode.name,
                    created_at = session.createdAt,
                    updated_at = session.updatedAt,
                    id = session.id,
                )
            }
        }
        refresh()
    }

    override suspend fun detail(sessionId: String): InterviewSessionDetail? = withContext(dispatchers.io) {
        val session = queries.selectSessionById(sessionId).executeAsOneOrNull()?.toDomain()
            ?: return@withContext null
        InterviewSessionDetail(
            session = session,
            resume = queries.selectResumeById(session.resumeId).executeAsOneOrNull()?.toDomain(),
            transcripts = queries.selectTranscriptsBySession(sessionId).executeAsList()
                .map(TranscriptSegmentEntity::toDomain),
            answers = queries.selectAnswersBySession(sessionId).executeAsList()
                .map(AssistantAnswerEntity::toDomain),
        )
    }

    override suspend fun saveTranscript(segment: TranscriptSegment) {
        withContext(dispatchers.io) {
            queries.upsertTranscript(
                id = segment.id,
                session_id = segment.sessionId,
                sequence = segment.sequence,
                text = segment.text,
                is_final = if (segment.isFinal) 1L else 0L,
                created_at = segment.createdAt,
            )
        }
    }

    override suspend fun saveAnswer(answer: AssistantAnswer) {
        withContext(dispatchers.io) {
            queries.upsertAnswer(
                id = answer.id,
                session_id = answer.sessionId,
                question = answer.question,
                content = answer.content,
                model = answer.model,
                status = answer.status.name,
                created_at = answer.createdAt,
                updated_at = answer.updatedAt,
            )
        }
    }

    override suspend fun deleteSession(sessionId: String) {
        withContext(dispatchers.io) {
            database.transaction {
                queries.deleteAnswersBySession(sessionId)
                queries.deleteTranscriptsBySession(sessionId)
                queries.deleteSession(sessionId)
            }
        }
        refresh()
    }

    override suspend fun clearAll() {
        val storedPaths = withContext(dispatchers.io) {
            queries.selectAllResumes().executeAsList().map(ResumeEntity::stored_path)
        }
        withContext(dispatchers.io) {
            database.transaction {
                queries.deleteAllAnswers()
                queries.deleteAllTranscripts()
                queries.deleteAllSessions()
                queries.deleteAllOcrJobs()
                queries.deleteAllResumes()
            }
        }
        storedPaths.forEach { fileStore.delete(it) }
        refresh()
    }

    private suspend fun refresh() {
        mutableSessions.value = withContext(dispatchers.io) {
            queries.selectAllSessions().executeAsList().map(InterviewSessionEntity::toDomain)
        }
    }
}

/**
 * Creates a stable-enough local identifier without requiring platform UUID APIs.
 */
fun newId(prefix: String): String {
    return "$prefix-${TimeProvider.currentTimeMillis()}-${Random.nextLong()}"
}

private fun ResumeEntity.toDomain(): Resume {
    return Resume(
        id = id,
        displayName = display_name,
        originalFileName = original_file_name,
        storedPath = stored_path,
        mimeType = mime_type,
        ocrStatus = enumValueOrDefault(ocr_status, OcrStatus.FAILED),
        ocrText = ocr_text,
        ocrError = ocr_error,
        createdAt = created_at,
        updatedAt = updated_at,
        ocrOriginalText = ocr_original_text ?: ocr_text,
    )
}

private fun OcrJobEntity.toDomain(): OcrJob {
    return OcrJob(
        resumeId = resume_id,
        providerJobId = provider_job_id,
        state = enumValueOrDefault(state, OcrStatus.FAILED),
        progressCurrent = progress_current,
        progressTotal = progress_total,
        resultUrl = result_url,
        errorMessage = error_message,
        updatedAt = updated_at,
    )
}

private fun InterviewSessionEntity.toDomain(): InterviewSession {
    return InterviewSession(
        id = id,
        resumeId = resume_id,
        title = title,
        status = enumValueOrDefault(status, InterviewSessionStatus.ERROR),
        triggerMode = enumValueOrDefault(trigger_mode, AnswerTriggerMode.MANUAL),
        createdAt = created_at,
        updatedAt = updated_at,
    )
}

private fun TranscriptSegmentEntity.toDomain(): TranscriptSegment {
    return TranscriptSegment(
        id = id,
        sessionId = session_id,
        sequence = sequence,
        text = text,
        isFinal = is_final != 0L,
        createdAt = created_at,
    )
}

private fun AssistantAnswerEntity.toDomain(): AssistantAnswer {
    return AssistantAnswer(
        id = id,
        sessionId = session_id,
        question = question,
        content = content,
        model = model,
        status = enumValueOrDefault(status, AssistantAnswerStatus.FAILED),
        createdAt = created_at,
        updatedAt = updated_at,
    )
}

private inline fun <reified T : Enum<T>> enumValueOrDefault(value: String, default: T): T {
    return enumValues<T>().firstOrNull { it.name == value } ?: default
}
