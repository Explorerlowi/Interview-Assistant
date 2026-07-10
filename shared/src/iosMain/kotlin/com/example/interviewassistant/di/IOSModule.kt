package com.example.interviewassistant.di

import com.example.interviewassistant.core.i18n.IOSStringsProvider
import com.example.interviewassistant.core.i18n.StringsProvider
import com.russhwolf.settings.NSUserDefaultsSettings
import com.russhwolf.settings.Settings
import org.koin.dsl.module
import platform.Foundation.NSUserDefaults

actual fun platformModule() = module {
    single<StringsProvider> { IOSStringsProvider() }
    single<Settings> {
        NSUserDefaultsSettings(NSUserDefaults.standardUserDefaults)
    }
}
