package com.example.interviewassistant.desktop.feature.interviewassistant

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import com.example.interviewassistant.core.design.components.AppSwitchRow
import com.example.interviewassistant.core.design.components.SettingsFormField
import com.example.interviewassistant.core.design.components.SettingsSecretField
import com.example.interviewassistant.core.design.theme.AppDesign
import com.example.interviewassistant.core.design.util.noRippleClickable
import com.example.interviewassistant.core.i18n.AppStringId
import com.example.interviewassistant.core.i18n.StringsProvider
import com.example.interviewassistant.feature.interviewassistant.domain.model.ProviderSecretUpdate
import com.example.interviewassistant.feature.interviewassistant.domain.model.SpeechRecognitionMode
import com.example.interviewassistant.feature.interviewassistant.domain.model.SpeechUnderstandingMetadata
import com.example.interviewassistant.feature.interviewassistant.domain.model.XunfeiConfiguration
import com.example.interviewassistant.feature.interviewassistant.domain.repository.SpeechModelDescriptor
import com.example.interviewassistant.feature.interviewassistant.domain.repository.SpeechModelFailure
import com.example.interviewassistant.feature.interviewassistant.domain.repository.SpeechModelState
import com.example.interviewassistant.feature.interviewassistant.presentation.state.ProviderSettingsUiEvent
import com.example.interviewassistant.feature.interviewassistant.presentation.state.ProviderSettingsUiState
import com.example.interviewassistant.feature.interviewassistant.presentation.state.SpeechRecognitionTestUiState

/**
 * Desktop provider settings form with two-column sections where space permits.
 */
