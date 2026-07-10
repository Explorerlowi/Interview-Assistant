package com.example.interviewassistant.core.error

/**
 * Unified application error types exposed by repositories and use cases.
 */
sealed class AppError : Throwable() {
    data object Network : AppError() {
        private fun readResolve(): Any = Network
    }

    data class Server(val code: Int, override val message: String?) : AppError()

    data object Unauthorized : AppError() {
        private fun readResolve(): Any = Unauthorized
    }

    data class Unknown(override val cause: Throwable? = null) : AppError() {
        override val message: String?
            get() = cause?.message
    }
}

/**
 * Unified result wrapper for domain and data layer APIs.
 */
sealed class AppResult<out T> {
    data class Success<T>(val data: T) : AppResult<T>()
    data class Error(val error: AppError) : AppResult<Nothing>()

    val isSuccess: Boolean get() = this is Success
    val isError: Boolean get() = this is Error

    fun getOrNull(): T? = (this as? Success)?.data
    fun errorOrNull(): AppError? = (this as? Error)?.error

    inline fun onSuccess(action: (T) -> Unit): AppResult<T> {
        if (this is Success) action(data)
        return this
    }

    inline fun onError(action: (AppError) -> Unit): AppResult<T> {
        if (this is Error) action(error)
        return this
    }
}
