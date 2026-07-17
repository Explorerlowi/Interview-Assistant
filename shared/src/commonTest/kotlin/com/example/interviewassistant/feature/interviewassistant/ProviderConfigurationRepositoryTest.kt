package com.example.interviewassistant.feature.interviewassistant

import com.example.interviewassistant.core.security.SecretKeys
import com.example.interviewassistant.core.security.SecretStore
import com.example.interviewassistant.feature.interviewassistant.data.local.SettingsProviderConfigurationRepository
import com.example.interviewassistant.feature.interviewassistant.domain.model.LlmConfiguration
import com.example.interviewassistant.feature.interviewassistant.domain.model.ProviderConfiguration
import com.example.interviewassistant.feature.interviewassistant.domain.model.ProviderSecretUpdate
import com.example.interviewassistant.feature.interviewassistant.domain.model.SenseVoiceConfiguration
import com.example.interviewassistant.feature.interviewassistant.domain.model.SpeechRecognitionMode
import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ProviderConfigurationRepositoryTest {
    @Test
    fun `secrets stay outside settings and null updates preserve existing values`() {
        val settings = MapSettings()
        val secretStore = FakeSecretStore()
        val repository = SettingsProviderConfigurationRepository(settings, secretStore)

        repository.save(
            ProviderConfiguration(
                llm = LlmConfiguration(baseUrl = " https://api.deepseek.com/ ", model = " deepseek-v4-flash "),
            ),
            ProviderSecretUpdate(llmApiKey = " secret-key "),
        )
        repository.save(repository.configuration.value, ProviderSecretUpdate())

        assertEquals("secret-key", repository.llmApiKey())
        assertEquals("secret-key", repository.secrets().llmApiKey)
        assertEquals("https://api.deepseek.com", repository.configuration.value.llm.baseUrl)
        assertFalse(settings.keys.any { settings.getStringOrNull(it)?.contains("secret-key") == true })
        assertTrue(repository.secretStatus().hasLlmApiKey)
    }

    @Test
    fun `empty secret update removes one credential`() {
        val secretStore = FakeSecretStore()
        val repository = SettingsProviderConfigurationRepository(MapSettings(), secretStore)
        repository.save(ProviderConfiguration(), ProviderSecretUpdate(paddleToken = "token"))

        repository.save(repository.configuration.value, ProviderSecretUpdate(paddleToken = ""))

        assertEquals(null, secretStore.get(SecretKeys.PADDLE_TOKEN))
    }

    @Test
    fun `on-device speech settings are normalized and persisted`() {
        val settings = MapSettings()
        val secretStore = FakeSecretStore()
        val repository = SettingsProviderConfigurationRepository(settings, secretStore)

        repository.save(
            ProviderConfiguration(
                speechRecognitionMode = SpeechRecognitionMode.SENSE_VOICE_ON_DEVICE,
                senseVoice = SenseVoiceConfiguration(
                    language = " EN ",
                    partialIntervalMillis = 100,
                    maxSpeechDurationSeconds = 60,
                ),
            ),
            ProviderSecretUpdate(),
        )
        val restored = SettingsProviderConfigurationRepository(settings, secretStore).configuration.value

        assertEquals(SpeechRecognitionMode.SENSE_VOICE_ON_DEVICE, restored.speechRecognitionMode)
        assertEquals("en", restored.senseVoice.language)
        assertEquals(400, restored.senseVoice.partialIntervalMillis)
        assertEquals(30, restored.senseVoice.maxSpeechDurationSeconds)
    }

    private class FakeSecretStore : SecretStore {
        private val values = mutableMapOf<String, String>()

        override fun put(key: String, value: String) {
            values[key] = value
        }

        override fun get(key: String): String? = values[key]

        override fun remove(key: String) {
            values.remove(key)
        }

        override fun clear() {
            values.clear()
        }
    }
}
