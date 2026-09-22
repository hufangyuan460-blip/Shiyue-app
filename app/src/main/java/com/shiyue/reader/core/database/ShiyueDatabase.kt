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
        NoteEntity::class,
    ],
    version = 4,
    exportSchema = true,
)
@TypeConverters(BookStatusConverter::class, ReadingSessionStateConverter::class)
abstract class ShiyueDatabase : RoomDatabase() {
    abstract fun bookDao(): BookDao
    abstract fun categoryDao(): CategoryDao
    abstract fun readingSessionDao(): ReadingSessionDao
    abstract fun noteDao(): NoteDao

    companion object {
        const val DATABASE_NAME = "shiyue.db"
    }
}
