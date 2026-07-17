package com.example.interviewassistant.feature.interviewassistant.data.local

import com.example.interviewassistant.core.security.SecretKeys
import com.example.interviewassistant.core.security.SecretStore
import com.example.interviewassistant.feature.interviewassistant.domain.model.AnswerTriggerMode
import com.example.interviewassistant.feature.interviewassistant.domain.model.LlmConfiguration
import com.example.interviewassistant.feature.interviewassistant.domain.model.PaddleConfiguration
import com.example.interviewassistant.feature.interviewassistant.domain.model.ProviderConfiguration
import com.example.interviewassistant.feature.interviewassistant.domain.model.ProviderSecretStatus
import com.example.interviewassistant.feature.interviewassistant.domain.model.ProviderSecretUpdate
import com.example.interviewassistant.feature.interviewassistant.domain.model.ProviderSecrets
import com.example.interviewassistant.feature.interviewassistant.domain.model.SenseVoiceConfiguration
import com.example.interviewassistant.feature.interviewassistant.domain.model.SpeechRecognitionMode
import com.example.interviewassistant.feature.interviewassistant.domain.model.XunfeiConfiguration
import com.example.interviewassistant.feature.interviewassistant.domain.model.XunfeiCredentials
import com.example.interviewassistant.feature.interviewassistant.domain.repository.ProviderConfigurationRepository
import com.russhwolf.settings.Settings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Stores ordinary settings in [Settings] and delegates credentials to [SecretStore].
 */
