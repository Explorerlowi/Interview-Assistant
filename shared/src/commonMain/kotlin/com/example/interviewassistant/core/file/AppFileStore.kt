package com.example.interviewassistant.core.file

/**
 * Metadata for a document copied into application-private storage.
 */
data class StoredFile(
    val originalName: String,
    val path: String,
    val mimeType: String,
    val sizeBytes: Long,
)

/**
 * Manages resume source files and OCR asset caches without exposing platform file APIs to common code.
 */
interface AppFileStore {
    /**
     * Copies [content] into private storage and returns its stable location.
     */
    suspend fun importResume(
        originalName: String,
        mimeType: String,
        content: ByteArray,
    ): StoredFile

    /**
     * Reads a previously imported file.
     */
    suspend fun read(path: String): ByteArray

    /**
     * Removes a previously imported file.
     */
    suspend fun delete(path: String)

    /**
     * Returns whether a stored path still exists.
     */
    suspend fun exists(path: String): Boolean

    /**
     * Saves one OCR-referenced image under the resume-scoped asset directory.
     *
     * @param resumeId Resume identifier used as the cache namespace.
     * @param relativePath Provider-relative path such as `imgs/seal.jpg`.
     * @param content Raw image bytes downloaded from the provider.
     * @return Absolute filesystem path of the cached file.
     */
    suspend fun saveOcrAsset(resumeId: String, relativePath: String, content: ByteArray): String

    /**
     * Returns a `file://` URI (with trailing slash) usable as an HTML `<base href>` for OCR assets.
     */
    suspend fun ocrAssetBaseUri(resumeId: String): String

    /**
     * Deletes the OCR asset directory for [resumeId], if present.
     */
    suspend fun deleteOcrAssets(resumeId: String)
}
