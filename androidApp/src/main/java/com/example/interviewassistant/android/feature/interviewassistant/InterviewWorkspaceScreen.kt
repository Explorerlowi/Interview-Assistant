package com.example.interviewassistant.android.feature.interviewassistant

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.interviewassistant.core.design.theme.AppDesign
import com.example.interviewassistant.core.design.util.noRippleClickable
import com.example.interviewassistant.core.i18n.AppStringId
import com.example.interviewassistant.core.i18n.StringsProvider
import com.example.interviewassistant.feature.interviewassistant.domain.model.AnswerTriggerMode
import com.example.interviewassistant.feature.interviewassistant.presentation.state.InterviewSessionUiEvent
import com.example.interviewassistant.feature.interviewassistant.presentation.state.InterviewSessionUiState

/**
 * Full-screen Android workspace for recognition and answer generation.
 */
@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun InterviewWorkspaceScreen(
    state: InterviewSessionUiState,
    strings: StringsProvider,
    onEvent: (InterviewSessionUiEvent) -> Unit,
    onNavigateBack: () -> Unit,
) {
    BackHandler { onNavigateBack() }
    val context = LocalContext.current
    var permissionDenied by remember { mutableStateOf(false) }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        permissionDenied = !granted
        if (granted) onEvent(InterviewSessionUiEvent.StartListening)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(state.session?.title ?: strings.get(AppStringId.ASSISTANT_TITLE)) },
                navigationIcon = {
                    TextButton(onClick = onNavigateBack) {
                        Text(strings.get(AppStringId.COMMON_BACK))
                    }
                },
                actions = {
                    TextButton(onClick = { onEvent(InterviewSessionUiEvent.CompleteSession) }) {
                        Text(strings.get(AppStringId.COMPLETE_SESSION))
                    }
                },
            )
        },
        bottomBar = {
            WorkspaceControls(
                state = state,
                strings = strings,
                onListen = {
                    if (context.checkSelfPermission(Manifest.permission.RECORD_AUDIO) ==
                        PackageManager.PERMISSION_GRANTED
                    ) {
                        onEvent(InterviewSessionUiEvent.StartListening)
                    } else {
                        permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    }
                },
                onEvent = onEvent,
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(AppDesign.spacing.lg)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(AppDesign.spacing.md),
        ) {
            TriggerModeSelector(state.triggerMode, strings, onEvent)
            if (permissionDenied) {
                Text(
                    strings.get(AppStringId.ERROR_PERMISSION_MICROPHONE),
                    color = MaterialTheme.colorScheme.error,
                )
            }
            state.errorMessage?.let {
                Text(it, color = MaterialTheme.colorScheme.error)
            }
            TranscriptPanel(state, strings, onEvent)
            AnswerPanel(state, strings)
        }
    }
}

@Composable
private fun TriggerModeSelector(
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
private fun TranscriptPanel(
    state: InterviewSessionUiState,
    strings: StringsProvider,
    onEvent: (InterviewSessionUiEvent) -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = AppDesign.colors.surfaceMuted),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(
            modifier = Modifier.padding(AppDesign.spacing.lg),
            verticalArrangement = Arrangement.spacedBy(AppDesign.spacing.sm),
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
                label = { Text(strings.get(AppStringId.WORKSPACE_TRANSCRIPT)) },
                minLines = 2,
            )
        }
    }
}

@Composable
private fun AnswerPanel(
    state: InterviewSessionUiState,
    strings: StringsProvider,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = AppDesign.colors.surfaceMuted),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(
            modifier = Modifier.padding(AppDesign.spacing.lg),
            verticalArrangement = Arrangement.spacedBy(AppDesign.spacing.sm),
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
private fun WorkspaceControls(
    state: InterviewSessionUiState,
    strings: StringsProvider,
    onListen: () -> Unit,
    onEvent: (InterviewSessionUiEvent) -> Unit,
) {
    Surface(tonalElevation = AppDesign.spacing.xs) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(AppDesign.spacing.md),
            horizontalArrangement = Arrangement.spacedBy(AppDesign.spacing.sm),
        ) {
            Button(
                onClick = if (state.isListening) {
                    { onEvent(InterviewSessionUiEvent.StopListening) }
                } else {
                    onListen
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
