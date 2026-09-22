package com.shiyue.reader.core.data

import com.shiyue.reader.domain.repository.CaptureTarget
import com.shiyue.reader.domain.repository.NoteImageStorage
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TestNoteImageStorage @Inject constructor() : NoteImageStorage {
    override suspend fun createCaptureTarget(): CaptureTarget =
        CaptureTarget("content://test/note-capture", "note-capture.jpg")

    override suspend fun processAndStoreImage(sourceUri: String) = "notes/test.jpg"

    override suspend fun deleteImage(relativePath: String) = true

    override suspend fun deleteTemporary(token: String) = Unit

    override suspend fun cleanupTemporaryFiles() = Unit

    override suspend fun cleanupOrphanedImages(referencedPaths: Set<String>) = Unit
}
