package com.example.interviewassistant.core.network

actual fun platformLog(tag: String, message: String) {
    println("[$tag] $message")
}
