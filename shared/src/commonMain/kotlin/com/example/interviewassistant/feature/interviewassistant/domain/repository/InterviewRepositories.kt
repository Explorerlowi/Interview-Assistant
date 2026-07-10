package com.example.interviewassistant.feature.interviewassistant.domain.repository

import com.example.interviewassistant.feature.interviewassistant.domain.model.AssistantAnswer
import com.example.interviewassistant.feature.interviewassistant.domain.model.InterviewSession
import com.example.interviewassistant.feature.interviewassistant.domain.model.InterviewSessionDetail
import com.example.interviewassistant.feature.interviewassistant.domain.model.OcrJob
import com.example.interviewassistant.feature.interviewassistant.domain.model.Resume
import com.example.interviewassistant.feature.interviewassistant.domain.model.TranscriptSegment
import kotlinx.coroutines.flow.StateFlow

/**
 * Manages imported resume files, metadata and OCR state.
 */
interface ResumeRepository {
    /** Current resume library ordered by recent activity. */
    val resumes: StateFlow<List<Resume>>

    /** Loads persisted data into [resumes]. */
    suspend fun refresh()

    /** Copies and records a resume before OCR starts. */
    suspend fun import(
        displayName: String,
        originalFileName: String,
        mimeType: String,
        content: ByteArray,
    ): Resume

    /** Finds one resume by identifier. */
    suspend fun get(id: String): Resume?

    /** Persists an updated resume. */
    suspend fun save(resume: Resume)

    /** Removes resume metadata, source file and dependent sessions. */
    suspend fun delete(id: String)

    /** Persists a recoverable OCR job. */
    suspend fun saveOcrJob(job: OcrJob)

    /** Returns one resume's OCR job, if present. */
    suspend fun getOcrJob(resumeId: String): OcrJob?

    /** Returns jobs that were active when the app stopped. */
    suspend fun recoverableOcrJobs(): List<OcrJob>
}

/**
 * Persists interview sessions, transcript segments and model answers.
 */
interface InterviewSessionRepository {
    /** Current session history ordered by recent activity. */
    val sessions: StateFlow<List<InterviewSession>>

    /** Loads history and marks interrupted work as paused. */
    suspend fun initialize()

    /** Creates or updates a session. */
    suspend fun saveSession(session: InterviewSession)

    /** Returns one complete session detail. */
    suspend fun detail(sessionId: String): InterviewSessionDetail?

    /** Adds or updates a recognized transcript segment. */
    suspend fun saveTranscript(segment: TranscriptSegment)

    /** Adds or updates a generated answer. */
    suspend fun saveAnswer(answer: AssistantAnswer)

    /** Removes one session and all dependent content. */
    suspend fun deleteSession(sessionId: String)

    /** Removes every resume, session and generated record. */
    suspend fun clearAll()
}
