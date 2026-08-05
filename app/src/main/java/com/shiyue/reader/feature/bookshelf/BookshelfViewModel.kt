package com.shiyue.reader.feature.bookshelf

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shiyue.reader.core.model.Book
import com.shiyue.reader.domain.usecase.ObserveBooksUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class BookshelfUiState(
    val isLoading: Boolean = true,
    val books: List<Book> = emptyList(),
    val loadFailed: Boolean = false,
)

@HiltViewModel
class BookshelfViewModel @Inject constructor(
    observeBooks: ObserveBooksUseCase,
) : ViewModel() {
    val uiState = observeBooks()
        .map { books ->
            BookshelfUiState(
                isLoading = false,
                books = books.sortedByDescending(Book::updatedAt),
            )
        }
        .catch {
            emit(BookshelfUiState(isLoading = false, loadFailed = true))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = BookshelfUiState(),
        )
}
