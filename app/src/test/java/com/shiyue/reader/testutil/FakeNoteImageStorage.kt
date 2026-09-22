package com.shiyue.reader.testutil

import com.shiyue.reader.domain.repository.CaptureTarget
import com.shiyue.reader.domain.repository.NoteImageStorage

class FakeNoteImageStorage : NoteImageStorage {
    val storedImages = mutableListOf<String>()
    val deletedImages = mutableListOf<String>()
    var nextPath = 0
        private set

    override suspend fun createCaptureTarget(): CaptureTarget =
        CaptureTarget("content://test/note-capture", "note-capture.jpg")

    override suspend fun processAndStoreImage(sourceUri: String): String {
        nextPath += 1
        val path = "notes/fake-$nextPath.jpg"
        storedImages += path
        return path
    }

    override suspend fun deleteImage(relativePath: String): Boolean {
        deletedImages += relativePath
        return true
    }

    override suspend fun deleteTemporary(token: String) = Unit
    override suspend fun cleanupTemporaryFiles() = Unit
    override suspend fun cleanupOrphanedImages(referencedPaths: Set<String>) = Unit
}
