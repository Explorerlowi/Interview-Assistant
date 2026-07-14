package com.example.interviewassistant.feature.interviewassistant.data.remote.speech

import com.example.interviewassistant.core.audio.AudioFrame
import com.example.interviewassistant.core.network.XunfeiAuthUrlFactory
import com.example.interviewassistant.core.util.TimeProvider
import com.example.interviewassistant.feature.interviewassistant.domain.model.XunfeiConfiguration
import com.example.interviewassistant.feature.interviewassistant.domain.model.XunfeiCredentials
import io.ktor.client.HttpClient
import io.ktor.client.plugins.websocket.webSocket
import io.ktor.util.encodeBase64
import io.ktor.websocket.Frame
import io.ktor.websocket.readText
import io.ktor.websocket.send
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.takeWhile
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Incremental recognition events emitted by the iFlytek data source.
 */
sealed interface SpeechRecognitionEvent {
    data class Partial(val text: String) : SpeechRecognitionEvent
    data class Final(val text: String) : SpeechRecognitionEvent
    data class Failure(val code: Int?, val message: String) : SpeechRecognitionEvent
    data object SessionRotated : SpeechRecognitionEvent
}

/**
 * Contract for one iFlytek WebSocket recognition session.
 */
interface XunfeiSpeechGateway {
    /**
     * Streams audio until endpoint detection or the provider's safe session limit.
     */
    fun recognizeSession(
        audio: Flow<AudioFrame>,
        configuration: XunfeiConfiguration,
        credentials: XunfeiCredentials,
    ): Flow<SpeechRecognitionEvent>
}

/**
 * Ktor WebSocket implementation of iFlytek streaming dictation.
 */
class XunfeiSpeechRemoteDataSource(
    private val client: HttpClient,
    private val authUrlFactory: XunfeiAuthUrlFactory,
    private val json: Json = defaultSpeechJson(),
) : XunfeiSpeechGateway {
    override fun recognizeSession(
        audio: Flow<AudioFrame>,
        configuration: XunfeiConfiguration,
        credentials: XunfeiCredentials,
    ): Flow<SpeechRecognitionEvent> = channelFlow {
        val url = authUrlFactory.create(
            endpoint = configuration.endpoint,
            apiKey = credentials.apiKey,
            apiSecret = credentials.apiSecret,
        )
        val reducer = XunfeiTranscriptReducer()
        val startedAt = TimeProvider.currentTimeMillis()
        var sentFirstFrame = false
        var rotatedByLimit = false
        var lastPartialText = ""
        var emittedFinal = false

        client.webSocket(urlString = url) {
            val remoteDone = CompletableDeferred<Unit>()
            val receiver = launch {
                for (frame in incoming) {
                    if (frame !is Frame.Text) continue
                    val response = try {
                        json.decodeFromString<XunfeiResponse>(frame.readText())
                    } catch (error: Throwable) {
                        this@channelFlow.send(
                            SpeechRecognitionEvent.Failure(null, error.message ?: "Invalid iFlytek response"),
                        )
                        remoteDone.complete(Unit)
                        break
                    }
                    if (response.code != 0) {
                        this@channelFlow.send(
                            SpeechRecognitionEvent.Failure(response.code, response.message),
                        )
                        remoteDone.complete(Unit)
                        break
                    }
                    val result = response.data?.result
                    if (result != null) {
                        val text = reducer.accept(result)
                        if (response.data.status == STATUS_LAST) {
                            this@channelFlow.send(SpeechRecognitionEvent.Final(text))
                            emittedFinal = true
                            remoteDone.complete(Unit)
                            break
                        } else {
                            lastPartialText = text
                            this@channelFlow.send(SpeechRecognitionEvent.Partial(text))
                        }
                    }
                }
                remoteDone.complete(Unit)
            }

            audio.takeWhile {
                val withinLimit = !XunfeiSessionPolicy.shouldRotate(
                    startedAtMillis = startedAt,
                    nowMillis = TimeProvider.currentTimeMillis(),
                )
                if (!withinLimit) rotatedByLimit = true
                withinLimit && !remoteDone.isCompleted
            }.collect { frame ->
                val status = if (sentFirstFrame) STATUS_CONTINUE else STATUS_FIRST
                send(
                    Frame.Text(
                        json.encodeToString(
                            request(
                                status = status,
                                audio = frame.pcm,
                                configuration = configuration,
                                appId = credentials.appId,
                                includeMetadata = !sentFirstFrame,
                            ),
                        ),
                    ),
                )
                sentFirstFrame = true
            }

            if (sentFirstFrame && !remoteDone.isCompleted) {
                send(
                    Frame.Text(
                        json.encodeToString(
                            request(
                                status = STATUS_LAST,
                                audio = ByteArray(0),
                                configuration = configuration,
                                appId = credentials.appId,
                                includeMetadata = false,
                            ),
                        ),
                    ),
                )
            }
            withTimeoutOrNull(RECEIVER_GRACE_MILLIS) {
                receiver.join()
            }
            receiver.cancel()
        }
        if (rotatedByLimit) {
            val pending = XunfeiSessionPolicy.pendingFinalOnRotate(
                lastPartialText = lastPartialText,
                emittedFinal = emittedFinal,
            )
            if (pending != null) {
                send(SpeechRecognitionEvent.Final(pending))
            }
            send(SpeechRecognitionEvent.SessionRotated)
        }
    }

    private fun request(
        status: Int,
        audio: ByteArray,
        configuration: XunfeiConfiguration,
        appId: String,
        includeMetadata: Boolean,
    ): XunfeiRequest {
        return XunfeiRequest(
            common = if (includeMetadata) XunfeiCommon(appId) else null,
            business = if (includeMetadata) {
                XunfeiBusiness(
                    language = configuration.language,
                    domain = configuration.domain,
                    accent = configuration.accent,
                    eos = configuration.endOfSpeechMillis,
                )
            } else {
                null
            },
            data = XunfeiAudioData(
                status = status,
                format = AUDIO_FORMAT,
                encoding = AUDIO_ENCODING,
                audio = audio.encodeBase64(),
            ),
        )
    }

    private companion object {
        const val STATUS_FIRST = 0
        const val STATUS_CONTINUE = 1
        const val STATUS_LAST = 2
        const val RECEIVER_GRACE_MILLIS = 3_000L
        const val AUDIO_FORMAT = "audio/L16;rate=16000"
        const val AUDIO_ENCODING = "raw"
    }
}

