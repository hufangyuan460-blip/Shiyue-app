package com.shiyue.reader.feature.reading

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shiyue.reader.app.navigation.ShiyueRoutes
import com.shiyue.reader.core.model.ReadingHistorySummary
import com.shiyue.reader.core.model.ReadingSession
import com.shiyue.reader.domain.usecase.ObserveBookReadingHistoryUseCase
import com.shiyue.reader.domain.usecase.ObserveBookReadingSummaryUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn

data class ReadingHistoryUiState(
    val summary: ReadingHistorySummary = ReadingHistorySummary(),
    val sessions: List<ReadingSession> = emptyList(),
)

@HiltViewModel
class ReadingHistoryViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    observeHistory: ObserveBookReadingHistoryUseCase,
    observeSummary: ObserveBookReadingSummaryUseCase,
) : ViewModel() {
    private val bookId: String? = savedStateHandle[ShiyueRoutes.BookIdArgument]
    val uiState = (bookId?.let { id ->
        combine(observeSummary(id), observeHistory(id)) { summary, sessions -> ReadingHistoryUiState(summary, sessions) }
    } ?: flowOf(ReadingHistoryUiState()))
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ReadingHistoryUiState())
}
