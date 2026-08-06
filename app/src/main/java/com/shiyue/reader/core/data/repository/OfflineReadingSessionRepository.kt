package com.shiyue.reader.core.data.repository

import androidx.room.withTransaction
import com.shiyue.reader.core.database.ActiveReadingSessionEntity
import com.shiyue.reader.core.database.ShiyueDatabase
import com.shiyue.reader.core.database.asEntity
import com.shiyue.reader.core.database.asExternalModel
import com.shiyue.reader.core.model.BookStatus
import com.shiyue.reader.core.model.ReadingHistorySummary
import com.shiyue.reader.core.model.ReadingSession
import com.shiyue.reader.core.model.ReadingSessionState
import com.shiyue.reader.domain.repository.ActiveSessionInspection
import com.shiyue.reader.domain.repository.BookProgressUpdate
import com.shiyue.reader.domain.repository.ReadingSessionRepository
import com.shiyue.reader.domain.repository.StartReadingResult
import com.shiyue.reader.domain.time.TimeSource
import com.shiyue.reader.domain.time.read
import java.time.Instant
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Singleton
class OfflineReadingSessionRepository @Inject constructor(
    private val database: ShiyueDatabase,
    private val timeSource: TimeSource,
) : ReadingSessionRepository {
    private val dao get() = database.readingSessionDao()
    private val books get() = database.bookDao()

    override fun observeActiveSession(): Flow<ReadingSession?> =
        dao.observeActiveSession().map { it?.asExternalModel() }

    override fun observeSession(id: String): Flow<ReadingSession?> =
        dao.observeSession(id).map { it?.asExternalModel() }

    override fun observeCompletedSessions(bookId: String): Flow<List<ReadingSession>> =
        dao.observeCompletedForBook(bookId).map { list -> list.map { it.asExternalModel() } }

    override fun observeHistorySummary(bookId: String): Flow<ReadingHistorySummary> =
        observeCompletedSessions(bookId).map { sessions ->
            val effective = sessions.filter { it.activeDurationMs > 0 }
            val zone = ZoneId.systemDefault()
            ReadingHistorySummary(
                totalDurationMs = sessions.sumOf { it.activeDurationMs },
                completedCount = sessions.size,
                readingDays = effective.mapNotNull { it.endedAtEpochMs }
                    .map { Instant.ofEpochMilli(it).atZone(zone).toLocalDate() }.distinct().size,
                lastReadAtEpochMs = sessions.maxOfOrNull { it.endedAtEpochMs ?: it.startedAtEpochMs },
            )
        }

    override suspend fun getSession(id: String): ReadingSession? = dao.getSession(id)?.asExternalModel()

    override suspend fun inspectActiveSession(): ActiveSessionInspection = database.withTransaction {
        val active = dao.getActiveSession()?.asExternalModel()
        val unfinished = dao.getUnfinishedSessions().map { it.asExternalModel() }
        ActiveSessionInspection(
            session = active,
            duration = active?.durationAt(timeSource.read()),
            additionalUnfinishedSessionIds = unfinished.map { it.id }.filter { it != active?.id },
        )
    }

    override suspend fun startReading(
        bookId: String,
        startPage: Int,
        switchBookToReading: Boolean,
    ): StartReadingResult = database.withTransaction {
        dao.getActiveSession()?.asExternalModel()?.let { return@withTransaction StartReadingResult(it, false) }
        val book = checkNotNull(books.getById(bookId)) { "Book not found: $bookId" }.asExternalModel()
        require(startPage in 0..book.totalPages) { "Start page is outside the book" }
        val now = timeSource.read()
        val session = ReadingSession.start(bookId, startPage, now)
        dao.insertSession(session.asEntity())
        dao.insertActiveSlot(ActiveReadingSessionEntity(sessionId = session.id))
        if (switchBookToReading && book.status != BookStatus.READING) {
            check(books.update(book.updated(status = BookStatus.READING, updatedAt = now.wallClockMs).asEntity()) == 1)
        }
        StartReadingResult(session, true)
    }

    override suspend fun pause(sessionId: String): ReadingSession = database.withTransaction {
        val current = requiredActive(sessionId)
        val updated = current.pause(timeSource.read())
        if (updated != current) check(dao.updateSession(updated.asEntity()) == 1)
        updated
    }

    override suspend fun resume(sessionId: String): ReadingSession = database.withTransaction {
        val current = requiredActive(sessionId)
        val updated = current.resume(timeSource.read())
        if (updated != current) check(dao.updateSession(updated.asEntity()) == 1)
        updated
    }

    override suspend fun complete(
        sessionId: String,
        endPage: Int,
        progressUpdate: BookProgressUpdate,
    ): ReadingSession = database.withTransaction {
        var current = requiredActive(sessionId)
        val now = timeSource.read()
        if (current.state == ReadingSessionState.ACTIVE) current = current.pause(now)
        val book = checkNotNull(books.getById(current.bookId)) { "Book not found: ${current.bookId}" }.asExternalModel()
        require(endPage in 0..book.totalPages) { "End page is outside the book" }
        val completed = current.complete(endPage, progressUpdate != BookProgressUpdate.DO_NOT_UPDATE, now.wallClockMs)
        check(dao.updateSession(completed.asEntity()) == 1)
        check(dao.deleteActiveSlot(sessionId) == 1)
        if (progressUpdate != BookProgressUpdate.DO_NOT_UPDATE) {
            val status = if (progressUpdate == BookProgressUpdate.UPDATE_AND_MARK_FINISHED) BookStatus.FINISHED else book.status
            check(books.update(book.updated(currentPage = endPage, status = status, updatedAt = now.wallClockMs).asEntity()) == 1)
        }
        completed
    }

    override suspend fun discard(sessionId: String): ReadingSession = database.withTransaction {
        val current = requiredActive(sessionId)
        val discarded = current.discard(timeSource.wallClockMillis())
        check(dao.updateSession(discarded.asEntity()) == 1)
        check(dao.deleteActiveSlot(sessionId) == 1)
        discarded
    }

    override suspend fun recoverAndContinue(sessionId: String, acceptedEstimatedSegmentMs: Long): ReadingSession =
        database.withTransaction {
            val updated = requiredActive(sessionId).recover(timeSource.read(), acceptedEstimatedSegmentMs)
            check(dao.updateSession(updated.asEntity()) == 1)
            updated
        }

    override suspend fun replaceReliableDuration(sessionId: String, durationMs: Long): ReadingSession =
        database.withTransaction {
            val updated = requiredActive(sessionId).setReliableDuration(durationMs, timeSource.wallClockMillis())
            check(dao.updateSession(updated.asEntity()) == 1)
            updated
        }

    override suspend fun correctCompletedSession(
        sessionId: String,
        startedAtEpochMs: Long,
        endedAtEpochMs: Long,
        activeDurationMs: Long,
        startPage: Int,
        endPage: Int,
    ): ReadingSession = database.withTransaction {
        val current = checkNotNull(dao.getSession(sessionId)) { "Session not found: $sessionId" }.asExternalModel()
        val book = checkNotNull(books.getById(current.bookId)) { "Book not found: ${current.bookId}" }.asExternalModel()
        require(startPage in 0..book.totalPages && endPage in 0..book.totalPages)
        val updated = current.corrected(
            startedAtEpochMs, endedAtEpochMs, activeDurationMs, startPage, endPage,
            timeSource.wallClockMillis(),
        )
        check(dao.updateSession(updated.asEntity()) == 1)
        updated
    }

    override suspend fun deleteCompletedSession(sessionId: String) {
        check(dao.deleteCompleted(sessionId) == 1) { "Completed session not found: $sessionId" }
    }

    private suspend fun requiredActive(id: String): ReadingSession {
        val active = checkNotNull(dao.getActiveSession()) { "No active reading session" }.asExternalModel()
        check(active.id == id) { "Session is not the active reading session" }
        return active
    }
}