/**
 * Keeps each provider session below iFlytek's hard 60-second limit.
 */
object XunfeiSessionPolicy {
    const val SAFE_SESSION_MILLIS = 55_000L

    /** Returns whether the active connection should be finalized and replaced. */
    fun shouldRotate(startedAtMillis: Long, nowMillis: Long): Boolean {
        return nowMillis - startedAtMillis >= SAFE_SESSION_MILLIS
    }

    /**
     * Returns pending partial text that should be flushed as a synthetic Final before
     * [SpeechRecognitionEvent.SessionRotated], or null when nothing needs flushing.
     */
    fun pendingFinalOnRotate(lastPartialText: String, emittedFinal: Boolean): String? {
        if (emittedFinal) return null
        return lastPartialText.trim().takeIf { it.isNotEmpty() }
    }
}

/**
 * Applies iFlytek `apd` and `rpl` dynamic-correction semantics.
 */
class XunfeiTranscriptReducer {
    private val segments = mutableMapOf<Int, String>()

    /**
     * Merges one provider result and returns the complete current transcript.
     */
    fun accept(result: XunfeiResult): String {
        if (result.pgs == "rpl") {
            val range = result.rg
            if (range != null && range.size >= 2) {
                for (index in range[0]..range[1]) segments.remove(index)
            }
        }
        val text = result.ws.joinToString(separator = "") { word ->
            word.cw.firstOrNull()?.word.orEmpty()
        }
        segments[result.sn] = text
        return segments.toSortedMap().values.joinToString(separator = "")
    }

    /** Clears all accumulated segments before a new independent question. */
    fun reset() {
        segments.clear()
    }
}

private fun defaultSpeechJson(): Json = Json {
    ignoreUnknownKeys = true
    explicitNulls = false
}

@Serializable
private data class XunfeiRequest(
    val common: XunfeiCommon? = null,
    val business: XunfeiBusiness? = null,
    val data: XunfeiAudioData,
)

@Serializable
private data class XunfeiCommon(
    @SerialName("app_id")
    val appId: String,
)

@Serializable
private data class XunfeiBusiness(
    val language: String,
    val domain: String,
    val accent: String,
    val eos: Int,
    val dwa: String = "wpgs",
    val ptt: Int = 1,
)

@Serializable
private data class XunfeiAudioData(
    val status: Int,
    val format: String,
    val encoding: String,
    val audio: String,
)

@Serializable
private data class XunfeiResponse(
    val code: Int,
    val message: String = "",
    val sid: String? = null,
    val data: XunfeiResponseData? = null,
)

@Serializable
private data class XunfeiResponseData(
    val status: Int = 0,
    val result: XunfeiResult? = null,
)

@Serializable
data class XunfeiResult(
    val sn: Int,
    val pgs: String? = null,
    val rg: List<Int>? = null,
    val ws: List<XunfeiWordSegment> = emptyList(),
)

@Serializable
data class XunfeiWordSegment(
    val cw: List<XunfeiCandidateWord> = emptyList(),
)

@Serializable
data class XunfeiCandidateWord(
    @SerialName("w")
    val word: String = "",
)
