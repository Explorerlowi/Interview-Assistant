package com.example.interviewassistant.feature.interviewassistant.presentation.state

import com.example.interviewassistant.feature.interviewassistant.domain.model.AnswerTriggerMode
import com.example.interviewassistant.feature.interviewassistant.domain.model.AssistantAnswer
import com.example.interviewassistant.feature.interviewassistant.domain.model.InterviewSession
import com.example.interviewassistant.feature.interviewassistant.domain.model.Resume
import com.example.interviewassistant.feature.interviewassistant.domain.model.TranscriptSegment

/**
 * State for one live or restored interview-assistant workspace.
 */
data class InterviewSessionUiState(
    val session: InterviewSession? = null,
    val resume: Resume? = null,
    val transcripts: List<TranscriptSegment> = emptyList(),
    val answers: List<AssistantAnswer> = emptyList(),
    val liveTranscript: String = "",
    val currentQuestion: String = "",
    val streamingAnswer: String = "",
    val triggerMode: AnswerTriggerMode = AnswerTriggerMode.MANUAL,
    val isListening: Boolean = false,
    val isGenerating: Boolean = false,
    val errorMessage: String? = null,
)

/**
 * Live-workspace events accepted by [InterviewSessionViewModel].
 */
sealed interface InterviewSessionUiEvent {
    data class StartSession(
        val resumeId: String,
        val title: String,
        val triggerMode: AnswerTriggerMode,
    ) : InterviewSessionUiEvent

    data class OpenSession(val sessionId: String) : InterviewSessionUiEvent
    data object StartListening : InterviewSessionUiEvent
    data object StopListening : InterviewSessionUiEvent
    data class UpdateQuestion(val text: String) : InterviewSessionUiEvent
    data object GenerateAnswer : InterviewSessionUiEvent
    data object CancelGeneration : InterviewSessionUiEvent
    data class SetTriggerMode(val mode: AnswerTriggerMode) : InterviewSessionUiEvent
    data object CompleteSession : InterviewSessionUiEvent
    data object ClearError : InterviewSessionUiEvent
}

/**
 * One-time workspace effects consumed by platform UI.
 */
sealed interface InterviewSessionUiEffect {
    data object SessionCompleted : InterviewSessionUiEffect
}
