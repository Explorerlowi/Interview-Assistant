package com.example.interviewassistant.feature.login.data.local

/**
 * In-memory local data source for the Login sample.
 */
class InMemoryLoginLocalDataSource : LoginLocalDataSource {
    private var lastUsername: String? = null

    override fun saveLastUsername(username: String) {
        lastUsername = username
    }

    override fun getLastUsername(): String? = lastUsername
}
