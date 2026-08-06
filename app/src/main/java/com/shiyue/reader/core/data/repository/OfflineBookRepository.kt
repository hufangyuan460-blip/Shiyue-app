package com.shiyue.reader.core.data.repository

import com.shiyue.reader.core.database.BookDao
import com.shiyue.reader.core.database.BookCategoryCrossRef
import com.shiyue.reader.core.database.ShiyueDatabase
import com.shiyue.reader.core.database.asEntity
import com.shiyue.reader.core.database.asExternalModel
import com.shiyue.reader.core.model.Book
import com.shiyue.reader.core.model.LibraryBook
import com.shiyue.reader.domain.repository.BookRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import androidx.room.withTransaction

@Singleton
class OfflineBookRepository @Inject constructor(
    private val database: ShiyueDatabase,
) : BookRepository {
    private val bookDao: BookDao get() = database.bookDao()

    override suspend fun addBook(book: Book, categoryIds: Set<String>) {
        database.withTransaction {
            bookDao.insert(book.asEntity())
            insertRefs(book.id, categoryIds)
        }
    }

    override suspend fun getBook(id: String): Book? = bookDao.getById(id)?.asExternalModel()

    override fun observeBook(id: String): Flow<Book?> = bookDao.observeById(id).map { book ->
        book?.asExternalModel()
    }

    override fun observeLibraryBook(id: String): Flow<LibraryBook?> =
        bookDao.observeByIdWithCategories(id).map { it?.asExternalModel() }

    override fun observeBooks(): Flow<List<Book>> = bookDao.observeAll().map { books ->
        books.map { it.asExternalModel() }
    }

    override fun observeLibraryBooks(): Flow<List<LibraryBook>> =
        bookDao.observeAllWithCategories().map { books -> books.map { it.asExternalModel() } }

    override suspend fun updateBook(book: Book) {
        check(bookDao.update(book.asEntity()) == 1) { "Book not found: ${book.id}" }
    }

    override suspend fun updateBookWithCategories(book: Book, categoryIds: Set<String>) {
        database.withTransaction {
            check(bookDao.update(book.asEntity()) == 1) { "Book not found: ${book.id}" }
            bookDao.deleteCategoryRefsForBook(book.id)
            insertRefs(book.id, categoryIds)
        }
    }

    override suspend fun replaceCategories(bookId: String, categoryIds: Set<String>) {
        database.withTransaction {
            check(bookDao.getById(bookId) != null) { "Book not found: $bookId" }
            bookDao.deleteCategoryRefsForBook(bookId)
            insertRefs(bookId, categoryIds)
        }
    }

    override suspend fun deleteBook(id: String) {
        database.withTransaction {
            check(database.readingSessionDao().unfinishedCountForBook(id) == 0) {
                "Cannot delete a book with an active reading session"
            }
            check(bookDao.deleteById(id) == 1) { "Book not found: $id" }
        }
    }

    private suspend fun insertRefs(bookId: String, categoryIds: Set<String>) {
        if (categoryIds.isNotEmpty()) {
            bookDao.insertCategoryRefs(categoryIds.distinct().map { BookCategoryCrossRef(bookId, it) })
        }
    }

}
