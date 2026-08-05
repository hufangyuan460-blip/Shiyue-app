package com.shiyue.reader.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [BookEntity::class, CategoryEntity::class, BookCategoryCrossRef::class],
    version = 2,
    exportSchema = true,
)
@TypeConverters(BookStatusConverter::class)
abstract class ShiyueDatabase : RoomDatabase() {
    abstract fun bookDao(): BookDao
    abstract fun categoryDao(): CategoryDao

    companion object {
        const val DATABASE_NAME = "shiyue.db"
    }
}
