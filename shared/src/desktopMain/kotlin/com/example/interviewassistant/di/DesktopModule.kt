package com.example.interviewassistant.di

import com.example.interviewassistant.core.audio.AudioSource
import com.example.interviewassistant.core.audio.DesktopLoopbackAudioSource
import com.example.interviewassistant.core.audio.DesktopMicrophoneAudioSource
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
import com.example.interviewassistant.feature.interviewassistant.data.repository.OnDeviceSpeechGateway
import com.example.interviewassistant.feature.interviewassistant.domain.repository.SpeechModelManager
import com.example.interviewassistant.feature.interviewassistant.speech.DesktopSenseVoiceModelManager
import com.example.interviewassistant.feature.interviewassistant.speech.DesktopSenseVoiceSpeechGateway
import com.russhwolf.settings.PreferencesSettings
import com.russhwolf.settings.Settings
import org.koin.dsl.module
import org.koin.core.qualifier.named
import java.util.prefs.Preferences

actual fun platformModule() = module {
    single<AudioSource>(named(SESSION_AUDIO_SOURCE)) { DesktopLoopbackAudioSource(get()) }
    single<AudioSource>(named(TEST_AUDIO_SOURCE)) { DesktopMicrophoneAudioSource(get()) }
    single<XunfeiAuthUrlFactory> { JvmXunfeiAuthUrlFactory() }
    single<StringsProvider> { DesktopStringsProvider() }
    single<SecretStore> { DesktopSecretStore() }
    single<DatabaseDriverFactory> { DesktopDatabaseDriverFactory() }
    single<AppFileStore> { DesktopAppFileStore(get()) }
    single { DesktopSenseVoiceModelManager(get()) }
    single<SpeechModelManager> { get<DesktopSenseVoiceModelManager>() }
    single<OnDeviceSpeechGateway>(named(SESSION_SENSEVOICE_GATEWAY)) {
        DesktopSenseVoiceSpeechGateway(get(), get())
    }
    single<OnDeviceSpeechGateway>(named(TEST_SENSEVOICE_GATEWAY)) {
        DesktopSenseVoiceSpeechGateway(get(), get())
    }
    single<Settings> {
        PreferencesSettings(Preferences.userRoot().node("com.example.interviewassistant"))
    }
}
