package com.example.interviewassistant.feature.interviewassistant.data.repository

import com.example.interviewassistant.feature.interviewassistant.domain.repository.SpeechModelManager
import com.example.interviewassistant.feature.interviewassistant.domain.repository.SpeechModelState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Model manager used on platforms that do not yet support local SenseVoice inference.
 */
class UnavailableSpeechModelManager : SpeechModelManager {
    override val descriptor = null
    override val state: StateFlow<SpeechModelState> = MutableStateFlow(SpeechModelState.Unavailable)

    override suspend fun install() = Unit

    override suspend fun delete() = Unit
}
