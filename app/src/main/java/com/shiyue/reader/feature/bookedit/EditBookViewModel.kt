package com.shiyue.reader.feature.bookedit

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shiyue.reader.app.navigation.ShiyueRoutes
import com.shiyue.reader.core.model.BookStatus
import com.shiyue.reader.domain.usecase.ObserveBookUseCase
import com.shiyue.reader.domain.usecase.UpdateBookMetadataUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class EditBookUiState(
    val isLoading: Boolean = true,
    val notFound: Boolean = false,
    val loadFailed: Boolean = false,
    val title: String = "",
    val author: String = "",
    val totalPages: String = "",
    val status: BookStatus = BookStatus.READING,
    val currentPage: Int = 0,
    val titleError: Boolean = false,
    val totalPagesError: EditTotalPagesError? = null,
    val saveFailed: Boolean = false,
    val isSaving: Boolean = false,
    val isDirty: Boolean = false,
    val initialized: Boolean = false,
)

enum class EditTotalPagesError { INVALID, BELOW_CURRENT_PAGE }

sealed interface EditBookEvent {
    data object Saved : EditBookEvent
}

@HiltViewModel
class EditBookViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    observeBook: ObserveBookUseCase,
    private val updateBookMetadata: UpdateBookMetadataUseCase,
) : ViewModel() {
    private val bookId: String? = savedStateHandle[ShiyueRoutes.BookIdArgument]
    private val observeBook = observeBook
    private val _uiState = MutableStateFlow(EditBookUiState())
    val uiState = _uiState.asStateFlow()
    private val _events = Channel<EditBookEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()
    private var loadJob: Job? = null

    init {
        retry()
    }

    fun retry() {
        val id = bookId
        if (id.isNullOrBlank()) {
            _uiState.value = EditBookUiState(isLoading = false, notFound = true)
        } else {
            loadJob?.cancel()
            loadJob = viewModelScope.launch {
                _uiState.update { it.copy(isLoading = true, loadFailed = false) }
                observeBook(id)
                    .catch { _uiState.update { it.copy(isLoading = false, loadFailed = true) } }
                    .collect { book ->
                        if (book == null) {
                            _uiState.update { it.copy(isLoading = false, notFound = true) }
                        } else if (!_uiState.value.isDirty) {
                            _uiState.update {
                                it.copy(
                                    isLoading = false,
                                    notFound = false,
                                    loadFailed = false,
                                    title = book.title,
                                    author = book.author.orEmpty(),
                                    totalPages = book.totalPages.toString(),
                                    status = book.status,
                                    currentPage = book.currentPage,
                                    initialized = true,
                                )
                            }
                        }
                    }
            }
        }
    }

    fun onTitleChanged(value: String) = updateForm {
        it.copy(title = value, titleError = false)
    }

    fun onAuthorChanged(value: String) = updateForm { it.copy(author = value) }

    fun onTotalPagesChanged(value: String) = updateForm {
        it.copy(totalPages = value, totalPagesError = null)
    }

    fun onStatusChanged(value: BookStatus) = updateForm { it.copy(status = value) }

    private fun updateForm(transform: (EditBookUiState) -> EditBookUiState) {
        if (_uiState.value.isSaving || !_uiState.value.initialized) return
        _uiState.update { transform(it).copy(isDirty = true, saveFailed = false) }
    }

    fun save() {
        val current = _uiState.value
        if (current.isSaving || !current.initialized) return
        val totalPages = current.totalPages.toIntOrNull()
        val titleInvalid = current.title.trim().isEmpty()
        val pagesError = when {
            totalPages == null || totalPages <= 0 -> EditTotalPagesError.INVALID
            totalPages < current.currentPage -> EditTotalPagesError.BELOW_CURRENT_PAGE
            else -> null
        }
        if (titleInvalid || pagesError != null) {
            _uiState.update {
                it.copy(titleError = titleInvalid, totalPagesError = pagesError, saveFailed = false)
            }
            return
        }
        val id = requireNotNull(bookId)
        _uiState.update { it.copy(isSaving = true, saveFailed = false) }
        viewModelScope.launch {
            runCatching {
                updateBookMetadata(
                    bookId = id,
                    title = current.title,
                    author = current.author,
                    totalPages = requireNotNull(totalPages),
                    status = current.status,
                )
            }.onSuccess {
                _uiState.update { it.copy(isSaving = false, isDirty = false) }
                _events.send(EditBookEvent.Saved)
            }.onFailure {
                _uiState.update { it.copy(isSaving = false, saveFailed = true) }
            }
        }
    }
}
