package com.example.interviewassistant.core.file

/**
 * Placeholder file store while iOS is outside the first product scope.
 */
class UnsupportedIosFileStore : AppFileStore {
    override suspend fun importResume(
        originalName: String,
        mimeType: String,
        content: ByteArray,
    ): StoredFile = error(MESSAGE)

    override suspend fun read(path: String): ByteArray = error(MESSAGE)

    override suspend fun delete(path: String) = Unit

    override suspend fun exists(path: String): Boolean = false

    override suspend fun saveOcrAsset(
        resumeId: String,
        relativePath: String,
        content: ByteArray,
    ): String = error(MESSAGE)

    override suspend fun ocrAssetBaseUri(resumeId: String): String = error(MESSAGE)

    override suspend fun deleteOcrAssets(resumeId: String) = Unit

    private companion object {
        const val MESSAGE = "Resume file storage is not available on iOS yet"
    }
}
