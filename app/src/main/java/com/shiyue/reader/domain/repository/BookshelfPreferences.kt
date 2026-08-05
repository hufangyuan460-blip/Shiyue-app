package com.shiyue.reader.domain.repository

import com.shiyue.reader.core.model.BookSortMode
import kotlinx.coroutines.flow.Flow

interface BookshelfPreferences {
    val sortMode: Flow<BookSortMode>
    suspend fun setSortMode(mode: BookSortMode)
}