@Composable
@OptIn(ExperimentalLayoutApi::class)
fun DesktopProviderSettings(
    state: ProviderSettingsUiState,
    strings: StringsProvider,
    onEvent: (ProviderSettingsUiEvent) -> Unit,
) {
    var configuration by remember(state.configuration) { mutableStateOf(state.configuration) }
    var paddleToken by remember(state.secrets.paddleToken) { mutableStateOf(state.secrets.paddleToken) }
    var xunfeiAppId by remember(state.secrets.xunfeiAppId) { mutableStateOf(state.secrets.xunfeiAppId) }
    var xunfeiApiKey by remember(state.secrets.xunfeiApiKey) { mutableStateOf(state.secrets.xunfeiApiKey) }
    var xunfeiApiSecret by remember(state.secrets.xunfeiApiSecret) {
        mutableStateOf(state.secrets.xunfeiApiSecret)
    }
    var llmApiKey by remember(state.secrets.llmApiKey) { mutableStateOf(state.secrets.llmApiKey) }

    LazyColumn(
        modifier = Modifier.fillMaxWidth().padding(AppDesign.spacing.xxl),
        verticalArrangement = Arrangement.spacedBy(AppDesign.spacing.lg),
    ) {
        item { Text(strings.get(AppStringId.SETTINGS_TITLE), style = AppDesign.typography.pageTitle) }
        item {
            DesktopSettingsSection(
                title = strings.get(AppStringId.SETTINGS_SPEECH_RECOGNITION),
                modifier = Modifier.fillMaxWidth(),
            ) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(AppDesign.spacing.sm),
                    verticalArrangement = Arrangement.spacedBy(AppDesign.spacing.sm),
                ) {
                    SpeechModeOption(
                        selected = configuration.speechRecognitionMode == SpeechRecognitionMode.XUNFEI,
                        label = strings.get(AppStringId.SETTINGS_SPEECH_XUNFEI),
                        onClick = {
                            configuration = configuration.copy(
                                speechRecognitionMode = SpeechRecognitionMode.XUNFEI,
                            )
                            onEvent(ProviderSettingsUiEvent.ResetSpeechRecognitionTest)
                        },
                    )
                    SpeechModeOption(
                        selected = configuration.speechRecognitionMode ==
                            SpeechRecognitionMode.SENSE_VOICE_ON_DEVICE,
                        label = strings.get(AppStringId.SETTINGS_SPEECH_SENSEVOICE),
                        onClick = {
                            configuration = configuration.copy(
                                speechRecognitionMode = SpeechRecognitionMode.SENSE_VOICE_ON_DEVICE,
                            )
                            onEvent(ProviderSettingsUiEvent.ResetSpeechRecognitionTest)
                        },
                    )
                }
                Text(
                    text = strings.get(
                        if (configuration.speechRecognitionMode == SpeechRecognitionMode.XUNFEI) {
                            AppStringId.SETTINGS_XUNFEI_DESCRIPTION
                        } else {
                            AppStringId.SETTINGS_SENSEVOICE_DESCRIPTION
                        },
                    ),
                    color = AppDesign.colors.textSecondary,
                    style = MaterialTheme.typography.bodySmall,
                )
                if (configuration.speechRecognitionMode == SpeechRecognitionMode.SENSE_VOICE_ON_DEVICE) {
                    SenseVoiceLanguageSelector(
                        selectedLanguage = configuration.senseVoice.language,
                        strings = strings,
                        onLanguageSelected = { language ->
                            configuration = configuration.copy(
                                senseVoice = configuration.senseVoice.copy(language = language),
                            )
                            onEvent(ProviderSettingsUiEvent.ResetSpeechRecognitionTest)
                        },
                    )
                    SenseVoiceModelControls(
                        state = state.speechModelState,
                        strings = strings,
                        onInstall = { onEvent(ProviderSettingsUiEvent.InstallSpeechModel) },
                        onDelete = { onEvent(ProviderSettingsUiEvent.DeleteSpeechModel) },
                    )
                }
                SpeechRecognitionTest(
                    state = state.speechRecognitionTest,
                    selectedMode = configuration.speechRecognitionMode,
                    xunfeiConfiguration = configuration.xunfei,
                    hasXunfeiCredentials = state.secretStatus.hasXunfeiCredentials,
                    modelDescriptor = state.speechModelDescriptor,
                    modelState = state.speechModelState,
                    strings = strings,
                    onStart = {
                        onEvent(
                            ProviderSettingsUiEvent.StartSpeechRecognitionTest(
                                mode = configuration.speechRecognitionMode,
                                senseVoiceConfiguration = configuration.senseVoice,
                            ),
                        )
                    },
                    onStop = { onEvent(ProviderSettingsUiEvent.StopSpeechRecognitionTest) },
                )
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(AppDesign.spacing.lg)) {
                DesktopSettingsSection(
                    title = strings.get(AppStringId.SETTINGS_PADDLE),
                    modifier = Modifier.weight(1f),
                ) {
                    SettingsFormField(
                        value = configuration.paddle.endpoint,
                        onValueChange = {
                            configuration = configuration.copy(paddle = configuration.paddle.copy(endpoint = it))
                        },
                        label = strings.get(AppStringId.SETTINGS_ENDPOINT),
                    )
                    SettingsFormField(
                        value = configuration.paddle.model,
                        onValueChange = {
                            configuration = configuration.copy(paddle = configuration.paddle.copy(model = it))
                        },
                        label = strings.get(AppStringId.SETTINGS_MODEL),
                    )
                    SettingsSecretField(
                        value = paddleToken,
                        onValueChange = { paddleToken = it },
                        label = strings.get(AppStringId.SETTINGS_API_KEY),
                        showSecretLabel = strings.get(AppStringId.SETTINGS_SHOW_SECRET),
                        hideSecretLabel = strings.get(AppStringId.SETTINGS_HIDE_SECRET),
                    )
                }
                DesktopSettingsSection(
                    title = strings.get(AppStringId.SETTINGS_LLM),
                    modifier = Modifier.weight(1f),
                ) {
                    SettingsFormField(
                        value = configuration.llm.baseUrl,
                        onValueChange = {
                            configuration = configuration.copy(llm = configuration.llm.copy(baseUrl = it))
                        },
                        label = strings.get(AppStringId.SETTINGS_ENDPOINT),
                    )
                    SettingsFormField(
                        value = configuration.llm.model,
                        onValueChange = {
                            configuration = configuration.copy(llm = configuration.llm.copy(model = it))
                        },
                        label = strings.get(AppStringId.SETTINGS_MODEL),
                    )
                    SettingsSecretField(
                        value = llmApiKey,
                        onValueChange = { llmApiKey = it },
                        label = strings.get(AppStringId.SETTINGS_API_KEY),
                        showSecretLabel = strings.get(AppStringId.SETTINGS_SHOW_SECRET),
                        hideSecretLabel = strings.get(AppStringId.SETTINGS_HIDE_SECRET),
                    )
                    SettingsFormField(
                        value = configuration.llm.systemPrompt,
                        onValueChange = {
                            configuration = configuration.copy(llm = configuration.llm.copy(systemPrompt = it))
                        },
                        label = strings.get(AppStringId.SETTINGS_SYSTEM_PROMPT),
                        singleLine = false,
                        minHeight = 120.dp,
                    )
                    AppSwitchRow(
                        checked = configuration.llm.thinkingEnabled,
                        onCheckedChange = {
                            configuration = configuration.copy(
                                llm = configuration.llm.copy(thinkingEnabled = it),
                            )
                        },
                        label = strings.get(AppStringId.SETTINGS_THINKING),
                    )
                    AppSwitchRow(
                        checked = configuration.llm.redactPersonalInfo,
                        onCheckedChange = {
                            configuration = configuration.copy(
                                llm = configuration.llm.copy(redactPersonalInfo = it),
                            )
                        },
                        label = strings.get(AppStringId.SETTINGS_REDACT_PII),
                    )
                }
            }
        }
        item {
            DesktopSettingsSection(
                title = strings.get(AppStringId.SETTINGS_XUNFEI),
                modifier = Modifier.fillMaxWidth(),
            ) {
                SettingsFormField(
                    value = configuration.xunfei.endpoint,
                    onValueChange = {
                        configuration = configuration.copy(xunfei = configuration.xunfei.copy(endpoint = it))
                    },
                    label = strings.get(AppStringId.SETTINGS_ENDPOINT),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(AppDesign.spacing.md)) {
                    SettingsSecretField(
                        value = xunfeiAppId,
                        onValueChange = { xunfeiAppId = it },
                        label = strings.get(AppStringId.SETTINGS_APP_ID),
                        showSecretLabel = strings.get(AppStringId.SETTINGS_SHOW_SECRET),
                        hideSecretLabel = strings.get(AppStringId.SETTINGS_HIDE_SECRET),
                        modifier = Modifier.weight(1f),
                    )
                    SettingsSecretField(
                        value = xunfeiApiKey,
                        onValueChange = { xunfeiApiKey = it },
                        label = strings.get(AppStringId.SETTINGS_API_KEY),
                        showSecretLabel = strings.get(AppStringId.SETTINGS_SHOW_SECRET),
                        hideSecretLabel = strings.get(AppStringId.SETTINGS_HIDE_SECRET),
                        modifier = Modifier.weight(1f),
                    )
                    SettingsSecretField(
                        value = xunfeiApiSecret,
                        onValueChange = { xunfeiApiSecret = it },
                        label = strings.get(AppStringId.SETTINGS_API_SECRET),
                        showSecretLabel = strings.get(AppStringId.SETTINGS_SHOW_SECRET),
                        hideSecretLabel = strings.get(AppStringId.SETTINGS_HIDE_SECRET),
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(AppDesign.spacing.sm)) {
                Button(
                    enabled = !state.isSaving,
                    onClick = {
                        onEvent(
                            ProviderSettingsUiEvent.Save(
                                configuration,
                                ProviderSecretUpdate(
                                    paddleToken = paddleToken,
                                    xunfeiAppId = xunfeiAppId,
                                    xunfeiApiKey = xunfeiApiKey,
                                    xunfeiApiSecret = xunfeiApiSecret,
                                    llmApiKey = llmApiKey,
                                ),
                            ),
                        )
                    },
                ) {
                    Text(strings.get(AppStringId.COMMON_SAVE))
                }
                TextButton(onClick = { onEvent(ProviderSettingsUiEvent.ClearSecrets) }) {
                    Text(strings.get(AppStringId.SETTINGS_CLEAR_SECRETS))
                }
                TextButton(
                    enabled = !state.isTesting,
                    onClick = { onEvent(ProviderSettingsUiEvent.TestConnections) },
                ) {
                    Text(strings.get(AppStringId.SETTINGS_TEST_CONNECTION))
                }
            }
        }
        state.connectionResults.forEach { (provider, result) ->
            item {
                Text(
                    text = "${provider.name}: ${
                        strings.get(if (result.success) AppStringId.COMMON_OK else AppStringId.ERROR_GENERIC)
                    } — ${result.message}",
                )
            }
        }
        state.errorMessage?.let { message ->
            item {
                Text(message, color = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
private fun SpeechRecognitionTest(
    state: SpeechRecognitionTestUiState,
    selectedMode: SpeechRecognitionMode,
    xunfeiConfiguration: XunfeiConfiguration,
    hasXunfeiCredentials: Boolean,
    modelDescriptor: SpeechModelDescriptor?,
    modelState: SpeechModelState,
    strings: StringsProvider,
    onStart: () -> Unit,
    onStop: () -> Unit,
) {
    val modelReady = modelState == SpeechModelState.Ready
    val canStart = when (selectedMode) {
        SpeechRecognitionMode.XUNFEI -> hasXunfeiCredentials
        SpeechRecognitionMode.SENSE_VOICE_ON_DEVICE -> modelReady
    }
    val statusId = when {
        state.isListening -> AppStringId.SETTINGS_SENSEVOICE_TEST_LISTENING
        selectedMode == SpeechRecognitionMode.XUNFEI && !hasXunfeiCredentials -> {
            AppStringId.SETTINGS_SPEECH_TEST_CREDENTIALS_REQUIRED
        }
        selectedMode == SpeechRecognitionMode.SENSE_VOICE_ON_DEVICE && !modelReady -> {
            AppStringId.SETTINGS_SENSEVOICE_TEST_MODEL_REQUIRED
        }
        else -> AppStringId.SETTINGS_SENSEVOICE_TEST_IDLE
    }
    val engineName = when (selectedMode) {
        SpeechRecognitionMode.XUNFEI -> listOf(
            strings.get(AppStringId.SETTINGS_SPEECH_TEST_XUNFEI_ENGINE),
            xunfeiConfiguration.domain,
            xunfeiConfiguration.language,
        ).joinToString(separator = " · ")
        SpeechRecognitionMode.SENSE_VOICE_ON_DEVICE -> modelDescriptor?.let { descriptor ->
            listOf(
                descriptor.name,
                descriptor.version,
                descriptor.quantization,
            ).joinToString(separator = " · ")
        } ?: strings.get(AppStringId.SETTINGS_SPEECH_SENSEVOICE)
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.small,
        color = AppDesign.colors.surface,
        border = BorderStroke(1.dp, AppDesign.colors.border),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(AppDesign.spacing.md),
            verticalArrangement = Arrangement.spacedBy(AppDesign.spacing.sm),
        ) {
            Text(
                text = strings.get(AppStringId.SETTINGS_SENSEVOICE_TEST_TITLE),
                style = MaterialTheme.typography.labelLarge,
            )
            Text(
                text = strings.get(AppStringId.SETTINGS_SENSEVOICE_TEST_DESCRIPTION),
                color = AppDesign.colors.textSecondary,
                style = MaterialTheme.typography.bodySmall,
            )
            Surface(
                shape = MaterialTheme.shapes.small,
                color = AppDesign.colors.brandSubtle,
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(AppDesign.spacing.sm),
                    verticalArrangement = Arrangement.spacedBy(AppDesign.spacing.xxs),
                ) {
                    Text(
                        text = strings.get(AppStringId.SETTINGS_SENSEVOICE_TEST_CURRENT_MODEL),
                        color = AppDesign.colors.textSecondary,
                        style = MaterialTheme.typography.bodySmall,
                    )
                    Text(
                        text = engineName,
                        color = AppDesign.colors.brand,
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
            }
            if (selectedMode == SpeechRecognitionMode.XUNFEI) {
                Text(
                    text = strings.get(AppStringId.SETTINGS_SPEECH_TEST_SAVED_CONFIG_HINT),
                    color = AppDesign.colors.textSecondary,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(AppDesign.spacing.sm),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (state.isListening) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                    )
                }
                Text(
                    text = strings.get(statusId),
                    color = if (state.isListening) AppDesign.colors.brand else AppDesign.colors.textSecondary,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            Surface(
                modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 72.dp),
                shape = MaterialTheme.shapes.small,
                color = AppDesign.colors.surfaceMuted,
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(AppDesign.spacing.md),
                    verticalArrangement = Arrangement.spacedBy(AppDesign.spacing.xs),
                ) {
                    if (state.transcript.isBlank() && state.partialTranscript.isBlank()) {
                        Text(
                            text = strings.get(AppStringId.SETTINGS_SENSEVOICE_TEST_RESULT_EMPTY),
                            color = AppDesign.colors.textSecondary,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    } else {
                        if (state.transcript.isNotBlank()) {
                            Text(state.transcript, style = MaterialTheme.typography.bodyMedium)
                        }
                        if (state.partialTranscript.isNotBlank()) {
                            Text(
                                text = state.partialTranscript,
                                color = AppDesign.colors.brand,
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                    }
                }
            }
            if (selectedMode == SpeechRecognitionMode.SENSE_VOICE_ON_DEVICE) {
                SpeechUnderstandingResults(state.metadata, strings)
            }
            if (state.failed) {
                Text(
                    text = state.errorMessage?.ifBlank { null }
                        ?: strings.get(AppStringId.ERROR_SPEECH_RECOGNITION),
                    color = AppDesign.colors.danger,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            Button(
                enabled = canStart,
                onClick = if (state.isListening) onStop else onStart,
            ) {
                Text(
                    strings.get(
                        if (state.isListening) {
                            AppStringId.SETTINGS_SENSEVOICE_TEST_STOP
                        } else {
                            AppStringId.SETTINGS_SENSEVOICE_TEST_START
                        },
                    ),
                )
            }
        }
    }
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun SenseVoiceLanguageSelector(
    selectedLanguage: String,
    strings: StringsProvider,
    onLanguageSelected: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(AppDesign.spacing.sm)) {
        Text(
            text = strings.get(AppStringId.SETTINGS_SENSEVOICE_LANGUAGE),
            style = MaterialTheme.typography.labelLarge,
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(AppDesign.spacing.sm),
            verticalArrangement = Arrangement.spacedBy(AppDesign.spacing.sm),
        ) {
            senseVoiceLanguageOptions.forEach { option ->
                val selected = selectedLanguage == option.code
                Surface(
                    modifier = Modifier.noRippleClickable { onLanguageSelected(option.code) },
                    shape = MaterialTheme.shapes.large,
                    color = if (selected) AppDesign.colors.brandSubtle else AppDesign.colors.surface,
                    border = BorderStroke(
                        width = 1.dp,
                        color = if (selected) AppDesign.colors.brand else AppDesign.colors.border,
                    ),
                ) {
                    Text(
                        text = strings.get(option.labelId),
                        modifier = Modifier.padding(
                            horizontal = AppDesign.spacing.md,
                            vertical = AppDesign.spacing.sm,
                        ),
                        color = if (selected) AppDesign.colors.brand else AppDesign.colors.textPrimary,
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
            }
        }
    }
}

@Composable
private fun SenseVoiceModelControls(
    state: SpeechModelState,
    strings: StringsProvider,
    onInstall: () -> Unit,
    onDelete: () -> Unit,
) {
    val stateLabel = when (state) {
        SpeechModelState.Unavailable -> AppStringId.SETTINGS_SENSEVOICE_MODEL_UNAVAILABLE
        SpeechModelState.NotInstalled -> AppStringId.SETTINGS_SENSEVOICE_MODEL_NOT_INSTALLED
        SpeechModelState.Preparing -> AppStringId.SETTINGS_SENSEVOICE_MODEL_PREPARING
        is SpeechModelState.Downloading -> AppStringId.SETTINGS_SENSEVOICE_MODEL_DOWNLOADING
        SpeechModelState.Ready -> AppStringId.SETTINGS_SENSEVOICE_MODEL_READY
        is SpeechModelState.Failed -> state.reason.stringId()
    }
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.small,
        color = AppDesign.colors.surface,
        border = BorderStroke(1.dp, AppDesign.colors.border),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(AppDesign.spacing.md),
            verticalArrangement = Arrangement.spacedBy(AppDesign.spacing.sm),
        ) {
            Text(
                text = strings.get(AppStringId.SETTINGS_SENSEVOICE_MODEL),
                style = MaterialTheme.typography.labelLarge,
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(AppDesign.spacing.sm),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (state == SpeechModelState.Preparing || state is SpeechModelState.Downloading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                    )
                }
                Text(
                    text = strings.get(stateLabel),
                    color = when (state) {
                        SpeechModelState.Ready -> AppDesign.colors.success
                        is SpeechModelState.Failed -> AppDesign.colors.danger
                        else -> AppDesign.colors.textSecondary
                    },
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            if (state is SpeechModelState.Downloading) {
                val progress = if (state.totalBytes > 0L) {
                    state.downloadedBytes.toFloat() / state.totalBytes.toFloat()
                } else {
                    0f
                }
                LinearProgressIndicator(
                    progress = { progress.coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    text = "${(progress * 100).toInt().coerceIn(0, 100)}%",
                    color = AppDesign.colors.textSecondary,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            if (state is SpeechModelState.Failed) {
                Text(
                    text = strings.get(AppStringId.SETTINGS_SENSEVOICE_RETRY_HINT),
                    color = AppDesign.colors.textSecondary,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            when (state) {
                SpeechModelState.NotInstalled -> Button(onClick = onInstall) {
                    Text(strings.get(AppStringId.SETTINGS_SENSEVOICE_DOWNLOAD))
                }
                is SpeechModelState.Failed -> Button(onClick = onInstall) {
                    Text(strings.get(AppStringId.COMMON_RETRY))
                }
                SpeechModelState.Ready -> TextButton(onClick = onDelete) {
                    Text(strings.get(AppStringId.COMMON_DELETE))
                }
                SpeechModelState.Unavailable,
                SpeechModelState.Preparing,
                is SpeechModelState.Downloading,
                -> Unit
            }
        }
    }
}

@Composable
private fun SpeechUnderstandingResults(
    metadata: SpeechUnderstandingMetadata,
    strings: StringsProvider,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.small,
        color = AppDesign.colors.surfaceMuted,
        border = BorderStroke(1.dp, AppDesign.colors.border),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(AppDesign.spacing.md),
            verticalArrangement = Arrangement.spacedBy(AppDesign.spacing.sm),
        ) {
            Text(
                text = strings.get(AppStringId.SETTINGS_SPEECH_UNDERSTANDING),
                style = MaterialTheme.typography.labelLarge,
            )
            UnderstandingResultRow(
                label = strings.get(AppStringId.SETTINGS_DETECTED_LANGUAGE),
                value = metadata.language.localizedLanguage(strings),
            )
            UnderstandingResultRow(
                label = strings.get(AppStringId.SETTINGS_DETECTED_EMOTION),
                value = metadata.emotion.localizedEmotion(strings),
            )
            UnderstandingResultRow(
                label = strings.get(AppStringId.SETTINGS_DETECTED_AUDIO_EVENT),
                value = metadata.audioEvent.localizedAudioEvent(strings),
            )
        }
    }
}

@Composable
private fun UnderstandingResultRow(
    label: String,
    value: String,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            color = AppDesign.colors.textSecondary,
            style = MaterialTheme.typography.bodySmall,
        )
        Text(
            text = value,
            color = AppDesign.colors.textPrimary,
            style = MaterialTheme.typography.labelMedium,
        )
    }
}

@Composable
private fun SpeechModeOption(
    selected: Boolean,
    label: String,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier.width(240.dp).noRippleClickable(onClick = onClick),
        shape = MaterialTheme.shapes.small,
        color = if (selected) AppDesign.colors.brandSubtle else AppDesign.colors.surface,
        border = BorderStroke(
            width = 1.dp,
            color = if (selected) AppDesign.colors.brand else AppDesign.colors.border,
        ),
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(AppDesign.spacing.md),
            color = if (selected) AppDesign.colors.brand else AppDesign.colors.textPrimary,
            style = MaterialTheme.typography.labelLarge,
        )
    }
}

private data class SenseVoiceLanguageOption(
    val code: String,
    val labelId: AppStringId,
)

private val senseVoiceLanguageOptions = listOf(
    SenseVoiceLanguageOption("auto", AppStringId.LANGUAGE_AUTO),
    SenseVoiceLanguageOption("zh", AppStringId.LANGUAGE_CHINESE),
    SenseVoiceLanguageOption("en", AppStringId.LANGUAGE_ENGLISH),
    SenseVoiceLanguageOption("yue", AppStringId.LANGUAGE_CANTONESE),
    SenseVoiceLanguageOption("ja", AppStringId.LANGUAGE_JAPANESE),
    SenseVoiceLanguageOption("ko", AppStringId.LANGUAGE_KOREAN),
)

private fun SpeechModelFailure.stringId(): AppStringId = when (this) {
    SpeechModelFailure.NETWORK -> AppStringId.SETTINGS_SENSEVOICE_FAILURE_NETWORK
    SpeechModelFailure.TIMEOUT -> AppStringId.SETTINGS_SENSEVOICE_FAILURE_TIMEOUT
    SpeechModelFailure.SERVER -> AppStringId.SETTINGS_SENSEVOICE_FAILURE_SERVER
    SpeechModelFailure.STORAGE -> AppStringId.SETTINGS_SENSEVOICE_FAILURE_STORAGE
    SpeechModelFailure.VERIFICATION -> AppStringId.SETTINGS_SENSEVOICE_FAILURE_VERIFICATION
    SpeechModelFailure.UNKNOWN -> AppStringId.SETTINGS_SENSEVOICE_FAILURE_UNKNOWN
}

private fun String?.localizedLanguage(strings: StringsProvider): String = localizedMetadata(
    strings = strings,
    labelId = when (normalizedMetadataLabel()) {
        "ZH" -> AppStringId.LANGUAGE_CHINESE
        "EN" -> AppStringId.LANGUAGE_ENGLISH
        "YUE" -> AppStringId.LANGUAGE_CANTONESE
        "JA" -> AppStringId.LANGUAGE_JAPANESE
        "KO" -> AppStringId.LANGUAGE_KOREAN
        else -> null
    },
)

private fun String?.localizedEmotion(strings: StringsProvider): String = localizedMetadata(
    strings = strings,
    labelId = when (normalizedMetadataLabel()) {
        "HAPPY" -> AppStringId.EMOTION_HAPPY
        "SAD" -> AppStringId.EMOTION_SAD
        "ANGRY" -> AppStringId.EMOTION_ANGRY
        "NEUTRAL" -> AppStringId.EMOTION_NEUTRAL
        "FEARFUL" -> AppStringId.EMOTION_FEARFUL
        "DISGUSTED" -> AppStringId.EMOTION_DISGUSTED
        "SURPRISED" -> AppStringId.EMOTION_SURPRISED
        else -> null
    },
)

private fun String?.localizedAudioEvent(strings: StringsProvider): String = localizedMetadata(
    strings = strings,
    labelId = when (normalizedMetadataLabel()) {
        "SPEECH" -> AppStringId.AUDIO_EVENT_SPEECH
        "BGM" -> AppStringId.AUDIO_EVENT_BGM
        "APPLAUSE" -> AppStringId.AUDIO_EVENT_APPLAUSE
        "LAUGHTER" -> AppStringId.AUDIO_EVENT_LAUGHTER
        "CRY" -> AppStringId.AUDIO_EVENT_CRY
        "SNEEZE" -> AppStringId.AUDIO_EVENT_SNEEZE
        "BREATH" -> AppStringId.AUDIO_EVENT_BREATH
        "COUGH" -> AppStringId.AUDIO_EVENT_COUGH
        else -> null
    },
)

private fun String?.localizedMetadata(
    strings: StringsProvider,
    labelId: AppStringId?,
): String {
    if (isNullOrBlank()) return strings.get(AppStringId.SETTINGS_RESULT_PENDING)
    return labelId?.let(strings::get) ?: this
}

private fun String?.normalizedMetadataLabel(): String? = this
    ?.trim()
    ?.removeSurrounding(prefix = "<|", suffix = "|>")
    ?.uppercase()

@Composable
private fun DesktopSettingsSection(
    title: String,
    modifier: Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = AppDesign.colors.surfaceMuted),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(AppDesign.spacing.lg),
            verticalArrangement = Arrangement.spacedBy(AppDesign.spacing.md),
        ) {
            Text(title, style = AppDesign.typography.sectionTitle)
            content()
        }
    }
}
