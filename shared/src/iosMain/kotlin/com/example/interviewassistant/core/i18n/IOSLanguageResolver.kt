package com.example.interviewassistant.core.i18n

import platform.Foundation.NSLocale
import platform.Foundation.currentLocale
import platform.Foundation.languageCode

actual fun getCurrentLanguage(): AppLanguage {
    val code = NSLocale.currentLocale.languageCode
    return if (code == "zh") AppLanguage.ZH_CN else AppLanguage.EN
}
