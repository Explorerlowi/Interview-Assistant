package com.example.interviewassistant.core.file

import com.example.interviewassistant.core.util.CoroutineDispatcherProvider
import kotlinx.coroutines.withContext
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption

/**
 * Windows desktop file store rooted in the current user's app-data directory.
 */
class DesktopAppFileStore(
    private val dispatchers: CoroutineDispatcherProvider,
) : AppFileStore {
    private val resumeDirectory: Path = applicationDataDirectory().resolve("resumes")
    private val ocrAssetRoot: Path = applicationDataDirectory().resolve("ocr-assets")

    override suspend fun importResume(
        originalName: String,
        mimeType: String,
        content: ByteArray,
    ): StoredFile = withContext(dispatchers.io) {
        Files.createDirectories(resumeDirectory)
        val safeName = originalName.replace(UNSAFE_FILE_NAME, "_").ifBlank { "resume" }
        val destination = resumeDirectory.resolve("${System.currentTimeMillis()}-$safeName")
        Files.write(
            destination,
            content,
            StandardOpenOption.CREATE_NEW,
            StandardOpenOption.WRITE,
        )
        StoredFile(originalName, destination.toString(), mimeType, content.size.toLong())
    }

    override suspend fun read(path: String): ByteArray = withContext(dispatchers.io) {
        Files.readAllBytes(Path.of(path))
    }

    override suspend fun delete(path: String) {
        withContext(dispatchers.io) {
            Files.deleteIfExists(Path.of(path))
        }
    }

    override suspend fun exists(path: String): Boolean = withContext(dispatchers.io) {
        Files.isRegularFile(Path.of(path))
    }

    override suspend fun saveOcrAsset(
        resumeId: String,
        relativePath: String,
        content: ByteArray,
    ): String = withContext(dispatchers.io) {
        val destination = resolveOcrAssetPath(resumeId, relativePath)
        Files.createDirectories(destination.parent)
        Files.write(
            destination,
            content,
            StandardOpenOption.CREATE,
            StandardOpenOption.TRUNCATE_EXISTING,
            StandardOpenOption.WRITE,
        )
        destination.toAbsolutePath().toString()
    }

    override suspend fun ocrAssetBaseUri(resumeId: String): String = withContext(dispatchers.io) {
        val directory = ocrAssetDirectory(resumeId)
        Files.createDirectories(directory)
        directory.toUri().toString().ensureTrailingSlash()
    }

    override suspend fun deleteOcrAssets(resumeId: String) {
        withContext(dispatchers.io) {
            val directory = ocrAssetDirectory(resumeId)
            if (Files.exists(directory)) {
                Files.walk(directory)
                    .sorted(Comparator.reverseOrder())
                    .forEach(Files::deleteIfExists)
            }
        }
    }

    private fun ocrAssetDirectory(resumeId: String): Path =
        ocrAssetRoot.resolve(resumeId.sanitizedSegment())

    private fun resolveOcrAssetPath(resumeId: String, relativePath: String): Path {
        val normalized = relativePath.replace('\\', '/').trimStart('/')
        var path = ocrAssetDirectory(resumeId)
        normalized.split('/').filter(String::isNotBlank).forEach { segment ->
            path = path.resolve(segment.sanitizedSegment())
        }
        return path
    }

    private fun applicationDataDirectory(): Path {
        val root = System.getenv("APPDATA")
            ?.takeIf(String::isNotBlank)
            ?: System.getProperty("user.home")
        return Path.of(root, "InterviewAssistant")
    }

    private companion object {
        val UNSAFE_FILE_NAME = Regex("[^A-Za-z0-9._\\-\\u4e00-\\u9fa5]")
    }
}

private fun String.sanitizedSegment(): String =
    replace(Regex("[^A-Za-z0-9._\\-\\u4e00-\\u9fa5]"), "_").ifBlank { "asset" }

private fun String.ensureTrailingSlash(): String = if (endsWith("/")) this else "$this/"
