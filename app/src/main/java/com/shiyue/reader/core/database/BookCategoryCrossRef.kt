package com.shiyue.reader.core.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "book_category_cross_ref",
    primaryKeys = ["book_id", "category_id"],
    foreignKeys = [
        ForeignKey(
            entity = BookEntity::class,
            parentColumns = ["id"],
            childColumns = ["book_id"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["category_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("book_id"), Index("category_id")],
)
data class BookCategoryCrossRef(
    @ColumnInfo(name = "book_id") val bookId: String,
    @ColumnInfo(name = "category_id") val categoryId: String,
)
