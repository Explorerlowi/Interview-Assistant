package com.example.interviewassistant.core.i18n

/**
 * Desktop string provider using in-code English defaults.
 *
 * Product apps can load from `.properties` files under desktop resources.
 */
class DesktopStringsProvider : StringsProvider {
    override fun get(id: AppStringId): String = when (id) {
        AppStringId.COMMON_OK -> "OK"
        AppStringId.COMMON_CANCEL -> "Cancel"
        AppStringId.LOGIN_TITLE -> "Login"
        AppStringId.LOGIN_USERNAME -> "Username"
        AppStringId.LOGIN_BTN -> "Sign in"
    }
}
