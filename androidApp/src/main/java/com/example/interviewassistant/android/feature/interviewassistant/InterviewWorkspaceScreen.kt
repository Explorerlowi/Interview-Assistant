package com.example.interviewassistant.android.feature.interviewassistant

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.interviewassistant.android.R
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
        containerColor = AppDesign.colors.pageBackground,
        topBar = {
            WorkspaceTopBar(
                title = state.session?.title ?: strings.get(AppStringId.ASSISTANT_TITLE),
                canComplete = !state.isLoadingSession && state.session != null,
                backLabel = strings.get(AppStringId.COMMON_BACK),
                completeLabel = strings.get(AppStringId.COMPLETE_SESSION),
                onNavigateBack = onNavigateBack,
                onComplete = { onEvent(InterviewSessionUiEvent.CompleteSession) },
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
        WorkspaceContent(
            state = state,
            strings = strings,
            permissionDenied = permissionDenied,
            contentPadding = padding,
            onEvent = onEvent,
        )
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun WorkspaceTopBar(
    title: String,
    canComplete: Boolean,
    backLabel: String,
    completeLabel: String,
    onNavigateBack: () -> Unit,
    onComplete: () -> Unit,
) {
    TopAppBar(
        title = {
            Text(
                text = title,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        navigationIcon = {
            IconButton(onClick = onNavigateBack) {
                Icon(
                    painter = painterResource(R.drawable.ic_arrow_back),
                    contentDescription = backLabel,
                )
            }
        },
        actions = {
            TextButton(enabled = canComplete, onClick = onComplete) {
                Text(completeLabel)
            }
        },
    )
}

@Composable
private fun WorkspaceContent(
    state: InterviewSessionUiState,
    strings: StringsProvider,
    permissionDenied: Boolean,
    contentPadding: PaddingValues,
    onEvent: (InterviewSessionUiEvent) -> Unit,
) {
    Box(modifier = Modifier.fillMaxSize().padding(contentPadding)) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(AppDesign.spacing.lg),
            verticalArrangement = Arrangement.spacedBy(AppDesign.spacing.md),
        ) {
            item { TriggerModeSelector(state.triggerMode, strings, onEvent) }
            if (permissionDenied) {
                item { WorkspaceError(strings.get(AppStringId.ERROR_PERMISSION_MICROPHONE)) }
            }
            state.errorMessage?.let { message ->
                item { WorkspaceError(message) }
            }
            item { TranscriptPanel(state, strings, onEvent) }
            item { AnswerPanel(state, strings) }
        }
        if (state.isLoadingSession) {
            Surface(
                modifier = Modifier.align(Alignment.Center),
                shape = MaterialTheme.shapes.large,
                color = AppDesign.colors.surfaceElevated,
                shadowElevation = 6.dp,
            ) {
                CircularProgressIndicator(modifier = Modifier.padding(AppDesign.spacing.xl).size(28.dp))
            }
        }
    }
}

@Composable
private fun TriggerModeSelector(
    selected: AnswerTriggerMode,
    strings: StringsProvider,
    onEvent: (InterviewSessionUiEvent) -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = AppDesign.colors.surfaceMuted,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(AppDesign.spacing.xs),
            horizontalArrangement = Arrangement.spacedBy(AppDesign.spacing.xs),
        ) {
            AnswerTriggerMode.entries.forEach { mode ->
                val label = strings.get(
                    if (mode == AnswerTriggerMode.MANUAL) {
                        AppStringId.TRIGGER_MANUAL
                    } else {
                        AppStringId.TRIGGER_AUTOMATIC
                    },
                )
                Surface(
                    modifier = Modifier.weight(1f).noRippleClickable {
                        onEvent(InterviewSessionUiEvent.SetTriggerMode(mode))
                    },
                    shape = MaterialTheme.shapes.medium,
                    color = if (selected == mode) AppDesign.colors.surface else AppDesign.colors.surfaceMuted,
                    border = if (selected == mode) BorderStroke(1.dp, AppDesign.colors.border) else null,
                    shadowElevation = if (selected == mode) 1.dp else 0.dp,
                ) {
                    Text(
                        text = label,
                        modifier = Modifier.padding(
                            horizontal = AppDesign.spacing.md,
                            vertical = AppDesign.spacing.sm,
                        ),
                        style = AppDesign.typography.bodyStrong,
                        color = if (selected == mode) {
                            AppDesign.colors.brand
                        } else {
                            AppDesign.colors.textSecondary
                        },
                        textAlign = TextAlign.Center,
                    )
                }
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
    WorkspaceCard(
        title = strings.get(AppStringId.WORKSPACE_TRANSCRIPT),
        iconRes = R.drawable.ic_mock_interview,
        inProgress = state.isListening,
    ) {
        val transcript = state.transcripts.joinToString(separator = "\n") { it.text }
        Surface(
            modifier = Modifier.fillMaxWidth().heightIn(min = 88.dp),
            shape = MaterialTheme.shapes.medium,
            color = AppDesign.colors.surfaceMuted,
        ) {
            Column(
                modifier = Modifier.padding(AppDesign.spacing.md),
                verticalArrangement = Arrangement.spacedBy(AppDesign.spacing.sm),
            ) {
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
                    Text(
                        text = state.liveTranscript,
                        style = AppDesign.typography.bodyStrong,
                        color = AppDesign.colors.brand,
                    )
                }
            }
        }
        OutlinedTextField(
            value = state.currentQuestion,
            onValueChange = { onEvent(InterviewSessionUiEvent.UpdateQuestion(it)) },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text(strings.get(AppStringId.TRANSCRIPT_EMPTY)) },
            minLines = 2,
            maxLines = 5,
        )
    }
}

@Composable
private fun AnswerPanel(
    state: InterviewSessionUiState,
    strings: StringsProvider,
) {
    WorkspaceCard(
        title = strings.get(AppStringId.WORKSPACE_ANSWER),
        iconRes = R.drawable.ic_assistant,
        inProgress = state.isGenerating,
    ) {
        val answer = state.streamingAnswer.ifBlank { state.answers.lastOrNull()?.content.orEmpty() }
        Surface(
            modifier = Modifier.fillMaxWidth().heightIn(min = 132.dp).animateContentSize(),
            shape = MaterialTheme.shapes.medium,
            color = AppDesign.colors.surfaceMuted,
        ) {
            Text(
                text = answer.ifBlank { strings.get(AppStringId.ANSWER_EMPTY) },
                modifier = Modifier.padding(AppDesign.spacing.md),
                style = AppDesign.typography.body,
                color = if (answer.isBlank()) {
                    AppDesign.colors.textSecondary
                } else {
                    AppDesign.colors.textPrimary
                },
            )
        }
        if (state.isGenerating) {
            LinearProgressIndicator(
                modifier = Modifier.fillMaxWidth(),
                color = AppDesign.colors.brand,
                trackColor = AppDesign.colors.brandSubtle,
            )
        }
    }
}

@Composable
private fun WorkspaceCard(
    title: String,
    iconRes: Int,
    inProgress: Boolean,
    content: @Composable () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = AppDesign.colors.surface),
        border = BorderStroke(1.dp, AppDesign.colors.divider),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(AppDesign.spacing.lg),
            verticalArrangement = Arrangement.spacedBy(AppDesign.spacing.md),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(AppDesign.spacing.sm),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    painter = painterResource(iconRes),
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = AppDesign.colors.brand,
                )
                Text(text = title, modifier = Modifier.weight(1f), style = AppDesign.typography.sectionTitle)
                if (inProgress) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                }
            }
            content()
        }
    }
}

