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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = Application::class, manifest = Config.NONE)
class NoteDaoTest {
    private lateinit var db: ShiyueDatabase
    private lateinit var dao: NoteDao

    @Before fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, ShiyueDatabase::class.java).allowMainThreadQueries().build()
        dao = db.noteDao()
        runBlocking { db.bookDao().insert(book()) }
    }

    @After fun close() = db.close()

    @Test fun insertObserveAndImagePathsWork() = runBlocking {
        dao.insert(note("...-a1", createdAt = 100, imagePath = "notes/a.jpg"))
        dao.insert(note("...-a2", createdAt = 200))
        assertEquals(listOf("...-a2", "...-a1"), dao.observeAll().first().map { it.id })
        assertEquals(listOf("notes/a.jpg"), dao.imagePaths())
        assertEquals(1, dao.deleteById("...-a1"))
        assertEquals(listOf("...-a2"), dao.observeAll().first().map { it.id })
    }

    @Test fun observeByBookFiltersNotes() = runBlocking {
        val otherBook = BookEntity("00000000-0000-0000-0000-000000000002", "Other", null, null, 100, 0, BookStatus.READING, 1, 1)
        db.bookDao().insert(otherBook)
        dao.insert(note("...-a1"))
        dao.insert(note("...-a2", bookId = otherBook.id))
        assertEquals(listOf("...-a1"), dao.observeByBook(book().id).first().map { it.id })
        assertEquals(2, dao.observeAll().first().size)
    }

    @Test fun deletingBookCascadesToNotes() = runBlocking {
        dao.insert(note("...-a1"))
        assertEquals(1, db.bookDao().deleteById(book().id))
        assertEquals(0, dao.observeAll().first().size)
    }

    @Test fun deletingSessionSetsNoteSessionToNull() = runBlocking {
        val session = ReadingSessionEntity(
            id = "20000000-0000-0000-0000-000000000001", bookId = book().id, state = ReadingSessionState.COMPLETED,
            startedAtEpochMs = 1, endedAtEpochMs = 2, startPage = 0, endPage = 1, activeDurationMs = 1000,
            activeSegmentStartedAtEpochMs = null, activeSegmentStartedAtElapsedRealtimeMs = null,
            updateBookProgress = false, createdAt = 1, updatedAt = 2,
        )
        db.readingSessionDao().insertSession(session)
        dao.insert(note("...-a1", sessionId = session.id))
        assertTrue(dao.observeAll().first().single().sessionId != null)
        db.readingSessionDao().deleteCompleted(session.id)
        assertNull(dao.observeAll().first().single().sessionId)
    }

    private fun book() = BookEntity("00000000-0000-0000-0000-000000000001", "Book", null, null, 100, 0, BookStatus.READING, 1, 1)

    private fun note(
        id: String,
        bookId: String = book().id,
        sessionId: String? = null,
        content: String = "Note",
        imagePath: String? = null,
        createdAt: Long = 100,
    ) = NoteEntity(
        id = id, bookId = bookId, sessionId = sessionId, pageNumber = 1, content = content,
        imagePath = imagePath, createdAt = createdAt, updatedAt = createdAt,
    )
}
