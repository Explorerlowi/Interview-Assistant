package com.example.interviewassistant.core.security

/**
 * Stores provider credentials using operating-system protected storage.
 *
 * Implementations must never persist values as plaintext or include them in logs.
 */
interface SecretStore {
    /**
     * Saves [value] under [key], replacing any previous value.
     */
    fun put(key: String, value: String)

    /**
     * Returns a previously stored value, or `null` when no value exists.
     */
    fun get(key: String): String?

    /**
     * Removes one stored value.
     */
    fun remove(key: String)

    /**
     * Removes all credentials owned by this application.
     */
    fun clear()
}

/**
 * Stable identifiers used by provider repositories.
 */
object SecretKeys {
    const val PADDLE_TOKEN = "provider.paddle.token"
    const val XUNFEI_APP_ID = "provider.xunfei.appId"
    const val XUNFEI_API_KEY = "provider.xunfei.apiKey"
    const val XUNFEI_API_SECRET = "provider.xunfei.apiSecret"
    const val LLM_API_KEY = "provider.llm.apiKey"
}
