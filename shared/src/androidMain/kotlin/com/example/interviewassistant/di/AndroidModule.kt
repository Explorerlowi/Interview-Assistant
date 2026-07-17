package com.example.interviewassistant.di

import android.content.Context
import com.example.interviewassistant.core.audio.AndroidMicrophoneAudioSource
import com.example.interviewassistant.core.audio.AudioSource
import com.example.interviewassistant.core.database.AndroidDatabaseDriverFactory
import com.example.interviewassistant.core.database.DatabaseDriverFactory
import com.example.interviewassistant.core.file.AndroidAppFileStore
import com.example.interviewassistant.core.file.AppFileStore
import com.example.interviewassistant.core.i18n.AndroidStringsProvider
import com.example.interviewassistant.core.i18n.StringsProvider
import com.example.interviewassistant.core.network.JvmXunfeiAuthUrlFactory
import com.example.interviewassistant.core.network.XunfeiAuthUrlFactory
import com.example.interviewassistant.core.security.AndroidSecretStore
import com.example.interviewassistant.core.security.SecretStore
import com.example.interviewassistant.feature.interviewassistant.data.repository.OnDeviceSpeechGateway
import com.example.interviewassistant.feature.interviewassistant.domain.repository.SpeechModelManager
import com.example.interviewassistant.feature.interviewassistant.speech.AndroidSenseVoiceModelManager
import com.example.interviewassistant.feature.interviewassistant.speech.AndroidSenseVoiceSpeechGateway
import com.russhwolf.settings.Settings
import com.russhwolf.settings.SharedPreferencesSettings
import org.koin.android.ext.koin.androidContext
import org.koin.core.qualifier.named
import org.koin.dsl.module

actual fun platformModule() = module {
    single<AudioSource>(named(SESSION_AUDIO_SOURCE)) { AndroidMicrophoneAudioSource(androidContext(), get()) }
    single<AudioSource>(named(TEST_AUDIO_SOURCE)) { AndroidMicrophoneAudioSource(androidContext(), get()) }
    single<XunfeiAuthUrlFactory> { JvmXunfeiAuthUrlFactory() }
    single<StringsProvider> { AndroidStringsProvider(androidContext()) }
    single<SecretStore> { AndroidSecretStore(androidContext()) }
    single<DatabaseDriverFactory> { AndroidDatabaseDriverFactory(androidContext()) }
    single<AppFileStore> { AndroidAppFileStore(androidContext(), get()) }
    single { AndroidSenseVoiceModelManager(androidContext(), get()) }
    single<SpeechModelManager> { get<AndroidSenseVoiceModelManager>() }
    single<OnDeviceSpeechGateway>(named(SESSION_SENSEVOICE_GATEWAY)) {
        AndroidSenseVoiceSpeechGateway(get(), get())
    }
    single<OnDeviceSpeechGateway>(named(TEST_SENSEVOICE_GATEWAY)) {
        AndroidSenseVoiceSpeechGateway(get(), get())
    }
    single<Settings> {
        SharedPreferencesSettings(
            androidContext().getSharedPreferences("interview_assistant_prefs", Context.MODE_PRIVATE),
        )
    }
}
