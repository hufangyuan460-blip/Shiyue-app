package com.shiyue.reader.core.data

import com.shiyue.reader.core.model.BookReadingSummary
import com.shiyue.reader.core.model.BookStatus
import com.shiyue.reader.core.model.ReadingHistorySummary
import com.shiyue.reader.core.model.ReadingSession
import com.shiyue.reader.core.model.ReviewStatistics
import com.shiyue.reader.core.model.aggregateReadingTimes
import com.shiyue.reader.core.model.computeReviewStatistics
import com.shiyue.reader.domain.repository.ActiveSessionInspection
import com.shiyue.reader.domain.repository.BookProgressUpdate
import com.shiyue.reader.domain.repository.ReadingSessionRepository
import com.shiyue.reader.domain.repository.StartReadingResult
import com.shiyue.reader.domain.time.TimeReading
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update

@Singleton
class TestReadingSessionRepository @Inject constructor(
    private val books: TestBookRepository,
) : ReadingSessionRepository {
    private val sessions = MutableStateFlow<List<ReadingSession>>(emptyList())
    private val activeId = MutableStateFlow<String?>(null)
    private var wall = 1_700_000_000_000L
    private var elapsed = 100_000L
    private fun now() = TimeReading(wall, elapsed)

    override fun observeActiveSession(): Flow<ReadingSession?> = combine(sessions, activeId) { list, id -> list.firstOrNull { it.id == id } }
    override fun observeSession(id: String): Flow<ReadingSession?> = sessions.map { it.firstOrNull { item -> item.id == id } }
    override fun observeCompletedSessions(bookId: String): Flow<List<ReadingSession>> = sessions.map { list -> list.filter { it.bookId == bookId && it.endedAtEpochMs != null }.sortedByDescending { it.startedAtEpochMs } }
    override fun observeHistorySummary(bookId: String): Flow<ReadingHistorySummary> = observeCompletedSessions(bookId).map { list -> ReadingHistorySummary(list.sumOf { it.activeDurationMs }, list.size, if (list.isEmpty()) 0 else 1, list.maxOfOrNull { requireNotNull(it.endedAtEpochMs) }) }
    override fun observeReadingTimes(): Flow<Map<String, BookReadingSummary>> = sessions.map { list -> aggregateReadingTimes(list, wall) }
    override fun observeReviewStatistics(): Flow<ReviewStatistics> = combine(sessions, books.observeBooks()) { list, bookList ->
        computeReviewStatistics(list, bookList.filter { it.status == BookStatus.FINISHED }.map { it.id }.toSet(), wall)
    }
    override suspend fun getSession(id: String) = sessions.value.firstOrNull { it.id == id }
    override suspend fun inspectActiveSession(): ActiveSessionInspection { val session = getSession(activeId.value ?: return ActiveSessionInspection(null)); return ActiveSessionInspection(session, session?.durationAt(now())) }
    override suspend fun startReading(bookId: String, startPage: Int, switchBookToReading: Boolean): StartReadingResult {
        getSession(activeId.value ?: "")?.let { return StartReadingResult(it, false) }
        val book = checkNotNull(books.getBook(bookId)); require(startPage in 0..book.totalPages)
        val session = ReadingSession.start(bookId, startPage, now()); sessions.value = listOf(session); activeId.value = session.id
        if (switchBookToReading) books.updateBook(book.updated(status = BookStatus.READING, updatedAt = wall))
        return StartReadingResult(session, true)
    }
    override suspend fun pause(sessionId: String) = replace(requireActive(sessionId).pause(now()))
    override suspend fun resume(sessionId: String) = replace(requireActive(sessionId).resume(now()))
    override suspend fun complete(sessionId: String, endPage: Int, progressUpdate: BookProgressUpdate): ReadingSession {
        var session = requireActive(sessionId); if (session.activeSegmentStartedAtEpochMs != null) session = session.pause(now())
        val completed = session.complete(endPage, progressUpdate != BookProgressUpdate.DO_NOT_UPDATE, wall)
        replace(completed); activeId.value = null
        if (progressUpdate != BookProgressUpdate.DO_NOT_UPDATE) {
            val book = checkNotNull(books.getBook(session.bookId))
            books.updateBook(book.updated(currentPage = endPage, status = if (progressUpdate == BookProgressUpdate.UPDATE_AND_MARK_FINISHED) BookStatus.FINISHED else book.status, updatedAt = wall))
        }
        return completed
    }
    override suspend fun discard(sessionId: String): ReadingSession { val value = replace(requireActive(sessionId).discard(wall)); activeId.value = null; return value }
    override suspend fun recoverAndContinue(sessionId: String, acceptedEstimatedSegmentMs: Long) = replace(requireActive(sessionId).recover(now(), acceptedEstimatedSegmentMs))
    override suspend fun replaceReliableDuration(sessionId: String, durationMs: Long) = replace(requireActive(sessionId).setReliableDuration(durationMs, wall))
    override suspend fun correctCompletedSession(sessionId: String, startedAtEpochMs: Long, endedAtEpochMs: Long, activeDurationMs: Long, startPage: Int, endPage: Int) = replace(checkNotNull(getSession(sessionId)).corrected(startedAtEpochMs, endedAtEpochMs, activeDurationMs, startPage, endPage, wall))
    override suspend fun deleteCompletedSession(sessionId: String) { sessions.update { it.filterNot { item -> item.id == sessionId } } }
    private suspend fun requireActive(id: String) = checkNotNull(getSession(id)).also { check(activeId.value == id) }
    private fun replace(value: ReadingSession): ReadingSession { sessions.update { list -> list.map { if (it.id == value.id) value else it } }; return value }
    fun reset() { sessions.value = emptyList(); activeId.value = null; wall = 1_700_000_000_000L; elapsed = 100_000L }
}
