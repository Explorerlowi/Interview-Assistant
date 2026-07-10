package com.example.interviewassistant.core.util

actual object TimeProvider {
    actual fun currentTimeMillis(): Long = System.currentTimeMillis()
}
