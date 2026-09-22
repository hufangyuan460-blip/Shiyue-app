package com.shiyue.reader.feature.note

import com.shiyue.reader.core.model.Book
import com.shiyue.reader.core.model.Note
import com.shiyue.reader.domain.usecase.ObserveLibraryBooksUseCase
import com.shiyue.reader.domain.usecase.ObserveNotesUseCase
import com.shiyue.reader.testutil.FakeBookRepository
import com.shiyue.reader.testutil.FakeNoteRepository
import com.shiyue.reader.testutil.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class NoteListViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `lists notes with book titles and filters by book`() = runTest {
        val bookA = Book.create("甲书", null, 100)
        val bookB = Book.create("乙书", null, 100)
        val noteA = Note.create(bookA.id, null, 1, "A", timestamp = 100)
        val noteB = Note.create(bookB.id, null, 1, "B", timestamp = 200)
        val viewModel = NoteListViewModel(
            ObserveNotesUseCase(FakeNoteRepository(listOf(noteA, noteB))),
            ObserveLibraryBooksUseCase(FakeBookRepository(listOf(bookA, bookB))),
        )
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect() }
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isLoading)
        assertEquals(2, viewModel.uiState.value.notes.size)
        assertEquals("甲书", viewModel.uiState.value.notes.first { it.note.id == noteA.id }.bookTitle)
        assertEquals(listOf("乙书", "甲书"), viewModel.uiState.value.bookFilters.map { it.second })

        viewModel.onBookFilterSelected(bookA.id)
        advanceUntilIdle()
        assertEquals(1, viewModel.uiState.value.notes.size)
        assertEquals("A", viewModel.uiState.value.notes.first().note.content)

        viewModel.onBookFilterSelected(null)
        advanceUntilIdle()
        assertEquals(2, viewModel.uiState.value.notes.size)
    }
}
