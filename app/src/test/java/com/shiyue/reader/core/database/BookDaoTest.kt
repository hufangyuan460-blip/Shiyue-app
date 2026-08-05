package com.shiyue.reader.core.database

import android.app.Application
import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.shiyue.reader.core.model.BookStatus
import com.shiyue.reader.core.model.Category
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
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
class BookDaoTest {
    private lateinit var database: ShiyueDatabase
    private lateinit var dao: BookDao

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, ShiyueDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = database.bookDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun insertAndQueryById() = runBlocking {
        val book = entity(idSuffix = 1)

        dao.insert(book)

        assertEquals(book, dao.getById(book.id))
    }

    @Test
    fun duplicateIdAbortsInsteadOfReplacingData() = runBlocking {
        val original = entity(idSuffix = 1, title = "原书名")
        dao.insert(original)

        assertThrows(Exception::class.java) {
            runBlocking {
                dao.insert(original.copy(title = "意外覆盖"))
            }
        }
        assertEquals(original, dao.getById(original.id))
    }

    @Test
    fun observeAllEmitsWhenBooksChange() = runBlocking {
        val emissions = mutableListOf<List<BookEntity>>()
        val firstEmission = CompletableDeferred<Unit>()
        val job = launch {
            dao.observeAll().take(2).collect { books ->
                emissions += books
                if (emissions.size == 1) firstEmission.complete(Unit)
            }
        }

        withTimeout(5_000) { firstEmission.await() }
        dao.insert(entity(idSuffix = 1))
        withTimeout(5_000) { job.join() }

        assertEquals(emptyList<BookEntity>(), emissions[0])
        assertEquals(1, emissions[1].size)
        assertEquals(entity(idSuffix = 1).id, emissions[1].first().id)
    }

    @Test
    fun observeByIdEmitsNullThenInsertedAndUpdatedBook() = runBlocking {
        val original = entity(idSuffix = 4, title = "原书名")
        val updated = original.copy(title = "新书名", updatedAt = 900)
        val emissions = mutableListOf<BookEntity?>()
        val emitted = Channel<Unit>(Channel.UNLIMITED)
        val job = launch {
            dao.observeById(original.id).take(3).collect { book ->
                emissions += book
                emitted.send(Unit)
            }
        }

        withTimeout(5_000) { emitted.receive() }
        dao.insert(original)
        withTimeout(5_000) { emitted.receive() }
        dao.update(updated)
        withTimeout(5_000) { emitted.receive() }
        withTimeout(5_000) { job.join() }

        assertEquals(listOf(null, original, updated), emissions)
    }

    @Test
    fun observeByIdReturnsNullForMissingBook() = runBlocking {
        assertNull(dao.observeById(entity(idSuffix = 9).id).first())
    }

    @Test
    fun observeAllSortsByUpdatedAtDescendingThenIdAscending() = runBlocking {
        val first = entity(idSuffix = 1, updatedAt = 100)
        val second = entity(idSuffix = 2, updatedAt = 300)
        val third = entity(idSuffix = 3, updatedAt = 300)
        dao.insert(first)
        dao.insert(third)
        dao.insert(second)

        val books = dao.observeAll().first()

        assertEquals(listOf(second.id, third.id, first.id), books.map { it.id })
    }

    @Test
    fun updateChangesExistingBook() = runBlocking {
        val original = entity(idSuffix = 1)
        dao.insert(original)
        val updated = original.copy(title = "更新后的书名", updatedAt = 500)

        assertEquals(1, dao.update(updated))

        assertEquals(updated, dao.getById(original.id))
    }

    @Test
    fun deleteRemovesExistingBook() = runBlocking {
        val book = entity(idSuffix = 1)
        dao.insert(book)

        assertEquals(1, dao.deleteById(book.id))

        assertNull(dao.getById(book.id))
    }

    @Test
    fun bookCategoryRelationsAreLoadedAndCascadeWithoutDeletingBooks() = runBlocking {
        val book = entity(idSuffix = 1)
        val category = Category.create("文学", 0, timestamp = 100).asEntity()
        dao.insert(book)
        database.categoryDao().insert(category)
        dao.insertCategoryRefs(listOf(BookCategoryCrossRef(book.id, category.id)))

        val related = dao.observeAllWithCategories().first().single()
        assertEquals(listOf(category.id), related.categories.map { it.id })
        assertEquals(1, database.categoryDao().observeAllWithBookCount().first().single().bookCount)

        database.categoryDao().delete(category)
        assertEquals(book, dao.getById(book.id))
        assertEquals(emptyList<CategoryEntity>(), dao.getByIdWithCategories(book.id)?.categories)
    }

    private fun entity(
        idSuffix: Int,
        title: String = "书名$idSuffix",
        updatedAt: Long = 200,
    ): BookEntity = BookEntity(
        id = "00000000-0000-0000-0000-${idSuffix.toString().padStart(12, '0')}",
        title = title,
        author = null,
        coverPath = null,
        totalPages = 100,
        currentPage = 0,
        status = BookStatus.WISH,
        createdAt = 100,
        updatedAt = updatedAt,
    )
}
