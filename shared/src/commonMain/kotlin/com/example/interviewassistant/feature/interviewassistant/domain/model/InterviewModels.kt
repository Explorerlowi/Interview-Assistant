package com.example.interviewassistant.feature.interviewassistant.domain.model

/**
 * Lifecycle of OCR processing for one resume.
 */
enum class OcrStatus {
    QUEUED,
    PENDING,
    RUNNING,
    READY,
    FAILED,
}

/**
 * Resume source and extracted text stored locally.
 *
 * @property ocrText Current resume text used by the assistant (may be manually edited).
 * @property ocrOriginalText Immutable OCR output captured when recognition finished.
 */
data class Resume(
    val id: String,
    val displayName: String,
    val originalFileName: String,
    val storedPath: String,
    val mimeType: String,
    val ocrStatus: OcrStatus,
    val ocrText: String?,
    val ocrError: String?,
    val createdAt: Long,
    val updatedAt: Long,
    val ocrOriginalText: String? = null,
) {
    /** Whether the working text differs from the initial OCR output. */
    val hasEditedOcrText: Boolean
        get() = !ocrOriginalText.isNullOrEmpty() && ocrText != ocrOriginalText
}

/**
 * Recoverable PaddleOCR job state.
 */
data class OcrJob(
    val resumeId: String,
    val providerJobId: String?,
    val state: OcrStatus,
    val progressCurrent: Long,
    val progressTotal: Long,
    val resultUrl: String?,
    val errorMessage: String?,
    val updatedAt: Long,
)

/**
 * Persisted interview-session lifecycle.
 */
enum class InterviewSessionStatus {
    IDLE,
    LISTENING,
    QUESTION_READY,
    GENERATING,
    PAUSED,
    COMPLETED,
    ERROR,
}

/**
 * A resume-scoped interview-assistant session.
 */
data class InterviewSession(
    val id: String,
    val resumeId: String,
    val title: String,
    val status: InterviewSessionStatus,
    val triggerMode: AnswerTriggerMode,
    val createdAt: Long,
    val updatedAt: Long,
)

/**
 * One ordered piece of recognized interview speech.
 */
data class TranscriptSegment(
    val id: String,
    val sessionId: String,
    val sequence: Long,
    val text: String,
    val isFinal: Boolean,
    val createdAt: Long,
)

/**
 * Lifecycle of one generated answer.
 */
enum class AssistantAnswerStatus {
    GENERATING,
    COMPLETED,
    INTERRUPTED,
    FAILED,
}

/**
 * Persisted model answer associated with its source question.
 */
data class AssistantAnswer(
    val id: String,
    val sessionId: String,
    val question: String,
    val content: String,
    val model: String,
    val status: AssistantAnswerStatus,
    val createdAt: Long,
    val updatedAt: Long,
)

/**
 * Complete session detail used by history and workspace screens.
 */
data class InterviewSessionDetail(
    val session: InterviewSession,
    val resume: Resume?,
    val transcripts: List<TranscriptSegment>,
    val answers: List<AssistantAnswer>,
)
