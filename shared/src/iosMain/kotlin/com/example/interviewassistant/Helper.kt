package com.example.interviewassistant

import com.example.interviewassistant.di.initKoin
import com.example.interviewassistant.feature.login.presentation.viewmodel.LoginViewModel
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

fun doInitKoin() {
    initKoin()
}

class LoginViewModelHelper : KoinComponent {
    private val viewModel: LoginViewModel by inject()
    fun getLoginViewModel(): LoginViewModel = viewModel
}
