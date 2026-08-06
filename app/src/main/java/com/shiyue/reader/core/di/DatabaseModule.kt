package com.shiyue.reader.core.di

import android.content.Context
import androidx.room.Room
import com.shiyue.reader.core.database.BookDao
import com.shiyue.reader.core.database.CategoryDao
import com.shiyue.reader.core.database.MIGRATION_1_2
import com.shiyue.reader.core.database.MIGRATION_2_3
import com.shiyue.reader.core.database.ReadingSessionDao
import com.shiyue.reader.core.database.ShiyueDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context,
    ): ShiyueDatabase = Room.databaseBuilder(
        context,
        ShiyueDatabase::class.java,
        ShiyueDatabase.DATABASE_NAME,
    ).addMigrations(MIGRATION_1_2, MIGRATION_2_3).build()

    @Provides
    fun provideBookDao(database: ShiyueDatabase): BookDao = database.bookDao()

    @Provides
    fun provideCategoryDao(database: ShiyueDatabase): CategoryDao = database.categoryDao()

    @Provides
    fun provideReadingSessionDao(database: ShiyueDatabase): ReadingSessionDao = database.readingSessionDao()
}
