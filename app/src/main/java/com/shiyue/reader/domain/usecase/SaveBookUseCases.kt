package com.shiyue.reader.domain.usecase

import com.shiyue.reader.core.model.Book
import com.shiyue.reader.core.model.BookStatus
import com.shiyue.reader.domain.repository.BookRepository
import com.shiyue.reader.domain.repository.CoverStorage
import com.shiyue.reader.domain.repository.CoverTransform
import javax.inject.Inject

data class PendingCover(
    val sourceUri: String,
    val transform: CoverTransform,
)

sealed interface CoverChange {
    data object Keep : CoverChange
    data object Remove : CoverChange
    data class Replace(val pending: PendingCover) : CoverChange
}

class AddBookWithCoverUseCase @Inject constructor(
    private val repository: BookRepository,
    private val storage: CoverStorage,
) {
    suspend operator fun invoke(
        title: String,
        author: String?,
        totalPages: Int,
        status: BookStatus,
        categoryIds: Set<String>,
        pendingCover: PendingCover?,
    ): Book {
        val coverPath = pendingCover?.let {
            storage.processAndStoreCover(it.sourceUri, it.transform)
        }
        val book = Book.create(title, author, totalPages, coverPath = coverPath, status = status)
        try {
            repository.addBook(book, categoryIds)
        } catch (error: Throwable) {
            coverPath?.let { storage.deleteCover(it) }
            throw error
        }
        return book
    }
}

class UpdateBookWithCoverUseCase @Inject constructor(
    private val repository: BookRepository,
    private val storage: CoverStorage,
) {
    suspend operator fun invoke(
        bookId: String,
        title: String,
        author: String?,
        totalPages: Int,
        status: BookStatus,
        categoryIds: Set<String>,
        coverChange: CoverChange,
        timestamp: Long = System.currentTimeMillis(),
    ): Book {
        val existing = checkNotNull(repository.getBook(bookId)) { "Book not found: $bookId" }
        val newPath = when (coverChange) {
            CoverChange.Keep -> existing.coverPath
            CoverChange.Remove -> null
            is CoverChange.Replace -> storage.processAndStoreCover(
                coverChange.pending.sourceUri,
                coverChange.pending.transform,
            )
        }
        val updated = existing.updated(
            title = title,
            author = author,
            coverPath = newPath,
            totalPages = totalPages,
            status = status,
            updatedAt = timestamp,
        )
        try {
            repository.updateBookWithCategories(updated, categoryIds)
        } catch (error: Throwable) {
            if (coverChange is CoverChange.Replace) newPath?.let { storage.deleteCover(it) }
            throw error
        }
        if (coverChange !is CoverChange.Keep && existing.coverPath != null && existing.coverPath != newPath) {
            storage.deleteCover(existing.coverPath)
        }
        return updated
    }
}
