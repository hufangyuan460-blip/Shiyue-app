package com.shiyue.reader.domain.repository

import com.shiyue.reader.core.model.Book
import kotlinx.coroutines.flow.Flow

interface BookRepository {
    suspend fun addBook(book: Book)

    suspend fun getBook(id: String): Book?

    fun observeBooks(): Flow<List<Book>>

    suspend fun updateBook(book: Book)

    suspend fun deleteBook(id: String)
}
