package com.example.interviewassistant.core.audio

import com.example.interviewassistant.core.util.DefaultDispatcherProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.util.Locale
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioSystem
import kotlin.math.PI
import kotlin.math.sin
import kotlin.test.Test
import kotlin.test.assertEquals

class DesktopLoopbackAudioSourceTest {
    @Test
    fun capturesSystemPlaybackAsXunfeiFrames() = runBlocking {
        if (!System.getProperty("os.name").lowercase(Locale.ROOT).contains("windows")) return@runBlocking
        val source = DesktopLoopbackAudioSource(DefaultDispatcherProvider())
        val captured = async {
            withTimeout(10_000) {
                source.frames().first()
            }
        }
        delay(300)
        playDiagnosticTone()
        val frame = captured.await()
        assertEquals(AUDIO_FRAME_BYTES, frame.pcm.size)
        source.stop()
    }

    private suspend fun playDiagnosticTone() = withContext(Dispatchers.IO) {
        val format = AudioFormat(48_000f, 16, 1, true, false)
        val line = AudioSystem.getSourceDataLine(format)
        val samples = ByteArray(48_000 / 2 * 2)
        for (index in 0 until samples.size / 2) {
            val value = (sin(2.0 * PI * 440.0 * index / 48_000.0) * Short.MAX_VALUE * 0.15)
                .toInt()
                .toShort()
            samples[index * 2] = (value.toInt() and 0xFF).toByte()
            samples[index * 2 + 1] = (value.toInt() shr 8 and 0xFF).toByte()
        }
        try {
            line.open(format)
            line.start()
            line.write(samples, 0, samples.size)
            line.drain()
        } finally {
            line.stop()
            line.close()
        }
    }
}
