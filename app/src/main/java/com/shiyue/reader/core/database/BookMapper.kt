package com.shiyue.reader.core.database

import com.shiyue.reader.core.model.Book

internal fun BookEntity.asExternalModel(): Book = Book.restore(
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

internal fun Book.asEntity(): BookEntity = BookEntity(
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
