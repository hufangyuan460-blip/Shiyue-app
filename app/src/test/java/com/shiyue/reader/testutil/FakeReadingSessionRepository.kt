package com.shiyue.reader.testutil

import com.shiyue.reader.core.model.BookReadingSummary
import com.shiyue.reader.core.model.ReadingHistorySummary
import com.shiyue.reader.core.model.ReadingSession
import com.shiyue.reader.core.model.ReviewStatistics
import com.shiyue.reader.domain.repository.ActiveSessionInspection
import com.shiyue.reader.domain.repository.BookProgressUpdate
import com.shiyue.reader.domain.repository.ReadingSessionRepository
import com.shiyue.reader.domain.repository.StartReadingResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf

class FakeReadingSessionRepository(
    initialReadingTimes: Map<String, BookReadingSummary> = emptyMap(),
    reviewStatistics: ReviewStatistics = ReviewStatistics(),
) : ReadingSessionRepository {
    val readingTimes = MutableStateFlow(initialReadingTimes)
    val reviewStats = MutableStateFlow(reviewStatistics)

    override fun observeReadingTimes(): Flow<Map<String, BookReadingSummary>> = readingTimes
    override fun observeReviewStatistics(): Flow<ReviewStatistics> = reviewStats
    override fun observeActiveSession(): Flow<ReadingSession?> = flowOf(null)
    override fun observeSession(id: String): Flow<ReadingSession?> = flowOf(null)
    override fun observeCompletedSessions(bookId: String): Flow<List<ReadingSession>> = flowOf(emptyList())
    override fun observeHistorySummary(bookId: String): Flow<ReadingHistorySummary> = flowOf(ReadingHistorySummary())
    override suspend fun getSession(id: String): ReadingSession? = null
    override suspend fun inspectActiveSession(): ActiveSessionInspection = ActiveSessionInspection(null)
    override suspend fun startReading(bookId: String, startPage: Int, switchBookToReading: Boolean): StartReadingResult =
        error("Not used in this test")
    override suspend fun pause(sessionId: String): ReadingSession = error("Not used in this test")
    override suspend fun resume(sessionId: String): ReadingSession = error("Not used in this test")
    override suspend fun complete(sessionId: String, endPage: Int, progressUpdate: BookProgressUpdate): ReadingSession =
        error("Not used in this test")
    override suspend fun discard(sessionId: String): ReadingSession = error("Not used in this test")
    override suspend fun recoverAndContinue(sessionId: String, acceptedEstimatedSegmentMs: Long): ReadingSession =
        error("Not used in this test")
    override suspend fun replaceReliableDuration(sessionId: String, durationMs: Long): ReadingSession =
        error("Not used in this test")
    override suspend fun correctCompletedSession(
        sessionId: String,
        startedAtEpochMs: Long,
        endedAtEpochMs: Long,
        activeDurationMs: Long,
        startPage: Int,
        endPage: Int,
    ): ReadingSession = error("Not used in this test")
    override suspend fun deleteCompletedSession(sessionId: String) = Unit
}
