package com.example.interviewassistant.core.i18n

import java.util.Locale

actual fun getCurrentLanguage(): AppLanguage {
    val locale = Locale.getDefault()
    return if (locale.language == "zh") AppLanguage.ZH_CN else AppLanguage.EN
}
