package com.shiyue.reader.core.data

import com.shiyue.reader.core.model.Book
import com.shiyue.reader.core.model.LibraryBook
import com.shiyue.reader.domain.repository.BookRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update

@Singleton
class TestBookRepository @Inject constructor(
    private val categoryRepository: TestCategoryRepository,
) : BookRepository {
    private val books = MutableStateFlow<List<Book>>(emptyList())
    private val categoryIds = MutableStateFlow<Map<String, Set<String>>>(emptyMap())

    override suspend fun addBook(book: Book, categoryIds: Set<String>) {
        books.update { current -> (current + book).sortedByDescending(Book::updatedAt) }
        this.categoryIds.update { it + (book.id to categoryIds) }
    }

    override suspend fun getBook(id: String): Book? = books.value.firstOrNull { it.id == id }

    override fun observeBook(id: String): Flow<Book?> = combine(books, categoryIds) { current, _ ->
        current.firstOrNull { it.id == id }
    }

    override fun observeLibraryBook(id: String): Flow<LibraryBook?> =
        combine(books, categoryIds, categoryRepository.categories) { current, references, categories ->
            current.firstOrNull { it.id == id }?.let { book ->
                LibraryBook(book, categories.map { it.category }.filter { it.id in references[book.id].orEmpty() })
            }
        }

    override fun observeBooks(): Flow<List<Book>> = books

    override fun observeLibraryBooks(): Flow<List<LibraryBook>> =
        combine(books, categoryIds, categoryRepository.categories) { list, references, categories ->
            list.map { book ->
                LibraryBook(book, categories.map { it.category }.filter { it.id in references[book.id].orEmpty() })
            }
        }

    override suspend fun updateBook(book: Book) {
        books.update { current ->
            current.map { existing -> if (existing.id == book.id) book else existing }
                .sortedByDescending(Book::updatedAt)
        }
    }

    override suspend fun updateBookWithCategories(book: Book, categoryIds: Set<String>) {
        updateBook(book)
        replaceCategories(book.id, categoryIds)
    }

    override suspend fun replaceCategories(bookId: String, categoryIds: Set<String>) {
        this.categoryIds.update { it + (bookId to categoryIds) }
    }

    override suspend fun deleteBook(id: String) {
        books.update { current -> current.filterNot { it.id == id } }
        categoryIds.update { it - id }
    }

    fun reset() {
        books.value = emptyList()
        categoryIds.value = emptyMap()
    }

    fun seed(vararg initialBooks: Book) {
        books.value = initialBooks.toList().sortedByDescending(Book::updatedAt)
    }

    fun booksSnapshot(): List<Book> = books.value
}
