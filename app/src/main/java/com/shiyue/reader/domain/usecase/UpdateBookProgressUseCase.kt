package com.shiyue.reader.domain.usecase

import com.shiyue.reader.core.model.Book
import com.shiyue.reader.core.model.BookStatus
import com.shiyue.reader.domain.repository.BookRepository
import javax.inject.Inject

class UpdateBookProgressUseCase @Inject constructor(
    private val repository: BookRepository,
) {
    suspend operator fun invoke(
        bookId: String,
        currentPage: Int,
        markFinished: Boolean,
        timestamp: Long = System.currentTimeMillis(),
    ): Book {
        val existing = checkNotNull(repository.getBook(bookId)) { "Book not found: $bookId" }
        val updated = existing.updated(
            currentPage = currentPage,
            status = if (markFinished) BookStatus.FINISHED else existing.status,
            updatedAt = timestamp,
        )
        repository.updateBook(updated)
        return updated
    }
}
