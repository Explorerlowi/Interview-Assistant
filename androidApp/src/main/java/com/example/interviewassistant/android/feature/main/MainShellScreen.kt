package com.example.interviewassistant.android.feature.main

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.interviewassistant.android.R
import com.example.interviewassistant.android.feature.interviewassistant.AssistantDashboardScreen
import com.example.interviewassistant.android.feature.interviewassistant.InterviewWorkspaceScreen
import com.example.interviewassistant.android.feature.interviewassistant.ProviderSettingsScreen
import com.example.interviewassistant.core.design.theme.AppDesign
import com.example.interviewassistant.core.i18n.AppStringId
import com.example.interviewassistant.core.i18n.StringsProvider
import com.example.interviewassistant.feature.interviewassistant.presentation.state.InterviewSessionUiEffect
import com.example.interviewassistant.feature.interviewassistant.presentation.state.InterviewSessionUiEvent
import com.example.interviewassistant.feature.interviewassistant.presentation.state.ProviderSettingsUiEffect
import com.example.interviewassistant.feature.interviewassistant.presentation.state.ResumeLibraryUiEffect
import com.example.interviewassistant.feature.interviewassistant.presentation.viewmodel.InterviewSessionViewModel
import com.example.interviewassistant.feature.interviewassistant.presentation.viewmodel.ProviderSettingsViewModel
import com.example.interviewassistant.feature.interviewassistant.presentation.viewmodel.ResumeLibraryViewModel
import com.example.interviewassistant.feature.interviewassistant.presentation.viewmodel.SessionHistoryViewModel
import org.koin.core.context.GlobalContext

private enum class MainDestination(
    val labelId: AppStringId,
    val iconRes: Int,
) {
    ASSISTANT(AppStringId.NAV_ASSISTANT, R.drawable.ic_assistant),
    MOCK_INTERVIEW(AppStringId.NAV_MOCK_INTERVIEW, R.drawable.ic_mock_interview),
    SETTINGS(AppStringId.NAV_SETTINGS, R.drawable.ic_settings),
}

/**
 * Android root shell for the two product modules and provider settings.
 */
