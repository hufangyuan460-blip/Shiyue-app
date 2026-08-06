package com.shiyue.reader.feature.reading

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shiyue.reader.core.model.Book
import com.shiyue.reader.core.model.ReadingClockAnomaly
import com.shiyue.reader.core.model.ReadingSession
import com.shiyue.reader.core.model.ReadingSessionState
import com.shiyue.reader.domain.time.TimeSource
import com.shiyue.reader.domain.time.read
import com.shiyue.reader.domain.usecase.DiscardReadingUseCase
import com.shiyue.reader.domain.usecase.ObserveActiveReadingUseCase
import com.shiyue.reader.domain.usecase.ObserveBookUseCase
import com.shiyue.reader.domain.usecase.PauseReadingUseCase
import com.shiyue.reader.domain.usecase.ResumeReadingUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ActiveReadingUiState(
    val isLoading: Boolean = true,
    val session: ReadingSession? = null,
    val book: Book? = null,
    val displayedDurationMs: Long = 0,
    val anomaly: ReadingClockAnomaly? = null,
    val isWorking: Boolean = false,
    val failed: Boolean = false,
)

sealed interface ActiveReadingEvent {
    data class Finish(val sessionId: String) : ActiveReadingEvent
    data class Recover(val sessionId: String) : ActiveReadingEvent
    data object Discarded : ActiveReadingEvent
}

@HiltViewModel
class ActiveReadingViewModel @Inject constructor(
    observeActive: ObserveActiveReadingUseCase,
    private val observeBook: ObserveBookUseCase,
    private val pauseReading: PauseReadingUseCase,
    private val resumeReading: ResumeReadingUseCase,
    private val discardReading: DiscardReadingUseCase,
    private val timeSource: TimeSource,
) : ViewModel() {
    private val _uiState = MutableStateFlow(ActiveReadingUiState())
    val uiState = _uiState.asStateFlow()
    private val _events = Channel<ActiveReadingEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()
    private var bookJob: Job? = null

    init {
        viewModelScope.launch {
            observeActive().collect { session ->
                if (session == null) {
                    _uiState.value = ActiveReadingUiState(isLoading = false)
                    bookJob?.cancel()
                } else {
                    _uiState.update { it.copy(isLoading = false, session = session) }
                    refreshTicker()
                    if (_uiState.value.book?.id != session.bookId) {
                        bookJob?.cancel()
                        bookJob = viewModelScope.launch {
                            observeBook(session.bookId).collect { book -> _uiState.update { it.copy(book = book) } }
                        }
                    }
                }
            }
        }
    }

    fun refreshTicker() {
        val session = _uiState.value.session ?: return
        val duration = session.durationAt(timeSource.read())
        _uiState.update { it.copy(displayedDurationMs = duration.displayedDurationMs, anomaly = duration.anomaly) }
    }

    fun togglePause() {
        val current = _uiState.value
        if (current.isWorking) return
        val session = current.session ?: return
        if (current.anomaly != null) {
            viewModelScope.launch { _events.send(ActiveReadingEvent.Recover(session.id)) }
            return
        }
        _uiState.update { it.copy(isWorking = true, failed = false) }
        viewModelScope.launch {
            runCatching {
                if (session.state == ReadingSessionState.ACTIVE) pauseReading(session.id) else resumeReading(session.id)
            }.onSuccess { _uiState.update { it.copy(isWorking = false) } }
                .onFailure { _uiState.update { it.copy(isWorking = false, failed = true) } }
        }
    }

    fun finish() {
        val current = _uiState.value
        if (current.isWorking) return
        val session = current.session ?: return
        if (current.anomaly != null) {
            viewModelScope.launch { _events.send(ActiveReadingEvent.Recover(session.id)) }
            return
        }
        _uiState.update { it.copy(isWorking = true, failed = false) }
        viewModelScope.launch {
            runCatching { if (session.state == ReadingSessionState.ACTIVE) pauseReading(session.id) else session }
                .onSuccess { _events.send(ActiveReadingEvent.Finish(session.id)) }
                .onFailure { _uiState.update { it.copy(isWorking = false, failed = true) } }
        }
    }

    fun discard() {
        val current = _uiState.value
        if (current.isWorking) return
        val id = current.session?.id ?: return
        _uiState.update { it.copy(isWorking = true, failed = false) }
        viewModelScope.launch {
            runCatching { discardReading(id) }
                .onSuccess { _events.send(ActiveReadingEvent.Discarded) }
                .onFailure { _uiState.update { it.copy(isWorking = false, failed = true) } }
        }
    }
}
