package com.shiyue.reader.feature.bookdetail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shiyue.reader.app.navigation.ShiyueRoutes
import com.shiyue.reader.core.model.LibraryBook
import com.shiyue.reader.domain.usecase.DeleteBookUseCase
import com.shiyue.reader.domain.usecase.ObserveLibraryBookUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface BookDetailUiState {
    data object Loading : BookDetailUiState
    data class Content(
        val libraryBook: LibraryBook,
        val isDeleting: Boolean = false,
        val deleteFailed: Boolean = false,
    ) : BookDetailUiState
    data object NotFound : BookDetailUiState
    data object Error : BookDetailUiState
}

sealed interface BookDetailEvent { data object Deleted : BookDetailEvent }

@HiltViewModel
class BookDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val observeBook: ObserveLibraryBookUseCase,
    private val deleteBook: DeleteBookUseCase,
) : ViewModel() {
    private val bookId: String? = savedStateHandle[ShiyueRoutes.BookIdArgument]
    private val _uiState = MutableStateFlow<BookDetailUiState>(BookDetailUiState.Loading)
    val uiState = _uiState.asStateFlow()
    private val _events = Channel<BookDetailEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()
    private var job: Job? = null

    init { retry() }

    fun retry() {
        val id = bookId
        if (id.isNullOrBlank()) { _uiState.value = BookDetailUiState.NotFound; return }
        job?.cancel()
        job = viewModelScope.launch {
            _uiState.value = BookDetailUiState.Loading
            observeBook(id).catch { _uiState.value = BookDetailUiState.Error }.collect { value ->
                _uiState.value = value?.let { BookDetailUiState.Content(it) } ?: BookDetailUiState.NotFound
            }
        }
    }

    fun delete() {
        val content = _uiState.value as? BookDetailUiState.Content ?: return
        if (content.isDeleting) return
        _uiState.value = content.copy(isDeleting = true, deleteFailed = false)
        viewModelScope.launch {
            runCatching { deleteBook(content.libraryBook.book.id) }
                .onSuccess { _events.send(BookDetailEvent.Deleted) }
                .onFailure { _uiState.update { current -> (current as? BookDetailUiState.Content)?.copy(isDeleting = false, deleteFailed = true) ?: current } }
        }
    }
}
