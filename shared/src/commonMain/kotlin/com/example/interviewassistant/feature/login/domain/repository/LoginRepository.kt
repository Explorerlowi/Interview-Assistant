package com.example.interviewassistant.feature.login.domain.repository

import com.example.interviewassistant.core.error.AppResult

/**
 * Login repository contract.
 */
interface LoginRepository {
    /**
     * Attempts to sign in with [username].
     *
     * @return [AppResult.Success] when credentials are accepted.
     */
    suspend fun login(username: String): AppResult<Boolean>
}
