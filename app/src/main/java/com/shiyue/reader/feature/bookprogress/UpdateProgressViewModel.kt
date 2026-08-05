package com.shiyue.reader.feature.bookprogress

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shiyue.reader.app.navigation.ShiyueRoutes
import com.shiyue.reader.core.model.Book
import com.shiyue.reader.domain.usecase.ObserveBookUseCase
import com.shiyue.reader.domain.usecase.UpdateBookProgressUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlin.math.roundToInt
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class ProgressConfirmation { REWIND, FINISH }

data class UpdateProgressUiState(
    val isLoading: Boolean = true,
    val notFound: Boolean = false,
    val loadFailed: Boolean = false,
    val book: Book? = null,
    val pageInput: String = "",
    val pageError: Boolean = false,
    val isSaving: Boolean = false,
    val saveFailed: Boolean = false,
    val confirmation: ProgressConfirmation? = null,
) {
    val parsedPage: Int? = pageInput.toIntOrNull()
    val previewPercent: Int? = parsedPage
        ?.takeIf { page -> book != null && page in 0..book.totalPages }
        ?.let { page -> ((page.toDouble() / requireNotNull(book).totalPages) * 100).roundToInt() }
}

sealed interface UpdateProgressEvent {
    data object Saved : UpdateProgressEvent
}

@HiltViewModel
class UpdateProgressViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    observeBook: ObserveBookUseCase,
    private val updateBookProgress: UpdateBookProgressUseCase,
) : ViewModel() {
    private val bookId: String? = savedStateHandle[ShiyueRoutes.BookIdArgument]
    private val observeBook = observeBook
    private val _uiState = MutableStateFlow(UpdateProgressUiState())
    val uiState = _uiState.asStateFlow()
    private val _events = Channel<UpdateProgressEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()
    private var initialized = false
    private var loadJob: Job? = null

    init {
        retry()
    }

    fun retry() {
        val id = bookId
        if (id.isNullOrBlank()) {
            _uiState.value = UpdateProgressUiState(isLoading = false, notFound = true)
        } else {
            loadJob?.cancel()
            loadJob = viewModelScope.launch {
                _uiState.update { it.copy(isLoading = true, loadFailed = false) }
                observeBook(id)
                    .catch { _uiState.update { it.copy(isLoading = false, loadFailed = true) } }
                    .collect { book ->
                        if (book == null) {
                            _uiState.update { it.copy(isLoading = false, notFound = true, book = null) }
                        } else {
                            _uiState.update {
                                it.copy(
                                    isLoading = false,
                                    notFound = false,
                                    loadFailed = false,
                                    book = book,
                                    pageInput = if (initialized) it.pageInput else book.currentPage.toString(),
                                )
                            }
                            initialized = true
                        }
                    }
            }
        }
    }

    fun onPageChanged(value: String) {
        if (_uiState.value.isSaving) return
        _uiState.update {
            it.copy(pageInput = value, pageError = false, saveFailed = false, confirmation = null)
        }
    }

    fun save() {
        val current = _uiState.value
        if (current.isSaving) return
        val book = current.book ?: return
        val page = current.parsedPage
        if (page == null || page !in 0..book.totalPages) {
            _uiState.update { it.copy(pageError = true, saveFailed = false) }
            return
        }
        when {
            page == book.currentPage -> {
                _uiState.update { it.copy(isSaving = true) }
                viewModelScope.launch { _events.send(UpdateProgressEvent.Saved) }
            }
            page < book.currentPage -> _uiState.update { it.copy(confirmation = ProgressConfirmation.REWIND) }
            page == book.totalPages -> _uiState.update { it.copy(confirmation = ProgressConfirmation.FINISH) }
            else -> persist(page = page, markFinished = false)
        }
    }

    fun confirmRewind() {
        val page = _uiState.value.parsedPage ?: return
        _uiState.update { it.copy(confirmation = null) }
        persist(page = page, markFinished = false)
    }

    fun finishAndMarkRead() {
        val page = _uiState.value.parsedPage ?: return
        _uiState.update { it.copy(confirmation = null) }
        persist(page = page, markFinished = true)
    }

    fun finishKeepingStatus() {
        val page = _uiState.value.parsedPage ?: return
        _uiState.update { it.copy(confirmation = null) }
        persist(page = page, markFinished = false)
    }

    fun cancelConfirmation() {
        _uiState.update { it.copy(confirmation = null) }
    }

    private fun persist(page: Int, markFinished: Boolean) {
        val current = _uiState.value
        if (current.isSaving) return
        val id = bookId ?: return
        _uiState.update { it.copy(isSaving = true, saveFailed = false) }
        viewModelScope.launch {
            runCatching {
                updateBookProgress(id, page, markFinished)
            }.onSuccess {
                _uiState.update { it.copy(isSaving = false) }
                _events.send(UpdateProgressEvent.Saved)
            }.onFailure {
                _uiState.update { it.copy(isSaving = false, saveFailed = true) }
            }
        }
    }
}
