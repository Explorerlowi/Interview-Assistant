package com.example.interviewassistant.feature.login.presentation.state

data class LoginUiState(
    val isLoading: Boolean = false,
    val username: String = "",
    val error: String? = null,
    val isLoggedIn: Boolean = false
)

sealed class LoginUiEvent {
    data class OnUsernameChanged(val value: String) : LoginUiEvent()
    object OnLoginClicked : LoginUiEvent()
}

sealed class LoginUiEffect {
    object NavigateToHome : LoginUiEffect()
    data class ShowToast(val message: String) : LoginUiEffect()
}
