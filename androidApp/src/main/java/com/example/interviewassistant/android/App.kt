package com.example.interviewassistant.android

import android.app.Application
import com.example.interviewassistant.di.initKoin
import org.koin.android.ext.koin.androidContext

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        
        initKoin {
            androidContext(this@App)
        }
    }
}
