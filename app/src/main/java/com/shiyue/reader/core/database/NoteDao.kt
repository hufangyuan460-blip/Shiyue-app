package com.shiyue.reader.core.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(note: NoteEntity)

    @Update(onConflict = OnConflictStrategy.ABORT)
    suspend fun update(note: NoteEntity): Int

    @Query("SELECT * FROM notes WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): NoteEntity?

    @Query("SELECT * FROM notes WHERE id = :id LIMIT 1")
    fun observeById(id: String): Flow<NoteEntity?>

    @Query("SELECT * FROM notes ORDER BY created_at DESC, id ASC")
    fun observeAll(): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE book_id = :bookId ORDER BY created_at DESC, id ASC")
    fun observeByBook(bookId: String): Flow<List<NoteEntity>>

    @Query("DELETE FROM notes WHERE id = :id")
    suspend fun deleteById(id: String): Int

    @Query("SELECT image_path FROM notes WHERE image_path IS NOT NULL")
    suspend fun imagePaths(): List<String>
}
