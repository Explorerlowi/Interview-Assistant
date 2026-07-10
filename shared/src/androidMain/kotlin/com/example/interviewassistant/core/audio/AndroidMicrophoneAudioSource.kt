package com.example.interviewassistant.core.audio

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import com.example.interviewassistant.core.util.CoroutineDispatcherProvider
import com.example.interviewassistant.core.util.TimeProvider
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext

/**
 * Android microphone source using [AudioRecord] in voice-recognition mode.
 */
class AndroidMicrophoneAudioSource(
    private val context: Context,
    private val dispatchers: CoroutineDispatcherProvider,
) : AudioSource {
    @Volatile
    private var activeRecord: AudioRecord? = null

    override val isAvailable: Boolean
        get() = AudioRecord.getMinBufferSize(
            AUDIO_SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
        ) > 0

    override fun frames(): Flow<AudioFrame> = flow {
        ensurePermission()
        val minBufferSize = AudioRecord.getMinBufferSize(
            AUDIO_SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
        )
        if (minBufferSize <= 0) {
            throw AudioCaptureException("The Android audio device does not support 16 kHz mono PCM")
        }
        val recorder = AudioRecord(
            MediaRecorder.AudioSource.VOICE_RECOGNITION,
            AUDIO_SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            maxOf(minBufferSize * 2, AUDIO_FRAME_BYTES * 4),
        )
        if (recorder.state != AudioRecord.STATE_INITIALIZED) {
            recorder.release()
            throw AudioCaptureException("Unable to initialize the Android microphone")
        }

        activeRecord = recorder
        try {
            recorder.startRecording()
            while (
                currentCoroutineContext().isActive &&
                recorder.recordingState == AudioRecord.RECORDSTATE_RECORDING
            ) {
                val frame = ByteArray(AUDIO_FRAME_BYTES)
                var offset = 0
                while (offset < frame.size && currentCoroutineContext().isActive) {
                    val count = recorder.read(
                        frame,
                        offset,
                        frame.size - offset,
                        AudioRecord.READ_BLOCKING,
                    )
                    if (count <= 0) {
                        throw AudioCaptureException("Android microphone read failed: $count")
                    }
                    offset += count
                }
                if (offset == frame.size) {
                    emit(AudioFrame(frame, TimeProvider.currentTimeMillis()))
                }
            }
        } finally {
            runCatching { recorder.stop() }
            recorder.release()
            if (activeRecord === recorder) activeRecord = null
        }
    }.flowOn(dispatchers.io)

    override suspend fun stop() {
        withContext(dispatchers.io) {
            activeRecord?.let { recorder ->
                runCatching { recorder.stop() }
            }
        }
    }

    private fun ensurePermission() {
        if (context.checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            throw AudioCaptureException("Microphone permission is required")
        }
    }
}
