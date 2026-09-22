package com.shiyue.reader.domain.usecase

import com.shiyue.reader.core.model.Note
import com.shiyue.reader.testutil.FakeNoteImageStorage
import com.shiyue.reader.testutil.FakeNoteRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SaveNoteUseCaseTest {
    private val bookId = "00000000-0000-0000-0000-000000000001"

    @Test fun createWithoutImageStoresNote() = runBlocking {
        val repository = FakeNoteRepository()
        val storage = FakeNoteImageStorage()
        val note = SaveNoteUseCase(repository, storage)(
            noteId = null, bookId = bookId, sessionId = null, pageNumber = 5,
            content = "内容", newImageUri = null, removeImage = false, timestamp = 100,
        )
        assertEquals("内容", note.content)
        assertNull(note.imagePath)
        assertEquals(1, repository.notes.value.size)
    }

    @Test fun createWithImageStoresAndKeepsFile() = runBlocking {
        val repository = FakeNoteRepository()
        val storage = FakeNoteImageStorage()
        val note = SaveNoteUseCase(repository, storage)(
            noteId = null, bookId = bookId, sessionId = null, pageNumber = null,
            content = "内容", newImageUri = "content://pick/1", removeImage = false, timestamp = 100,
        )
        assertEquals("notes/fake-1.jpg", note.imagePath)
        assertEquals(listOf("notes/fake-1.jpg"), storage.storedImages)
        assertEquals(0, storage.deletedImages.size)
    }

    @Test fun updateReplacingImageDeletesOldAndKeepsNew() = runBlocking {
        val existing = Note.create(bookId, null, 1, "旧", imagePath = "notes/old.jpg", timestamp = 100)
        val repository = FakeNoteRepository(listOf(existing))
        val storage = FakeNoteImageStorage()
        val note = SaveNoteUseCase(repository, storage)(
            noteId = existing.id, bookId = bookId, sessionId = null, pageNumber = 2,
            content = "新", newImageUri = "content://pick/2", removeImage = false, timestamp = 200,
        )
        assertEquals("notes/fake-1.jpg", note.imagePath)
        assertEquals(listOf("notes/old.jpg"), storage.deletedImages)
    }

    @Test fun updateRemovingImageDeletesOldAndClearsPath() = runBlocking {
        val existing = Note.create(bookId, null, 1, "旧", imagePath = "notes/old.jpg", timestamp = 100)
        val repository = FakeNoteRepository(listOf(existing))
        val storage = FakeNoteImageStorage()
        val note = SaveNoteUseCase(repository, storage)(
            noteId = existing.id, bookId = bookId, sessionId = null, pageNumber = null,
            content = "新", newImageUri = null, removeImage = true, timestamp = 200,
        )
        assertNull(note.imagePath)
        assertEquals(listOf("notes/old.jpg"), storage.deletedImages)
    }

    @Test fun databaseFailureCleansUpNewlyProcessedImage() = runBlocking {
        val repository = FakeNoteRepository(updateFailure = IllegalStateException("boom"))
        val storage = FakeNoteImageStorage()
        val existing = Note.create(bookId, null, 1, "旧", timestamp = 100)
        repository.addNote(existing)
        val error = runCatching {
            SaveNoteUseCase(repository, storage)(
                noteId = existing.id, bookId = bookId, sessionId = null, pageNumber = null,
                content = "新", newImageUri = "content://pick/3", removeImage = false, timestamp = 200,
            )
        }.exceptionOrNull()
        assertEquals("boom", error?.message)
        assertEquals(listOf("notes/fake-1.jpg"), storage.deletedImages)
        assertEquals("旧", repository.getById(existing.id)?.content)
    }

    @Test fun deleteRemovesNoteAndItsImage() = runBlocking {
        val existing = Note.create(bookId, null, 1, "内容", imagePath = "notes/a.jpg", timestamp = 100)
        val repository = FakeNoteRepository(listOf(existing))
        val storage = FakeNoteImageStorage()
        DeleteNoteUseCase(repository, storage)(existing.id)
        assertEquals(0, repository.notes.value.size)
        assertEquals(listOf("notes/a.jpg"), storage.deletedImages)
    }
}
