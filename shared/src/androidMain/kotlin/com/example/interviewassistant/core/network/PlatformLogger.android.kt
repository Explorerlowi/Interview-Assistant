package com.example.interviewassistant.core.network

import android.util.Log

actual fun platformLog(tag: String, message: String) {
    Log.d(tag, message)
}
