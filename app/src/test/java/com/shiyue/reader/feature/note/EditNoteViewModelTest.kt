package com.shiyue.reader.feature.note

import androidx.lifecycle.SavedStateHandle
import com.shiyue.reader.app.navigation.ShiyueRoutes
import com.shiyue.reader.core.model.Book
import com.shiyue.reader.core.model.Note
import com.shiyue.reader.domain.usecase.CreateNoteImageCaptureTargetUseCase
import com.shiyue.reader.domain.usecase.DeleteNoteUseCase
import com.shiyue.reader.domain.usecase.DeleteTemporaryNoteImageUseCase
import com.shiyue.reader.domain.usecase.ObserveBookUseCase
import com.shiyue.reader.domain.usecase.ObserveNoteUseCase
import com.shiyue.reader.domain.usecase.SaveNoteUseCase
import com.shiyue.reader.testutil.FakeBookRepository
import com.shiyue.reader.testutil.FakeNoteImageStorage
import com.shiyue.reader.testutil.FakeNoteRepository
import com.shiyue.reader.testutil.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class EditNoteViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `loads existing note and saves edits`() = runTest {
        val book = Book.create("书名", null, 200)
        val note = Note.create(book.id, "20000000-0000-0000-0000-000000000001", 5, "原内容", timestamp = 100)
        val noteRepository = FakeNoteRepository(listOf(note))
        val viewModel = viewModel(book, note, noteRepository)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect() }
        advanceUntilIdle()

        assertEquals("原内容", viewModel.uiState.value.content)
        assertEquals("5", viewModel.uiState.value.pageText)
        viewModel.onContentChanged("新内容")
        viewModel.onPageChanged("")
        viewModel.save()
        advanceUntilIdle()

        assertEquals("新内容", noteRepository.notes.value.first().content)
        assertEquals(null, noteRepository.notes.value.first().pageNumber)
    }

    @Test
    fun `delete removes the note`() = runTest {
        val book = Book.create("书名", null, 200)
        val note = Note.create(book.id, null, 1, "内容", timestamp = 100)
        val noteRepository = FakeNoteRepository(listOf(note))
        val viewModel = viewModel(book, note, noteRepository)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect() }
        advanceUntilIdle()

        viewModel.delete()
        advanceUntilIdle()
        assertEquals(0, noteRepository.notes.value.size)
    }

    @Test
    fun `shows not found for missing note`() = runTest {
        val book = Book.create("书名", null, 200)
        val viewModel = viewModel(book, null, FakeNoteRepository())
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect() }
        advanceUntilIdle()

        assertEquals(true, viewModel.uiState.value.notFound)
    }

    private fun viewModel(
        book: Book,
        note: Note?,
        noteRepository: FakeNoteRepository,
    ): EditNoteViewModel {
        val imageStorage = FakeNoteImageStorage()
        return EditNoteViewModel(
            SavedStateHandle(mapOf(ShiyueRoutes.NoteIdArgument to (note?.id ?: "30000000-0000-0000-0000-000000000001"))),
            ObserveNoteUseCase(noteRepository),
            ObserveBookUseCase(FakeBookRepository(listOf(book))),
            SaveNoteUseCase(noteRepository, imageStorage),
            DeleteNoteUseCase(noteRepository, imageStorage),
            CreateNoteImageCaptureTargetUseCase(imageStorage),
            DeleteTemporaryNoteImageUseCase(imageStorage),
        )
    }
}
