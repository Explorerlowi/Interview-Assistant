package com.example.interviewassistant.core.i18n

import platform.Foundation.NSLocalizedString

/**
 * iOS [StringsProvider] backed by Localizable.strings keys.
 */
class IOSStringsProvider : StringsProvider {
    override fun get(id: AppStringId): String {
        val key = when (id) {
            AppStringId.COMMON_OK -> "common_ok"
            AppStringId.COMMON_CANCEL -> "common_cancel"
            AppStringId.LOGIN_TITLE -> "login_title"
            AppStringId.LOGIN_USERNAME -> "login_username"
            AppStringId.LOGIN_BTN -> "login_btn"
        }
        return NSLocalizedString(key, comment = "")
    }
}
