package com.shiyue.reader.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [
        BookEntity::class,
        CategoryEntity::class,
        BookCategoryCrossRef::class,
        ReadingSessionEntity::class,
        ActiveReadingSessionEntity::class,
    ],
    version = 3,
    exportSchema = true,
)
@TypeConverters(BookStatusConverter::class, ReadingSessionStateConverter::class)
abstract class ShiyueDatabase : RoomDatabase() {
    abstract fun bookDao(): BookDao
    abstract fun categoryDao(): CategoryDao
    abstract fun readingSessionDao(): ReadingSessionDao

    companion object {
        const val DATABASE_NAME = "shiyue.db"
    }
}
