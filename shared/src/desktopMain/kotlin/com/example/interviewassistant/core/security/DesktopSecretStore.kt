package com.example.interviewassistant.core.security

import com.sun.jna.Platform
import com.sun.jna.platform.win32.Crypt32Util
import java.util.Base64
import java.util.prefs.Preferences

/**
 * Windows credential store protected for the current user through DPAPI.
 */
class DesktopSecretStore(
    private val preferences: Preferences = Preferences.userRoot()
        .node("com.example.interviewassistant/secrets"),
) : SecretStore {
    override fun put(key: String, value: String) {
        checkWindows()
        val encrypted = Crypt32Util.cryptProtectData(value.encodeToByteArray())
        preferences.put(key, Base64.getEncoder().encodeToString(encrypted))
        preferences.flush()
    }

    override fun get(key: String): String? {
        checkWindows()
        val encoded = preferences.get(key, null) ?: return null
        return runCatching {
            val encrypted = Base64.getDecoder().decode(encoded)
            Crypt32Util.cryptUnprotectData(encrypted).decodeToString()
        }.getOrNull()
    }

    override fun remove(key: String) {
        preferences.remove(key)
        preferences.flush()
    }

    override fun clear() {
        preferences.clear()
        preferences.flush()
    }

    private fun checkWindows() {
        check(Platform.isWindows()) {
            "Desktop secure storage currently supports Windows only"
        }
    }
}
