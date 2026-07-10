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
import com.russhwolf.settings.Settings
import com.russhwolf.settings.SharedPreferencesSettings
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

actual fun platformModule() = module {
    single<AudioSource> { AndroidMicrophoneAudioSource(androidContext(), get()) }
    single<XunfeiAuthUrlFactory> { JvmXunfeiAuthUrlFactory() }
    single<StringsProvider> { AndroidStringsProvider(androidContext()) }
    single<SecretStore> { AndroidSecretStore(androidContext()) }
    single<DatabaseDriverFactory> { AndroidDatabaseDriverFactory(androidContext()) }
    single<AppFileStore> { AndroidAppFileStore(androidContext(), get()) }
    single<Settings> {
        SharedPreferencesSettings(
            androidContext().getSharedPreferences("interview_assistant_prefs", Context.MODE_PRIVATE),
        )
    }
}
