package com.shiyue.reader.feature.reading

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shiyue.reader.app.navigation.ShiyueRoutes
import com.shiyue.reader.core.model.Book
import com.shiyue.reader.core.model.ReadingSession
import com.shiyue.reader.domain.repository.BookProgressUpdate
import com.shiyue.reader.domain.usecase.CompleteReadingUseCase
import com.shiyue.reader.domain.usecase.ObserveBookUseCase
import com.shiyue.reader.domain.usecase.ObserveReadingSessionUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class FinishConfirmation { REWIND, LAST_PAGE }

data class FinishReadingUiState(
    val isLoading: Boolean = true,
    val session: ReadingSession? = null,
    val book: Book? = null,
    val endPage: String = "",
    val updateProgress: Boolean = true,
    val pageError: Boolean = false,
    val confirmation: FinishConfirmation? = null,
    val isSaving: Boolean = false,
    val failed: Boolean = false,
    val notFound: Boolean = false,
)

sealed interface FinishReadingEvent { data class Saved(val bookId: String) : FinishReadingEvent }

@HiltViewModel
class FinishReadingViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    observeSession: ObserveReadingSessionUseCase,
    private val observeBook: ObserveBookUseCase,
    private val completeReading: CompleteReadingUseCase,
) : ViewModel() {
    private val sessionId: String? = savedStateHandle[ShiyueRoutes.SessionIdArgument]
    private val _uiState = MutableStateFlow(FinishReadingUiState())
    val uiState = _uiState.asStateFlow()
    private val _events = Channel<FinishReadingEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()
    private var bookJob: Job? = null

    init {
        val id = sessionId
        if (id.isNullOrBlank()) _uiState.value = FinishReadingUiState(isLoading = false, notFound = true)
        else viewModelScope.launch {
            observeSession(id).collect { session ->
                if (session == null) _uiState.update { it.copy(isLoading = false, notFound = true) }
                else {
                    _uiState.update { it.copy(isLoading = false, session = session) }
                    if (_uiState.value.book?.id != session.bookId) {
                        bookJob?.cancel()
                        bookJob = viewModelScope.launch {
                            observeBook(session.bookId).collect { book ->
                                _uiState.update {
                                    it.copy(
                                        book = book,
                                        endPage = if (it.endPage.isEmpty() && book != null) book.currentPage.toString() else it.endPage,
                                        updateProgress = if (it.endPage.isEmpty()) false else it.updateProgress,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    fun onEndPageChanged(value: String) = _uiState.update {
        val page = value.toIntOrNull()
        val book = it.book
        it.copy(
            endPage = value,
            pageError = false,
            failed = false,
            updateProgress = page != null && book != null && page > book.currentPage,
            confirmation = null,
        )
    }

    fun onUpdateProgressChanged(value: Boolean) = _uiState.update { it.copy(updateProgress = value) }

    fun save() {
        val current = _uiState.value
        if (current.isSaving) return
        val book = current.book ?: return
        val page = current.endPage.toIntOrNull()
        if (page == null || page !in 0..book.totalPages) {
            _uiState.update { it.copy(pageError = true) }
            return
        }
        when {
            current.updateProgress && page < book.currentPage -> _uiState.update { it.copy(confirmation = FinishConfirmation.REWIND) }
            current.updateProgress && page == book.totalPages -> _uiState.update { it.copy(confirmation = FinishConfirmation.LAST_PAGE) }
            else -> persist(if (current.updateProgress) BookProgressUpdate.UPDATE_KEEP_STATUS else BookProgressUpdate.DO_NOT_UPDATE)
        }
    }

    fun confirmRewind() = persist(BookProgressUpdate.UPDATE_KEEP_STATUS)
    fun markFinished() = persist(BookProgressUpdate.UPDATE_AND_MARK_FINISHED)
    fun keepStatusAtLastPage() = persist(BookProgressUpdate.UPDATE_KEEP_STATUS)
    fun completeWithoutProgress() = persist(BookProgressUpdate.DO_NOT_UPDATE)
    fun cancelConfirmation() = _uiState.update { it.copy(confirmation = null) }

    private fun persist(update: BookProgressUpdate) {
        val current = _uiState.value
        if (current.isSaving) return
        val id = sessionId ?: return
        val page = current.endPage.toIntOrNull() ?: return
        val bookId = current.book?.id ?: return
        _uiState.update { it.copy(isSaving = true, failed = false, confirmation = null) }
        viewModelScope.launch {
            runCatching { completeReading(id, page, update) }
                .onSuccess { _events.send(FinishReadingEvent.Saved(bookId)) }
                .onFailure { _uiState.update { it.copy(isSaving = false, failed = true) } }
        }
    }
}
