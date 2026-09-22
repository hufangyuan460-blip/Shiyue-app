package com.shiyue.reader.core.di

import com.shiyue.reader.core.data.TestBookRepository
import com.shiyue.reader.core.data.TestBookshelfPreferences
import com.shiyue.reader.core.data.TestCategoryRepository
import com.shiyue.reader.core.data.TestCoverStorage
import com.shiyue.reader.core.data.TestNoteImageStorage
import com.shiyue.reader.core.data.TestNoteRepository
import com.shiyue.reader.core.data.TestReadingSessionRepository
import com.shiyue.reader.domain.repository.BookRepository
import com.shiyue.reader.domain.repository.BookshelfPreferences
import com.shiyue.reader.domain.repository.CategoryRepository
import com.shiyue.reader.domain.repository.CoverStorage
import com.shiyue.reader.domain.repository.NoteImageStorage
import com.shiyue.reader.domain.repository.NoteRepository
import com.shiyue.reader.domain.repository.ReadingSessionRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import javax.inject.Singleton

@Module
@TestInstallIn(
    components = [SingletonComponent::class],
    replaces = [RepositoryModule::class],
)
abstract class TestRepositoryModule {
    @Binds
    @Singleton
    abstract fun bindBookRepository(repository: TestBookRepository): BookRepository

    @Binds
    @Singleton
    abstract fun bindCategoryRepository(repository: TestCategoryRepository): CategoryRepository

    @Binds
    @Singleton
    abstract fun bindBookshelfPreferences(preferences: TestBookshelfPreferences): BookshelfPreferences

    @Binds
    @Singleton
    abstract fun bindCoverStorage(storage: TestCoverStorage): CoverStorage

    @Binds
    @Singleton
    abstract fun bindReadingSessionRepository(repository: TestReadingSessionRepository): ReadingSessionRepository

    @Binds
    @Singleton
    abstract fun bindNoteRepository(repository: TestNoteRepository): NoteRepository

    @Binds
    @Singleton
    abstract fun bindNoteImageStorage(storage: TestNoteImageStorage): NoteImageStorage
}
