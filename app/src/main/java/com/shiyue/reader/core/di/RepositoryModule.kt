package com.shiyue.reader.core.di

import com.shiyue.reader.core.data.repository.OfflineBookRepository
import com.shiyue.reader.core.data.repository.OfflineCategoryRepository
import com.shiyue.reader.core.data.storage.AndroidCoverStorage
import com.shiyue.reader.core.data.storage.AndroidNoteImageStorage
import com.shiyue.reader.domain.repository.BookRepository
import com.shiyue.reader.domain.repository.CategoryRepository
import com.shiyue.reader.domain.repository.BookshelfPreferences
import com.shiyue.reader.domain.repository.CoverStorage
import com.shiyue.reader.domain.repository.NoteImageStorage
import com.shiyue.reader.domain.repository.NoteRepository
import com.shiyue.reader.core.data.preferences.DataStoreBookshelfPreferences
import com.shiyue.reader.core.data.repository.OfflineNoteRepository
import com.shiyue.reader.core.data.repository.OfflineReadingSessionRepository
import com.shiyue.reader.domain.repository.ReadingSessionRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds
    @Singleton
    abstract fun bindBookRepository(
        repository: OfflineBookRepository,
    ): BookRepository

    @Binds
    @Singleton
    abstract fun bindCategoryRepository(repository: OfflineCategoryRepository): CategoryRepository

    @Binds
    @Singleton
    abstract fun bindBookshelfPreferences(preferences: DataStoreBookshelfPreferences): BookshelfPreferences

    @Binds
    @Singleton
    abstract fun bindCoverStorage(storage: AndroidCoverStorage): CoverStorage

    @Binds
    @Singleton
    abstract fun bindReadingSessionRepository(repository: OfflineReadingSessionRepository): ReadingSessionRepository

    @Binds
    @Singleton
    abstract fun bindNoteRepository(repository: OfflineNoteRepository): NoteRepository

    @Binds
    @Singleton
    abstract fun bindNoteImageStorage(storage: AndroidNoteImageStorage): NoteImageStorage
}
