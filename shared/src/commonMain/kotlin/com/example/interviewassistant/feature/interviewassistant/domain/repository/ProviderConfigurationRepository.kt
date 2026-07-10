package com.example.interviewassistant.feature.interviewassistant.domain.repository

import com.example.interviewassistant.feature.interviewassistant.domain.model.ProviderConfiguration
import com.example.interviewassistant.feature.interviewassistant.domain.model.ProviderSecretStatus
import com.example.interviewassistant.feature.interviewassistant.domain.model.ProviderSecretUpdate
import com.example.interviewassistant.feature.interviewassistant.domain.model.ProviderSecrets
import com.example.interviewassistant.feature.interviewassistant.domain.model.XunfeiCredentials
import kotlinx.coroutines.flow.StateFlow

/**
 * Persists provider settings and keeps credentials isolated from presentation state.
 */
interface ProviderConfigurationRepository {
    /** Current non-secret settings. */
    val configuration: StateFlow<ProviderConfiguration>

    /** Saves non-secret settings and optional credential changes. */
    fun save(configuration: ProviderConfiguration, secrets: ProviderSecretUpdate = ProviderSecretUpdate())

    /** Returns credential-presence flags suitable for display. */
    fun secretStatus(): ProviderSecretStatus

    /** Returns stored credentials for the settings editor. */
    fun secrets(): ProviderSecrets

    /** Returns the PaddleOCR token for remote data sources. */
    fun paddleToken(): String?

    /** Returns all iFlytek credentials when configured completely. */
    fun xunfeiCredentials(): XunfeiCredentials?

    /** Returns the OpenAI-compatible provider key. */
    fun llmApiKey(): String?

    /** Removes every provider credential while retaining non-secret settings. */
    fun clearSecrets()

    /** Restores default non-secret settings and removes all credentials. */
    fun reset()
}
