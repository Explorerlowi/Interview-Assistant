package com.example.interviewassistant.core.i18n

import platform.Foundation.NSLocalizedString

/**
 * iOS [StringsProvider] backed by Localizable.strings keys.
 */
class IOSStringsProvider : StringsProvider {
    override fun get(id: AppStringId): String {
        val key = id.name.lowercase()
        return NSLocalizedString(key, comment = "")
    }
}
