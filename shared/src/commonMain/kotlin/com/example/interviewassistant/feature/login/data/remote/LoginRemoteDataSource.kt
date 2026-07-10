package com.example.interviewassistant.feature.login.data.remote

/**
 * Remote login API placeholder.
 *
 * Wire this to [com.example.interviewassistant.core.network.ApiClient] when a real backend exists.
 */
interface LoginRemoteDataSource {
    suspend fun login(username: String): Boolean
}
