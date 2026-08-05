package com.shiyue.reader.feature.bookedit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shiyue.reader.core.model.BookStatus
import com.shiyue.reader.domain.usecase.AddBookUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AddBookUiState(
    val title: String = "",
    val author: String = "",
    val totalPages: String = "",
    val status: BookStatus = BookStatus.READING,
    val titleError: Boolean = false,
    val totalPagesError: Boolean = false,
    val saveFailed: Boolean = false,
    val isSaving: Boolean = false,
)

sealed interface AddBookEvent {
    data object Saved : AddBookEvent
}

@HiltViewModel
class AddBookViewModel @Inject constructor(
    private val addBook: AddBookUseCase,
) : ViewModel() {
    private val _uiState = MutableStateFlow(AddBookUiState())
    val uiState = _uiState.asStateFlow()

    private val _events = Channel<AddBookEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    fun onTitleChanged(value: String) {
        _uiState.update { it.copy(title = value, titleError = false, saveFailed = false) }
    }

    fun onAuthorChanged(value: String) {
        _uiState.update { it.copy(author = value, saveFailed = false) }
    }

    fun onTotalPagesChanged(value: String) {
        if (value.all(Char::isDigit)) {
            _uiState.update {
                it.copy(totalPages = value, totalPagesError = false, saveFailed = false)
            }
        }
    }

    fun onStatusChanged(value: BookStatus) {
        _uiState.update { it.copy(status = value, saveFailed = false) }
    }

    fun save() {
        val current = _uiState.value
        if (current.isSaving) return

        val normalizedTitle = current.title.trim()
        val totalPages = current.totalPages.toIntOrNull()
        val titleInvalid = normalizedTitle.isEmpty()
        val pagesInvalid = totalPages == null || totalPages <= 0
        if (titleInvalid || pagesInvalid) {
            _uiState.update {
                it.copy(
                    titleError = titleInvalid,
                    totalPagesError = pagesInvalid,
                    saveFailed = false,
                )
            }
            return
        }
        val validTotalPages = requireNotNull(totalPages)

        _uiState.update { it.copy(isSaving = true, saveFailed = false) }
        viewModelScope.launch {
            runCatching {
                addBook(
                    title = normalizedTitle,
                    author = current.author,
                    totalPages = validTotalPages,
                    status = current.status,
                )
            }.onSuccess {
                _uiState.update { it.copy(isSaving = false) }
                _events.send(AddBookEvent.Saved)
            }.onFailure {
                _uiState.update { it.copy(isSaving = false, saveFailed = true) }
            }
        }
    }
}
