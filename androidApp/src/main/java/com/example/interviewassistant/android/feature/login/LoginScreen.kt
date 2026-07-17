package com.example.interviewassistant.android.feature.login

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.example.interviewassistant.android.R
import com.example.interviewassistant.core.design.theme.AppDesign
import com.example.interviewassistant.feature.login.presentation.state.LoginUiEffect
import com.example.interviewassistant.feature.login.presentation.state.LoginUiEvent
import com.example.interviewassistant.feature.login.presentation.viewmodel.LoginViewModel
import kotlinx.coroutines.flow.collectLatest
import org.koin.core.context.GlobalContext

/**
 * Android Login sample screen.
 */
@Composable
fun LoginScreen() {
    val viewModel: LoginViewModel = remember { GlobalContext.get().get() }
    val state by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val keyboardController = LocalSoftwareKeyboardController.current

    LaunchedEffect(Unit) {
        viewModel.effect.collectLatest { effect ->
            when (effect) {
                is LoginUiEffect.NavigateToHome -> {
                    // Navigate to home when a destination exists
                }
                is LoginUiEffect.ShowToast -> {
                    snackbarHostState.showSnackbar(effect.message)
                }
            }
        }
    }

    Scaffold(
        containerColor = AppDesign.colors.pageBackground,
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(AppDesign.spacing.lg),
            contentAlignment = Alignment.Center,
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth().widthIn(max = 440.dp),
                shape = MaterialTheme.shapes.extraLarge,
                color = AppDesign.colors.surface,
                shadowElevation = 2.dp,
            ) {
                Column(
                    modifier = Modifier.padding(AppDesign.spacing.xxl),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Surface(
                        shape = MaterialTheme.shapes.extraLarge,
                        color = AppDesign.colors.brandSubtle,
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_assistant),
                            contentDescription = null,
                            modifier = Modifier.padding(AppDesign.spacing.lg).size(32.dp),
                            tint = AppDesign.colors.brand,
                        )
                    }
                    Spacer(modifier = Modifier.height(AppDesign.spacing.lg))
                    Text(
                        text = stringResource(R.string.login_title),
                        style = AppDesign.typography.pageTitle,
                    )
                    Spacer(modifier = Modifier.height(AppDesign.spacing.xl))
                    OutlinedTextField(
                        value = state.username,
                        onValueChange = { viewModel.onEvent(LoginUiEvent.OnUsernameChanged(it)) },
                        label = { Text(stringResource(R.string.login_username)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                if (state.username.isNotBlank() && !state.isLoading) {
                                    keyboardController?.hide()
                                    viewModel.onEvent(LoginUiEvent.OnLoginClicked)
                                }
                            },
                        ),
                    )
                    Spacer(modifier = Modifier.height(AppDesign.spacing.lg))
                    Button(
                        onClick = {
                            keyboardController?.hide()
                            viewModel.onEvent(LoginUiEvent.OnLoginClicked)
                        },
                        enabled = state.username.isNotBlank() && !state.isLoading,
                        modifier = Modifier.fillMaxWidth().height(AppDesign.sizes.buttonLargeHeight),
                    ) {
                        if (state.isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(22.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp,
                            )
                        } else {
                            Text(stringResource(R.string.login_btn))
                        }
                    }
                    state.error?.let { error ->
                        Text(
                            text = error,
                            color = MaterialTheme.colorScheme.error,
                            style = AppDesign.typography.caption,
                            modifier = Modifier.fillMaxWidth().padding(top = AppDesign.spacing.sm),
                        )
                    }
                }
            }
        }
    }
}
