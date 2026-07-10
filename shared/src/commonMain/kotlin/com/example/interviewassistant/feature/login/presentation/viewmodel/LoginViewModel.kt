package com.example.interviewassistant.feature.login.presentation.viewmodel

import com.example.interviewassistant.core.error.AppResult
import com.example.interviewassistant.core.util.CoroutineDispatcherProvider
import com.example.interviewassistant.feature.login.domain.usecase.LoginUseCase
import com.example.interviewassistant.feature.login.presentation.state.LoginUiEffect
import com.example.interviewassistant.feature.login.presentation.state.LoginUiEvent
import com.example.interviewassistant.feature.login.presentation.state.LoginUiState
import com.example.interviewassistant.presentation.viewmodel.BaseViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel for the Login sample feature.
 */
class LoginViewModel(
    private val loginUseCase: LoginUseCase,
    dispatcherProvider: CoroutineDispatcherProvider,
) : BaseViewModel<LoginUiState, LoginUiEvent, LoginUiEffect>(
    initialState = LoginUiState(),
    dispatcherProvider = dispatcherProvider,
) {

    private val _uiState = MutableStateFlow(LoginUiState())
    override val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    private val _effect = MutableSharedFlow<LoginUiEffect>()
    override val effect: SharedFlow<LoginUiEffect> = _effect.asSharedFlow()

    override fun onEvent(event: LoginUiEvent) {
        when (event) {
            is LoginUiEvent.OnUsernameChanged -> {
                _uiState.update { it.copy(username = event.value, error = null) }
            }
            LoginUiEvent.OnLoginClicked -> login()
        }
    }

    private fun login() {
        val username = uiState.value.username
        if (username.isBlank()) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            when (val result = loginUseCase(username)) {
                is AppResult.Success -> {
                    _uiState.update { it.copy(isLoading = false, isLoggedIn = true) }
                    _effect.emit(LoginUiEffect.NavigateToHome)
                }
                is AppResult.Error -> {
                    val message = result.error.message ?: "Error"
                    _uiState.update { it.copy(isLoading = false, error = message) }
                    _effect.emit(LoginUiEffect.ShowToast(message))
                }
            }
        }
    }
}
