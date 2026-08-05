package com.shiyue.reader.core.data.repository

import android.app.Application
import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.shiyue.reader.core.database.ShiyueDatabase
import com.shiyue.reader.core.model.Book
import com.shiyue.reader.core.model.BookStatus
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
class OfflineBookRepositoryTest {
    private lateinit var database: ShiyueDatabase
    private lateinit var repository: OfflineBookRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, ShiyueDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = OfflineBookRepository(database.bookDao())
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun addObserveUpdateAndDeleteBook() = runBlocking {
        val book = book(idSuffix = 1, updatedAt = 100)
        val newerBook = book(idSuffix = 2, updatedAt = 200)
        repository.addBook(book)
        repository.addBook(newerBook)

        assertEquals(book, repository.getBook(book.id))
        assertEquals(
            listOf(newerBook.id, book.id),
            repository.observeBooks().first().map { it.id },
        )

        val updated = book.updated(currentPage = 50, updatedAt = 300)
        repository.updateBook(updated)
        assertEquals(updated, repository.getBook(book.id))

        repository.deleteBook(book.id)
        assertNull(repository.getBook(book.id))
    }

    private fun book(idSuffix: Int, updatedAt: Long): Book = Book.restore(
        id = "00000000-0000-0000-0000-${idSuffix.toString().padStart(12, '0')}",
        title = "书名$idSuffix",
        author = null,
        coverPath = null,
        totalPages = 100,
        currentPage = 0,
        status = BookStatus.WISH,
        createdAt = 100,
        updatedAt = updatedAt,
    )
}
