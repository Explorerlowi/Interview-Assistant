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
import com.example.interviewassistant.feature.interviewassistant.data.repository.OnDeviceSpeechGateway
import com.example.interviewassistant.feature.interviewassistant.data.repository.UnavailableOnDeviceSpeechGateway
import com.example.interviewassistant.feature.interviewassistant.data.repository.UnavailableSpeechModelManager
import com.example.interviewassistant.feature.interviewassistant.domain.repository.SpeechModelManager
import com.russhwolf.settings.NSUserDefaultsSettings
import com.russhwolf.settings.Settings
import org.koin.dsl.module
import org.koin.core.qualifier.named
import platform.Foundation.NSUserDefaults

actual fun platformModule() = module {
    single<AudioSource>(named(SESSION_AUDIO_SOURCE)) { UnsupportedIosAudioSource() }
    single<AudioSource>(named(TEST_AUDIO_SOURCE)) { UnsupportedIosAudioSource() }
    single<XunfeiAuthUrlFactory> { UnsupportedIosXunfeiAuthUrlFactory() }
    single<StringsProvider> { IOSStringsProvider() }
    single<SecretStore> { UnsupportedIosSecretStore() }
    single<DatabaseDriverFactory> { IosDatabaseDriverFactory() }
    single<AppFileStore> { UnsupportedIosFileStore() }
    single<SpeechModelManager> { UnavailableSpeechModelManager() }
    single<OnDeviceSpeechGateway>(named(SESSION_SENSEVOICE_GATEWAY)) { UnavailableOnDeviceSpeechGateway() }
    single<OnDeviceSpeechGateway>(named(TEST_SENSEVOICE_GATEWAY)) { UnavailableOnDeviceSpeechGateway() }
    single<Settings> {
        NSUserDefaultsSettings(NSUserDefaults.standardUserDefaults)
    }
}
