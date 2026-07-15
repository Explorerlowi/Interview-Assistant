package com.example.interviewassistant.android.feature.interviewassistant

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.interviewassistant.core.design.components.AppSwitchRow
import com.example.interviewassistant.core.design.components.SettingsFormField
import com.example.interviewassistant.core.design.components.SettingsSecretField
import com.example.interviewassistant.core.design.theme.AppDesign
import com.example.interviewassistant.core.i18n.AppStringId
import com.example.interviewassistant.core.i18n.StringsProvider
import com.example.interviewassistant.feature.interviewassistant.domain.model.ProviderSecretUpdate
import com.example.interviewassistant.feature.interviewassistant.presentation.state.ProviderSettingsUiEvent
import com.example.interviewassistant.feature.interviewassistant.presentation.state.ProviderSettingsUiState

/**
 * Android provider settings form. Saved secrets are loaded into the editor for review and updates.
 */
@Composable
fun ProviderSettingsScreen(
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
        modifier = Modifier.padding(AppDesign.spacing.lg),
        verticalArrangement = Arrangement.spacedBy(AppDesign.spacing.md),
    ) {
        item { Text(strings.get(AppStringId.SETTINGS_TITLE), style = AppDesign.typography.pageTitle) }
        item {
            SettingsSection(title = strings.get(AppStringId.SETTINGS_PADDLE)) {
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
        }
        item {
            SettingsSection(title = strings.get(AppStringId.SETTINGS_XUNFEI)) {
                SettingsFormField(
                    value = configuration.xunfei.endpoint,
                    onValueChange = {
                        configuration = configuration.copy(xunfei = configuration.xunfei.copy(endpoint = it))
                    },
                    label = strings.get(AppStringId.SETTINGS_ENDPOINT),
                )
                SettingsSecretField(
                    value = xunfeiAppId,
                    onValueChange = { xunfeiAppId = it },
                    label = strings.get(AppStringId.SETTINGS_APP_ID),
                    showSecretLabel = strings.get(AppStringId.SETTINGS_SHOW_SECRET),
                    hideSecretLabel = strings.get(AppStringId.SETTINGS_HIDE_SECRET),
                )
                SettingsSecretField(
                    value = xunfeiApiKey,
                    onValueChange = { xunfeiApiKey = it },
                    label = strings.get(AppStringId.SETTINGS_API_KEY),
                    showSecretLabel = strings.get(AppStringId.SETTINGS_SHOW_SECRET),
                    hideSecretLabel = strings.get(AppStringId.SETTINGS_HIDE_SECRET),
                )
                SettingsSecretField(
                    value = xunfeiApiSecret,
                    onValueChange = { xunfeiApiSecret = it },
                    label = strings.get(AppStringId.SETTINGS_API_SECRET),
                    showSecretLabel = strings.get(AppStringId.SETTINGS_SHOW_SECRET),
                    hideSecretLabel = strings.get(AppStringId.SETTINGS_HIDE_SECRET),
                )
            }
        }
        item {
            SettingsSection(title = strings.get(AppStringId.SETTINGS_LLM)) {
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
                        configuration = configuration.copy(llm = configuration.llm.copy(thinkingEnabled = it))
                    },
                    label = strings.get(AppStringId.SETTINGS_THINKING),
                )
                AppSwitchRow(
                    checked = configuration.llm.redactPersonalInfo,
                    onCheckedChange = {
                        configuration = configuration.copy(llm = configuration.llm.copy(redactPersonalInfo = it))
                    },
                    label = strings.get(AppStringId.SETTINGS_REDACT_PII),
                )
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(AppDesign.spacing.sm)) {
                Button(
                    enabled = !state.isSaving,
                    onClick = {
                        onEvent(
                            ProviderSettingsUiEvent.Save(
                                configuration = configuration,
                                secrets = ProviderSecretUpdate(
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
private fun SettingsSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
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
