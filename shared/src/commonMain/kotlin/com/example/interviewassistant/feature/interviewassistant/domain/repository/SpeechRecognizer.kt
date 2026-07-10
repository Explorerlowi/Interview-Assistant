package com.example.interviewassistant.feature.interviewassistant.domain.repository

import com.example.interviewassistant.feature.interviewassistant.data.remote.speech.SpeechRecognitionEvent
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