@Composable
private fun WorkspaceError(message: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.errorContainer,
    ) {
        Text(
            text = message,
            modifier = Modifier.padding(AppDesign.spacing.md),
            style = AppDesign.typography.body,
            color = MaterialTheme.colorScheme.onErrorContainer,
        )
    }
}

@Composable
private fun WorkspaceControls(
    state: InterviewSessionUiState,
    strings: StringsProvider,
    onListen: () -> Unit,
    onEvent: (InterviewSessionUiEvent) -> Unit,
) {
    val sessionReady = !state.isLoadingSession && state.session != null && state.resume != null
    Surface(
        color = AppDesign.colors.surfaceElevated,
        shadowElevation = 8.dp,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(AppDesign.spacing.md),
            horizontalArrangement = Arrangement.spacedBy(AppDesign.spacing.sm),
        ) {
            val listeningLabel = strings.get(
                if (state.isListening) AppStringId.STOP_LISTENING else AppStringId.START_LISTENING,
            )
            Button(
                modifier = Modifier.weight(1f),
                enabled = sessionReady || state.isListening,
                colors = if (state.isListening) {
                    ButtonDefaults.buttonColors(
                        containerColor = AppDesign.colors.danger,
                        contentColor = MaterialTheme.colorScheme.onError,
                    )
                } else {
                    ButtonDefaults.buttonColors()
                },
                contentPadding = PaddingValues(horizontal = AppDesign.spacing.md),
                onClick = if (state.isListening) {
                    { onEvent(InterviewSessionUiEvent.StopListening) }
                } else {
                    onListen
                },
            ) {
                Icon(
                    painter = painterResource(
                        if (state.isListening) R.drawable.ic_stop else R.drawable.ic_mock_interview,
                    ),
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Text(
                    text = listeningLabel,
                    modifier = Modifier.padding(start = AppDesign.spacing.sm),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            val generateLabel = strings.get(
                if (state.isGenerating) AppStringId.CANCEL_GENERATION else AppStringId.GENERATE_ANSWER,
            )
            Button(
                modifier = Modifier.weight(1f),
                enabled = sessionReady && (state.isGenerating || state.currentQuestion.isNotBlank()),
                contentPadding = PaddingValues(horizontal = AppDesign.spacing.md),
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
                Icon(
                    painter = painterResource(
                        if (state.isGenerating) R.drawable.ic_stop else R.drawable.ic_assistant,
                    ),
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Text(
                    text = generateLabel,
                    modifier = Modifier.padding(start = AppDesign.spacing.sm),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}
