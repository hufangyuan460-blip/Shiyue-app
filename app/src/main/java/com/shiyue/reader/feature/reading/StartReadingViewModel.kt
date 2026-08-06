package com.shiyue.reader.feature.reading

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shiyue.reader.app.navigation.ShiyueRoutes
import com.shiyue.reader.core.model.Book
import com.shiyue.reader.core.model.BookStatus
import com.shiyue.reader.domain.usecase.ObserveActiveReadingUseCase
import com.shiyue.reader.domain.usecase.ObserveBookUseCase
import com.shiyue.reader.domain.usecase.StartReadingUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class StartReadingUiState(
    val isLoading: Boolean = true,
    val book: Book? = null,
    val existingSessionBookId: String? = null,
    val startPage: String = "",
    val pageError: Boolean = false,
    val showStatusConfirmation: Boolean = false,
    val isStarting: Boolean = false,
    val failed: Boolean = false,
    val notFound: Boolean = false,
)

sealed interface StartReadingEvent {
    data class OpenSession(val sessionId: String, val belongsToRequestedBook: Boolean) : StartReadingEvent
}

@HiltViewModel
class StartReadingViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    observeBook: ObserveBookUseCase,
    observeActive: ObserveActiveReadingUseCase,
    private val startReading: StartReadingUseCase,
) : ViewModel() {
    private val bookId: String? = savedStateHandle[ShiyueRoutes.BookIdArgument]
    private val _uiState = MutableStateFlow(StartReadingUiState())
    val uiState = _uiState.asStateFlow()
    private val _events = Channel<StartReadingEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()
    private var job: Job? = null

    init {
        val id = bookId
        if (id.isNullOrBlank()) _uiState.value = StartReadingUiState(isLoading = false, notFound = true)
        else {
            job = viewModelScope.launch {
                combine(observeBook(id), observeActive()) { book, active -> book to active }.collect { (book, active) ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            book = book,
                            existingSessionBookId = active?.bookId,
                            startPage = if (it.startPage.isEmpty() && book != null) book.currentPage.toString() else it.startPage,
                            notFound = book == null,
                        )
                    }
                }
            }
        }
    }

    fun onPageChanged(value: String) = _uiState.update { it.copy(startPage = value, pageError = false, failed = false) }

    fun begin() {
        val current = _uiState.value
        if (current.isStarting) return
        val book = current.book ?: return
        val page = current.startPage.toIntOrNull()
        if (page == null || page !in 0..book.totalPages) {
            _uiState.update { it.copy(pageError = true) }
            return
        }
        if (current.existingSessionBookId != null) {
            start(false)
        } else if (book.status == BookStatus.WISH || book.status == BookStatus.PAUSED) {
            _uiState.update { it.copy(showStatusConfirmation = true) }
        } else start(false)
    }

    fun confirmSwitchStatus() { _uiState.update { it.copy(showStatusConfirmation = false) }; start(true) }
    fun startWithoutStatusChange() { _uiState.update { it.copy(showStatusConfirmation = false) }; start(false) }
    fun dismissStatusConfirmation() = _uiState.update { it.copy(showStatusConfirmation = false) }

    private fun start(switchStatus: Boolean) {
        val current = _uiState.value
        if (current.isStarting) return
        val book = current.book ?: return
        val page = current.startPage.toIntOrNull() ?: return
        _uiState.update { it.copy(isStarting = true, failed = false) }
        viewModelScope.launch {
            runCatching { startReading(book.id, page, switchStatus) }
                .onSuccess { result ->
                    _uiState.update { it.copy(isStarting = false) }
                    _events.send(StartReadingEvent.OpenSession(result.session.id, result.session.bookId == book.id))
                }
                .onFailure { _uiState.update { it.copy(isStarting = false, failed = true) } }
        }
    }
}
