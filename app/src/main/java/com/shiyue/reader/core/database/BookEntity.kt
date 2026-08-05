package com.shiyue.reader.core.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.shiyue.reader.core.model.BookStatus

@Entity(
    tableName = "books",
    indices = [
        Index(value = ["status"]),
        Index(value = ["updated_at"]),
    ],
)
data class BookEntity(
    @PrimaryKey
    val id: String,
    val title: String,
    val author: String?,
    @ColumnInfo(name = "cover_path")
    val coverPath: String?,
    @ColumnInfo(name = "total_pages")
    val totalPages: Int,
    @ColumnInfo(name = "current_page", defaultValue = "0")
    val currentPage: Int,
    val status: BookStatus,
    @ColumnInfo(name = "created_at")
    val createdAt: Long,
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long,
) {
    init {
        require(title.isNotBlank()) { "Book title must not be blank" }
        require(totalPages > 0) { "Book totalPages must be greater than 0" }
        require(currentPage in 0..totalPages) {
            "Book currentPage must be between 0 and totalPages"
        }
    }
}
