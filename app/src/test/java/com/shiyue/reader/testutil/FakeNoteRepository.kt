package com.shiyue.reader.testutil

import com.shiyue.reader.core.model.Note
import com.shiyue.reader.domain.repository.NoteRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

class FakeNoteRepository(
    initialNotes: List<Note> = emptyList(),
    private val addFailure: Throwable? = null,
    private val updateFailure: Throwable? = null,
) : NoteRepository {
    val notes = MutableStateFlow(initialNotes)

    override fun observeAll(): Flow<List<Note>> = notes
    override fun observeByBook(bookId: String): Flow<List<Note>> =
        notes.map { list -> list.filter { it.bookId == bookId } }
    override fun observeById(id: String): Flow<Note?> = notes.map { list -> list.firstOrNull { it.id == id } }
    override suspend fun getById(id: String): Note? = notes.value.firstOrNull { it.id == id }
    override suspend fun addNote(note: Note) {
        addFailure?.let { throw it }
        notes.update { it + note }
    }
    override suspend fun updateNote(note: Note) {
        updateFailure?.let { throw it }
        notes.update { list -> list.map { if (it.id == note.id) note else it } }
    }
    override suspend fun deleteNote(id: String) {
        notes.update { list -> list.filterNot { it.id == id } }
    }
    override suspend fun imagePaths(): List<String> = notes.value.mapNotNull { it.imagePath }
}
