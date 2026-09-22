package com.shiyue.reader.feature.note

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shiyue.reader.app.navigation.ShiyueRoutes
import com.shiyue.reader.core.model.Note
import com.shiyue.reader.domain.usecase.ObserveBookNotesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class BookNotesUiState(
    val isLoading: Boolean = true,
    val notes: List<Note> = emptyList(),
)

@HiltViewModel
class BookNotesViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    observeBookNotes: ObserveBookNotesUseCase,
) : ViewModel() {
    private val bookId: String? = savedStateHandle[ShiyueRoutes.BookIdArgument]

    val uiState = (bookId?.let { id ->
        observeBookNotes(id).map { notes -> BookNotesUiState(isLoading = false, notes = notes) }
    } ?: flowOf(BookNotesUiState(isLoading = false)))
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), BookNotesUiState())
}
