package com.example.interviewassistant.feature.interviewassistant.domain.repository

import com.example.interviewassistant.feature.interviewassistant.data.remote.speech.SpeechRecognitionEvent
import com.example.interviewassistant.feature.interviewassistant.domain.model.SenseVoiceConfiguration
import kotlinx.coroutines.flow.Flow

/**
 * Continuously captures platform audio and reconnects provider sessions as needed.
 */
interface SpeechRecognizer {
    /** Starts recognition and emits updates until collection is cancelled. */
    fun recognize(): Flow<SpeechRecognitionEvent>

    /** Stops platform audio capture. */
    suspend fun stop()
}

/**
 * On-device recognizer that can run with an explicit SenseVoice configuration.
 */
interface ConfigurableSenseVoiceSpeechRecognizer : SpeechRecognizer {
    /**
     * Starts recognition with the supplied language and decoding options.
     *
     * @param configuration SenseVoice configuration for this session only.
     * @return Recognition updates until collection is cancelled.
     */
    fun recognize(configuration: SenseVoiceConfiguration): Flow<SpeechRecognitionEvent>
}
