package com.shiyue.reader.core.data

import com.shiyue.reader.core.model.Note
import com.shiyue.reader.domain.repository.NoteRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

@Singleton
class TestNoteRepository @Inject constructor() : NoteRepository {
    private val notes = MutableStateFlow<List<Note>>(emptyList())

    override fun observeAll(): Flow<List<Note>> = notes
    override fun observeByBook(bookId: String): Flow<List<Note>> =
        notes.map { list -> list.filter { it.bookId == bookId } }
    override fun observeById(id: String): Flow<Note?> = notes.map { list -> list.firstOrNull { it.id == id } }
    override suspend fun getById(id: String): Note? = notes.value.firstOrNull { it.id == id }
    override suspend fun addNote(note: Note) {
        notes.update { (it + note).sortedByDescending { item -> item.createdAt } }
    }
    override suspend fun updateNote(note: Note) {
        notes.update { list -> list.map { if (it.id == note.id) note else it } }
    }
    override suspend fun deleteNote(id: String) {
        notes.update { list -> list.filterNot { it.id == id } }
    }
    override suspend fun imagePaths(): List<String> = notes.value.mapNotNull { it.imagePath }

    fun reset() {
        notes.value = emptyList()
    }
}
