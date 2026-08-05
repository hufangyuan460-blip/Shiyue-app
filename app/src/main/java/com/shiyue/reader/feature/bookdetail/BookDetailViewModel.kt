package com.shiyue.reader.feature.bookdetail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shiyue.reader.app.navigation.ShiyueRoutes
import com.shiyue.reader.core.model.Book
import com.shiyue.reader.domain.usecase.ObserveBookUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

sealed interface BookDetailUiState {
    data object Loading : BookDetailUiState
    data class Content(val book: Book) : BookDetailUiState
    data object NotFound : BookDetailUiState
    data object Error : BookDetailUiState
}

@HiltViewModel
class BookDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val observeBook: ObserveBookUseCase,
) : ViewModel() {
    private val bookId: String? = savedStateHandle[ShiyueRoutes.BookIdArgument]
    private val _uiState = MutableStateFlow<BookDetailUiState>(BookDetailUiState.Loading)
    val uiState = _uiState.asStateFlow()
    private var observeJob: Job? = null

    init {
        retry()
    }

    fun retry() {
        val id = bookId
        if (id.isNullOrBlank()) {
            _uiState.value = BookDetailUiState.NotFound
            return
        }
        observeJob?.cancel()
        observeJob = viewModelScope.launch {
            _uiState.value = BookDetailUiState.Loading
            observeBook(id)
                .catch { _uiState.value = BookDetailUiState.Error }
                .collect { book ->
                    _uiState.value = book?.let(BookDetailUiState::Content)
                        ?: BookDetailUiState.NotFound
                }
        }
    }
}
