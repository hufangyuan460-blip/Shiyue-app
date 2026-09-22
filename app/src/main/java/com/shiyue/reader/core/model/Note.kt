package com.shiyue.reader.core.model

import java.util.UUID

@ConsistentCopyVisibility
data class Note private constructor(
    val id: String,
    val bookId: String,
    val sessionId: String?,
    val pageNumber: Int?,
    val content: String,
    val imagePath: String?,
    val createdAt: Long,
    val updatedAt: Long,
) {
    fun updated(
        sessionId: String? = this.sessionId,
        pageNumber: Int? = this.pageNumber,
        content: String = this.content,
        imagePath: String? = this.imagePath,
        updatedAt: Long,
    ): Note = validated(
        id = id,
        bookId = bookId,
        sessionId = sessionId,
        pageNumber = pageNumber,
        content = content,
        imagePath = imagePath,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )

    companion object {
        fun create(
            bookId: String,
            sessionId: String?,
            pageNumber: Int?,
            content: String,
            imagePath: String? = null,
            timestamp: Long = System.currentTimeMillis(),
        ): Note = validated(
            id = UUID.randomUUID().toString(),
            bookId = bookId,
            sessionId = sessionId,
            pageNumber = pageNumber,
            content = content,
            imagePath = imagePath,
            createdAt = timestamp,
            updatedAt = timestamp,
        )

        internal fun restore(
            id: String,
            bookId: String,
            sessionId: String?,
            pageNumber: Int?,
            content: String,
            imagePath: String?,
            createdAt: Long,
            updatedAt: Long,
        ): Note = validated(
            id = id,
            bookId = bookId,
            sessionId = sessionId,
            pageNumber = pageNumber,
            content = content,
            imagePath = imagePath,
            createdAt = createdAt,
            updatedAt = updatedAt,
        )

        private fun validated(
            id: String,
            bookId: String,
            sessionId: String?,
            pageNumber: Int?,
            content: String,
            imagePath: String?,
            createdAt: Long,
            updatedAt: Long,
        ): Note {
            require(runCatching { UUID.fromString(id) }.isSuccess) { "Note id must be a valid UUID" }
            require(runCatching { UUID.fromString(bookId) }.isSuccess) { "Note bookId must be a valid UUID" }
            require(sessionId == null || runCatching { UUID.fromString(sessionId) }.isSuccess) {
                "Note sessionId must be a valid UUID or null"
            }
            require(pageNumber == null || pageNumber >= 0) { "Note pageNumber must be non-negative" }
            val normalizedContent = content.trim()
            require(normalizedContent.isNotEmpty()) { "Note content must not be blank" }
            return Note(id, bookId, sessionId, pageNumber, normalizedContent, imagePath, createdAt, updatedAt)
        }
    }
}
