package com.shiyue.reader.core.database

import com.shiyue.reader.core.model.Note

internal fun NoteEntity.asExternalModel(): Note = Note.restore(
    id = id,
    bookId = bookId,
    sessionId = sessionId,
    pageNumber = pageNumber,
    content = content,
    imagePath = imagePath,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

internal fun Note.asEntity(): NoteEntity = NoteEntity(
    id = id,
    bookId = bookId,
    sessionId = sessionId,
    pageNumber = pageNumber,
    content = content,
    imagePath = imagePath,
    createdAt = createdAt,
    updatedAt = updatedAt,
)
