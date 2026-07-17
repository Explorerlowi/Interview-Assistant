package com.example.interviewassistant.feature.interviewassistant.domain.usecase

import com.example.interviewassistant.feature.interviewassistant.data.remote.speech.SpeechRecognitionEvent
import com.example.interviewassistant.feature.interviewassistant.domain.model.SpeechRecognitionMode
import com.example.interviewassistant.feature.interviewassistant.domain.model.SenseVoiceConfiguration
import com.example.interviewassistant.feature.interviewassistant.domain.repository.ConfigurableSenseVoiceSpeechRecognizer
import com.example.interviewassistant.feature.interviewassistant.domain.repository.SpeechRecognizer
import kotlinx.coroutines.flow.Flow

/**
 * Runs a microphone-backed recognition session for the provider selected in settings.
 *
 * @param xunfeiRecognizer Recognizer bound specifically to iFlytek streaming dictation.
 * @param senseVoiceRecognizer Recognizer bound specifically to on-device SenseVoice.
 */
class TestSpeechRecognitionUseCase(
    private val xunfeiRecognizer: SpeechRecognizer,
    private val senseVoiceRecognizer: ConfigurableSenseVoiceSpeechRecognizer,
) {
    /**
     * Starts the requested recognition backend without changing the saved provider selection.
     *
     * @param mode Provider to test.
     * @return Partial, final, or failure recognition events.
     */
    operator fun invoke(
        mode: SpeechRecognitionMode,
        senseVoiceConfiguration: SenseVoiceConfiguration,
    ): Flow<SpeechRecognitionEvent> {
        return when (mode) {
            SpeechRecognitionMode.XUNFEI -> xunfeiRecognizer.recognize()
            SpeechRecognitionMode.SENSE_VOICE_ON_DEVICE -> {
                senseVoiceRecognizer.recognize(senseVoiceConfiguration)
            }
        }
    }
}
