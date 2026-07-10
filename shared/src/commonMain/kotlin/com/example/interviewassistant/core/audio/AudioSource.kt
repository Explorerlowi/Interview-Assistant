package com.example.interviewassistant.core.audio

import kotlinx.coroutines.flow.Flow

/**
 * One 40-millisecond PCM frame accepted by iFlytek streaming dictation.
 */
data class AudioFrame(
    val pcm: ByteArray,
    val capturedAtMillis: Long,
)

/**
 * Platform audio capture normalized to 16kHz, signed 16-bit, mono PCM.
 */
interface AudioSource {
    /** Whether the required platform capture backend is available. */
    val isAvailable: Boolean

    /**
     * Starts capture and emits 1280-byte frames until cancelled or [stop] is called.
     */
    fun frames(): Flow<AudioFrame>

    /** Stops the active capture session. */
    suspend fun stop()
}

/**
 * Platform audio capture failure with a user-actionable description.
 */
class AudioCaptureException(
    override val message: String,
    override val cause: Throwable? = null,
) : Exception(message, cause)

/** Required output sample rate. */
const val AUDIO_SAMPLE_RATE = 16_000

/** Required output bytes per 40-millisecond frame. */
const val AUDIO_FRAME_BYTES = 1_280
