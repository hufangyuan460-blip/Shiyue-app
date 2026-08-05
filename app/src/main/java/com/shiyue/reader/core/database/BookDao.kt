package com.shiyue.reader.core.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface BookDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(book: BookEntity)

    @Query("SELECT * FROM books WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): BookEntity?

    @Query("SELECT * FROM books ORDER BY updated_at DESC, id ASC")
    fun observeAll(): Flow<List<BookEntity>>

    @Update(onConflict = OnConflictStrategy.ABORT)
    suspend fun update(book: BookEntity): Int

    @Query("DELETE FROM books WHERE id = :id")
    suspend fun deleteById(id: String): Int
}
