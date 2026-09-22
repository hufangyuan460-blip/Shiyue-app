package com.shiyue.reader.feature.note

import androidx.lifecycle.SavedStateHandle
import com.shiyue.reader.app.navigation.ShiyueRoutes
import com.shiyue.reader.core.model.Book
import com.shiyue.reader.core.model.BookStatus
import com.shiyue.reader.domain.usecase.CreateNoteImageCaptureTargetUseCase
import com.shiyue.reader.domain.usecase.DeleteTemporaryNoteImageUseCase
import com.shiyue.reader.domain.usecase.ObserveBookUseCase
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
class CreateNoteViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()
    private val bookId = "00000000-0000-0000-0000-000000000001"

    @Test
    fun `initializes with book title and default page`() = runTest {
        val book = Book.restore(bookId, "书名", "作者", null, 200, 42, BookStatus.WISH, 1, 1)
        val viewModel = viewModel(FakeBookRepository(listOf(book)))
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect() }
        advanceUntilIdle()

        assertEquals("书名", viewModel.uiState.value.bookTitle)
        assertEquals("42", viewModel.uiState.value.pageText)
    }

    @Test
    fun `save creates a note with content and page`() = runTest {
        val book = Book.create("书名", null, 200, currentPage = 42)
        val noteRepository = FakeNoteRepository()
        val viewModel = viewModel(FakeBookRepository(listOf(book)), noteRepository)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect() }
        advanceUntilIdle()

        viewModel.onContentChanged("  记录一下  ")
        viewModel.onPageChanged("50")
        viewModel.save()
        advanceUntilIdle()

        assertEquals(1, noteRepository.notes.value.size)
        assertEquals("记录一下", noteRepository.notes.value.first().content)
        assertEquals(50, noteRepository.notes.value.first().pageNumber)
    }

    @Test
    fun `blank content does not create a note`() = runTest {
        val book = Book.create("书名", null, 200)
        val noteRepository = FakeNoteRepository()
        val viewModel = viewModel(FakeBookRepository(listOf(book)), noteRepository)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect() }
        advanceUntilIdle()

        viewModel.save()
        advanceUntilIdle()
        assertEquals(0, noteRepository.notes.value.size)
    }

    private fun viewModel(
        bookRepository: FakeBookRepository,
        noteRepository: FakeNoteRepository = FakeNoteRepository(),
        imageStorage: FakeNoteImageStorage = FakeNoteImageStorage(),
    ) = CreateNoteViewModel(
        SavedStateHandle(
            mapOf(
                ShiyueRoutes.BookIdArgument to bookId,
                ShiyueRoutes.SessionIdArgument to "20000000-0000-0000-0000-000000000001",
                ShiyueRoutes.PageArgument to "42",
            ),
        ),
        ObserveBookUseCase(bookRepository),
        SaveNoteUseCase(noteRepository, imageStorage),
        CreateNoteImageCaptureTargetUseCase(imageStorage),
        DeleteTemporaryNoteImageUseCase(imageStorage),
    )
}
