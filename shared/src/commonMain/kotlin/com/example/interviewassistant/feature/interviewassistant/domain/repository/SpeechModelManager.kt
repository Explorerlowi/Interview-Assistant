package com.example.interviewassistant.feature.interviewassistant.domain.repository

import kotlinx.coroutines.flow.StateFlow

/**
 * Identifies the exact on-device speech model used for inference.
 *
 * @property name Human-readable model family.
 * @property version Export or release version.
 * @property quantization Model quantization format.
 */
data class SpeechModelDescriptor(
    val name: String,
    val version: String,
    val quantization: String,
)

/**
 * Installation state of the optional on-device speech model.
 */
sealed interface SpeechModelState {
    /** The current platform has no on-device SenseVoice implementation. */
    data object Unavailable : SpeechModelState

    /** Model files have not been installed yet. */
    data object NotInstalled : SpeechModelState

    /** A download request was accepted and the manager is connecting to a model source. */
    data object Preparing : SpeechModelState

    /**
     * Model files are being downloaded and verified.
     *
     * @property downloadedBytes Bytes persisted so far across every model file.
     * @property totalBytes Expected total model size.
     */
    data class Downloading(
        val downloadedBytes: Long,
        val totalBytes: Long,
    ) : SpeechModelState

    /** Model files are installed and ready for inference. */
    data object Ready : SpeechModelState

    /**
     * The most recent install or delete operation failed.
     *
     * @property reason User-actionable failure category.
     */
    data class Failed(val reason: SpeechModelFailure) : SpeechModelState
}

/** User-actionable categories for model installation failures. */
enum class SpeechModelFailure {
    NETWORK,
    TIMEOUT,
    SERVER,
    STORAGE,
    VERIFICATION,
    UNKNOWN,
}

/**
 * Installs and removes the optional on-device speech-recognition model.
 */
interface SpeechModelManager {
    /** Exact model bundled or downloaded by this platform implementation. */
    val descriptor: SpeechModelDescriptor?

    /** Current installation state. */
    val state: StateFlow<SpeechModelState>

    /** Downloads, verifies, and activates all required model files. */
    suspend fun install()

    /** Removes downloaded model files from application-private storage. */
    suspend fun delete()
}
