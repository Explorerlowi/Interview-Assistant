package com.example.interviewassistant.feature.login.domain.usecase

import com.example.interviewassistant.core.error.AppResult
import com.example.interviewassistant.feature.login.domain.repository.LoginRepository

/**
 * Signs the user in with the given username.
 *
 * @param repository Login repository.
 */
class LoginUseCase(private val repository: LoginRepository) {
    suspend operator fun invoke(username: String): AppResult<Boolean> {
        return repository.login(username)
    }
}
