package com.shiyue.reader.domain.repository

import com.shiyue.reader.core.model.BookReadingSummary
import com.shiyue.reader.core.model.ReadingClockAnomaly
import com.shiyue.reader.core.model.ReadingDuration
import com.shiyue.reader.core.model.ReadingHistorySummary
import com.shiyue.reader.core.model.ReadingSession
import com.shiyue.reader.core.model.ReviewStatistics
import kotlinx.coroutines.flow.Flow

data class StartReadingResult(
    val session: ReadingSession,
    val created: Boolean,
)

enum class BookProgressUpdate {
    DO_NOT_UPDATE,
    UPDATE_KEEP_STATUS,
    UPDATE_AND_MARK_FINISHED,
}

data class ActiveSessionInspection(
    val session: ReadingSession?,
    val duration: ReadingDuration? = null,
    val anomaly: ReadingClockAnomaly? = duration?.anomaly,
    val additionalUnfinishedSessionIds: List<String> = emptyList(),
)

interface ReadingSessionRepository {
    fun observeActiveSession(): Flow<ReadingSession?>
    fun observeSession(id: String): Flow<ReadingSession?>
    fun observeCompletedSessions(bookId: String): Flow<List<ReadingSession>>
    fun observeHistorySummary(bookId: String): Flow<ReadingHistorySummary>
    fun observeReadingTimes(): Flow<Map<String, BookReadingSummary>>
    fun observeReviewStatistics(): Flow<ReviewStatistics>
    suspend fun getSession(id: String): ReadingSession?
    suspend fun inspectActiveSession(): ActiveSessionInspection
    suspend fun startReading(bookId: String, startPage: Int, switchBookToReading: Boolean): StartReadingResult
    suspend fun pause(sessionId: String): ReadingSession
    suspend fun resume(sessionId: String): ReadingSession
    suspend fun complete(sessionId: String, endPage: Int, progressUpdate: BookProgressUpdate): ReadingSession
    suspend fun discard(sessionId: String): ReadingSession
    suspend fun recoverAndContinue(sessionId: String, acceptedEstimatedSegmentMs: Long): ReadingSession
    suspend fun replaceReliableDuration(sessionId: String, durationMs: Long): ReadingSession
    suspend fun correctCompletedSession(
        sessionId: String,
        startedAtEpochMs: Long,
        endedAtEpochMs: Long,
        activeDurationMs: Long,
        startPage: Int,
        endPage: Int,
    ): ReadingSession
    suspend fun deleteCompletedSession(sessionId: String)
}
