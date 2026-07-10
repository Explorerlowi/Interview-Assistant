package com.example.interviewassistant.core.audio

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * Placeholder audio source while iOS is outside the first product scope.
 */
class UnsupportedIosAudioSource : AudioSource {
    override val isAvailable: Boolean = false

    override fun frames(): Flow<AudioFrame> = flow {
        throw AudioCaptureException("Audio capture is not available on iOS yet")
    }

    override suspend fun stop() = Unit
}
