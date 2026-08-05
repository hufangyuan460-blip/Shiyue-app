package com.shiyue.reader.domain.usecase

import com.shiyue.reader.core.model.Book
import com.shiyue.reader.core.model.BookStatus
import com.shiyue.reader.domain.repository.BookRepository
import javax.inject.Inject

class UpdateBookMetadataUseCase @Inject constructor(
    private val repository: BookRepository,
) {
    suspend operator fun invoke(
        bookId: String,
        title: String,
        author: String?,
        totalPages: Int,
        status: BookStatus,
        timestamp: Long = System.currentTimeMillis(),
        categoryIds: Set<String>? = null,
    ): Book {
        val existing = checkNotNull(repository.getBook(bookId)) { "Book not found: $bookId" }
        val updated = existing.updated(
            title = title,
            author = author,
            totalPages = totalPages,
            status = status,
            updatedAt = timestamp,
        )
        if (categoryIds == null) repository.updateBook(updated)
        else repository.updateBookWithCategories(updated, categoryIds)
        return updated
    }
}
