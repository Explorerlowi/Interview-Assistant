package com.example.interviewassistant.feature.interviewassistant

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.example.interviewassistant.core.file.AppFileStore
import com.example.interviewassistant.core.file.StoredFile
import com.example.interviewassistant.core.util.CoroutineDispatcherProvider
import com.example.interviewassistant.database.InterviewDatabase
import com.example.interviewassistant.feature.interviewassistant.data.local.SqlDelightInterviewSessionRepository
import com.example.interviewassistant.feature.interviewassistant.data.local.SqlDelightResumeRepository
import com.example.interviewassistant.feature.interviewassistant.domain.model.AnswerTriggerMode
import com.example.interviewassistant.feature.interviewassistant.domain.model.AssistantAnswer
import com.example.interviewassistant.feature.interviewassistant.domain.model.AssistantAnswerStatus
import com.example.interviewassistant.feature.interviewassistant.domain.model.InterviewSession
import com.example.interviewassistant.feature.interviewassistant.domain.model.InterviewSessionStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class SqlDelightRepositoriesTest {
    @Test
    fun `startup pauses active session and interrupts unfinished answer`() = runTest {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        InterviewDatabase.Schema.create(driver)
        val database = InterviewDatabase(driver)
        val fileStore = FakeFileStore()
        val dispatchers = TestDispatchers
        val resumes = SqlDelightResumeRepository(database, fileStore, dispatchers)
        val sessions = SqlDelightInterviewSessionRepository(database, fileStore, dispatchers)
        val resume = resumes.import("候选人", "resume.pdf", "application/pdf", byteArrayOf(1))
        val session = InterviewSession(
            id = "session",
            resumeId = resume.id,
            title = "Interview",
            status = InterviewSessionStatus.LISTENING,
            triggerMode = AnswerTriggerMode.MANUAL,
            createdAt = 1,
            updatedAt = 1,
        )
        sessions.saveSession(session)
        sessions.saveAnswer(
            AssistantAnswer(
                id = "answer",
                sessionId = session.id,
                question = "question",
                content = "partial",
                model = "model",
                status = AssistantAnswerStatus.GENERATING,
                createdAt = 1,
                updatedAt = 1,
            ),
        )

        sessions.initialize()

        val detail = assertNotNull(sessions.detail(session.id))
        assertEquals(InterviewSessionStatus.PAUSED, detail.session.status)
        assertEquals(AssistantAnswerStatus.INTERRUPTED, detail.answers.single().status)
        resumes.delete(resume.id)
        assertNull(sessions.detail(session.id))
        assertFalse(fileStore.exists(resume.storedPath))
        driver.close()
    }

    private object TestDispatchers : CoroutineDispatcherProvider {
        override val main = Dispatchers.Unconfined
        override val io = Dispatchers.Unconfined
        override val default = Dispatchers.Unconfined
    }

    private class FakeFileStore : AppFileStore {
        private val values = mutableMapOf<String, ByteArray>()

        override suspend fun importResume(
            originalName: String,
            mimeType: String,
            content: ByteArray,
        ): StoredFile {
            val path = "/private/$originalName"
            values[path] = content
            return StoredFile(originalName, path, mimeType, content.size.toLong())
        }

        override suspend fun read(path: String): ByteArray = values.getValue(path)

        override suspend fun delete(path: String) {
            values.remove(path)
        }

        override suspend fun exists(path: String): Boolean = path in values

        override suspend fun saveOcrAsset(
            resumeId: String,
            relativePath: String,
            content: ByteArray,
        ): String {
            val path = "/ocr/$resumeId/$relativePath"
            values[path] = content
            return path
        }

        override suspend fun ocrAssetBaseUri(resumeId: String): String = "file:///ocr/$resumeId/"

        override suspend fun deleteOcrAssets(resumeId: String) {
            values.keys.filter { it.startsWith("/ocr/$resumeId/") }.toList().forEach(values::remove)
        }
    }
}
