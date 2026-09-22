package com.shiyue.reader.domain.repository

import com.shiyue.reader.core.model.Note
import kotlinx.coroutines.flow.Flow

interface NoteRepository {
    fun observeAll(): Flow<List<Note>>
    fun observeByBook(bookId: String): Flow<List<Note>>
    fun observeById(id: String): Flow<Note?>
    suspend fun getById(id: String): Note?
    suspend fun addNote(note: Note)
    suspend fun updateNote(note: Note)
    suspend fun deleteNote(id: String)
    suspend fun imagePaths(): List<String>
}
