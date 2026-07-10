package com.example.interviewassistant.core.security

/**
 * Placeholder used while iOS is outside the first implementation scope.
 */
class UnsupportedIosSecretStore : SecretStore {
    override fun put(key: String, value: String) {
        error(MESSAGE)
    }

    override fun get(key: String): String? = null

    override fun remove(key: String) = Unit

    override fun clear() = Unit

    private companion object {
        const val MESSAGE = "Provider credential storage is not available on iOS yet"
    }
}
