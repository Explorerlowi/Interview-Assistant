package com.example.interviewassistant.desktop.feature.main

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.unit.dp
import com.example.interviewassistant.core.design.theme.AppDesign
import com.example.interviewassistant.core.i18n.AppStringId
import com.example.interviewassistant.core.i18n.StringsProvider
import com.example.interviewassistant.desktop.feature.interviewassistant.DesktopAssistantDashboard
import com.example.interviewassistant.desktop.feature.interviewassistant.DesktopInterviewWorkspace
import com.example.interviewassistant.desktop.feature.interviewassistant.DesktopProviderSettings
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import com.example.interviewassistant.feature.interviewassistant.presentation.state.InterviewSessionUiEffect
import com.example.interviewassistant.feature.interviewassistant.presentation.state.InterviewSessionUiEvent
import com.example.interviewassistant.feature.interviewassistant.presentation.state.ProviderSettingsUiEffect
import com.example.interviewassistant.feature.interviewassistant.presentation.state.ResumeLibraryUiEffect
import com.example.interviewassistant.feature.interviewassistant.presentation.viewmodel.InterviewSessionViewModel
import com.example.interviewassistant.feature.interviewassistant.presentation.viewmodel.ProviderSettingsViewModel
import com.example.interviewassistant.feature.interviewassistant.presentation.viewmodel.ResumeLibraryViewModel
import com.example.interviewassistant.feature.interviewassistant.presentation.viewmodel.SessionHistoryViewModel
import org.koin.core.context.GlobalContext

private enum class DesktopDestination(
    val labelId: AppStringId,
) {
    ASSISTANT(AppStringId.NAV_ASSISTANT),
    MOCK_INTERVIEW(AppStringId.NAV_MOCK_INTERVIEW),
    SETTINGS(AppStringId.NAV_SETTINGS),
}

/**
 * Desktop root shell with a persistent product-module sidebar.
 */
@Composable
fun DesktopMainShell(
    strings: StringsProvider = remember { GlobalContext.get().get() },
    compactMode: Boolean,
    alwaysOnTop: Boolean,
    onCompactModeChange: (Boolean) -> Unit,
    onAlwaysOnTopChange: (Boolean) -> Unit,
) {
    var destination by remember { mutableStateOf(DesktopDestination.ASSISTANT) }
    var showWorkspace by remember { mutableStateOf(false) }
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
        Row(modifier = Modifier.fillMaxSize()) {
            Surface(
                tonalElevation = 2.dp,
                modifier = Modifier.width(220.dp).fillMaxHeight(),
            ) {
                Column(
                    modifier = Modifier.padding(AppDesign.spacing.lg),
                    verticalArrangement = Arrangement.spacedBy(AppDesign.spacing.sm),
                ) {
                    Text(strings.get(AppStringId.APP_TITLE), style = AppDesign.typography.sectionTitle)
                    HorizontalDivider()
                    DesktopDestination.entries.forEach { item ->
                        TextButton(onClick = { destination = item }) {
                            Text(
                                text = strings.get(item.labelId),
                                color = if (destination == item) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurface
                                },
                            )
                        }
                    }
                }
            }
            Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                when (destination) {
                    DesktopDestination.ASSISTANT -> DesktopAssistantDashboard(
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
                    DesktopDestination.MOCK_INTERVIEW -> DesktopPlaceholder(
                        title = strings.get(AppStringId.MOCK_PLACEHOLDER_TITLE),
                        description = strings.get(AppStringId.MOCK_PLACEHOLDER_DESCRIPTION),
                    )
                    DesktopDestination.SETTINGS -> DesktopProviderSettings(
                        state = settingsState,
                        strings = strings,
                        onEvent = settingsViewModel::onEvent,
                    )
                }
                SnackbarHost(
                    hostState = snackbarHostState,
                    modifier = Modifier.align(Alignment.BottomCenter),
                )
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
            DesktopInterviewWorkspace(
                state = sessionState,
                strings = strings,
                compactMode = compactMode,
                alwaysOnTop = alwaysOnTop,
                onCompactModeChange = onCompactModeChange,
                onAlwaysOnTopChange = onAlwaysOnTopChange,
                onEvent = sessionViewModel::onEvent,
                onNavigateBack = {
                    sessionViewModel.onEvent(InterviewSessionUiEvent.LeaveWorkspace)
                    showWorkspace = false
                },
            )
        }
    }
}

@Composable
private fun DesktopPlaceholder(
    title: String,
    description: String,
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(AppDesign.spacing.xxl),
        verticalArrangement = Arrangement.spacedBy(AppDesign.spacing.md, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(title, style = AppDesign.typography.pageTitle)
        Text(description, style = AppDesign.typography.body)
    }
}
