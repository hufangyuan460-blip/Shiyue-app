package com.shiyue.reader.core.di

import android.content.Context
import androidx.room.Room
import com.shiyue.reader.core.database.BookDao
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
    ).build()

    @Provides
    fun provideBookDao(database: ShiyueDatabase): BookDao = database.bookDao()
}
