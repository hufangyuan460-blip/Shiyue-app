package com.shiyue.reader.domain.usecase

import com.shiyue.reader.core.model.Book
import com.shiyue.reader.core.model.BookStatus
import com.shiyue.reader.domain.repository.BookRepository
import javax.inject.Inject

class AddBookUseCase @Inject constructor(
    private val bookRepository: BookRepository,
) {
    suspend operator fun invoke(
        title: String,
        author: String?,
        totalPages: Int,
        status: BookStatus = BookStatus.WISH,
        categoryIds: Set<String> = emptySet(),
    ): Book {
        val book = Book.create(
            title = title,
            author = author,
            totalPages = totalPages,
            status = status,
        )
        bookRepository.addBook(book, categoryIds)
        return book
    }
}
