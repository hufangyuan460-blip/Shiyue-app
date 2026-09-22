package com.shiyue.reader.core.data.repository

import com.shiyue.reader.core.database.NoteDao
import com.shiyue.reader.core.database.ShiyueDatabase
import com.shiyue.reader.core.database.asEntity
import com.shiyue.reader.core.database.asExternalModel
import com.shiyue.reader.core.model.Note
import com.shiyue.reader.domain.repository.NoteRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Singleton
class OfflineNoteRepository @Inject constructor(
    private val database: ShiyueDatabase,
) : NoteRepository {
    private val dao: NoteDao get() = database.noteDao()

    override fun observeAll(): Flow<List<Note>> =
        dao.observeAll().map { notes -> notes.map { it.asExternalModel() } }

    override fun observeByBook(bookId: String): Flow<List<Note>> =
        dao.observeByBook(bookId).map { notes -> notes.map { it.asExternalModel() } }

    override fun observeById(id: String): Flow<Note?> =
        dao.observeById(id).map { it?.asExternalModel() }

    override suspend fun getById(id: String): Note? = dao.getById(id)?.asExternalModel()

    override suspend fun addNote(note: Note) = dao.insert(note.asEntity())

    override suspend fun updateNote(note: Note) {
        check(dao.update(note.asEntity()) == 1) { "Note not found: ${note.id}" }
    }

    override suspend fun deleteNote(id: String) {
        check(dao.deleteById(id) == 1) { "Note not found: $id" }
    }

    override suspend fun imagePaths(): List<String> = dao.imagePaths()
}
