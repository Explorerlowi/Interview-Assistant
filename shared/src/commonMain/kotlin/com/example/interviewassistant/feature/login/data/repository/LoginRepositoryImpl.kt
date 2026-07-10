package com.example.interviewassistant.feature.login.data.repository

import com.example.interviewassistant.core.error.AppError
import com.example.interviewassistant.core.error.AppResult
import com.example.interviewassistant.feature.login.data.local.LoginLocalDataSource
import com.example.interviewassistant.feature.login.data.remote.LoginRemoteDataSource
import com.example.interviewassistant.feature.login.domain.repository.LoginRepository
import kotlinx.coroutines.delay

/**
 * Sample login repository using mock remote/local sources.
 */
class LoginRepositoryImpl(
    private val remote: LoginRemoteDataSource,
    private val local: LoginLocalDataSource,
) : LoginRepository {
    override suspend fun login(username: String): AppResult<Boolean> {
        delay(500)
        return if (username.isNotBlank()) {
            val ok = remote.login(username)
            if (ok) {
                local.saveLastUsername(username)
                AppResult.Success(true)
            } else {
                AppResult.Error(AppError.Unauthorized)
            }
        } else {
            AppResult.Error(AppError.Unknown(IllegalArgumentException("Username cannot be empty")))
        }
    }
}
