package com.shiyue.reader.testutil

import com.shiyue.reader.core.model.Book
import com.shiyue.reader.core.model.LibraryBook
import com.shiyue.reader.core.model.Category
import com.shiyue.reader.domain.repository.BookRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update

class FakeBookRepository(
    initialBooks: List<Book> = emptyList(),
    private val addFailure: Throwable? = null,
    private val addGate: CompletableDeferred<Unit>? = null,
    private val updateFailure: Throwable? = null,
    private val updateGate: CompletableDeferred<Unit>? = null,
    private val observeFailure: Throwable? = null,
) : BookRepository {
    val books = MutableStateFlow(initialBooks)
    val categoryAssignments = MutableStateFlow<Map<String, List<Category>>>(emptyMap())
    var addCalls = 0
        private set
    var updateCalls = 0
        private set

    override suspend fun addBook(book: Book, categoryIds: Set<String>) {
        addCalls += 1
        addGate?.await()
        addFailure?.let { throw it }
        books.update { current -> current + book }
    }

    override suspend fun getBook(id: String): Book? = books.value.firstOrNull { it.id == id }

    override fun observeBook(id: String): Flow<Book?> = observeFailure?.let { failure ->
        kotlinx.coroutines.flow.flow { throw failure }
    } ?: books.map { current -> current.firstOrNull { it.id == id } }

    override fun observeLibraryBook(id: String): Flow<LibraryBook?> = observeFailure?.let { failure ->
        kotlinx.coroutines.flow.flow { throw failure }
    } ?: combine(books, categoryAssignments) { list, assignments ->
        list.firstOrNull { it.id == id }?.let { LibraryBook(it, assignments[it.id].orEmpty()) }
    }

    override fun observeBooks(): Flow<List<Book>> = books

    override fun observeLibraryBooks(): Flow<List<LibraryBook>> = combine(books, categoryAssignments) { list, assignments ->
        list.map { LibraryBook(it, assignments[it.id].orEmpty()) }
    }

    override suspend fun updateBook(book: Book) {
        updateCalls += 1
        updateGate?.await()
        updateFailure?.let { throw it }
        books.update { current -> current.map { if (it.id == book.id) book else it } }
    }

    override suspend fun updateBookWithCategories(book: Book, categoryIds: Set<String>) = updateBook(book)

    override suspend fun replaceCategories(bookId: String, categoryIds: Set<String>) = Unit

    override suspend fun deleteBook(id: String) {
        books.update { current -> current.filterNot { it.id == id } }
    }
}
