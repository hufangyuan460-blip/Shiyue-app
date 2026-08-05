package com.shiyue.reader.core.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {
    @Query(
        """SELECT categories.*, COUNT(book_category_cross_ref.book_id) AS book_count
        FROM categories
        LEFT JOIN book_category_cross_ref ON categories.id = book_category_cross_ref.category_id
        GROUP BY categories.id
        ORDER BY categories.sort_order ASC, categories.id ASC""",
    )
    fun observeAllWithBookCount(): Flow<List<CategoryWithBookCount>>

    @Query("SELECT * FROM categories ORDER BY sort_order ASC, id ASC")
    suspend fun getAll(): List<CategoryEntity>

    @Query("SELECT * FROM categories WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): CategoryEntity?

    @Query("SELECT * FROM categories WHERE normalized_name = :normalizedName LIMIT 1")
    suspend fun getByNormalizedName(normalizedName: String): CategoryEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(category: CategoryEntity)

    @Update(onConflict = OnConflictStrategy.ABORT)
    suspend fun update(category: CategoryEntity): Int

    @Update(onConflict = OnConflictStrategy.ABORT)
    suspend fun updateAll(categories: List<CategoryEntity>)

    @Delete
    suspend fun delete(category: CategoryEntity): Int
}
