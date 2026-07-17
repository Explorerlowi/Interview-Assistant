package com.example.interviewassistant.feature.interviewassistant.presentation.viewmodel

import com.example.interviewassistant.core.util.CoroutineDispatcherProvider
import com.example.interviewassistant.feature.interviewassistant.data.remote.speech.SpeechRecognitionEvent
import com.example.interviewassistant.feature.interviewassistant.domain.model.SpeechRecognitionMode
import com.example.interviewassistant.feature.interviewassistant.domain.model.SenseVoiceConfiguration
import com.example.interviewassistant.feature.interviewassistant.domain.model.SpeechUnderstandingMetadata
import com.example.interviewassistant.feature.interviewassistant.domain.repository.ProviderConfigurationRepository
import com.example.interviewassistant.feature.interviewassistant.domain.repository.SpeechModelManager
import com.example.interviewassistant.feature.interviewassistant.domain.repository.SpeechModelState
import com.example.interviewassistant.feature.interviewassistant.domain.usecase.ProviderConnectionTester
import com.example.interviewassistant.feature.interviewassistant.domain.usecase.TestSpeechRecognitionUseCase
import com.example.interviewassistant.feature.interviewassistant.presentation.state.ProviderSettingsUiEffect
import com.example.interviewassistant.feature.interviewassistant.presentation.state.ProviderSettingsUiEvent
import com.example.interviewassistant.feature.interviewassistant.presentation.state.ProviderSettingsUiState
import com.example.interviewassistant.feature.interviewassistant.presentation.state.SpeechRecognitionTestUiState
import com.example.interviewassistant.presentation.viewmodel.BaseViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
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
    private val speechModelManager: SpeechModelManager,
    private val testSpeechRecognition: TestSpeechRecognitionUseCase,
    dispatcherProvider: CoroutineDispatcherProvider,
) : BaseViewModel<ProviderSettingsUiState, ProviderSettingsUiEvent, ProviderSettingsUiEffect>(
    ProviderSettingsUiState(),
    dispatcherProvider,
) {
    private val mutableUiState = MutableStateFlow(readState())
    private val mutableEffect = MutableSharedFlow<ProviderSettingsUiEffect>()
    private var speechRecognitionTestJob: Job? = null

    override val uiState: StateFlow<ProviderSettingsUiState> = mutableUiState.asStateFlow()
    override val effect: SharedFlow<ProviderSettingsUiEffect> = mutableEffect.asSharedFlow()

    init {
        viewModelScope.launch {
            speechModelManager.state.collect { modelState ->
                mutableUiState.value = mutableUiState.value.copy(speechModelState = modelState)
                if (
                    modelState != SpeechModelState.Ready &&
                    mutableUiState.value.speechRecognitionTest.mode == SpeechRecognitionMode.SENSE_VOICE_ON_DEVICE
                ) {
                    stopSpeechRecognitionTest()
                }
            }
        }
    }

    override fun onEvent(event: ProviderSettingsUiEvent) {
        when (event) {
            is ProviderSettingsUiEvent.Save -> {
                viewModelScope.launch(dispatcherProvider.io) {
                    mutableUiState.value = mutableUiState.value.copy(isSaving = true, errorMessage = null)
                    try {
                        repository.save(event.configuration, event.secrets)
                        mutableUiState.value = readState(mutableUiState.value.speechRecognitionTest)
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
                        mutableUiState.value = readState(mutableUiState.value.speechRecognitionTest)
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
            ProviderSettingsUiEvent.InstallSpeechModel -> {
                viewModelScope.launch {
                    speechModelManager.install()
                }
            }
            ProviderSettingsUiEvent.DeleteSpeechModel -> {
                stopSpeechRecognitionTest()
                viewModelScope.launch {
                    speechModelManager.delete()
                }
            }
            is ProviderSettingsUiEvent.StartSpeechRecognitionTest -> {
                startSpeechRecognitionTest(event.mode, event.senseVoiceConfiguration)
            }
            ProviderSettingsUiEvent.StopSpeechRecognitionTest -> stopSpeechRecognitionTest()
            ProviderSettingsUiEvent.ResetSpeechRecognitionTest -> {
                stopSpeechRecognitionTest()
                mutableUiState.value = mutableUiState.value.copy(
                    speechRecognitionTest = SpeechRecognitionTestUiState(),
                )
            }
            ProviderSettingsUiEvent.Refresh -> {
                mutableUiState.value = readState(mutableUiState.value.speechRecognitionTest)
            }
        }
    }

    private fun startSpeechRecognitionTest(
        mode: SpeechRecognitionMode,
        senseVoiceConfiguration: SenseVoiceConfiguration,
    ) {
        if (speechRecognitionTestJob?.isActive == true) return
        if (
            mode == SpeechRecognitionMode.SENSE_VOICE_ON_DEVICE &&
            mutableUiState.value.speechModelState != SpeechModelState.Ready
        ) {
            return
        }

        mutableUiState.value = mutableUiState.value.copy(
            speechRecognitionTest = SpeechRecognitionTestUiState(
                mode = mode,
                isListening = true,
            ),
        )
        speechRecognitionTestJob = viewModelScope.launch {
            try {
                testSpeechRecognition(mode, senseVoiceConfiguration).collect(::handleSpeechRecognitionEvent)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Throwable) {
                updateSpeechRecognitionTest { it.copy(failed = true) }
            } finally {
                updateSpeechRecognitionTest { it.copy(isListening = false) }
                speechRecognitionTestJob = null
            }
        }
    }

    private fun stopSpeechRecognitionTest() {
        val activeJob = speechRecognitionTestJob ?: return
        speechRecognitionTestJob = null
        activeJob.cancel()
        updateSpeechRecognitionTest { it.copy(isListening = false) }
    }

    private fun handleSpeechRecognitionEvent(event: SpeechRecognitionEvent) {
        when (event) {
            is SpeechRecognitionEvent.Partial -> updateSpeechRecognitionTest {
                it.copy(
                    partialTranscript = event.text.trim(),
                    metadata = event.metadata.ifPresentOr(it.metadata),
                )
            }
            is SpeechRecognitionEvent.Final -> updateSpeechRecognitionTest {
                val finalText = event.text.trim()
                it.copy(
                    transcript = appendTranscript(it.transcript, finalText),
                    partialTranscript = "",
                    metadata = event.metadata.ifPresentOr(it.metadata),
                )
            }
            is SpeechRecognitionEvent.Failure -> updateSpeechRecognitionTest {
                it.copy(errorMessage = event.message, failed = true)
            }
            SpeechRecognitionEvent.SessionRotated -> Unit
        }
    }

    private fun updateSpeechRecognitionTest(
        update: (SpeechRecognitionTestUiState) -> SpeechRecognitionTestUiState,
    ) {
        val currentState = mutableUiState.value
        mutableUiState.value = currentState.copy(
            speechRecognitionTest = update(currentState.speechRecognitionTest),
        )
    }

    private fun appendTranscript(transcript: String, segment: String): String {
        if (segment.isBlank()) return transcript
        return listOf(transcript, segment).filter(String::isNotBlank).joinToString(separator = "\n")
    }

    private fun SpeechUnderstandingMetadata.ifPresentOr(
        fallback: SpeechUnderstandingMetadata,
    ): SpeechUnderstandingMetadata = if (isEmpty) fallback else this

    private fun readState(
        speechRecognitionTest: SpeechRecognitionTestUiState = SpeechRecognitionTestUiState(),
    ): ProviderSettingsUiState {
        return ProviderSettingsUiState(
            configuration = repository.configuration.value,
            secretStatus = repository.secretStatus(),
            secrets = repository.secrets(),
            speechModelDescriptor = speechModelManager.descriptor,
            speechModelState = speechModelManager.state.value,
            speechRecognitionTest = speechRecognitionTest,
        )
    }
}
