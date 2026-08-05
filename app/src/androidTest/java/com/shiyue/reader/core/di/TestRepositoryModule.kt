package com.shiyue.reader.core.di

import com.shiyue.reader.core.data.TestBookRepository
import com.shiyue.reader.domain.repository.BookRepository
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
}
