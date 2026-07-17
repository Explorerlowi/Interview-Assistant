package com.example.interviewassistant.feature.interviewassistant.speech

import android.content.Context
import android.util.Log
import com.example.interviewassistant.core.util.CoroutineDispatcherProvider
import com.example.interviewassistant.feature.interviewassistant.domain.repository.SpeechModelManager
import com.example.interviewassistant.feature.interviewassistant.domain.repository.SpeechModelDescriptor
import com.example.interviewassistant.feature.interviewassistant.domain.repository.SpeechModelFailure
import com.example.interviewassistant.feature.interviewassistant.domain.repository.SpeechModelState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.net.ConnectException
import java.net.HttpURLConnection
import java.net.NoRouteToHostException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.net.URL
import java.security.MessageDigest
import kotlin.coroutines.coroutineContext

/**
 * Absolute paths required to initialize the Android SenseVoice runtime.
 *
 * @property model Quantized SenseVoice ONNX model.
 * @property tokens Token table paired with the exported model.
 * @property vad Silero voice-activity detector.
 */
data class AndroidSenseVoiceModelFiles(
    val model: String,
    val tokens: String,
    val vad: String,
)

/**
 * Downloads and verifies the SenseVoice Android model in application-private storage.
 */
class AndroidSenseVoiceModelManager(
    context: Context,
    private val dispatchers: CoroutineDispatcherProvider,
) : SpeechModelManager {
    private val modelsDirectory = File(context.filesDir, MODELS_DIRECTORY)
    private val modelDirectory = File(modelsDirectory, "sensevoice-$MODEL_VERSION")
    private val installMutex = Mutex()
    private val mutableState = MutableStateFlow(initialState())

    override val descriptor = SpeechModelDescriptor(
        name = "SenseVoiceSmall",
        version = "2024-07-17",
        quantization = "INT8",
    )
    override val state: StateFlow<SpeechModelState> = mutableState.asStateFlow()

    override suspend fun install() {
        if (state.value == SpeechModelState.Ready || state.value.isInstalling()) return
        installMutex.withLock {
            if (state.value == SpeechModelState.Ready || state.value.isInstalling()) return
            mutableState.value = SpeechModelState.Preparing
            try {
                withContext(dispatchers.io) {
                    removeLegacyModels()
                    if (!modelDirectory.exists() && !modelDirectory.mkdirs()) {
                        throw ModelStorageException("Unable to create the model directory")
                    }
                    ensureAvailableStorage()
                    var completedBytes = 0L
                    MODEL_FILES.forEach { specification ->
                        installFile(specification, completedBytes)
                        completedBytes += specification.sizeBytes
                    }
                    markerFile().writeText(MODEL_VERSION)
                    mutableState.value = SpeechModelState.Ready
                }
            } catch (cancelled: CancellationException) {
                mutableState.value = SpeechModelState.NotInstalled
                throw cancelled
            } catch (error: Throwable) {
                Log.e(TAG, "Unable to install SenseVoice model", error)
                mutableState.value = SpeechModelState.Failed(error.toFailureReason())
            }
        }
    }

    override suspend fun delete() {
        installMutex.withLock {
            try {
                withContext(dispatchers.io) {
                    modelDirectory.deleteRecursively()
                    mutableState.value = SpeechModelState.NotInstalled
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Throwable) {
                Log.e(TAG, "Unable to delete SenseVoice model", error)
                mutableState.value = SpeechModelState.Failed(SpeechModelFailure.STORAGE)
            }
        }
    }

    /**
     * Returns verified model paths, or `null` when installation has not completed.
     */
    fun modelFilesOrNull(): AndroidSenseVoiceModelFiles? {
        if (state.value != SpeechModelState.Ready) return null
        return AndroidSenseVoiceModelFiles(
            model = destination(MODEL_FILES[0]).absolutePath,
            tokens = destination(MODEL_FILES[1]).absolutePath,
            vad = destination(MODEL_FILES[2]).absolutePath,
        )
    }

    private suspend fun installFile(specification: ModelFile, completedBytes: Long) {
        val destination = destination(specification)
        if (destination.length() == specification.sizeBytes && destination.sha256() == specification.sha256) {
            mutableState.value = SpeechModelState.Downloading(
                downloadedBytes = completedBytes + specification.sizeBytes,
                totalBytes = TOTAL_MODEL_BYTES,
            )
            return
        }

        destination.delete()
        val partial = File(destination.parentFile, "${destination.name}.part")
        if (partial.length() > specification.sizeBytes) partial.delete()
        var existingBytes = partial.length()

        val connection = openConnection(specification.urls, existingBytes)
        val append = existingBytes > 0L && connection.responseCode == HttpURLConnection.HTTP_PARTIAL
        if (!append) {
            partial.delete()
            existingBytes = 0L
        }
        mutableState.value = SpeechModelState.Downloading(
            downloadedBytes = completedBytes + existingBytes,
            totalBytes = TOTAL_MODEL_BYTES,
        )
        try {
            val rawInput = try {
                connection.inputStream
            } catch (error: IOException) {
                throw ModelNetworkException(error)
            }
            rawInput.use {
                BufferedInputStream(it).use { input ->
                    val fileOutput = try {
                        FileOutputStream(partial, append)
                    } catch (error: IOException) {
                        throw ModelStorageException("Unable to open the partial model file", error)
                    }
                    BufferedOutputStream(fileOutput).use { output ->
                        val buffer = ByteArray(DOWNLOAD_BUFFER_BYTES)
                        var currentBytes = existingBytes
                        while (true) {
                            coroutineContext.ensureActive()
                            val count = try {
                                input.read(buffer)
                            } catch (error: IOException) {
                                throw ModelNetworkException(error)
                            }
                            if (count < 0) break
                            try {
                                output.write(buffer, 0, count)
                            } catch (error: IOException) {
                                throw ModelStorageException("Unable to write the partial model file", error)
                            }
                            currentBytes += count
                            mutableState.value = SpeechModelState.Downloading(
                                downloadedBytes = completedBytes + currentBytes,
                                totalBytes = TOTAL_MODEL_BYTES,
                            )
                        }
                    }
                }
            }
        } finally {
            connection.disconnect()
        }

        if (partial.length() != specification.sizeBytes) {
            throw ModelVerificationException(
                "Unexpected ${specification.name} size: ${partial.length()}",
            )
        }
        if (partial.sha256() != specification.sha256) {
            partial.delete()
            throw ModelVerificationException("Checksum verification failed for ${specification.name}")
        }
        if (!partial.renameTo(destination)) {
            throw ModelStorageException("Unable to activate ${specification.name}")
        }
    }

    private fun openConnection(urls: List<String>, existingBytes: Long): HttpURLConnection {
        var lastFailure: Throwable? = null
        urls.forEachIndexed { index, url ->
            var connection: HttpURLConnection? = null
            try {
                connection = (URL(url).openConnection() as HttpURLConnection).apply {
                    instanceFollowRedirects = true
                    connectTimeout = CONNECT_TIMEOUT_MILLIS
                    readTimeout = READ_TIMEOUT_MILLIS
                    setRequestProperty("User-Agent", USER_AGENT)
                    if (existingBytes > 0L) setRequestProperty("Range", "bytes=$existingBytes-")
                    connect()
                }
                if (connection.responseCode !in 200..299) {
                    throw ModelHttpException(connection.responseCode)
                }
                return connection
            } catch (error: Throwable) {
                if (error is CancellationException) throw error
                connection?.disconnect()
                lastFailure = error
                if (index < urls.lastIndex) {
                    Log.w(TAG, "Model source failed, trying fallback: ${URL(url).host}", error)
                }
            }
        }
        throw lastFailure ?: ModelNetworkException()
    }

    private fun initialState(): SpeechModelState {
        if (markerFile().readTextOrNull() != MODEL_VERSION) return SpeechModelState.NotInstalled
        val complete = MODEL_FILES.all { destination(it).length() == it.sizeBytes }
        return if (complete) SpeechModelState.Ready else SpeechModelState.NotInstalled
    }

    private fun destination(specification: ModelFile): File = File(modelDirectory, specification.name)

    private fun markerFile(): File = File(modelDirectory, INSTALL_MARKER)

    private fun removeLegacyModels() {
        modelsDirectory.listFiles()
            ?.filter { it.isDirectory && it != modelDirectory && it.name.startsWith("sensevoice-") }
            ?.forEach(File::deleteRecursively)
    }

    private fun ensureAvailableStorage() {
        val reusableBytes = MODEL_FILES.sumOf { specification ->
            val destination = destination(specification)
            if (destination.length() == specification.sizeBytes) {
                specification.sizeBytes
            } else {
                File(modelDirectory, "${specification.name}.part").length()
                    .coerceAtMost(specification.sizeBytes)
            }
        }
        val requiredBytes = (TOTAL_MODEL_BYTES - reusableBytes) + MINIMUM_FREE_SPACE_BYTES
        if (modelDirectory.usableSpace < requiredBytes) {
            throw ModelStorageException("Not enough free storage for the SenseVoice model")
        }
    }

    private fun SpeechModelState.isInstalling(): Boolean =
        this == SpeechModelState.Preparing || this is SpeechModelState.Downloading

    private fun Throwable.toFailureReason(): SpeechModelFailure {
        val causes = generateSequence(this as Throwable?) { it.cause }.toList()
        return when {
            causes.any { it is ModelStorageException } -> SpeechModelFailure.STORAGE
            causes.any { it is ModelVerificationException } -> SpeechModelFailure.VERIFICATION
            causes.any { it is SocketTimeoutException } -> SpeechModelFailure.TIMEOUT
            causes.any { it is ModelHttpException } -> SpeechModelFailure.SERVER
            causes.any {
                it is UnknownHostException ||
                    it is ConnectException ||
                    it is NoRouteToHostException ||
                    it is ModelNetworkException
            } -> SpeechModelFailure.NETWORK
            else -> SpeechModelFailure.UNKNOWN
        }
    }

    private fun File.readTextOrNull(): String? = runCatching { readText() }.getOrNull()

    private fun File.sha256(): String {
        val digest = MessageDigest.getInstance("SHA-256")
        FileInputStream(this).use { input ->
            val buffer = ByteArray(HASH_BUFFER_BYTES)
            while (true) {
                val count = input.read(buffer)
                if (count < 0) break
                digest.update(buffer, 0, count)
            }
        }
        return digest.digest().joinToString(separator = "") { byte ->
            (byte.toInt() and 0xff).toString(radix = 16).padStart(length = 2, padChar = '0')
        }
    }

    private data class ModelFile(
        val name: String,
        val urls: List<String>,
        val sizeBytes: Long,
        val sha256: String,
    )

    private class ModelNetworkException(cause: Throwable? = null) : IOException(cause)

    private class ModelHttpException(val statusCode: Int) : IOException("Model source returned HTTP $statusCode")

    private class ModelStorageException(
        message: String,
        cause: Throwable? = null,
    ) : IOException(message, cause)

    private class ModelVerificationException(message: String) : IOException(message)

    private companion object {
        const val TAG = "SenseVoiceModel"
        const val MODEL_VERSION = "2024-07-17-int8"
        const val MODELS_DIRECTORY = "speech-models"
        const val INSTALL_MARKER = "installed.version"
        const val CONNECT_TIMEOUT_MILLIS = 12_000
        const val READ_TIMEOUT_MILLIS = 60_000
        const val DOWNLOAD_BUFFER_BYTES = 64 * 1024
        const val HASH_BUFFER_BYTES = 256 * 1024
        const val MINIMUM_FREE_SPACE_BYTES = 64L * 1024L * 1024L
        const val USER_AGENT = "InterviewAssistant/1.0"
        const val MODEL_REPOSITORY = "sherpa-onnx-sense-voice-zh-en-ja-ko-yue-2024-07-17"
        const val MODELSCOPE_BASE_URL =
            "https://modelscope.cn/models/fengge2024/$MODEL_REPOSITORY/resolve/master"
        const val HUGGING_FACE_BASE_URL =
            "https://huggingface.co/csukuangfj/$MODEL_REPOSITORY/resolve/main"

        val MODEL_FILES = listOf(
            ModelFile(
                name = "model.int8.onnx",
                urls = listOf(
                    "$MODELSCOPE_BASE_URL/model.int8.onnx",
                    "$HUGGING_FACE_BASE_URL/model.int8.onnx",
                ),
                sizeBytes = 239_233_841L,
                sha256 = "c71f0ce00bec95b07744e116345e33d8cbbe08cef896382cf907bf4b51a2cd51",
            ),
            ModelFile(
                name = "tokens.txt",
                urls = listOf(
                    "$MODELSCOPE_BASE_URL/tokens.txt",
                    "$HUGGING_FACE_BASE_URL/tokens.txt",
                ),
                sizeBytes = 315_894L,
                sha256 = "f449eb28dc567533d7fa59be34e2abca8784f771850c78a47fb731a31429a1dc",
            ),
            ModelFile(
                name = "silero_vad.onnx",
                urls = listOf(
                    "https://modelscope.cn/models/xiaowangge/" +
                        "sherpa-onnx-sense-voice-small/resolve/master/silero-vad/model.onnx",
                ),
                sizeBytes = 2_327_524L,
                sha256 = "1a153a22f4509e292a94e67d6f9b85e8deb25b4988682b7e174c65279d8788e3",
            ),
        )
        val TOTAL_MODEL_BYTES = MODEL_FILES.sumOf(ModelFile::sizeBytes)
    }
}
