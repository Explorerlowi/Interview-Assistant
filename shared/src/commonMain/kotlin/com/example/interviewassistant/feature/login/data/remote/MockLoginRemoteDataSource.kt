package com.example.interviewassistant.feature.login.data.remote

/**
 * In-memory mock remote data source for the Login sample.
 */
class MockLoginRemoteDataSource : LoginRemoteDataSource {
    override suspend fun login(username: String): Boolean = username.isNotBlank()
}
