package com.shiyue.reader.core.database

import com.shiyue.reader.core.model.Category
import com.shiyue.reader.core.model.CategorySummary
import com.shiyue.reader.core.model.LibraryBook

fun Category.asEntity() = CategoryEntity(id, name, normalizedName, sortOrder, createdAt, updatedAt)

fun CategoryEntity.asExternalModel() = Category.restore(
    id, name, normalizedName, sortOrder, createdAt, updatedAt,
)

fun BookWithCategoriesEntity.asExternalModel() = LibraryBook(
    book = book.asExternalModel(),
    categories = categories.map(CategoryEntity::asExternalModel).sortedBy(Category::sortOrder),
)

fun CategoryWithBookCount.asExternalModel() = CategorySummary(category.asExternalModel(), bookCount)
