package com.example.interviewassistant.feature.interviewassistant.presentation.state

import com.example.interviewassistant.feature.interviewassistant.domain.model.ProviderConfiguration
import com.example.interviewassistant.feature.interviewassistant.domain.model.ProviderSecretStatus
import com.example.interviewassistant.feature.interviewassistant.domain.model.ProviderSecretUpdate
import com.example.interviewassistant.feature.interviewassistant.domain.model.ProviderSecrets
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
    val errorMessage: String? = null,
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
    data object Refresh : ProviderSettingsUiEvent
}

/**
 * One-time settings effects consumed by platform UI.
 */
sealed interface ProviderSettingsUiEffect {
    data object Saved : ProviderSettingsUiEffect
}
