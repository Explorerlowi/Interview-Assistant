package com.example.interviewassistant.feature.interviewassistant.data.repository

import com.example.interviewassistant.core.audio.AudioFrame
import com.example.interviewassistant.core.audio.AudioSource
import com.example.interviewassistant.core.i18n.AppStringId
import com.example.interviewassistant.core.i18n.StringsProvider
import com.example.interviewassistant.feature.interviewassistant.data.remote.speech.SpeechRecognitionEvent
import com.example.interviewassistant.feature.interviewassistant.domain.model.SenseVoiceConfiguration
import com.example.interviewassistant.feature.interviewassistant.domain.model.SpeechRecognitionMode
import com.example.interviewassistant.feature.interviewassistant.domain.repository.ProviderConfigurationRepository
import com.example.interviewassistant.feature.interviewassistant.domain.repository.ConfigurableSenseVoiceSpeechRecognizer
import com.example.interviewassistant.feature.interviewassistant.domain.repository.SpeechModelManager
import com.example.interviewassistant.feature.interviewassistant.domain.repository.SpeechModelState
import com.example.interviewassistant.feature.interviewassistant.domain.repository.SpeechRecognizer
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow

/**
 * Platform bridge for local SenseVoice inference.
 */
interface OnDeviceSpeechGateway {
    /**
     * Consumes normalized microphone frames and emits simulated-streaming recognition updates.
     *
     * @param audio Continuous 16 kHz mono PCM frames.
     * @param configuration SenseVoice decoding and endpoint settings.
     * @return Recognition updates until the flow is cancelled or audio ends.
     */
    fun recognize(
        audio: Flow<AudioFrame>,
        configuration: SenseVoiceConfiguration,
    ): Flow<SpeechRecognitionEvent>

    /** Stops active inference and releases per-session buffers. */
    suspend fun stop()
}

/**
 * Recognizer that connects the shared audio source to the platform SenseVoice gateway.
 */
class OnDeviceSenseVoiceSpeechRecognizer(
    private val audioSource: AudioSource,
    private val providers: ProviderConfigurationRepository,
    private val modelManager: SpeechModelManager,
    private val gateway: OnDeviceSpeechGateway,
    private val strings: StringsProvider,
) : ConfigurableSenseVoiceSpeechRecognizer {
    override fun recognize(): Flow<SpeechRecognitionEvent> = recognize(providers.configuration.value.senseVoice)

    override fun recognize(configuration: SenseVoiceConfiguration): Flow<SpeechRecognitionEvent> = channelFlow {
        val modelState = modelManager.state.value
        if (modelState != SpeechModelState.Ready) {
            val messageId = if (modelState == SpeechModelState.Unavailable) {
                AppStringId.ERROR_SENSEVOICE_UNAVAILABLE
            } else {
                AppStringId.ERROR_SENSEVOICE_MODEL_NOT_READY
            }
            send(SpeechRecognitionEvent.Failure(code = null, message = strings.get(messageId)))
            return@channelFlow
        }
        if (!audioSource.isAvailable) {
            send(
                SpeechRecognitionEvent.Failure(
                    code = null,
                    message = strings.get(AppStringId.ERROR_PERMISSION_MICROPHONE),
                ),
            )
            return@channelFlow
        }

        try {
            gateway.recognize(
                audio = audioSource.frames(),
                configuration = configuration,
            ).collect(::send)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Throwable) {
            send(
                SpeechRecognitionEvent.Failure(
                    code = null,
                    message = strings.get(AppStringId.ERROR_SENSEVOICE_INFERENCE),
                ),
            )
        } finally {
            gateway.stop()
            audioSource.stop()
        }
    }

    override suspend fun stop() {
        gateway.stop()
        audioSource.stop()
    }
}

/**
 * Chooses the configured speech-recognition backend at the start of each listening session.
 */
class ConfigurableSpeechRecognizer(
    private val providers: ProviderConfigurationRepository,
    private val xunfei: ContinuousSpeechRecognizer,
    private val senseVoice: OnDeviceSenseVoiceSpeechRecognizer,
) : SpeechRecognizer {
    @Volatile
    private var activeRecognizer: SpeechRecognizer? = null

    override fun recognize(): Flow<SpeechRecognitionEvent> = channelFlow {
        val selected = when (providers.configuration.value.speechRecognitionMode) {
            SpeechRecognitionMode.XUNFEI -> xunfei
            SpeechRecognitionMode.SENSE_VOICE_ON_DEVICE -> senseVoice
        }
        activeRecognizer = selected
        try {
            selected.recognize().collect(::send)
        } finally {
            if (activeRecognizer === selected) activeRecognizer = null
        }
    }

    override suspend fun stop() {
        val recognizer = activeRecognizer
        activeRecognizer = null
        recognizer?.stop()
    }
}

/**
 * Platform fallback used where on-device SenseVoice is not implemented.
 */
class UnavailableOnDeviceSpeechGateway : OnDeviceSpeechGateway {
    override fun recognize(
        audio: Flow<AudioFrame>,
        configuration: SenseVoiceConfiguration,
    ): Flow<SpeechRecognitionEvent> = flow {
        error("On-device SenseVoice is unavailable on this platform")
    }

    override suspend fun stop() = Unit
}
