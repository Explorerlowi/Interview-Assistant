package com.example.interviewassistant.feature.login.data.local

/**
 * Local login cache / session placeholder.
 */
interface LoginLocalDataSource {
    fun saveLastUsername(username: String)
    fun getLastUsername(): String?
}
