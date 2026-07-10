package com.example.interviewassistant.feature.interviewassistant.data.repository

import com.example.interviewassistant.core.audio.AudioFrame
import com.example.interviewassistant.core.audio.AudioSource
import com.example.interviewassistant.feature.interviewassistant.data.remote.speech.SpeechRecognitionEvent
import com.example.interviewassistant.feature.interviewassistant.data.remote.speech.XunfeiSpeechGateway
import com.example.interviewassistant.feature.interviewassistant.domain.repository.ProviderConfigurationRepository
import com.example.interviewassistant.feature.interviewassistant.domain.repository.SpeechRecognizer
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Keeps platform capture alive while rotating iFlytek WebSocket sessions.
 */
class ContinuousSpeechRecognizer(
    private val audioSource: AudioSource,
    private val providerConfiguration: ProviderConfigurationRepository,
    private val gateway: XunfeiSpeechGateway,
) : SpeechRecognizer {
    override fun recognize(): Flow<SpeechRecognitionEvent> = channelFlow {
        val credentials = providerConfiguration.xunfeiCredentials()
        if (credentials == null) {
            send(SpeechRecognitionEvent.Failure(null, "iFlytek credentials are not configured"))
            return@channelFlow
        }
        if (!audioSource.isAvailable) {
            send(SpeechRecognitionEvent.Failure(null, "The selected audio source is unavailable"))
            return@channelFlow
        }

        val audioBus = MutableSharedFlow<AudioFrame>(
            extraBufferCapacity = AUDIO_BUFFER_FRAMES,
            onBufferOverflow = BufferOverflow.DROP_OLDEST,
        )
        val captureJob = launch {
            audioSource.frames().collect(audioBus::emit)
        }
        try {
            while (currentCoroutineContext().isActive) {
                var providerFailed = false
                try {
                    gateway.recognizeSession(
                        audio = audioBus,
                        configuration = providerConfiguration.configuration.value.xunfei,
                        credentials = credentials,
                    ).collect { event ->
                        send(event)
                        if (event is SpeechRecognitionEvent.Failure) providerFailed = true
                    }
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (error: Throwable) {
                    send(
                        SpeechRecognitionEvent.Failure(
                            code = null,
                            message = error.message ?: "Speech recognition connection failed",
                        ),
                    )
                    providerFailed = true
                }
                if (providerFailed) break
                delay(RECONNECT_DELAY_MILLIS)
            }
        } finally {
            captureJob.cancel()
            audioSource.stop()
        }
    }

    override suspend fun stop() {
        audioSource.stop()
    }

    private companion object {
        const val AUDIO_BUFFER_FRAMES = 100
        const val RECONNECT_DELAY_MILLIS = 120L
    }
}
