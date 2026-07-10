package com.example.interviewassistant.feature.interviewassistant.data.remote.ocr

import com.example.interviewassistant.core.error.AppError
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.forms.MultiPartFormDataContent
import io.ktor.client.request.forms.formData
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Provider-neutral OCR job state returned to the resume use case.
 */
data class RemoteOcrJob(
    val jobId: String,
    val state: String,
    val progressCurrent: Long,
    val progressTotal: Long,
    val resultUrl: String?,
    val errorMessage: String?,
)

/**
 * Parsed OCR document text plus provider image URLs keyed by relative markdown paths.
 */
data class OcrDocumentResult(
    val text: String,
    val images: Map<String, String>,
)

/**
 * Contract used by resume processing and deterministic tests.
 */
interface PaddleOcrGateway {
    /** Uploads a source document and returns the provider job identifier. */
    suspend fun submit(
        endpoint: String,
        token: String,
        model: String,
        fileName: String,
        mimeType: String,
        content: ByteArray,
    ): String

    /** Reads the latest state for [jobId]. */
    suspend fun query(endpoint: String, token: String, jobId: String): RemoteOcrJob

    /**
     * Downloads OCR JSONL from the provider's pre-signed result URL and extracts text plus image URLs.
     */
    suspend fun downloadDocument(resultUrl: String): OcrDocumentResult

    /** Downloads raw bytes from a provider asset URL without auth headers. */
    suspend fun downloadBytes(url: String): ByteArray
}

/**
 * Ktor implementation of PaddleOCR's asynchronous job API.
 */
class PaddleOcrRemoteDataSource(
    private val client: HttpClient,
    private val json: Json = defaultJson(),
) : PaddleOcrGateway {
    override suspend fun submit(
        endpoint: String,
        token: String,
        model: String,
        fileName: String,
        mimeType: String,
        content: ByteArray,
    ): String {
        val response = client.post(endpoint) {
            header(HttpHeaders.Authorization, authorizationHeader(token))
            setBody(
                MultiPartFormDataContent(
                    formData {
                        append("model", model)
                        append("optionalPayload", OPTIONAL_PAYLOAD)
                        append(
                            key = "file",
                            value = content,
                            headers = Headers.build {
                                append(HttpHeaders.ContentType, mimeType)
                                append(
                                    HttpHeaders.ContentDisposition,
                                    "filename=\"${fileName.sanitizedHeaderValue()}\"",
                                )
                            },
                        )
                    },
                ),
            )
        }
        val body = response.bodyAsText()
        ensureSuccess(response.status, body)
        return json.decodeFromString<SubmitJobResponse>(body).data.jobId
    }

    override suspend fun query(endpoint: String, token: String, jobId: String): RemoteOcrJob {
        val response = client.get("${endpoint.trimEnd('/')}/$jobId") {
            header(HttpHeaders.Authorization, authorizationHeader(token))
        }
        val body = response.bodyAsText()
        ensureSuccess(response.status, body)
        val data = json.decodeFromString<JobStatusResponse>(body).data
        return RemoteOcrJob(
            jobId = jobId,
            state = data.state,
            progressCurrent = data.extractProgress?.extractedPages ?: 0,
            progressTotal = data.extractProgress?.totalPages ?: 0,
            resultUrl = data.resultUrl?.jsonUrl,
            errorMessage = data.errorMsg,
        )
    }

    /**
     * Downloads OCR JSONL from the provider's pre-signed result URL.
     *
     * Must not send Content-Type / Authorization: signed object-storage URLs reject extra headers
     * with SignatureDoesNotMatch.
     */
    override suspend fun downloadDocument(resultUrl: String): OcrDocumentResult {
        val response = client.get(resultUrl)
        val body = response.bodyAsText()
        ensureSuccess(response.status, body)
        return parseOcrDocument(body, json)
    }

    override suspend fun downloadBytes(url: String): ByteArray {
        val response = client.get(url)
        if (response.status.value !in 200..299) {
            throw AppError.Server(response.status.value, "Failed to download OCR asset")
        }
        return response.body()
    }

    private fun authorizationHeader(token: String): String {
        val normalized = token.trim().removePrefix("Bearer ").removePrefix("bearer ").trim()
        return "bearer $normalized"
    }

    private fun ensureSuccess(status: HttpStatusCode, body: String) {
        if (status.value !in 200..299) {
            throw AppError.Server(status.value, body.take(MAX_ERROR_LENGTH))
        }
    }

    private fun String.sanitizedHeaderValue(): String = replace("\"", "_").replace("\r", "_").replace("\n", "_")

    private companion object {
        const val OPTIONAL_PAYLOAD =
            """{"useDocOrientationClassify":false,"useDocUnwarping":false,"useChartRecognition":false}"""
        const val MAX_ERROR_LENGTH = 500
    }
}

/**
 * Parses the JSONL result into ordered page text and a relative-path → URL image map.
 */
fun parseOcrDocument(content: String, json: Json = defaultJson()): OcrDocumentResult {
    val texts = mutableListOf<String>()
    val images = linkedMapOf<String, String>()
    content.lineSequence()
        .map(String::trim)
        .filter(String::isNotEmpty)
        .forEach { line ->
            json.decodeFromString<OcrJsonLine>(line)
                .result
                .layoutParsingResults
                .forEach { result ->
                    val text = result.markdown.text.trim()
                    if (text.isNotEmpty()) texts += text
                    result.markdown.images.forEach { (path, url) ->
                        if (path.isNotBlank() && url.isNotBlank()) {
                            images.putIfAbsent(path, url)
                        }
                    }
                }
        }
    return OcrDocumentResult(
        text = texts.joinToString(separator = "\n\n"),
        images = images,
    )
}

/**
 * Parses the JSONL result and returns page Markdown in provider order.
 */
fun parseJsonLines(content: String, json: Json = defaultJson()): String =
    parseOcrDocument(content, json).text

private fun defaultJson(): Json = Json {
    ignoreUnknownKeys = true
    isLenient = true
    explicitNulls = false
}

@Serializable
private data class SubmitJobResponse(
    val data: SubmitJobData,
)

@Serializable
private data class SubmitJobData(
    val jobId: String,
)

@Serializable
private data class JobStatusResponse(
    val data: JobStatusData,
)

@Serializable
private data class JobStatusData(
    val state: String,
    val extractProgress: ExtractProgress? = null,
    val resultUrl: ResultUrl? = null,
    val errorMsg: String? = null,
)

@Serializable
private data class ExtractProgress(
    val totalPages: Long = 0,
    val extractedPages: Long = 0,
)

@Serializable
private data class ResultUrl(
    val jsonUrl: String? = null,
)

@Serializable
private data class OcrJsonLine(
    val result: OcrJsonResult,
)

@Serializable
private data class OcrJsonResult(
    @SerialName("layoutParsingResults")
    val layoutParsingResults: List<LayoutParsingResult> = emptyList(),
)

@Serializable
private data class LayoutParsingResult(
    val markdown: MarkdownResult,
)

@Serializable
private data class MarkdownResult(
    val text: String = "",
    val images: Map<String, String> = emptyMap(),
)
