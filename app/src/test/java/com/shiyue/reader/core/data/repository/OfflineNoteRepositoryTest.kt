package com.shiyue.reader.core.data.repository

import android.app.Application
import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.shiyue.reader.core.database.ShiyueDatabase
import com.shiyue.reader.core.model.Book
import com.shiyue.reader.core.model.BookStatus
import com.shiyue.reader.core.model.Note
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = Application::class, manifest = Config.NONE)
class OfflineNoteRepositoryTest {
    private lateinit var db: ShiyueDatabase
    private lateinit var repository: OfflineNoteRepository
    private val bookId = "00000000-0000-0000-0000-000000000001"

    @Before fun setup() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, ShiyueDatabase::class.java).allowMainThreadQueries().build()
        repository = OfflineNoteRepository(db)
        db.bookDao().insert(com.shiyue.reader.core.database.BookEntity(
            bookId, "Book", null, null, 100, 0, BookStatus.READING, 1, 1,
        ))
    }

    @After fun close() = db.close()

    @Test fun addObserveUpdateAndImagePathsWork() = runBlocking {
        val note = Note.create(bookId, null, 5, "第一条", "notes/a.jpg", timestamp = 100)
        repository.addNote(note)
        assertEquals(listOf("第一条"), repository.observeAll().first().map { it.content })
        assertEquals(listOf("notes/a.jpg"), repository.imagePaths())
        val updated = note.updated(content = "改过", updatedAt = 200)
        repository.updateNote(updated)
        assertEquals("改过", repository.getById(note.id)?.content)
    }

    @Test fun deleteRemovesNoteAndUnknownUpdateFails() = runBlocking {
        val note = Note.create(bookId, null, null, "临时", timestamp = 100)
        repository.addNote(note)
        repository.deleteNote(note.id)
        assertNull(repository.getById(note.id))
        assertThrows(IllegalStateException::class.java) { runBlocking { repository.deleteNote(note.id) } }
        Unit
    }

    @Test fun observeByBookFilters() = runBlocking {
        val note = Note.create(bookId, null, 1, "book", timestamp = 100)
        repository.addNote(note)
        assertEquals(1, repository.observeByBook(bookId).first().size)
        assertEquals(0, repository.observeByBook("00000000-0000-0000-0000-000000000099").first().size)
    }
}
