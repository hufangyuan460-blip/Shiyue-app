package com.shiyue.reader.core.database

import androidx.room.Embedded
import androidx.room.ColumnInfo
import androidx.room.Junction
import androidx.room.Relation

data class BookWithCategoriesEntity(
    @Embedded val book: BookEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "id",
        associateBy = Junction(
            value = BookCategoryCrossRef::class,
            parentColumn = "book_id",
            entityColumn = "category_id",
        ),
    )
    val categories: List<CategoryEntity>,
)

data class CategoryWithBookCount(
    @Embedded val category: CategoryEntity,
    @ColumnInfo(name = "book_count") val bookCount: Int,
)
