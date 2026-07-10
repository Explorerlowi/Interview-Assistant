package com.example.interviewassistant.di

import com.example.interviewassistant.core.audio.AudioSource
import com.example.interviewassistant.core.audio.DesktopLoopbackAudioSource
import com.example.interviewassistant.core.database.DatabaseDriverFactory
import com.example.interviewassistant.core.database.DesktopDatabaseDriverFactory
import com.example.interviewassistant.core.file.AppFileStore
import com.example.interviewassistant.core.file.DesktopAppFileStore
import com.example.interviewassistant.core.i18n.DesktopStringsProvider
import com.example.interviewassistant.core.i18n.StringsProvider
import com.example.interviewassistant.core.network.JvmXunfeiAuthUrlFactory
import com.example.interviewassistant.core.network.XunfeiAuthUrlFactory
import com.example.interviewassistant.core.security.DesktopSecretStore
import com.example.interviewassistant.core.security.SecretStore
import com.russhwolf.settings.PreferencesSettings
import com.russhwolf.settings.Settings
import org.koin.dsl.module
import java.util.prefs.Preferences

actual fun platformModule() = module {
    single<AudioSource> { DesktopLoopbackAudioSource(get()) }
    single<XunfeiAuthUrlFactory> { JvmXunfeiAuthUrlFactory() }
    single<StringsProvider> { DesktopStringsProvider() }
    single<SecretStore> { DesktopSecretStore() }
    single<DatabaseDriverFactory> { DesktopDatabaseDriverFactory() }
    single<AppFileStore> { DesktopAppFileStore(get()) }
    single<Settings> {
        PreferencesSettings(Preferences.userRoot().node("com.example.interviewassistant"))
    }
}
