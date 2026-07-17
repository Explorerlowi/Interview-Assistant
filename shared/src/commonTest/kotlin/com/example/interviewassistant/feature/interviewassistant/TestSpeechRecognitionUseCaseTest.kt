package com.example.interviewassistant.feature.interviewassistant

import com.example.interviewassistant.feature.interviewassistant.data.remote.speech.SpeechRecognitionEvent
import com.example.interviewassistant.feature.interviewassistant.domain.model.SpeechRecognitionMode
import com.example.interviewassistant.feature.interviewassistant.domain.model.SenseVoiceConfiguration
import com.example.interviewassistant.feature.interviewassistant.domain.repository.ConfigurableSenseVoiceSpeechRecognizer
import com.example.interviewassistant.feature.interviewassistant.domain.repository.SpeechRecognizer
import com.example.interviewassistant.feature.interviewassistant.domain.usecase.TestSpeechRecognitionUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class TestSpeechRecognitionUseCaseTest {
    @Test
    fun `routes each test mode to its dedicated recognizer`() = runTest {
        val xunfei = FakeRecognizer("xunfei")
        val senseVoice = FakeSenseVoiceRecognizer("sensevoice")
        val useCase = TestSpeechRecognitionUseCase(xunfei, senseVoice)

        val configuration = SenseVoiceConfiguration(language = "yue")
        val xunfeiEvent = useCase(SpeechRecognitionMode.XUNFEI, configuration).first()
        val senseVoiceEvent = useCase(SpeechRecognitionMode.SENSE_VOICE_ON_DEVICE, configuration).first()

        assertEquals(SpeechRecognitionEvent.Final("xunfei"), xunfeiEvent)
        assertEquals(SpeechRecognitionEvent.Final("sensevoice"), senseVoiceEvent)
        assertEquals(1, xunfei.startCount)
        assertEquals(1, senseVoice.startCount)
        assertEquals("yue", senseVoice.lastConfiguration?.language)
    }

    private open class FakeRecognizer(
        private val result: String,
    ) : SpeechRecognizer {
        var startCount = 0

        override fun recognize(): Flow<SpeechRecognitionEvent> {
            startCount += 1
            return flowOf(SpeechRecognitionEvent.Final(result))
        }

        override suspend fun stop() = Unit
    }

    private class FakeSenseVoiceRecognizer(
        result: String,
    ) : FakeRecognizer(result), ConfigurableSenseVoiceSpeechRecognizer {
        var lastConfiguration: SenseVoiceConfiguration? = null

        override fun recognize(configuration: SenseVoiceConfiguration): Flow<SpeechRecognitionEvent> {
            lastConfiguration = configuration
            return recognize()
        }
    }
}
