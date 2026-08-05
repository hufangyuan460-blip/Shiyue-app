package com.shiyue.reader.domain.usecase

import com.shiyue.reader.core.model.Book
import com.shiyue.reader.core.model.BookStatus
import com.shiyue.reader.domain.repository.BookRepository
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AddBookUseCaseTest {
    @Test
    fun createsValidatedBookAndSavesItThroughRepository() = runBlocking {
        val repository = RecordingBookRepository()
        val useCase = AddBookUseCase(repository)

        val result = useCase(
            title = "  书名  ",
            author = "   ",
            totalPages = 120,
            status = BookStatus.READING,
        )

        UUID.fromString(result.id)
        assertEquals("书名", result.title)
        assertNull(result.author)
        assertEquals(120, result.totalPages)
        assertEquals(0, result.currentPage)
        assertEquals(BookStatus.READING, result.status)
        assertEquals(result, repository.addedBook)
    }

    private class RecordingBookRepository : BookRepository {
        var addedBook: Book? = null

        override suspend fun addBook(book: Book, categoryIds: Set<String>) {
            addedBook = book
        }

        override suspend fun getBook(id: String): Book? = null

        override fun observeBook(id: String): Flow<Book?> = flowOf(null)

        override fun observeLibraryBook(id: String) = flowOf<com.shiyue.reader.core.model.LibraryBook?>(null)

        override fun observeBooks(): Flow<List<Book>> = flowOf(emptyList())

        override fun observeLibraryBooks() = flowOf(emptyList<com.shiyue.reader.core.model.LibraryBook>())

        override suspend fun updateBook(book: Book) = Unit

        override suspend fun updateBookWithCategories(book: Book, categoryIds: Set<String>) = Unit

        override suspend fun replaceCategories(bookId: String, categoryIds: Set<String>) = Unit

        override suspend fun deleteBook(id: String) = Unit
    }
}
