package com.example.interviewassistant.core.audio

import com.example.interviewassistant.core.util.CoroutineDispatcherProvider
import com.example.interviewassistant.core.util.TimeProvider
import com.sun.jna.Platform
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.DataLine
import javax.sound.sampled.TargetDataLine
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/** Default Windows microphone capture normalized to the application's 16 kHz mono PCM contract. */
class DesktopMicrophoneAudioSource(
    private val dispatchers: CoroutineDispatcherProvider,
) : AudioSource {
    @Volatile
    private var activeLine: TargetDataLine? = null

    override val isAvailable: Boolean
        get() = Platform.isWindows() && runCatching {
            AudioSystem.isLineSupported(DataLine.Info(TargetDataLine::class.java, AUDIO_FORMAT))
        }.getOrDefault(false)

    override fun frames(): Flow<AudioFrame> = callbackFlow {
        if (!isAvailable) {
            close(AudioCaptureException("No compatible Windows microphone is available"))
            return@callbackFlow
        }

        val line = try {
            AudioSystem.getTargetDataLine(AUDIO_FORMAT).apply {
                open(AUDIO_FORMAT, AUDIO_FRAME_BYTES * CAPTURE_BUFFER_FRAMES)
                start()
            }
        } catch (error: Throwable) {
            close(AudioCaptureException("Unable to open the default Windows microphone", error))
            return@callbackFlow
        }
        activeLine = line
        val job = launch(dispatchers.io) {
            val frame = ByteArray(AUDIO_FRAME_BYTES)
            var frameOffset = 0
            try {
                while (isActive && line.isOpen) {
                    val count = line.read(frame, frameOffset, frame.size - frameOffset)
                    if (count < 0) break
                    frameOffset += count
                    if (frameOffset == frame.size) {
                        if (!trySend(AudioFrame(frame.copyOf(), TimeProvider.currentTimeMillis())).isSuccess) break
                        frameOffset = 0
                    }
                }
                close()
            } catch (error: Throwable) {
                if (line.isOpen) {
                    close(AudioCaptureException("Windows microphone capture failed", error))
                }
            } finally {
                closeLine(line)
                if (activeLine === line) activeLine = null
            }
        }

        awaitClose {
            closeLine(line)
            job.cancel()
        }
    }

    override suspend fun stop() {
        activeLine?.let(::closeLine)
        activeLine = null
    }

    private fun closeLine(line: TargetDataLine) {
        runCatching { line.stop() }
        runCatching { line.flush() }
        runCatching { line.close() }
    }

    private companion object {
        val AUDIO_FORMAT = AudioFormat(
            AUDIO_SAMPLE_RATE.toFloat(),
            16,
            1,
            true,
            false,
        )
        const val CAPTURE_BUFFER_FRAMES = 8
    }
}
