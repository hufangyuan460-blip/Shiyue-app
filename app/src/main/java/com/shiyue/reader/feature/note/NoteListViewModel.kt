package com.shiyue.reader.feature.note

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shiyue.reader.core.model.Note
import com.shiyue.reader.domain.usecase.ObserveLibraryBooksUseCase
import com.shiyue.reader.domain.usecase.ObserveNotesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class NoteListItem(
    val note: Note,
    val bookTitle: String,
)

data class NoteListUiState(
    val isLoading: Boolean = true,
    val failed: Boolean = false,
    val notes: List<NoteListItem> = emptyList(),
    val bookFilters: List<Pair<String, String>> = emptyList(),
    val selectedBookId: String? = null,
)

@HiltViewModel
class NoteListViewModel @Inject constructor(
    observeNotes: ObserveNotesUseCase,
    observeBooks: ObserveLibraryBooksUseCase,
) : ViewModel() {
    private val selectedBookId = MutableStateFlow<String?>(null)

    val uiState = combine(observeNotes(), observeBooks(), selectedBookId) { notes, books, selected ->
        val titleById = books.associate { it.book.id to it.book.title }
        val bookFilters = notes.mapNotNull { note -> titleById[note.bookId]?.let { note.bookId to it } }
            .distinctBy { it.first }
            .sortedBy { it.second }
        val validSelected = selected?.takeIf { id -> bookFilters.any { it.first == id } }
        val visible = if (validSelected == null) notes else notes.filter { it.bookId == validSelected }
        NoteListUiState(
            isLoading = false,
            notes = visible.map { NoteListItem(it, titleById[it.bookId].orEmpty()) },
            bookFilters = bookFilters,
            selectedBookId = validSelected,
        )
    }.catch {
        emit(NoteListUiState(isLoading = false, failed = true))
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), NoteListUiState())

    fun onBookFilterSelected(bookId: String?) {
        selectedBookId.value = bookId
    }
}
