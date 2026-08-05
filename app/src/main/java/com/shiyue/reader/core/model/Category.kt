package com.shiyue.reader.core.model

import java.text.Normalizer
import java.util.Locale
import java.util.UUID

@ConsistentCopyVisibility
data class Category private constructor(
    val id: String,
    val name: String,
    val normalizedName: String,
    val sortOrder: Int,
    val createdAt: Long,
    val updatedAt: Long,
) {
    fun renamed(name: String, timestamp: Long = System.currentTimeMillis()): Category = validated(
        id, name, sortOrder, createdAt, timestamp,
    )

    fun reordered(sortOrder: Int, timestamp: Long = System.currentTimeMillis()): Category = validated(
        id, name, sortOrder, createdAt, timestamp,
    )

    companion object {
        const val MAX_NAME_CODE_POINTS = 20

        fun create(
            name: String,
            sortOrder: Int,
            timestamp: Long = System.currentTimeMillis(),
        ): Category = validated(UUID.randomUUID().toString(), name, sortOrder, timestamp, timestamp)

        internal fun restore(
            id: String,
            name: String,
            normalizedName: String,
            sortOrder: Int,
            createdAt: Long,
            updatedAt: Long,
        ): Category {
            val category = validated(id, name, sortOrder, createdAt, updatedAt)
            require(category.normalizedName == normalizedName) { "Category normalizedName is invalid" }
            return category
        }

        fun normalize(name: String): String = Normalizer.normalize(name.trim(), Normalizer.Form.NFKC)
            .lowercase(Locale.ROOT)

        private fun validated(
            id: String,
            name: String,
            sortOrder: Int,
            createdAt: Long,
            updatedAt: Long,
        ): Category {
            require(runCatching { UUID.fromString(id) }.isSuccess) { "Category id must be a valid UUID" }
            val cleanName = name.trim()
            require(cleanName.isNotEmpty()) { "Category name must not be blank" }
            require(cleanName.codePointCount(0, cleanName.length) <= MAX_NAME_CODE_POINTS) {
                "Category name must not exceed $MAX_NAME_CODE_POINTS characters"
            }
            require(sortOrder >= 0) { "Category sortOrder must not be negative" }
            return Category(id, cleanName, normalize(cleanName), sortOrder, createdAt, updatedAt)
        }
    }
}
