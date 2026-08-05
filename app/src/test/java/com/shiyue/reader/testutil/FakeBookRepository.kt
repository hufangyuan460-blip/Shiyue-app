package com.shiyue.reader.testutil

import com.shiyue.reader.core.model.Book
import com.shiyue.reader.domain.repository.BookRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
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
    var addCalls = 0
        private set
    var updateCalls = 0
        private set

    override suspend fun addBook(book: Book) {
        addCalls += 1
        addGate?.await()
        addFailure?.let { throw it }
        books.update { current -> current + book }
    }

    override suspend fun getBook(id: String): Book? = books.value.firstOrNull { it.id == id }

    override fun observeBook(id: String): Flow<Book?> = observeFailure?.let { failure ->
        kotlinx.coroutines.flow.flow { throw failure }
    } ?: books.map { current -> current.firstOrNull { it.id == id } }

    override fun observeBooks(): Flow<List<Book>> = books

    override suspend fun updateBook(book: Book) {
        updateCalls += 1
        updateGate?.await()
        updateFailure?.let { throw it }
        books.update { current -> current.map { if (it.id == book.id) book else it } }
    }

    override suspend fun deleteBook(id: String) {
        books.update { current -> current.filterNot { it.id == id } }
    }
}
