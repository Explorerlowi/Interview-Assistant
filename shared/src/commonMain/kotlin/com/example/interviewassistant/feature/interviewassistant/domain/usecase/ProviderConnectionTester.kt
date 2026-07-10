package com.example.interviewassistant.feature.interviewassistant.domain.usecase

import com.example.interviewassistant.core.network.XunfeiAuthUrlFactory
import com.example.interviewassistant.feature.interviewassistant.data.remote.llm.LlmMessage
import com.example.interviewassistant.feature.interviewassistant.data.remote.llm.OpenAiStreamGateway
import com.example.interviewassistant.feature.interviewassistant.domain.repository.ProviderConfigurationRepository
import io.ktor.client.HttpClient
import io.ktor.client.plugins.websocket.webSocket
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.http.HttpHeaders
import io.ktor.websocket.CloseReason
import io.ktor.websocket.close
import kotlinx.coroutines.flow.firstOrNull

/**
 * Provider identifiers used by settings connection-test results.
 */
enum class ProviderKind {
    PADDLE_OCR,
    XUNFEI,
    LLM,
}

/**
 * Result of one non-destructive provider connection test.
 */
data class ProviderConnectionResult(
    val success: Boolean,
    val message: String,
)

/**
 * Performs lightweight, non-destructive checks using the saved configuration.
 */
class ProviderConnectionTester(
    private val client: HttpClient,
    private val providers: ProviderConfigurationRepository,
    private val xunfeiAuthUrlFactory: XunfeiAuthUrlFactory,
    private val llmGateway: OpenAiStreamGateway,
) {
    /** Tests every configured provider and returns independent results. */
    suspend fun testAll(): Map<ProviderKind, ProviderConnectionResult> {
        return mapOf(
            ProviderKind.PADDLE_OCR to testPaddle(),
            ProviderKind.XUNFEI to testXunfei(),
            ProviderKind.LLM to testLlm(),
        )
    }

    private suspend fun testPaddle(): ProviderConnectionResult {
        val token = providers.paddleToken()
            ?: return missing("PaddleOCR token")
        return runCatching {
            val response = client.get(providers.configuration.value.paddle.endpoint) {
                header(HttpHeaders.Authorization, "Bearer $token")
            }
            if (response.status.value in setOf(401, 403)) {
                ProviderConnectionResult(false, "HTTP ${response.status.value}")
            } else {
                ProviderConnectionResult(true, "HTTP ${response.status.value}")
            }
        }.getOrElse(::failure)
    }

    private suspend fun testXunfei(): ProviderConnectionResult {
        val credentials = providers.xunfeiCredentials()
            ?: return missing("iFlytek credentials")
        return runCatching {
            val configuration = providers.configuration.value.xunfei
            val url = xunfeiAuthUrlFactory.create(
                configuration.endpoint,
                credentials.apiKey,
                credentials.apiSecret,
            )
            client.webSocket(urlString = url) {
                close(CloseReason(CloseReason.Codes.NORMAL, "configuration-test"))
            }
            ProviderConnectionResult(true, "WebSocket handshake succeeded")
        }.getOrElse(::failure)
    }

    private suspend fun testLlm(): ProviderConnectionResult {
        val apiKey = providers.llmApiKey()
            ?: return missing("Language-model API key")
        return runCatching {
            val chunk = llmGateway.stream(
                providers.configuration.value.llm,
                apiKey,
                listOf(
                    LlmMessage("system", "Return OK."),
                    LlmMessage("user", "OK"),
                ),
            ).firstOrNull()
            if (chunk == null) {
                ProviderConnectionResult(false, "Provider returned no stream data")
            } else {
                ProviderConnectionResult(true, "Streaming response succeeded")
            }
        }.getOrElse(::failure)
    }

    private fun missing(name: String): ProviderConnectionResult {
        return ProviderConnectionResult(false, "$name is not configured")
    }

    private fun failure(error: Throwable): ProviderConnectionResult {
        return ProviderConnectionResult(false, error.message ?: error::class.simpleName.orEmpty())
    }
}
