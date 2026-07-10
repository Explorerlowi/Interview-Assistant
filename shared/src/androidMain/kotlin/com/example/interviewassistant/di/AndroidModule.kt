package com.example.interviewassistant.di

import android.content.Context
import com.example.interviewassistant.core.i18n.AndroidStringsProvider
import com.example.interviewassistant.core.i18n.StringsProvider
import com.russhwolf.settings.Settings
import com.russhwolf.settings.SharedPreferencesSettings
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

actual fun platformModule() = module {
    single<StringsProvider> { AndroidStringsProvider(androidContext()) }
    single<Settings> {
        SharedPreferencesSettings(
            androidContext().getSharedPreferences("kmp_template_prefs", Context.MODE_PRIVATE),
        )
    }
}