@Composable
fun MainShellScreen(
    strings: StringsProvider = remember { GlobalContext.get().get() },
) {
    var destination by remember { mutableStateOf(MainDestination.ASSISTANT) }
    var showWorkspace by remember { mutableStateOf(false) }
    var previousWorkspaceVisibility by remember { mutableStateOf(showWorkspace) }
    var workspaceAnimationRunning by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val resumeViewModel = remember { GlobalContext.get().get<ResumeLibraryViewModel>() }
    val historyViewModel = remember { GlobalContext.get().get<SessionHistoryViewModel>() }
    val sessionViewModel = remember { GlobalContext.get().get<InterviewSessionViewModel>() }
    val settingsViewModel = remember { GlobalContext.get().get<ProviderSettingsViewModel>() }
    val resumeState by resumeViewModel.uiState.collectAsState()
    val historyState by historyViewModel.uiState.collectAsState()
    val sessionState by sessionViewModel.uiState.collectAsState()
    val settingsState by settingsViewModel.uiState.collectAsState()

    LaunchedEffect(sessionViewModel) {
        sessionViewModel.effect.collect { effect ->
            if (effect == InterviewSessionUiEffect.SessionCompleted) showWorkspace = false
        }
    }
    LaunchedEffect(showWorkspace) {
        if (previousWorkspaceVisibility != showWorkspace) {
            previousWorkspaceVisibility = showWorkspace
            workspaceAnimationRunning = true
            kotlinx.coroutines.delay(300)
            workspaceAnimationRunning = false
        }
    }
    LaunchedEffect(settingsViewModel) {
        settingsViewModel.effect.collect { effect ->
            if (effect == ProviderSettingsUiEffect.Saved) {
                snackbarHostState.showSnackbar(strings.get(AppStringId.SETTINGS_SAVED))
            }
        }
    }
    LaunchedEffect(resumeViewModel) {
        resumeViewModel.effect.collect { effect ->
            if (effect is ResumeLibraryUiEffect.ImportCompleted) {
                snackbarHostState.showSnackbar(strings.get(AppStringId.IMPORT_RESUME_COMPLETED))
            }
            if (effect is ResumeLibraryUiEffect.OcrTextSaved) {
                snackbarHostState.showSnackbar(strings.get(AppStringId.RESUME_SAVED))
            }
        }
    }
    DisposableEffect(Unit) {
        onDispose {
            resumeViewModel.onCleared()
            historyViewModel.onCleared()
            sessionViewModel.onCleared()
            settingsViewModel.onCleared()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            containerColor = AppDesign.colors.pageBackground,
            snackbarHost = { SnackbarHost(snackbarHostState) },
            bottomBar = {
                NavigationBar(
                    containerColor = AppDesign.colors.surfaceElevated,
                    tonalElevation = 0.dp,
                ) {
                    MainDestination.entries.forEach { item ->
                        val label = strings.get(item.labelId)
                        NavigationBarItem(
                            selected = destination == item,
                            onClick = { destination = item },
                            icon = {
                                Icon(
                                    painter = painterResource(item.iconRes),
                                    contentDescription = label,
                                    modifier = Modifier.size(22.dp),
                                )
                            },
                            label = { Text(label, style = AppDesign.typography.caption) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = AppDesign.colors.brand,
                                selectedTextColor = AppDesign.colors.brand,
                                indicatorColor = AppDesign.colors.brandSubtle,
                                unselectedIconColor = AppDesign.colors.textTertiary,
                                unselectedTextColor = AppDesign.colors.textSecondary,
                            ),
                        )
                    }
                }
            },
        ) { padding ->
            Box(modifier = Modifier.fillMaxSize().padding(padding)) {
                Crossfade(
                    targetState = destination,
                    animationSpec = tween(220),
                    label = "main_destination",
                ) { currentDestination ->
                    when (currentDestination) {
                        MainDestination.ASSISTANT -> AssistantDashboardScreen(
                            resumeState = resumeState,
                            historyState = historyState,
                            strings = strings,
                            onResumeEvent = resumeViewModel::onEvent,
                            onHistoryEvent = historyViewModel::onEvent,
                            onStartSession = { resume ->
                                sessionViewModel.onEvent(
                                    InterviewSessionUiEvent.StartSession(
                                        resumeId = resume.id,
                                        title = resume.displayName,
                                        triggerMode = settingsState.configuration.answerTriggerMode,
                                    ),
                                )
                                showWorkspace = true
                            },
                            onOpenSession = { session ->
                                sessionViewModel.onEvent(InterviewSessionUiEvent.OpenSession(session.id))
                                showWorkspace = true
                            },
                        )
                        MainDestination.MOCK_INTERVIEW -> ShellMessage(
                            padding = PaddingValues(),
                            title = strings.get(AppStringId.MOCK_PLACEHOLDER_TITLE),
                            description = strings.get(AppStringId.MOCK_PLACEHOLDER_DESCRIPTION),
                        )
                        MainDestination.SETTINGS -> ProviderSettingsScreen(
                            state = settingsState,
                            strings = strings,
                            onEvent = settingsViewModel::onEvent,
                        )
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = showWorkspace,
            enter = slideInHorizontally(
                initialOffsetX = { it },
                animationSpec = tween(300),
            ) + fadeIn(animationSpec = tween(300)),
            exit = slideOutHorizontally(
                targetOffsetX = { it },
                animationSpec = tween(300),
            ) + fadeOut(animationSpec = tween(300)),
        ) {
            InterviewWorkspaceScreen(
                state = sessionState,
                strings = strings,
                onEvent = sessionViewModel::onEvent,
                onNavigateBack = {
                    sessionViewModel.onEvent(InterviewSessionUiEvent.LeaveWorkspace)
                    showWorkspace = false
                },
            )
        }
        if (workspaceAnimationRunning) {
            Box(
                modifier = Modifier.fillMaxSize().pointerInput(Unit) {
                    awaitPointerEventScope {
                        while (true) {
                            awaitPointerEvent().changes.forEach { it.consume() }
                        }
                    }
                },
            )
        }
    }
}

@Composable
private fun ShellMessage(
    padding: PaddingValues,
    title: String,
    description: String,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .padding(AppDesign.spacing.xxl),
        verticalArrangement = Arrangement.spacedBy(AppDesign.spacing.md, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .background(AppDesign.colors.brandSubtle, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_mock_interview),
                contentDescription = null,
                modifier = Modifier.size(32.dp),
                tint = AppDesign.colors.brand,
            )
        }
        Text(text = title, style = AppDesign.typography.pageTitle)
        Text(
            text = description,
            style = AppDesign.typography.body,
            color = AppDesign.colors.textSecondary,
            textAlign = TextAlign.Center,
        )
    }
}
