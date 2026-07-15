package com.example.interviewassistant.feature.interviewassistant.presentation.viewmodel

import com.example.interviewassistant.core.error.AppResult
import com.example.interviewassistant.core.util.CoroutineDispatcherProvider
import com.example.interviewassistant.core.util.TimeProvider
import com.example.interviewassistant.feature.interviewassistant.domain.repository.InterviewSessionRepository
import com.example.interviewassistant.feature.interviewassistant.domain.repository.ResumeRepository
import com.example.interviewassistant.feature.interviewassistant.domain.usecase.ResumeOcrCoordinator
import com.example.interviewassistant.feature.interviewassistant.presentation.state.ResumeLibraryUiEffect
import com.example.interviewassistant.feature.interviewassistant.presentation.state.ResumeLibraryUiEvent
import com.example.interviewassistant.feature.interviewassistant.presentation.state.ResumeLibraryUiState
import com.example.interviewassistant.presentation.viewmodel.BaseViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Coordinates resume import, OCR progress, retry, text editing and deletion.
 */
class ResumeLibraryViewModel(
    private val repository: ResumeRepository,
    private val ocrCoordinator: ResumeOcrCoordinator,
    private val sessionRepository: InterviewSessionRepository,
    dispatcherProvider: CoroutineDispatcherProvider,
) : BaseViewModel<ResumeLibraryUiState, ResumeLibraryUiEvent, ResumeLibraryUiEffect>(
    ResumeLibraryUiState(),
    dispatcherProvider,
) {
    private val mutableUiState = MutableStateFlow(ResumeLibraryUiState())
    private val mutableEffect = MutableSharedFlow<ResumeLibraryUiEffect>()

    override val uiState: StateFlow<ResumeLibraryUiState> = mutableUiState.asStateFlow()
    override val effect: SharedFlow<ResumeLibraryUiEffect> = mutableEffect.asSharedFlow()

    init {
        viewModelScope.launch {
            repository.resumes.collect { resumes ->
                mutableUiState.value = mutableUiState.value.copy(
                    resumes = resumes,
                    isLoading = false,
                )
            }
        }
        refreshAndRecover()
    }

    override fun onEvent(event: ResumeLibraryUiEvent) {
        when (event) {
            ResumeLibraryUiEvent.Refresh -> refreshAndRecover()
            is ResumeLibraryUiEvent.Import -> import(event)
            is ResumeLibraryUiEvent.RetryOcr -> retry(event.resumeId)
            is ResumeLibraryUiEvent.UpdateOcrText -> updateOcrText(event)
            is ResumeLibraryUiEvent.Delete -> delete(event.resumeId)
            ResumeLibraryUiEvent.ClearError -> {
                mutableUiState.value = mutableUiState.value.copy(errorMessage = null)
            }
        }
    }

    private fun refreshAndRecover() {
        viewModelScope.launch(dispatcherProvider.io) {
            repository.refresh()
            ocrCoordinator.recoverPendingJobs()
        }
    }

    private fun import(event: ResumeLibraryUiEvent.Import) {
        viewModelScope.launch(dispatcherProvider.io) {
            mutableUiState.value = mutableUiState.value.copy(
                importingFileName = event.fileName,
                errorMessage = null,
            )
            when (
                val result = ocrCoordinator.importAndProcess(
                    displayName = event.displayName,
                    originalFileName = event.fileName,
                    mimeType = event.mimeType,
                    content = event.content,
                )
            ) {
                is AppResult.Success -> {
                    markProcessing(result.data.id, active = false)
                    mutableEffect.emit(ResumeLibraryUiEffect.ImportCompleted(result.data.id))
                }
                is AppResult.Error -> {
                    mutableUiState.value = mutableUiState.value.copy(
                        errorMessage = result.error.message ?: result.error::class.simpleName,
                    )
                }
            }
            mutableUiState.value = mutableUiState.value.copy(importingFileName = null)
        }
    }

    private fun retry(resumeId: String) {
        viewModelScope.launch(dispatcherProvider.io) {
            markProcessing(resumeId, active = true)
            mutableUiState.value = mutableUiState.value.copy(errorMessage = null)
            when (val result = ocrCoordinator.process(resumeId)) {
                is AppResult.Success -> Unit
                is AppResult.Error -> {
                    mutableUiState.value = mutableUiState.value.copy(
                        errorMessage = result.error.message ?: result.error::class.simpleName,
                    )
                }
            }
            markProcessing(resumeId, active = false)
        }
    }

    /**
     * Persists manually edited OCR text while keeping the current OCR status.
     */
    private fun updateOcrText(event: ResumeLibraryUiEvent.UpdateOcrText) {
        viewModelScope.launch(dispatcherProvider.io) {
            mutableUiState.value = mutableUiState.value.copy(errorMessage = null)
            try {
                val resume = repository.get(event.resumeId)
                if (resume == null) {
                    mutableUiState.value = mutableUiState.value.copy(
                        errorMessage = "Resume not found",
                    )
                    return@launch
                }
                repository.save(
                    resume.copy(
                        ocrText = event.ocrText,
                        // 首次保存编辑时，若尚无原文则把当前库里的文本固化为原文
                        ocrOriginalText = resume.ocrOriginalText ?: resume.ocrText,
                        updatedAt = TimeProvider.currentTimeMillis(),
                    ),
                )
                mutableEffect.emit(ResumeLibraryUiEffect.OcrTextSaved(event.resumeId))
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Throwable) {
                mutableUiState.value = mutableUiState.value.copy(
                    errorMessage = error.message ?: error::class.simpleName,
                )
            }
        }
    }

    private fun markProcessing(resumeId: String, active: Boolean) {
        val current = mutableUiState.value.processingResumeIds
        mutableUiState.value = mutableUiState.value.copy(
            processingResumeIds = if (active) current + resumeId else current - resumeId,
        )
    }

    private fun delete(resumeId: String) {
        viewModelScope.launch(dispatcherProvider.io) {
            repository.delete(resumeId)
            sessionRepository.initialize()
        }
    }
}
