package com.shiyue.reader.core.data.repository

import android.app.Application
import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.shiyue.reader.core.database.ReadingSessionEntity
import com.shiyue.reader.core.database.ShiyueDatabase
import com.shiyue.reader.core.model.Book
import com.shiyue.reader.core.model.BookReadingSummary
import com.shiyue.reader.core.model.BookStatus
import com.shiyue.reader.core.model.ReadingSessionState
import com.shiyue.reader.domain.repository.BookProgressUpdate
import com.shiyue.reader.domain.time.TimeSource
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = Application::class, manifest = Config.NONE)
class OfflineReadingSessionRepositoryTest {
    private lateinit var db: ShiyueDatabase
    private lateinit var books: OfflineBookRepository
    private lateinit var repository: OfflineReadingSessionRepository
    private val clock = FakeTimeSource(1_000, 5_000)
    private val bookId = "00000000-0000-0000-0000-000000000001"
    @Before fun setup() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, ShiyueDatabase::class.java).allowMainThreadQueries().build()
        books = OfflineBookRepository(db); repository = OfflineReadingSessionRepository(db, clock)
        books.addBook(Book.restore(bookId, "Book", null, null, 100, 5, BookStatus.WISH, 1, 1))
    }
    @After fun close() = db.close()

    @Test fun startIsAtomicAndReturnsExistingSingleSession() = runBlocking {
        val first = repository.startReading(bookId, 5, true)
        val second = repository.startReading(bookId, 20, false)
        assertEquals(first.session.id, second.session.id)
        assertFalse(second.created)
        assertEquals(BookStatus.READING, books.getBook(bookId)?.status)
    }

    @Test fun pauseResumeCompleteUpdatesProgressAndHistory() = runBlocking {
        val started = repository.startReading(bookId, 5, false).session
        clock.advance(60_000); repository.pause(started.id)
        clock.advance(10_000); repository.resume(started.id)
        clock.advance(30_000)
        val completed = repository.complete(started.id, 20, BookProgressUpdate.UPDATE_KEEP_STATUS)
        assertEquals(ReadingSessionState.COMPLETED, completed.state)
        assertEquals(90_000, completed.activeDurationMs)
        assertEquals(20, books.getBook(bookId)?.currentPage)
        assertNull(repository.observeActiveSession().first())
        assertEquals(1, repository.observeHistorySummary(bookId).first().completedCount)
    }

    @Test fun discardDoesNotEnterHistoryOrChangeProgress() = runBlocking {
        val started = repository.startReading(bookId, 5, false).session
        repository.discard(started.id)
        assertEquals(emptyList<Any>(), repository.observeCompletedSessions(bookId).first())
        assertEquals(5, books.getBook(bookId)?.currentPage)
    }

    @Test fun unfinishedSessionBlocksBookDeletion() = runBlocking {
        repository.startReading(bookId, 5, false)
        assertThrows(IllegalStateException::class.java) { runBlocking { books.deleteBook(bookId) } }
        Unit
    }

    @Test fun invalidPageRollsBackWithoutCreatingSlot() = runBlocking {
        assertThrows(IllegalArgumentException::class.java) { runBlocking { repository.startReading(bookId, 101, false) } }
        assertNull(repository.observeActiveSession().first())
    }

    @Test fun readingTimesAggregateCompletedSessionsPerBook() = runBlocking {
        val secondBook = Book.create("第二本书", null, 200)
        books.addBook(secondBook)
        clock.advance(1_789_000_000_000L)
        val todayStart = LocalDate.ofInstant(Instant.ofEpochMilli(clock.wall), ZoneId.systemDefault())
            .atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        insertCompleted("10000000-0000-0000-0000-000000000001", bookId, todayStart + 3_600_000L, 60_000)
        insertCompleted("10000000-0000-0000-0000-000000000002", bookId, todayStart + 7_200_000L, 90_000)
        insertCompleted("10000000-0000-0000-0000-000000000003", secondBook.id, todayStart + 3_600_000L, 30_000)
        val result = repository.observeReadingTimes().first()
        assertEquals(150_000, result.getValue(bookId).totalDurationMs)
        assertEquals(150_000, result.getValue(bookId).todayDurationMs)
        assertEquals(30_000, result.getValue(secondBook.id).totalDurationMs)
        assertEquals(2, result.size)
    }

    @Test fun readingTimesTodayOnlyCountsSessionsEndedOnCurrentDay() = runBlocking {
        clock.advance(1_789_000_000_000L)
        val todayStart = LocalDate.ofInstant(Instant.ofEpochMilli(clock.wall), ZoneId.systemDefault())
            .atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        insertCompleted("10000000-0000-0000-0000-000000000011", bookId, todayStart + 3_600_000L, 60_000)
        insertCompleted("10000000-0000-0000-0000-000000000012", bookId, todayStart - 3_600_000L, 120_000)
        val summary = repository.observeReadingTimes().first().getValue(bookId)
        assertEquals(180_000, summary.totalDurationMs)
        assertEquals(60_000, summary.todayDurationMs)
    }

    @Test fun readingTimesEmptyWhenNoCompletedSessions() = runBlocking {
        assertEquals(emptyMap<String, BookReadingSummary>(), repository.observeReadingTimes().first())
    }

    @Test fun reviewStatisticsAggregateAllBooksAndFinishedCount() = runBlocking {
        val secondBook = Book.create("第二本书", null, 200)
        books.addBook(secondBook)
        books.updateBook(secondBook.updated(status = BookStatus.FINISHED, updatedAt = 1))
        clock.advance(1_789_000_000_000L)
        val todayStart = LocalDate.ofInstant(Instant.ofEpochMilli(clock.wall), ZoneId.systemDefault())
            .atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        insertCompleted("10000000-0000-0000-0000-000000000021", bookId, todayStart + 3_600_000L, 60_000)
        insertCompleted("10000000-0000-0000-0000-000000000022", secondBook.id, todayStart - 3_600_000L, 120_000)
        val stats = repository.observeReviewStatistics().first()
        assertEquals(180_000, stats.totalDurationMs)
        assertEquals(60_000, stats.todayDurationMs)
        assertEquals(2, stats.readingDays)
        assertEquals(1, stats.finishedBookCount)
        assertEquals(secondBook.id, stats.topBookId)
    }

    private suspend fun insertCompleted(id: String, forBookId: String, endedAt: Long, durationMs: Long) {
        db.readingSessionDao().insertSession(
            ReadingSessionEntity(
                id = id, bookId = forBookId, state = ReadingSessionState.COMPLETED,
                startedAtEpochMs = endedAt - 3_600_000L, endedAtEpochMs = endedAt,
                startPage = 0, endPage = 1, activeDurationMs = durationMs,
                activeSegmentStartedAtEpochMs = null, activeSegmentStartedAtElapsedRealtimeMs = null,
                updateBookProgress = false, createdAt = endedAt, updatedAt = endedAt,
            ),
        )
    }

    private class FakeTimeSource(var wall: Long, var elapsed: Long) : TimeSource {
        override fun wallClockMillis() = wall
        override fun elapsedRealtimeMillis() = elapsed
        fun advance(value: Long) { wall += value; elapsed += value }
    }
}
