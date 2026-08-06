package com.shiyue.reader.core.di

import com.shiyue.reader.domain.time.SystemTimeSource
import com.shiyue.reader.domain.time.TimeSource
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class TimeModule {
    @Binds @Singleton
    abstract fun bindTimeSource(source: SystemTimeSource): TimeSource
}
