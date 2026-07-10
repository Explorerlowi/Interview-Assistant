package com.example.interviewassistant.di

import com.example.interviewassistant.core.audio.AudioSource
import com.example.interviewassistant.core.audio.UnsupportedIosAudioSource
import com.example.interviewassistant.core.database.DatabaseDriverFactory
import com.example.interviewassistant.core.database.IosDatabaseDriverFactory
import com.example.interviewassistant.core.file.AppFileStore
import com.example.interviewassistant.core.file.UnsupportedIosFileStore
import com.example.interviewassistant.core.i18n.IOSStringsProvider
import com.example.interviewassistant.core.i18n.StringsProvider
import com.example.interviewassistant.core.network.UnsupportedIosXunfeiAuthUrlFactory
import com.example.interviewassistant.core.network.XunfeiAuthUrlFactory
import com.example.interviewassistant.core.security.SecretStore
import com.example.interviewassistant.core.security.UnsupportedIosSecretStore
import com.russhwolf.settings.NSUserDefaultsSettings
import com.russhwolf.settings.Settings
import org.koin.dsl.module
import platform.Foundation.NSUserDefaults

actual fun platformModule() = module {
    single<AudioSource> { UnsupportedIosAudioSource() }
    single<XunfeiAuthUrlFactory> { UnsupportedIosXunfeiAuthUrlFactory() }
    single<StringsProvider> { IOSStringsProvider() }
    single<SecretStore> { UnsupportedIosSecretStore() }
    single<DatabaseDriverFactory> { IosDatabaseDriverFactory() }
    single<AppFileStore> { UnsupportedIosFileStore() }
    single<Settings> {
        NSUserDefaultsSettings(NSUserDefaults.standardUserDefaults)
    }
}
