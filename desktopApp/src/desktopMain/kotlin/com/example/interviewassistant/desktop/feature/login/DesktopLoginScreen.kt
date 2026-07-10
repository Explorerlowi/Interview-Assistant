package com.example.interviewassistant.desktop.feature.login

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.interviewassistant.core.design.theme.AppDesign
import com.example.interviewassistant.core.i18n.AppStringId
import com.example.interviewassistant.core.i18n.StringsProvider
import com.example.interviewassistant.feature.login.presentation.state.LoginUiEffect
import com.example.interviewassistant.feature.login.presentation.state.LoginUiEvent
import com.example.interviewassistant.feature.login.presentation.viewmodel.LoginViewModel
import kotlinx.coroutines.flow.collectLatest
import org.koin.core.context.GlobalContext

/**
 * Desktop Login sample screen wired to the shared [LoginViewModel].
 */
@Composable
fun DesktopLoginScreen() {
    val viewModel: LoginViewModel = remember { GlobalContext.get().get() }
    val strings: StringsProvider = remember { GlobalContext.get().get() }
    val state by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.effect.collectLatest { effect ->
            when (effect) {
                is LoginUiEffect.NavigateToHome -> {
                    snackbarHostState.showSnackbar("Signed in")
                }
                is LoginUiEffect.ShowToast -> {
                    snackbarHostState.showSnackbar(effect.message)
                }
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(AppDesign.spacing.lg),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = strings.get(AppStringId.LOGIN_TITLE),
                style = AppDesign.typography.pageTitle,
            )
            Spacer(modifier = Modifier.height(AppDesign.spacing.xl))
            OutlinedTextField(
                value = state.username,
                onValueChange = { viewModel.onEvent(LoginUiEvent.OnUsernameChanged(it)) },
                label = { Text(strings.get(AppStringId.LOGIN_USERNAME)) },
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(AppDesign.spacing.lg))
            Button(
                onClick = { viewModel.onEvent(LoginUiEvent.OnLoginClicked) },
                enabled = !state.isLoading,
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (state.isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                } else {
                    Text(strings.get(AppStringId.LOGIN_BTN))
                }
            }
            state.error?.let { error ->
                Text(
                    text = error,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = AppDesign.spacing.sm),
                )
            }
        }
    }
}
