package com.example.interviewassistant.feature.interviewassistant.presentation.state

import com.example.interviewassistant.feature.interviewassistant.domain.model.InterviewSession

/**
 * Persisted interview history shown on dashboard screens.
 */
data class SessionHistoryUiState(
    val sessions: List<InterviewSession> = emptyList(),
    val isLoading: Boolean = true,
)

/**
 * History actions.
 */
sealed interface SessionHistoryUiEvent {
    data object Refresh : SessionHistoryUiEvent
    data class Delete(val sessionId: String) : SessionHistoryUiEvent
    data object ClearAll : SessionHistoryUiEvent
}

/** History has no one-time effects. */
sealed interface SessionHistoryUiEffect
