package com.example.interviewassistant.core.security

import java.util.prefs.Preferences
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class DesktopSecretStoreTest {
    @Test
    fun `DPAPI round trip does not persist plaintext`() {
        if (!System.getProperty("os.name").startsWith("Windows", ignoreCase = true)) return
        val preferences = Preferences.userRoot().node("com.example.interviewassistant/tests/secrets")
        val store = DesktopSecretStore(preferences)
        val secret = "test-secret-value"

        try {
            store.put("key", secret)

            assertEquals(secret, store.get("key"))
            assertFalse(preferences.get("key", "").contains(secret))
        } finally {
            store.clear()
            preferences.removeNode()
        }
    }
}
