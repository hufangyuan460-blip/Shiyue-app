package com.shiyue.reader.feature.reading

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shiyue.reader.core.model.BookStatus
import com.shiyue.reader.core.model.LibraryBook
import com.shiyue.reader.core.model.ReadingSession
import com.shiyue.reader.domain.usecase.InspectActiveReadingUseCase
import com.shiyue.reader.domain.usecase.ObserveActiveReadingUseCase
import com.shiyue.reader.domain.usecase.ObserveLibraryBooksUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ReadingHomeUiState(
    val isLoading: Boolean = true,
    val activeSession: ReadingSession? = null,
    val readingBooks: List<LibraryBook> = emptyList(),
    val recentBooks: List<LibraryBook> = emptyList(),
    val failed: Boolean = false,
)

sealed interface ReadingHomeEvent {
    data class OpenActive(val sessionId: String, val needsRecovery: Boolean) : ReadingHomeEvent
}

@HiltViewModel
class ReadingHomeViewModel @Inject constructor(
    observeActive: ObserveActiveReadingUseCase,
    observeBooks: ObserveLibraryBooksUseCase,
    private val inspectActive: InspectActiveReadingUseCase,
) : ViewModel() {
    val uiState = combine(observeActive(), observeBooks()) { active, books ->
        ReadingHomeUiState(
            isLoading = false,
            activeSession = active,
            readingBooks = books.filter { it.book.status == BookStatus.READING },
            recentBooks = books.take(12),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ReadingHomeUiState())

    private val _events = Channel<ReadingHomeEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    fun openActive() {
        viewModelScope.launch {
            val inspection = inspectActive()
            val session = inspection.session ?: return@launch
            _events.send(ReadingHomeEvent.OpenActive(session.id, inspection.anomaly != null || inspection.additionalUnfinishedSessionIds.isNotEmpty()))
        }
    }
}
