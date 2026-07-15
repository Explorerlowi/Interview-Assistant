package com.example.interviewassistant.feature.interviewassistant.presentation.state

import com.example.interviewassistant.feature.interviewassistant.domain.model.Resume

/**
 * State rendered by resume-library screens.
 */
data class ResumeLibraryUiState(
    val resumes: List<Resume> = emptyList(),
    val isLoading: Boolean = true,
    val importingFileName: String? = null,
    val processingResumeIds: Set<String> = emptySet(),
    val errorMessage: String? = null,
)

/**
 * Resume-library user actions.
 */
sealed interface ResumeLibraryUiEvent {
    data object Refresh : ResumeLibraryUiEvent

    data class Import(
        val displayName: String,
        val fileName: String,
        val mimeType: String,
        val content: ByteArray,
    ) : ResumeLibraryUiEvent

    data class RetryOcr(val resumeId: String) : ResumeLibraryUiEvent
    data class UpdateOcrText(val resumeId: String, val ocrText: String) : ResumeLibraryUiEvent
    data class Delete(val resumeId: String) : ResumeLibraryUiEvent
    data object ClearError : ResumeLibraryUiEvent
}

/**
 * One-time resume-library effects.
 */
sealed interface ResumeLibraryUiEffect {
    data class ImportCompleted(val resumeId: String) : ResumeLibraryUiEffect
    data class OcrTextSaved(val resumeId: String) : ResumeLibraryUiEffect
}
