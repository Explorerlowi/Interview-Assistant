package com.example.interviewassistant.feature.interviewassistant.presentation.state

import com.example.interviewassistant.feature.interviewassistant.domain.model.ProviderConfiguration
import com.example.interviewassistant.feature.interviewassistant.domain.model.ProviderSecretStatus
import com.example.interviewassistant.feature.interviewassistant.domain.model.ProviderSecretUpdate
import com.example.interviewassistant.feature.interviewassistant.domain.model.ProviderSecrets
import com.example.interviewassistant.feature.interviewassistant.domain.model.SpeechRecognitionMode
import com.example.interviewassistant.feature.interviewassistant.domain.model.SenseVoiceConfiguration
import com.example.interviewassistant.feature.interviewassistant.domain.model.SpeechUnderstandingMetadata
import com.example.interviewassistant.feature.interviewassistant.domain.repository.SpeechModelState
import com.example.interviewassistant.feature.interviewassistant.domain.repository.SpeechModelDescriptor
import com.example.interviewassistant.feature.interviewassistant.domain.usecase.ProviderConnectionResult
import com.example.interviewassistant.feature.interviewassistant.domain.usecase.ProviderKind

/**
 * Presentation state for provider configuration.
 */
data class ProviderSettingsUiState(
    val configuration: ProviderConfiguration = ProviderConfiguration(),
    val secretStatus: ProviderSecretStatus = ProviderSecretStatus(false, false, false),
    val secrets: ProviderSecrets = ProviderSecrets(),
    val isSaving: Boolean = false,
    val isTesting: Boolean = false,
    val connectionResults: Map<ProviderKind, ProviderConnectionResult> = emptyMap(),
    val speechModelDescriptor: SpeechModelDescriptor? = null,
    val speechModelState: SpeechModelState = SpeechModelState.Unavailable,
    val speechRecognitionTest: SpeechRecognitionTestUiState = SpeechRecognitionTestUiState(),
    val errorMessage: String? = null,
)

/**
 * UI state for a non-persistent SenseVoice microphone test.
 *
 * @property mode Recognition backend used by the current or most recent test.
 * @property isListening Whether microphone capture and local inference are active.
 * @property transcript Finalized recognition segments from the current test.
 * @property partialTranscript Latest provisional recognition text.
 * @property metadata Latest language, emotion, and audio-event result.
 * @property errorMessage Localized recognizer error, when one is available.
 * @property failed Whether the test ended with an error requiring a generic fallback message.
 */
data class SpeechRecognitionTestUiState(
    val mode: SpeechRecognitionMode? = null,
    val isListening: Boolean = false,
    val transcript: String = "",
    val partialTranscript: String = "",
    val metadata: SpeechUnderstandingMetadata = SpeechUnderstandingMetadata(),
    val errorMessage: String? = null,
    val failed: Boolean = false,
)

/**
 * User events accepted by the provider-settings view model.
 */
sealed interface ProviderSettingsUiEvent {
    data class Save(
        val configuration: ProviderConfiguration,
        val secrets: ProviderSecretUpdate,
    ) : ProviderSettingsUiEvent

    data object ClearSecrets : ProviderSettingsUiEvent
    data object TestConnections : ProviderSettingsUiEvent
    data object InstallSpeechModel : ProviderSettingsUiEvent
    data object DeleteSpeechModel : ProviderSettingsUiEvent
    data class StartSpeechRecognitionTest(
        val mode: SpeechRecognitionMode,
        val senseVoiceConfiguration: SenseVoiceConfiguration,
    ) : ProviderSettingsUiEvent
    data object StopSpeechRecognitionTest : ProviderSettingsUiEvent
    data object ResetSpeechRecognitionTest : ProviderSettingsUiEvent
    data object Refresh : ProviderSettingsUiEvent
}

/**
 * One-time settings effects consumed by platform UI.
 */
sealed interface ProviderSettingsUiEffect {
    data object Saved : ProviderSettingsUiEffect
}
