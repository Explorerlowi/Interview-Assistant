package com.example.interviewassistant.feature.interviewassistant.data.remote.llm

import com.example.interviewassistant.core.error.AppError
import com.example.interviewassistant.feature.interviewassistant.domain.model.LlmConfiguration
import io.ktor.client.HttpClient
import io.ktor.client.request.accept
import io.ktor.client.request.header
import io.ktor.client.request.preparePost
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsChannel
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.readUTF8Line
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * One incremental OpenAI-compatible completion update.
 */
data class LlmChunk(
    val content: String = "",
    val reasoningContent: String = "",
    val finishReason: String? = null,
    val inputTokens: Long? = null,
    val outputTokens: Long? = null,
)

/**
 * One role/content message sent to an OpenAI-compatible provider.
 */
data class LlmMessage(
    val role: String,
    val content: String,
)

/**
 * Contract for streaming chat completions.
 */
interface OpenAiStreamGateway {
    /** Opens an SSE completion and emits parsed deltas. */
    fun stream(
        configuration: LlmConfiguration,
        apiKey: String,
        messages: List<LlmMessage>,
    ): Flow<LlmChunk>
}

/**
 * Raw Ktor implementation that remains compatible with configurable providers.
 */
class OpenAiStreamRemoteDataSource(
    private val client: HttpClient,
    private val json: Json = defaultLlmJson(),
) : OpenAiStreamGateway {
    override fun stream(
        configuration: LlmConfiguration,
        apiKey: String,
        messages: List<LlmMessage>,
    ): Flow<LlmChunk> = flow {
        client.preparePost(completionUrl(configuration.baseUrl)) {
            header(HttpHeaders.Authorization, "Bearer $apiKey")
            contentType(ContentType.Application.Json)
            accept(ContentType.Text.EventStream)
            setBody(requestBody(configuration, messages))
        }.execute { response ->
            if (response.status.value !in 200..299) {
                throw AppError.Server(response.status.value, response.bodyAsText().take(MAX_ERROR_LENGTH))
            }
            parseOpenAiSse(response.bodyAsChannel(), json) { emit(it) }
        }
    }

    private fun requestBody(
        configuration: LlmConfiguration,
        messages: List<LlmMessage>,
    ): JsonObject {
        return buildJsonObject {
            put("model", configuration.model)
            put("stream", true)
            put(
                "messages",
                JsonArray(
                    messages.map { message ->
                        buildJsonObject {
                            put("role", message.role)
                            put("content", message.content)
                        }
                    },
                ),
            )
            if (configuration.baseUrl.contains("deepseek.com", ignoreCase = true)) {
                put(
                    "thinking",
                    buildJsonObject {
                        put("type", if (configuration.thinkingEnabled) "enabled" else "disabled")
                    },
                )
            }
        }
    }

    private fun completionUrl(baseUrl: String): String {
        val normalized = baseUrl.trim().trimEnd('/')
        return if (normalized.endsWith("/chat/completions")) {
            normalized
        } else {
            "$normalized/chat/completions"
        }
    }

    private companion object {
        const val MAX_ERROR_LENGTH = 1_000
    }
}

/**
 * Parses an OpenAI SSE byte channel, including fragmented lines and `[DONE]`.
 */
suspend fun parseOpenAiSse(
    channel: ByteReadChannel,
    json: Json = defaultLlmJson(),
    onChunk: suspend (LlmChunk) -> Unit,
) {
    val eventData = mutableListOf<String>()

    suspend fun flushEvent(): Boolean {
        if (eventData.isEmpty()) return false
        val payload = eventData.joinToString(separator = "\n").trim()
        eventData.clear()
        if (payload == "[DONE]") return true
        val event = json.decodeFromString<OpenAiStreamEvent>(payload)
        event.error?.let {
            val message = listOfNotNull(it.code?.toString(), it.message)
                .joinToString(separator = ": ")
            throw AppError.Server(-1, message)
        }
        val choice = event.choices.firstOrNull()
        val usage = event.usage
        val chunk = LlmChunk(
            content = choice?.delta?.content.orEmpty(),
            reasoningContent = choice?.delta?.reasoningContent.orEmpty(),
            finishReason = choice?.finishReason,
            inputTokens = usage?.promptTokens,
            outputTokens = usage?.completionTokens,
        )
        if (
            chunk.content.isNotEmpty() ||
            chunk.reasoningContent.isNotEmpty() ||
            chunk.finishReason != null ||
            usage != null
        ) {
            onChunk(chunk)
        }
        return false
    }

    while (!channel.isClosedForRead) {
        val line = channel.readUTF8Line() ?: break
        when {
            line.isEmpty() -> if (flushEvent()) return
            line.startsWith("data:") -> eventData += line.removePrefix("data:").trimStart()
            line.startsWith(":") -> Unit
        }
    }
    flushEvent()
}

private fun defaultLlmJson(): Json = Json {
    ignoreUnknownKeys = true
    explicitNulls = false
}

@Serializable
private data class OpenAiStreamEvent(
    val choices: List<OpenAiChoice> = emptyList(),
    val usage: OpenAiUsage? = null,
    val error: OpenAiError? = null,
)

@Serializable
private data class OpenAiChoice(
    val delta: OpenAiDelta = OpenAiDelta(),
    @SerialName("finish_reason")
    val finishReason: String? = null,
)

@Serializable
private data class OpenAiDelta(
    val content: String? = null,
    @SerialName("reasoning_content")
    val reasoningContent: String? = null,
)

@Serializable
private data class OpenAiUsage(
    @SerialName("prompt_tokens")
    val promptTokens: Long? = null,
    @SerialName("completion_tokens")
    val completionTokens: Long? = null,
)

@Serializable
private data class OpenAiError(
    val message: String,
    val code: JsonElement? = null,
)
