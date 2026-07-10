package com.example.interviewassistant.core.network

import com.example.interviewassistant.core.error.AppError
import io.ktor.client.HttpClient
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.HttpResponseValidator
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.http.takeFrom
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

/**
 * Shared Ktor HTTP client.
 *
 * Engine is provided by each platform via [createHttpClientEngine].
 *
 * @param baseUrl API root URL; defaults to [ApiConfig.BASE_URL].
 */
class ApiClient(
    private val baseUrl: String = ApiConfig.BASE_URL,
) {
    val httpClient: HttpClient = HttpClient(createHttpClientEngine()) {
        expectSuccess = false

        defaultRequest {
            url.takeFrom(baseUrl)
        }

        install(ContentNegotiation) {
            json(
                Json {
                    prettyPrint = true
                    isLenient = true
                    ignoreUnknownKeys = true
                },
            )
        }

        install(Logging) {
            logger = object : Logger {
                override fun log(message: String) {
                    platformLog("Ktor", message)
                }
            }
            level = LogLevel.INFO
        }

        HttpResponseValidator {
            handleResponseExceptionWithRequest { exception, _ ->
                val clientException = exception as? ClientRequestException
                    ?: return@handleResponseExceptionWithRequest
                if (clientException.response.status.value == 401) {
                    throw AppError.Unauthorized
                }
            }
        }
    }
}
