package com.example.interviewassistant.feature.interviewassistant.presentation.viewmodel

import com.example.interviewassistant.core.util.CoroutineDispatcherProvider
import com.example.interviewassistant.feature.interviewassistant.domain.repository.ProviderConfigurationRepository
import com.example.interviewassistant.feature.interviewassistant.domain.usecase.ProviderConnectionTester
import com.example.interviewassistant.feature.interviewassistant.presentation.state.ProviderSettingsUiEffect
import com.example.interviewassistant.feature.interviewassistant.presentation.state.ProviderSettingsUiEvent
import com.example.interviewassistant.feature.interviewassistant.presentation.state.ProviderSettingsUiState
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
 * Coordinates provider configuration edits, including credential values shown in settings.
 */
class ProviderSettingsViewModel(
    private val repository: ProviderConfigurationRepository,
    private val connectionTester: ProviderConnectionTester,
    dispatcherProvider: CoroutineDispatcherProvider,
) : BaseViewModel<ProviderSettingsUiState, ProviderSettingsUiEvent, ProviderSettingsUiEffect>(
    ProviderSettingsUiState(),
    dispatcherProvider,
) {
    private val mutableUiState = MutableStateFlow(readState())
    private val mutableEffect = MutableSharedFlow<ProviderSettingsUiEffect>()

    override val uiState: StateFlow<ProviderSettingsUiState> = mutableUiState.asStateFlow()
    override val effect: SharedFlow<ProviderSettingsUiEffect> = mutableEffect.asSharedFlow()

    override fun onEvent(event: ProviderSettingsUiEvent) {
        when (event) {
            is ProviderSettingsUiEvent.Save -> {
                viewModelScope.launch(dispatcherProvider.io) {
                    mutableUiState.value = mutableUiState.value.copy(isSaving = true, errorMessage = null)
                    try {
                        repository.save(event.configuration, event.secrets)
                        mutableUiState.value = readState()
                        mutableEffect.emit(ProviderSettingsUiEffect.Saved)
                    } catch (cancelled: CancellationException) {
                        mutableUiState.value = mutableUiState.value.copy(isSaving = false)
                        throw cancelled
                    } catch (error: Throwable) {
                        mutableUiState.value = mutableUiState.value.copy(
                            isSaving = false,
                            errorMessage = error.message ?: "Failed to save settings",
                        )
                    }
                }
            }
            ProviderSettingsUiEvent.ClearSecrets -> {
                viewModelScope.launch(dispatcherProvider.io) {
                    try {
                        repository.clearSecrets()
                        mutableUiState.value = readState()
                    } catch (cancelled: CancellationException) {
                        throw cancelled
                    } catch (error: Throwable) {
                        mutableUiState.value = mutableUiState.value.copy(
                            errorMessage = error.message ?: "Failed to clear secrets",
                        )
                    }
                }
            }
            ProviderSettingsUiEvent.TestConnections -> {
                viewModelScope.launch(dispatcherProvider.io) {
                    mutableUiState.value = mutableUiState.value.copy(
                        isTesting = true,
                        connectionResults = emptyMap(),
                        errorMessage = null,
                    )
                    try {
                        val results = connectionTester.testAll()
                        mutableUiState.value = mutableUiState.value.copy(
                            isTesting = false,
                            connectionResults = results,
                        )
                    } catch (cancelled: CancellationException) {
                        mutableUiState.value = mutableUiState.value.copy(isTesting = false)
                        throw cancelled
                    } catch (error: Throwable) {
                        mutableUiState.value = mutableUiState.value.copy(
                            isTesting = false,
                            errorMessage = error.message ?: "Connection test failed",
                        )
                    }
                }
            }
            ProviderSettingsUiEvent.Refresh -> mutableUiState.value = readState()
        }
    }

    private fun readState(): ProviderSettingsUiState {
        return ProviderSettingsUiState(
            configuration = repository.configuration.value,
            secretStatus = repository.secretStatus(),
            secrets = repository.secrets(),
        )
    }
}
