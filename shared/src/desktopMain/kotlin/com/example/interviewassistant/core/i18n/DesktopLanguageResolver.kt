package com.example.interviewassistant.core.i18n

import java.util.Locale

actual fun getCurrentLanguage(): AppLanguage {
    val lang = Locale.getDefault().language.lowercase()
    return if (lang.startsWith("zh")) AppLanguage.ZH_CN else AppLanguage.EN
}
