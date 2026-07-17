package com.example.interviewassistant.di

import com.example.interviewassistant.core.audio.AudioSource
import com.example.interviewassistant.core.audio.DesktopLoopbackAudioSource
import com.example.interviewassistant.core.audio.DesktopMicrophoneAudioSource
import com.example.interviewassistant.feature.interviewassistant.data.repository.OnDeviceSpeechGateway
import com.example.interviewassistant.feature.interviewassistant.domain.repository.SpeechModelManager
import com.example.interviewassistant.feature.interviewassistant.domain.usecase.TestSpeechRecognitionUseCase
import com.example.interviewassistant.feature.interviewassistant.speech.DesktopSenseVoiceModelManager
import com.example.interviewassistant.feature.interviewassistant.speech.DesktopSenseVoiceSpeechGateway
import kotlin.test.Test
import kotlin.test.assertIs
import kotlin.test.assertNotSame
import org.koin.core.qualifier.named
import org.koin.dsl.koinApplication

class DesktopSpeechBindingsTest {
    @Test
    fun desktopUsesLoopbackForSessionsAndMicrophoneForSettingsTests() {
        val application = koinApplication {
            modules(appModule(), platformModule())
        }
        try {
            val koin = application.koin
            assertIs<DesktopLoopbackAudioSource>(koin.get<AudioSource>(named(SESSION_AUDIO_SOURCE)))
            assertIs<DesktopMicrophoneAudioSource>(koin.get<AudioSource>(named(TEST_AUDIO_SOURCE)))
            assertIs<DesktopSenseVoiceModelManager>(koin.get<SpeechModelManager>())

            val sessionGateway = koin.get<OnDeviceSpeechGateway>(named(SESSION_SENSEVOICE_GATEWAY))
            val testGateway = koin.get<OnDeviceSpeechGateway>(named(TEST_SENSEVOICE_GATEWAY))
            assertIs<DesktopSenseVoiceSpeechGateway>(sessionGateway)
            assertIs<DesktopSenseVoiceSpeechGateway>(testGateway)
            assertNotSame(sessionGateway, testGateway)
            koin.get<TestSpeechRecognitionUseCase>()
        } finally {
            application.close()
        }
    }
}
