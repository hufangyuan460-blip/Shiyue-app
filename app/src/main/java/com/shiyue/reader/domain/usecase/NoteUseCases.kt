package com.shiyue.reader.domain.usecase

import com.shiyue.reader.core.model.Note
import com.shiyue.reader.domain.repository.NoteImageStorage
import com.shiyue.reader.domain.repository.NoteRepository
import javax.inject.Inject

class ObserveNotesUseCase @Inject constructor(private val repository: NoteRepository) {
    operator fun invoke() = repository.observeAll()
}

class ObserveBookNotesUseCase @Inject constructor(private val repository: NoteRepository) {
    operator fun invoke(bookId: String) = repository.observeByBook(bookId)
}

class ObserveNoteUseCase @Inject constructor(private val repository: NoteRepository) {
    operator fun invoke(id: String) = repository.observeById(id)
}

class CreateNoteImageCaptureTargetUseCase @Inject constructor(private val storage: NoteImageStorage) {
    suspend operator fun invoke() = storage.createCaptureTarget()
}

class DeleteTemporaryNoteImageUseCase @Inject constructor(private val storage: NoteImageStorage) {
    suspend operator fun invoke(token: String) = storage.deleteTemporary(token)
}

class SaveNoteUseCase @Inject constructor(
    private val repository: NoteRepository,
    private val storage: NoteImageStorage,
) {
    suspend operator fun invoke(
        noteId: String?,
        bookId: String,
        sessionId: String?,
        pageNumber: Int?,
        content: String,
        newImageUri: String?,
        removeImage: Boolean,
        timestamp: Long = System.currentTimeMillis(),
    ): Note {
        val existing = noteId?.let { checkNotNull(repository.getById(it)) { "Note not found: $it" } }
        val newPath = newImageUri?.let { storage.processAndStoreImage(it) }
        val finalPath = when {
            newPath != null -> newPath
            removeImage -> null
            else -> existing?.imagePath
        }
        val note = if (existing == null) {
            Note.create(bookId, sessionId, pageNumber, content, finalPath, timestamp)
        } else {
            existing.updated(
                sessionId = existing.sessionId,
                pageNumber = pageNumber,
                content = content,
                imagePath = finalPath,
                updatedAt = timestamp,
            )
        }
        try {
            if (existing == null) repository.addNote(note) else repository.updateNote(note)
        } catch (error: Throwable) {
            newPath?.let { storage.deleteImage(it) }
            throw error
        }
        if (existing != null && existing.imagePath != null && existing.imagePath != finalPath) {
            storage.deleteImage(existing.imagePath)
        }
        return note
    }
}

class DeleteNoteUseCase @Inject constructor(
    private val repository: NoteRepository,
    private val storage: NoteImageStorage,
) {
    suspend operator fun invoke(noteId: String) {
        val existing = checkNotNull(repository.getById(noteId)) { "Note not found: $noteId" }
        repository.deleteNote(noteId)
        existing.imagePath?.let { storage.deleteImage(it) }
    }
}

class CleanupNoteFilesUseCase @Inject constructor(
    private val repository: NoteRepository,
    private val storage: NoteImageStorage,
) {
    suspend operator fun invoke() {
        val referenced = repository.imagePaths().toSet()
        storage.cleanupTemporaryFiles()
        storage.cleanupOrphanedImages(referenced)
    }
}
