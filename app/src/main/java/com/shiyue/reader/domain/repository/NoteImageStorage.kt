package com.shiyue.reader.domain.repository

interface NoteImageStorage {
    suspend fun createCaptureTarget(): CaptureTarget
    suspend fun processAndStoreImage(sourceUri: String): String
    suspend fun deleteImage(relativePath: String): Boolean
    suspend fun deleteTemporary(token: String)
    suspend fun cleanupTemporaryFiles()
    suspend fun cleanupOrphanedImages(referencedPaths: Set<String>)
}
