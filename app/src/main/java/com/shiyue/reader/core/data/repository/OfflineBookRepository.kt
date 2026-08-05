package com.shiyue.reader.core.data.repository

import com.shiyue.reader.core.database.BookDao
import com.shiyue.reader.core.database.asEntity
import com.shiyue.reader.core.database.asExternalModel
import com.shiyue.reader.core.model.Book
import com.shiyue.reader.domain.repository.BookRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Singleton
class OfflineBookRepository @Inject constructor(
    private val bookDao: BookDao,
) : BookRepository {
    override suspend fun addBook(book: Book) {
        bookDao.insert(book.asEntity())
    }

    override suspend fun getBook(id: String): Book? = bookDao.getById(id)?.asExternalModel()

    override fun observeBooks(): Flow<List<Book>> = bookDao.observeAll().map { books ->
        books.map { it.asExternalModel() }
    }

    override suspend fun updateBook(book: Book) {
        check(bookDao.update(book.asEntity()) == 1) { "Book not found: ${book.id}" }
    }

    override suspend fun deleteBook(id: String) {
        check(bookDao.deleteById(id) == 1) { "Book not found: $id" }
    }

}
