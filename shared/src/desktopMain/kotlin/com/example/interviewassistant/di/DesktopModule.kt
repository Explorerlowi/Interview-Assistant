package com.example.interviewassistant.di

import com.example.interviewassistant.core.i18n.DesktopStringsProvider
import com.example.interviewassistant.core.i18n.StringsProvider
import com.russhwolf.settings.PreferencesSettings
import com.russhwolf.settings.Settings
import org.koin.dsl.module
import java.util.prefs.Preferences

actual fun platformModule() = module {
    single<StringsProvider> { DesktopStringsProvider() }
    single<Settings> {
        PreferencesSettings(Preferences.userRoot().node("com.example.interviewassistant"))
    }
}
