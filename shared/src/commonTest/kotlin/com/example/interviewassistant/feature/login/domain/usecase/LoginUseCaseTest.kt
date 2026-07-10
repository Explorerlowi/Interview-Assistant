package com.example.interviewassistant.feature.login.domain.usecase

import com.example.interviewassistant.core.error.AppError
import com.example.interviewassistant.core.error.AppResult
import com.example.interviewassistant.feature.login.domain.repository.LoginRepository
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class LoginUseCaseTest {

    class MockLoginRepository : LoginRepository {
        var shouldSucceed = true
        override suspend fun login(username: String): AppResult<Boolean> {
            return if (shouldSucceed) {
                AppResult.Success(true)
            } else {
                AppResult.Error(AppError.Unknown(Exception("Error")))
            }
        }
    }

    @Test
    fun `login success returns success result`() = runTest {
        val repository = MockLoginRepository()
        val useCase = LoginUseCase(repository)

        val result = useCase("testuser")

        assertTrue(result.isSuccess)
        assertEquals(true, result.getOrNull())
    }

    @Test
    fun `login failure returns failure result`() = runTest {
        val repository = MockLoginRepository().apply { shouldSucceed = false }
        val useCase = LoginUseCase(repository)

        val result = useCase("testuser")

        assertTrue(result.isError)
    }
}
