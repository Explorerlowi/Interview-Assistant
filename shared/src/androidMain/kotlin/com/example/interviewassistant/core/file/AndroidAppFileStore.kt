package com.example.interviewassistant.core.file

import android.content.Context
import com.example.interviewassistant.core.util.CoroutineDispatcherProvider
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Android file store rooted in the application's private files directory.
 */
class AndroidAppFileStore(
    context: Context,
    private val dispatchers: CoroutineDispatcherProvider,
) : AppFileStore {
    private val resumeDirectory = File(context.filesDir, "resumes")
    private val ocrAssetRoot = File(context.filesDir, "ocr-assets")

    override suspend fun importResume(
        originalName: String,
        mimeType: String,
        content: ByteArray,
    ): StoredFile = withContext(dispatchers.io) {
        resumeDirectory.mkdirs()
        val safeName = originalName.replace(UNSAFE_FILE_NAME, "_").ifBlank { "resume" }
        val destination = File(resumeDirectory, "${currentTimeMillis()}-$safeName")
        destination.writeBytes(content)
        StoredFile(originalName, destination.absolutePath, mimeType, destination.length())
    }

    override suspend fun read(path: String): ByteArray = withContext(dispatchers.io) {
        File(path).readBytes()
    }

    override suspend fun delete(path: String) {
        withContext(dispatchers.io) {
            File(path).delete()
        }
    }

    override suspend fun exists(path: String): Boolean = withContext(dispatchers.io) {
        File(path).isFile
    }

    override suspend fun saveOcrAsset(
        resumeId: String,
        relativePath: String,
        content: ByteArray,
    ): String = withContext(dispatchers.io) {
        val destination = resolveOcrAssetPath(resumeId, relativePath)
        destination.parentFile?.mkdirs()
        destination.writeBytes(content)
        destination.absolutePath
    }

    override suspend fun ocrAssetBaseUri(resumeId: String): String = withContext(dispatchers.io) {
        val directory = ocrAssetDirectory(resumeId)
        directory.mkdirs()
        directory.toURI().toString().ensureTrailingSlash()
    }

    override suspend fun deleteOcrAssets(resumeId: String) {
        withContext(dispatchers.io) {
            ocrAssetDirectory(resumeId).deleteRecursively()
        }
    }

    private fun ocrAssetDirectory(resumeId: String): File =
        File(ocrAssetRoot, resumeId.sanitizedSegment())

    private fun resolveOcrAssetPath(resumeId: String, relativePath: String): File {
        val normalized = relativePath.replace('\\', '/').trimStart('/')
        var file = ocrAssetDirectory(resumeId)
        normalized.split('/').filter(String::isNotBlank).forEach { segment ->
            file = File(file, segment.sanitizedSegment())
        }
        return file
    }

    private fun currentTimeMillis(): Long = System.currentTimeMillis()

    private companion object {
        val UNSAFE_FILE_NAME = Regex("[^A-Za-z0-9._\\-\\u4e00-\\u9fa5]")
    }
}

private fun String.sanitizedSegment(): String =
    replace(Regex("[^A-Za-z0-9._\\-\\u4e00-\\u9fa5]"), "_").ifBlank { "asset" }

private fun String.ensureTrailingSlash(): String = if (endsWith("/")) this else "$this/"
