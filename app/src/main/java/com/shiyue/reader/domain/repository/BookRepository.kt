package com.shiyue.reader.domain.repository

import com.shiyue.reader.core.model.Book
import com.shiyue.reader.core.model.LibraryBook
import kotlinx.coroutines.flow.Flow

interface BookRepository {
    suspend fun addBook(book: Book, categoryIds: Set<String> = emptySet())

    suspend fun getBook(id: String): Book?

    fun observeBook(id: String): Flow<Book?>

    fun observeLibraryBook(id: String): Flow<LibraryBook?>

    fun observeBooks(): Flow<List<Book>>

    fun observeLibraryBooks(): Flow<List<LibraryBook>>

    suspend fun updateBook(book: Book)

    suspend fun updateBookWithCategories(book: Book, categoryIds: Set<String>)

    suspend fun replaceCategories(bookId: String, categoryIds: Set<String>)

    suspend fun deleteBook(id: String)
}
