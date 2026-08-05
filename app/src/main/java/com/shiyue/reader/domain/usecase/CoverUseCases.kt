package com.shiyue.reader.domain.usecase

import com.shiyue.reader.core.model.Book
import com.shiyue.reader.domain.repository.BookRepository
import com.shiyue.reader.domain.repository.CaptureTarget
import com.shiyue.reader.domain.repository.CoverStorage
import com.shiyue.reader.domain.repository.CoverTransform
import javax.inject.Inject
import kotlinx.coroutines.flow.first

class CreateCaptureTargetUseCase @Inject constructor(private val storage: CoverStorage) {
    suspend operator fun invoke(): CaptureTarget = storage.createCaptureTarget()
}

class ReplaceBookCoverUseCase @Inject constructor(
    private val repository: BookRepository,
    private val storage: CoverStorage,
) {
    suspend operator fun invoke(
        bookId: String,
        sourceUri: String,
        transform: CoverTransform,
        timestamp: Long = System.currentTimeMillis(),
    ): Book {
        val existing = checkNotNull(repository.getBook(bookId)) { "Book not found: $bookId" }
        val newPath = storage.processAndStoreCover(sourceUri, transform)
        val updated = existing.updated(coverPath = newPath, updatedAt = timestamp)
        try {
            repository.updateBook(updated)
        } catch (error: Throwable) {
            storage.deleteCover(newPath)
            throw error
        }
        existing.coverPath?.let { storage.deleteCover(it) }
        return updated
    }
}

class RemoveBookCoverUseCase @Inject constructor(
    private val repository: BookRepository,
    private val storage: CoverStorage,
) {
    suspend operator fun invoke(bookId: String, timestamp: Long = System.currentTimeMillis()): Book {
        val existing = checkNotNull(repository.getBook(bookId)) { "Book not found: $bookId" }
        val updated = existing.updated(coverPath = null, updatedAt = timestamp)
        repository.updateBook(updated)
        existing.coverPath?.let { storage.deleteCover(it) }
        return updated
    }
}

class DeleteBookUseCase @Inject constructor(
    private val repository: BookRepository,
    private val storage: CoverStorage,
) {
    suspend operator fun invoke(bookId: String) {
        val existing = checkNotNull(repository.getBook(bookId)) { "Book not found: $bookId" }
        repository.deleteBook(bookId)
        existing.coverPath?.let { storage.deleteCover(it) }
    }
}

class DeleteTemporaryCoverUseCase @Inject constructor(private val storage: CoverStorage) {
    suspend operator fun invoke(token: String) = storage.deleteTemporary(token)
}

class CleanupCoverFilesUseCase @Inject constructor(
    private val repository: BookRepository,
    private val storage: CoverStorage,
) {
    suspend operator fun invoke() {
        val referenced = repository.observeBooks().first().mapNotNullTo(mutableSetOf(), Book::coverPath)
        storage.cleanupTemporaryFiles()
        storage.cleanupOrphanedCovers(referenced)
    }
}
