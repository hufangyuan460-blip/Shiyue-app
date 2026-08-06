package com.shiyue.reader.core.database

import android.app.Application
import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.shiyue.reader.core.model.BookStatus
import com.shiyue.reader.core.model.ReadingSessionState
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = Application::class, manifest = Config.NONE)
class ReadingSessionDaoTest {
    private lateinit var db: ShiyueDatabase
    private lateinit var dao: ReadingSessionDao
    @Before fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, ShiyueDatabase::class.java).allowMainThreadQueries().build()
        dao = db.readingSessionDao()
        runBlocking { db.bookDao().insert(book()) }
    }
    @After fun close() = db.close()

    @Test fun activeSlotIsUniqueAndFlowTracksSession() = runBlocking {
        val session = entity(1, ReadingSessionState.ACTIVE)
        dao.insertSession(session); dao.insertActiveSlot(ActiveReadingSessionEntity(sessionId = session.id))
        assertEquals(session, dao.observeActiveSession().first())
        val paused = session.copy(state = ReadingSessionState.PAUSED, activeSegmentStartedAtEpochMs = null, activeSegmentStartedAtElapsedRealtimeMs = null)
        dao.updateSession(paused)
        assertEquals(paused, dao.observeActiveSession().first())
        dao.deleteActiveSlot(session.id)
        assertNull(dao.observeActiveSession().first())
    }

    @Test fun completedHistorySortsNewestFirstAndDeleteExcludesUnfinished() = runBlocking {
        val old = entity(1, ReadingSessionState.COMPLETED, 100)
        val newer = entity(2, ReadingSessionState.COMPLETED, 300)
        dao.insertSession(old); dao.insertSession(newer)
        assertEquals(listOf(newer.id, old.id), dao.observeCompletedForBook(book().id).first().map { it.id })
        assertEquals(0, dao.deleteCompleted("00000000-0000-0000-0000-000000000099"))
        assertEquals(1, dao.deleteCompleted(old.id))
    }

    private fun book() = BookEntity("00000000-0000-0000-0000-000000000001", "Book", null, null, 100, 0, BookStatus.READING, 1, 1)
    private fun entity(suffix: Int, state: ReadingSessionState, start: Long = 100) = ReadingSessionEntity(
        id = "10000000-0000-0000-0000-${suffix.toString().padStart(12, '0')}", bookId = book().id, state = state,
        startedAtEpochMs = start, endedAtEpochMs = if (state == ReadingSessionState.COMPLETED) start + 100 else null,
        startPage = 0, endPage = if (state == ReadingSessionState.COMPLETED) 1 else null, activeDurationMs = 100,
        activeSegmentStartedAtEpochMs = if (state == ReadingSessionState.ACTIVE) start else null,
        activeSegmentStartedAtElapsedRealtimeMs = if (state == ReadingSessionState.ACTIVE) start else null,
        updateBookProgress = false, createdAt = start, updatedAt = start,
    )
}
