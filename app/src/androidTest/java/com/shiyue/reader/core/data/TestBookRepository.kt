package com.shiyue.reader.core.data

import com.shiyue.reader.core.model.Book
import com.shiyue.reader.domain.repository.BookRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

@Singleton
class TestBookRepository @Inject constructor() : BookRepository {
    private val books = MutableStateFlow<List<Book>>(emptyList())

    override suspend fun addBook(book: Book) {
        books.update { current -> (current + book).sortedByDescending(Book::updatedAt) }
    }

    override suspend fun getBook(id: String): Book? = books.value.firstOrNull { it.id == id }

    override fun observeBooks(): Flow<List<Book>> = books

    override suspend fun updateBook(book: Book) {
        books.update { current ->
            current.map { existing -> if (existing.id == book.id) book else existing }
                .sortedByDescending(Book::updatedAt)
        }
    }

    override suspend fun deleteBook(id: String) {
        books.update { current -> current.filterNot { it.id == id } }
    }

    fun reset() {
        books.value = emptyList()
    }
}
