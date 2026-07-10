package com.example.interviewassistant.feature.interviewassistant.presentation.viewmodel

import com.example.interviewassistant.core.util.CoroutineDispatcherProvider
import com.example.interviewassistant.feature.interviewassistant.domain.repository.InterviewSessionRepository
import com.example.interviewassistant.feature.interviewassistant.domain.repository.ResumeRepository
import com.example.interviewassistant.feature.interviewassistant.presentation.state.SessionHistoryUiEffect
import com.example.interviewassistant.feature.interviewassistant.presentation.state.SessionHistoryUiEvent
import com.example.interviewassistant.feature.interviewassistant.presentation.state.SessionHistoryUiState
import com.example.interviewassistant.presentation.viewmodel.BaseViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Loads, deletes and clears persisted interview sessions.
 */
class SessionHistoryViewModel(
    private val repository: InterviewSessionRepository,
    private val resumeRepository: ResumeRepository,
    dispatcherProvider: CoroutineDispatcherProvider,
) : BaseViewModel<SessionHistoryUiState, SessionHistoryUiEvent, SessionHistoryUiEffect>(
    SessionHistoryUiState(),
    dispatcherProvider,
) {
    private val mutableUiState = MutableStateFlow(SessionHistoryUiState())
    private val mutableEffect = MutableSharedFlow<SessionHistoryUiEffect>()

    override val uiState: StateFlow<SessionHistoryUiState> = mutableUiState.asStateFlow()
    override val effect: SharedFlow<SessionHistoryUiEffect> = mutableEffect.asSharedFlow()

    init {
        viewModelScope.launch {
            repository.sessions.collect {
                mutableUiState.value = SessionHistoryUiState(it, isLoading = false)
            }
        }
        initialize()
    }

    override fun onEvent(event: SessionHistoryUiEvent) {
        when (event) {
            SessionHistoryUiEvent.Refresh -> initialize()
            is SessionHistoryUiEvent.Delete -> viewModelScope.launch(dispatcherProvider.io) {
                repository.deleteSession(event.sessionId)
            }
            SessionHistoryUiEvent.ClearAll -> viewModelScope.launch(dispatcherProvider.io) {
                repository.clearAll()
                resumeRepository.refresh()
            }
        }
    }

    private fun initialize() {
        viewModelScope.launch(dispatcherProvider.io) {
            repository.initialize()
        }
    }
}
