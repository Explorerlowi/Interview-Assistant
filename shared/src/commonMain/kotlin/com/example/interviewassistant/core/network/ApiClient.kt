package com.example.interviewassistant.core.network

import com.example.interviewassistant.core.error.AppError
import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpResponseValidator
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.http.takeFrom
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

/**
 * Shared Ktor HTTP client.
 *
 * Engine is provided by each platform via [createHttpClientEngine].
 *
 * @param baseUrl Optional API root URL. Provider clients generally pass absolute URLs.
 */
class ApiClient(
    private val baseUrl: String? = null,
) {
    val httpClient: HttpClient = HttpClient(createHttpClientEngine()) {
        expectSuccess = false

        defaultRequest {
            baseUrl?.takeIf(String::isNotBlank)?.let(url::takeFrom)
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

        install(HttpTimeout) {
            connectTimeoutMillis = CONNECT_TIMEOUT_MILLIS
            requestTimeoutMillis = REQUEST_TIMEOUT_MILLIS
            socketTimeoutMillis = SOCKET_TIMEOUT_MILLIS
        }

        install(Logging) {
            logger = object : Logger {
                override fun log(message: String) {
                    platformLog("Ktor", message)
                }
            }
            // Signed WebSocket URLs contain credentials, so provider traffic is never logged here.
            level = LogLevel.NONE
        }

        install(WebSockets) {
            pingInterval = 20_000
        }

        HttpResponseValidator {
            validateResponse { response ->
                if (response.status.value == 401) {
                    throw AppError.Unauthorized
                }
            }
        }
    }

    private companion object {
        const val CONNECT_TIMEOUT_MILLIS = 15_000L
        const val REQUEST_TIMEOUT_MILLIS = 60_000L
        const val SOCKET_TIMEOUT_MILLIS = 60_000L
    }
}