class SettingsProviderConfigurationRepository(
    private val settings: Settings,
    private val secretStore: SecretStore,
) : ProviderConfigurationRepository {
    private val mutableConfiguration = MutableStateFlow(readConfiguration())

    override val configuration: StateFlow<ProviderConfiguration> = mutableConfiguration.asStateFlow()

    override fun save(configuration: ProviderConfiguration, secrets: ProviderSecretUpdate) {
        val normalized = configuration.normalized()
        settings.putString(KEY_PADDLE_ENDPOINT, normalized.paddle.endpoint)
        settings.putString(KEY_PADDLE_MODEL, normalized.paddle.model)
        settings.putString(KEY_XUNFEI_ENDPOINT, normalized.xunfei.endpoint)
        settings.putString(KEY_XUNFEI_LANGUAGE, normalized.xunfei.language)
        settings.putString(KEY_XUNFEI_DOMAIN, normalized.xunfei.domain)
        settings.putString(KEY_XUNFEI_ACCENT, normalized.xunfei.accent)
        settings.putInt(KEY_XUNFEI_EOS, normalized.xunfei.endOfSpeechMillis)
        settings.putString(KEY_SPEECH_MODE, normalized.speechRecognitionMode.name)
        settings.putString(KEY_SENSEVOICE_LANGUAGE, normalized.senseVoice.language)
        settings.putBoolean(KEY_SENSEVOICE_ITN, normalized.senseVoice.useInverseTextNormalization)
        settings.putInt(KEY_SENSEVOICE_PARTIAL_INTERVAL, normalized.senseVoice.partialIntervalMillis)
        settings.putInt(KEY_SENSEVOICE_MAX_SPEECH, normalized.senseVoice.maxSpeechDurationSeconds)
        settings.putString(KEY_LLM_BASE_URL, normalized.llm.baseUrl)
        settings.putString(KEY_LLM_MODEL, normalized.llm.model)
        settings.putString(KEY_LLM_SYSTEM_PROMPT, normalized.llm.systemPrompt)
        settings.putBoolean(KEY_LLM_THINKING, normalized.llm.thinkingEnabled)
        settings.putInt(KEY_LLM_CONTEXT, normalized.llm.maxContextCharacters)
        settings.putBoolean(KEY_LLM_REDACT_PII, normalized.llm.redactPersonalInfo)
        settings.putString(KEY_TRIGGER_MODE, normalized.answerTriggerMode.name)
        updateSecret(SecretKeys.PADDLE_TOKEN, secrets.paddleToken)
        updateSecret(SecretKeys.XUNFEI_APP_ID, secrets.xunfeiAppId)
        updateSecret(SecretKeys.XUNFEI_API_KEY, secrets.xunfeiApiKey)
        updateSecret(SecretKeys.XUNFEI_API_SECRET, secrets.xunfeiApiSecret)
        updateSecret(SecretKeys.LLM_API_KEY, secrets.llmApiKey)
        mutableConfiguration.value = normalized
    }

    override fun secretStatus(): ProviderSecretStatus {
        return ProviderSecretStatus(
            hasPaddleToken = !paddleToken().isNullOrBlank(),
            hasXunfeiCredentials = xunfeiCredentials() != null,
            hasLlmApiKey = !llmApiKey().isNullOrBlank(),
        )
    }

    override fun secrets(): ProviderSecrets {
        return ProviderSecrets(
            paddleToken = secretStore.get(SecretKeys.PADDLE_TOKEN).orEmpty(),
            xunfeiAppId = secretStore.get(SecretKeys.XUNFEI_APP_ID).orEmpty(),
            xunfeiApiKey = secretStore.get(SecretKeys.XUNFEI_API_KEY).orEmpty(),
            xunfeiApiSecret = secretStore.get(SecretKeys.XUNFEI_API_SECRET).orEmpty(),
            llmApiKey = secretStore.get(SecretKeys.LLM_API_KEY).orEmpty(),
        )
    }

    override fun paddleToken(): String? = secretStore.get(SecretKeys.PADDLE_TOKEN)

    override fun xunfeiCredentials(): XunfeiCredentials? {
        val appId = secretStore.get(SecretKeys.XUNFEI_APP_ID).orEmpty()
        val apiKey = secretStore.get(SecretKeys.XUNFEI_API_KEY).orEmpty()
        val apiSecret = secretStore.get(SecretKeys.XUNFEI_API_SECRET).orEmpty()
        return if (appId.isBlank() || apiKey.isBlank() || apiSecret.isBlank()) {
            null
        } else {
            XunfeiCredentials(appId, apiKey, apiSecret)
        }
    }

    override fun llmApiKey(): String? = secretStore.get(SecretKeys.LLM_API_KEY)

    override fun clearSecrets() {
        secretStore.clear()
    }

    override fun reset() {
        settings.clear()
        secretStore.clear()
        mutableConfiguration.value = ProviderConfiguration()
    }

    private fun readConfiguration(): ProviderConfiguration {
        return ProviderConfiguration(
            paddle = PaddleConfiguration(
                endpoint = settings.getString(KEY_PADDLE_ENDPOINT, PaddleConfiguration.DEFAULT_ENDPOINT),
                model = settings.getString(KEY_PADDLE_MODEL, PaddleConfiguration.DEFAULT_MODEL),
            ),
            xunfei = XunfeiConfiguration(
                endpoint = settings.getString(KEY_XUNFEI_ENDPOINT, XunfeiConfiguration.DEFAULT_ENDPOINT),
                language = settings.getString(KEY_XUNFEI_LANGUAGE, "zh_cn"),
                domain = settings.getString(KEY_XUNFEI_DOMAIN, "iat"),
                accent = settings.getString(KEY_XUNFEI_ACCENT, "mandarin"),
                endOfSpeechMillis = settings.getInt(KEY_XUNFEI_EOS, 2_000),
            ),
            speechRecognitionMode = runCatching {
                SpeechRecognitionMode.valueOf(
                    settings.getString(KEY_SPEECH_MODE, SpeechRecognitionMode.XUNFEI.name),
                )
            }.getOrDefault(SpeechRecognitionMode.XUNFEI),
            senseVoice = SenseVoiceConfiguration(
                language = settings.getString(KEY_SENSEVOICE_LANGUAGE, "zh"),
                useInverseTextNormalization = settings.getBoolean(KEY_SENSEVOICE_ITN, true),
                partialIntervalMillis = settings.getInt(KEY_SENSEVOICE_PARTIAL_INTERVAL, 800),
                maxSpeechDurationSeconds = settings.getInt(KEY_SENSEVOICE_MAX_SPEECH, 15),
            ),
            llm = LlmConfiguration(
                baseUrl = settings.getString(KEY_LLM_BASE_URL, LlmConfiguration.DEFAULT_BASE_URL),
                model = settings.getString(KEY_LLM_MODEL, LlmConfiguration.DEFAULT_MODEL),
                systemPrompt = settings.getString(
                    KEY_LLM_SYSTEM_PROMPT,
                    LlmConfiguration.DEFAULT_SYSTEM_PROMPT,
                ),
                thinkingEnabled = settings.getBoolean(KEY_LLM_THINKING, false),
                maxContextCharacters = settings.getInt(KEY_LLM_CONTEXT, 24_000),
                redactPersonalInfo = settings.getBoolean(KEY_LLM_REDACT_PII, true),
            ),
            answerTriggerMode = runCatching {
                AnswerTriggerMode.valueOf(settings.getString(KEY_TRIGGER_MODE, AnswerTriggerMode.MANUAL.name))
            }.getOrDefault(AnswerTriggerMode.MANUAL),
        ).normalized()
    }

    private fun updateSecret(key: String, value: String?) {
        if (value == null) return
        val normalized = value.trim()
        if (normalized.isEmpty()) {
            secretStore.remove(key)
        } else {
            secretStore.put(key, normalized)
        }
    }

    private fun ProviderConfiguration.normalized(): ProviderConfiguration {
        return copy(
            paddle = paddle.copy(
                endpoint = paddle.endpoint.trim().trimEnd('/'),
                model = paddle.model.trim(),
            ),
            xunfei = xunfei.copy(
                endpoint = xunfei.endpoint.trim(),
                language = xunfei.language.trim(),
                domain = xunfei.domain.trim(),
                accent = xunfei.accent.trim(),
                endOfSpeechMillis = xunfei.endOfSpeechMillis.coerceIn(1_000, 10_000),
            ),
            senseVoice = senseVoice.copy(
                language = senseVoice.language.trim().lowercase().takeIf(SUPPORTED_SENSEVOICE_LANGUAGES::contains)
                    ?: "zh",
                partialIntervalMillis = senseVoice.partialIntervalMillis.coerceIn(400, 3_000),
                maxSpeechDurationSeconds = senseVoice.maxSpeechDurationSeconds.coerceIn(5, 30),
            ),
            llm = llm.copy(
                baseUrl = llm.baseUrl.trim().trimEnd('/'),
                model = llm.model.trim(),
                systemPrompt = llm.systemPrompt.trim().ifBlank { LlmConfiguration.DEFAULT_SYSTEM_PROMPT },
                maxContextCharacters = llm.maxContextCharacters.coerceIn(4_000, 200_000),
            ),
        )
    }

    private companion object {
        const val KEY_PADDLE_ENDPOINT = "provider.paddle.endpoint"
        const val KEY_PADDLE_MODEL = "provider.paddle.model"
        const val KEY_XUNFEI_ENDPOINT = "provider.xunfei.endpoint"
        const val KEY_XUNFEI_LANGUAGE = "provider.xunfei.language"
        const val KEY_XUNFEI_DOMAIN = "provider.xunfei.domain"
        const val KEY_XUNFEI_ACCENT = "provider.xunfei.accent"
        const val KEY_XUNFEI_EOS = "provider.xunfei.eos"
        const val KEY_SPEECH_MODE = "provider.speech.mode"
        const val KEY_SENSEVOICE_LANGUAGE = "provider.senseVoice.language"
        const val KEY_SENSEVOICE_ITN = "provider.senseVoice.itn"
        const val KEY_SENSEVOICE_PARTIAL_INTERVAL = "provider.senseVoice.partialInterval"
        const val KEY_SENSEVOICE_MAX_SPEECH = "provider.senseVoice.maxSpeechSeconds"
        const val KEY_LLM_BASE_URL = "provider.llm.baseUrl"
        const val KEY_LLM_MODEL = "provider.llm.model"
        const val KEY_LLM_SYSTEM_PROMPT = "provider.llm.systemPrompt"
        const val KEY_LLM_THINKING = "provider.llm.thinking"
        const val KEY_LLM_CONTEXT = "provider.llm.context"
        const val KEY_LLM_REDACT_PII = "provider.llm.redactPii"
        const val KEY_TRIGGER_MODE = "assistant.triggerMode"
        val SUPPORTED_SENSEVOICE_LANGUAGES = setOf("zh", "en", "ja", "ko", "yue", "auto")
    }
}
