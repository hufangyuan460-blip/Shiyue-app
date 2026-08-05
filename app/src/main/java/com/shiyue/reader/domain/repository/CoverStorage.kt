package com.shiyue.reader.domain.repository

data class CaptureTarget(val uri: String, val token: String)

data class CoverTransform(
    val scale: Float = 1f,
    val offsetXFraction: Float = 0f,
    val offsetYFraction: Float = 0f,
    val quarterTurns: Int = 0,
)

interface CoverStorage {
    suspend fun createCaptureTarget(): CaptureTarget
    suspend fun processAndStoreCover(sourceUri: String, transform: CoverTransform): String
    suspend fun deleteCover(relativePath: String): Boolean
    suspend fun deleteTemporary(token: String)
    suspend fun cleanupTemporaryFiles()
    suspend fun cleanupOrphanedCovers(referencedPaths: Set<String>)
}
