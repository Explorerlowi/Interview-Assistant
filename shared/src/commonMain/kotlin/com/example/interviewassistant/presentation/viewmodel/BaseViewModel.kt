package com.example.interviewassistant.presentation.viewmodel

import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import com.example.interviewassistant.core.util.CoroutineDispatcherProvider

abstract class BaseViewModel<STATE, EVENT, EFFECT>(
    initialState: STATE,
    protected val dispatcherProvider: CoroutineDispatcherProvider
) {
    private val viewModelJob = SupervisorJob()
    protected val viewModelScope = CoroutineScope(dispatcherProvider.main + viewModelJob)

    abstract val uiState: StateFlow<STATE>
    abstract val effect: SharedFlow<EFFECT>

    abstract fun onEvent(event: EVENT)

    open fun onCleared() {
        viewModelJob.cancel()
    }
}
