package com.shiyue.reader.core.model

import java.util.UUID

@ConsistentCopyVisibility
data class Book private constructor(
    val id: String,
    val title: String,
    val author: String?,
    val coverPath: String?,
    val totalPages: Int,
    val currentPage: Int,
    val status: BookStatus,
    val createdAt: Long,
    val updatedAt: Long,
) {
    val progress: Double
        get() = currentPage.toDouble() / totalPages.toDouble()

    fun updated(
        title: String = this.title,
        author: String? = this.author,
        totalPages: Int = this.totalPages,
        currentPage: Int = this.currentPage,
        status: BookStatus = this.status,
        updatedAt: Long,
    ): Book = validated(
        id = id,
        title = title,
        author = author,
        coverPath = coverPath,
        totalPages = totalPages,
        currentPage = currentPage,
        status = status,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )

    companion object {
        fun create(
            title: String,
            author: String?,
            totalPages: Int,
            currentPage: Int = 0,
            status: BookStatus = BookStatus.WISH,
            timestamp: Long = System.currentTimeMillis(),
        ): Book = validated(
            id = UUID.randomUUID().toString(),
            title = title,
            author = author,
            coverPath = null,
            totalPages = totalPages,
            currentPage = currentPage,
            status = status,
            createdAt = timestamp,
            updatedAt = timestamp,
        )

        internal fun restore(
            id: String,
            title: String,
            author: String?,
            coverPath: String?,
            totalPages: Int,
            currentPage: Int,
            status: BookStatus,
            createdAt: Long,
            updatedAt: Long,
        ): Book = validated(
            id = id,
            title = title,
            author = author,
            coverPath = coverPath,
            totalPages = totalPages,
            currentPage = currentPage,
            status = status,
            createdAt = createdAt,
            updatedAt = updatedAt,
        )

        private fun validated(
            id: String,
            title: String,
            author: String?,
            coverPath: String?,
            totalPages: Int,
            currentPage: Int,
            status: BookStatus,
            createdAt: Long,
            updatedAt: Long,
        ): Book {
            require(runCatching { UUID.fromString(id) }.isSuccess) {
                "Book id must be a valid UUID"
            }
            val normalizedTitle = title.trim()
            require(normalizedTitle.isNotEmpty()) { "Book title must not be blank" }
            require(totalPages > 0) { "Book totalPages must be greater than 0" }
            require(currentPage in 0..totalPages) {
                "Book currentPage must be between 0 and totalPages"
            }

            return Book(
                id = id,
                title = normalizedTitle,
                author = author?.trim()?.takeIf { it.isNotEmpty() },
                coverPath = coverPath,
                totalPages = totalPages,
                currentPage = currentPage,
                status = status,
                createdAt = createdAt,
                updatedAt = updatedAt,
            )
        }
    }
}
