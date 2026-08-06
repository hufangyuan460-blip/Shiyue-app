package com.shiyue.reader.core.data.repository

import android.app.Application
import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.shiyue.reader.core.database.ShiyueDatabase
import com.shiyue.reader.core.model.Book
import com.shiyue.reader.core.model.BookStatus
import com.shiyue.reader.core.model.ReadingSessionState
import com.shiyue.reader.domain.repository.BookProgressUpdate
import com.shiyue.reader.domain.time.TimeSource
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

    private class FakeTimeSource(var wall: Long, var elapsed: Long) : TimeSource {
        override fun wallClockMillis() = wall
        override fun elapsedRealtimeMillis() = elapsed
        fun advance(value: Long) { wall += value; elapsed += value }
    }
}
