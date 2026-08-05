package com.shiyue.reader.core.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.shiyue.reader.core.model.BookSortMode
import com.shiyue.reader.domain.repository.BookshelfPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.bookshelfDataStore by preferencesDataStore("bookshelf_preferences")

@Singleton
class DataStoreBookshelfPreferences @Inject constructor(
    @param:ApplicationContext private val context: Context,
) : BookshelfPreferences {
    override val sortMode: Flow<BookSortMode> = context.bookshelfDataStore.data.map { preferences ->
        preferences[SORT_MODE]?.let { runCatching { BookSortMode.valueOf(it) }.getOrNull() }
            ?: BookSortMode.UPDATED_DESC
    }

    override suspend fun setSortMode(mode: BookSortMode) {
        context.bookshelfDataStore.edit { it[SORT_MODE] = mode.name }
    }

    private companion object {
        val SORT_MODE = stringPreferencesKey("book_sort_mode")
    }
}
