package com.shiyue.reader.core.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface BookDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(book: BookEntity)

    @Query("SELECT * FROM books WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): BookEntity?

    @Query("SELECT * FROM books WHERE id = :id LIMIT 1")
    fun observeById(id: String): Flow<BookEntity?>

    @Query("SELECT * FROM books ORDER BY updated_at DESC, id ASC")
    fun observeAll(): Flow<List<BookEntity>>

    @Transaction
    @Query("SELECT * FROM books ORDER BY updated_at DESC, id ASC")
    fun observeAllWithCategories(): Flow<List<BookWithCategoriesEntity>>

    @Transaction
    @Query("SELECT * FROM books WHERE id = :id LIMIT 1")
    fun observeByIdWithCategories(id: String): Flow<BookWithCategoriesEntity?>

    @Transaction
    @Query("SELECT * FROM books WHERE id = :id LIMIT 1")
    suspend fun getByIdWithCategories(id: String): BookWithCategoriesEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertCategoryRefs(refs: List<BookCategoryCrossRef>)

    @Query("DELETE FROM book_category_cross_ref WHERE book_id = :bookId")
    suspend fun deleteCategoryRefsForBook(bookId: String): Int

    @Update(onConflict = OnConflictStrategy.ABORT)
    suspend fun update(book: BookEntity): Int

    @Query("DELETE FROM books WHERE id = :id")
    suspend fun deleteById(id: String): Int
}
