package com.example.interviewassistant.desktop.feature.interviewassistant

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.unit.dp
import com.example.interviewassistant.core.design.theme.AppDesign
import com.example.interviewassistant.core.design.util.noRippleClickable
import com.example.interviewassistant.core.i18n.AppStringId
import com.example.interviewassistant.core.i18n.StringsProvider
import com.example.interviewassistant.feature.interviewassistant.domain.model.AnswerTriggerMode
import com.example.interviewassistant.feature.interviewassistant.presentation.state.InterviewSessionUiEvent
import com.example.interviewassistant.feature.interviewassistant.presentation.state.InterviewSessionUiState

/**
 * Desktop two-pane workspace with app-scoped keyboard controls and compact mode.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DesktopInterviewWorkspace(
    state: InterviewSessionUiState,
    strings: StringsProvider,
    compactMode: Boolean,
    alwaysOnTop: Boolean,
    onCompactModeChange: (Boolean) -> Unit,
    onAlwaysOnTopChange: (Boolean) -> Unit,
    onEvent: (InterviewSessionUiEvent) -> Unit,
    onNavigateBack: () -> Unit,
) {
    val focusRequester = FocusRequester()
    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .focusRequester(focusRequester)
            .focusable()
            .onPreviewKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown || !event.isCtrlPressed) return@onPreviewKeyEvent false
                when (event.key) {
                    Key.Enter -> {
                        onEvent(InterviewSessionUiEvent.GenerateAnswer)
                        true
                    }
                    Key.L -> {
                        onEvent(
                            if (state.isListening) {
                                InterviewSessionUiEvent.StopListening
                            } else {
                                InterviewSessionUiEvent.StartListening
                            },
                        )
                        true
                    }
                    else -> false
                }
            },
        topBar = {
            TopAppBar(
                title = { Text(state.session?.title ?: strings.get(AppStringId.ASSISTANT_TITLE)) },
                navigationIcon = {
                    TextButton(onClick = onNavigateBack) {
                        Text(strings.get(AppStringId.COMMON_BACK))
                    }
                },
                actions = {
                    DesktopWindowToggle(
                        label = strings.get(AppStringId.DESKTOP_COMPACT_MODE),
                        checked = compactMode,
                        onCheckedChange = onCompactModeChange,
                    )
                    DesktopWindowToggle(
                        label = strings.get(AppStringId.DESKTOP_ALWAYS_ON_TOP),
                        checked = alwaysOnTop,
                        onCheckedChange = onAlwaysOnTopChange,
                    )
                    TextButton(onClick = { onEvent(InterviewSessionUiEvent.CompleteSession) }) {
                        Text(strings.get(AppStringId.COMPLETE_SESSION))
                    }
                },
            )
        },
        bottomBar = {
            DesktopWorkspaceControls(state, strings, onEvent)
        },
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(AppDesign.spacing.lg),
            verticalArrangement = Arrangement.spacedBy(AppDesign.spacing.md),
        ) {
            DesktopTriggerSelector(state.triggerMode, strings, onEvent)
            state.errorMessage?.let {
                Text(it, color = MaterialTheme.colorScheme.error)
            }
            if (compactMode) {
                DesktopAnswerPanel(
                    state = state,
                    strings = strings,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.spacedBy(AppDesign.spacing.lg),
                ) {
                    DesktopTranscriptPanel(
                        state = state,
                        strings = strings,
                        onEvent = onEvent,
                        modifier = Modifier.weight(1f),
                    )
                    DesktopAnswerPanel(
                        state = state,
                        strings = strings,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun DesktopWindowToggle(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row {
        Checkbox(checked = checked, onCheckedChange = onCheckedChange)
        Text(label, modifier = Modifier.padding(top = AppDesign.spacing.sm))
    }
}

@Composable
private fun DesktopTriggerSelector(
    selected: AnswerTriggerMode,
    strings: StringsProvider,
    onEvent: (InterviewSessionUiEvent) -> Unit,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(AppDesign.spacing.sm)) {
        AnswerTriggerMode.entries.forEach { mode ->
            val label = strings.get(
                if (mode == AnswerTriggerMode.MANUAL) {
                    AppStringId.TRIGGER_MANUAL
                } else {
                    AppStringId.TRIGGER_AUTOMATIC
                },
            )
            Surface(
                modifier = Modifier.noRippleClickable {
                    onEvent(InterviewSessionUiEvent.SetTriggerMode(mode))
                },
                shape = MaterialTheme.shapes.large,
                color = if (selected == mode) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.surface
                },
                border = BorderStroke(
                    AppDesign.spacing.xxs,
                    if (selected == mode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                ),
            ) {
                Text(
                    label,
                    modifier = Modifier.padding(
                        horizontal = AppDesign.spacing.lg,
                        vertical = AppDesign.spacing.sm,
                    ),
                )
            }
        }
    }
}

@Composable
private fun DesktopTranscriptPanel(
    state: InterviewSessionUiState,
    strings: StringsProvider,
    onEvent: (InterviewSessionUiEvent) -> Unit,
    modifier: Modifier,
) {
    Card(
        modifier = modifier.fillMaxSize(),
        colors = CardDefaults.cardColors(containerColor = AppDesign.colors.surfaceMuted),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(AppDesign.spacing.lg).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(AppDesign.spacing.md),
        ) {
            Text(strings.get(AppStringId.WORKSPACE_TRANSCRIPT), style = AppDesign.typography.sectionTitle)
            val transcript = state.transcripts.joinToString(separator = "\n") { it.text }
            Text(
                text = transcript.ifBlank { strings.get(AppStringId.TRANSCRIPT_EMPTY) },
                style = AppDesign.typography.body,
                color = if (transcript.isBlank()) {
                    AppDesign.colors.textSecondary
                } else {
                    AppDesign.colors.textPrimary
                },
            )
            if (state.liveTranscript.isNotBlank()) {
                Text(state.liveTranscript, color = MaterialTheme.colorScheme.primary)
            }
            OutlinedTextField(
                value = state.currentQuestion,
                onValueChange = { onEvent(InterviewSessionUiEvent.UpdateQuestion(it)) },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                label = { Text(strings.get(AppStringId.WORKSPACE_TRANSCRIPT)) },
            )
        }
    }
}

@Composable
private fun DesktopAnswerPanel(
    state: InterviewSessionUiState,
    strings: StringsProvider,
    modifier: Modifier,
) {
    Card(
        modifier = modifier.fillMaxSize(),
        colors = CardDefaults.cardColors(containerColor = AppDesign.colors.surfaceMuted),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(AppDesign.spacing.lg).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(AppDesign.spacing.md),
        ) {
            Text(strings.get(AppStringId.WORKSPACE_ANSWER), style = AppDesign.typography.sectionTitle)
            val answer = state.streamingAnswer.ifBlank { state.answers.lastOrNull()?.content.orEmpty() }
            Text(
                text = answer.ifBlank { strings.get(AppStringId.ANSWER_EMPTY) },
                style = AppDesign.typography.body,
                color = if (answer.isBlank()) {
                    AppDesign.colors.textSecondary
                } else {
                    AppDesign.colors.textPrimary
                },
            )
        }
    }
}

@Composable
private fun DesktopWorkspaceControls(
    state: InterviewSessionUiState,
    strings: StringsProvider,
    onEvent: (InterviewSessionUiEvent) -> Unit,
) {
    Surface(tonalElevation = AppDesign.spacing.xs) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(AppDesign.spacing.md),
            horizontalArrangement = Arrangement.spacedBy(AppDesign.spacing.sm),
        ) {
            Button(
                onClick = {
                    onEvent(
                        if (state.isListening) {
                            InterviewSessionUiEvent.StopListening
                        } else {
                            InterviewSessionUiEvent.StartListening
                        },
                    )
                },
            ) {
                Text(
                    strings.get(
                        if (state.isListening) AppStringId.STOP_LISTENING else AppStringId.START_LISTENING,
                    ),
                )
            }
            Button(
                enabled = state.currentQuestion.isNotBlank(),
                onClick = {
                    onEvent(
                        if (state.isGenerating) {
                            InterviewSessionUiEvent.CancelGeneration
                        } else {
                            InterviewSessionUiEvent.GenerateAnswer
                        },
                    )
                },
            ) {
                Text(
                    strings.get(
                        if (state.isGenerating) {
                            AppStringId.CANCEL_GENERATION
                        } else {
                            AppStringId.GENERATE_ANSWER
                        },
                    ),
                )
            }
        }
    }
}
