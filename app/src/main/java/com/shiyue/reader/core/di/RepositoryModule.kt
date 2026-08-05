package com.shiyue.reader.core.di

import com.shiyue.reader.core.data.repository.OfflineBookRepository
import com.shiyue.reader.domain.repository.BookRepository
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
}
