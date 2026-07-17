package com.example.interviewassistant.feature.interviewassistant

import com.example.interviewassistant.core.audio.AUDIO_FRAME_BYTES
import com.example.interviewassistant.core.audio.AudioFrame
import com.example.interviewassistant.core.util.CoroutineDispatcherProvider
import com.example.interviewassistant.feature.interviewassistant.data.remote.speech.SpeechRecognitionEvent
import com.example.interviewassistant.feature.interviewassistant.domain.model.SenseVoiceConfiguration
import com.example.interviewassistant.feature.interviewassistant.domain.repository.SpeechModelState
import com.example.interviewassistant.feature.interviewassistant.speech.DesktopSenseVoiceModelManager
import com.example.interviewassistant.feature.interviewassistant.speech.DesktopSenseVoiceSpeechGateway
import com.sun.jna.Platform
import java.io.File
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioSystem
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking

class DesktopSenseVoiceInferenceTest {
    @Test
    fun officialChineseWaveProducesTranscriptAndUnderstandingMetadata() = runBlocking {
        val wavePath = System.getenv(TEST_WAVE_ENVIRONMENT_VARIABLE) ?: return@runBlocking
        if (!Platform.isWindows() || !Platform.is64Bit()) return@runBlocking

        val modelManager = DesktopSenseVoiceModelManager(TestDispatchers)
        assertEquals(SpeechModelState.Ready, modelManager.state.value)
        val frames = File(wavePath).readPcmFrames()
        val gateway = DesktopSenseVoiceSpeechGateway(modelManager, TestDispatchers)

        val results = gateway.recognize(
            audio = frames.asFlow(),
            configuration = SenseVoiceConfiguration(language = "zh"),
        ).toList()
        val final = assertIs<SpeechRecognitionEvent.Final>(results.last())

        assertTrue(final.text.isNotBlank(), "SenseVoice should return a non-empty transcript")
        assertEquals("zh", final.metadata.language)
        assertTrue(final.metadata.emotion?.isNotBlank() == true)
        assertTrue(final.metadata.audioEvent?.isNotBlank() == true)
        println("SenseVoice integration result: text=${final.text}, metadata=${final.metadata}")
    }

    private fun File.readPcmFrames(): List<AudioFrame> {
        val bytes = AudioSystem.getAudioInputStream(this).use { input ->
            val format = input.format
            assertEquals(AudioFormat.Encoding.PCM_SIGNED, format.encoding)
            assertEquals(16_000f, format.sampleRate)
            assertEquals(16, format.sampleSizeInBits)
            assertEquals(1, format.channels)
            assertTrue(!format.isBigEndian)
            input.readAllBytes()
        }
        val audioWithTrailingSilence = bytes + ByteArray(AUDIO_FRAME_BYTES * TRAILING_SILENCE_FRAMES)
        return audioWithTrailingSilence.asList()
            .chunked(AUDIO_FRAME_BYTES)
            .mapIndexed { index, chunk ->
                AudioFrame(
                    pcm = chunk.toByteArray().copyOf(AUDIO_FRAME_BYTES),
                    capturedAtMillis = index * FRAME_DURATION_MILLIS,
                )
            }
    }

    private object TestDispatchers : CoroutineDispatcherProvider {
        override val main = Dispatchers.Default
        override val io = Dispatchers.IO
        override val default = Dispatchers.Default
    }

    private companion object {
        const val TEST_WAVE_ENVIRONMENT_VARIABLE = "SENSEVOICE_TEST_WAV"
        const val FRAME_DURATION_MILLIS = 40L
        const val TRAILING_SILENCE_FRAMES = 25
    }
}
