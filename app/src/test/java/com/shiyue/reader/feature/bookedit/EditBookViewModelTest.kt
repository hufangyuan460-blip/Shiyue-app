package com.shiyue.reader.feature.bookedit

import androidx.lifecycle.SavedStateHandle
import com.shiyue.reader.app.navigation.ShiyueRoutes
import com.shiyue.reader.core.model.Book
import com.shiyue.reader.core.model.BookStatus
import com.shiyue.reader.domain.usecase.ObserveBookUseCase
import com.shiyue.reader.domain.usecase.UpdateBookMetadataUseCase
import com.shiyue.reader.testutil.FakeBookRepository
import com.shiyue.reader.testutil.MainDispatcherRule
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class EditBookViewModelTest {
    @get:Rule val dispatcherRule = MainDispatcherRule()

    @Test
    fun `loads original data and user edits are not overwritten by flow`() = runTest {
        val original = book()
        val repository = FakeBookRepository(listOf(original))
        val viewModel = viewModel(original, repository)
        runCurrent()
        assertEquals("原书名", viewModel.uiState.value.title)

        viewModel.onTitleChanged("正在输入")
        repository.updateBook(original.updated(title = "外部更新", updatedAt = 3))
        runCurrent()
        assertEquals("正在输入", viewModel.uiState.value.title)
        assertTrue(viewModel.uiState.value.isDirty)
    }

    @Test
    fun `valid save normalizes author preserves page and emits event`() = runTest {
        val original = book(currentPage = 30)
        val repository = FakeBookRepository(listOf(original))
        val viewModel = viewModel(original, repository)
        runCurrent()
        val event = async { viewModel.events.first() }
        viewModel.onTitleChanged("  新书名 ")
        viewModel.onAuthorChanged("   ")
        viewModel.onTotalPagesChanged("150")
        viewModel.onStatusChanged(BookStatus.FINISHED)
        viewModel.save()
        advanceUntilIdle()

        assertEquals(EditBookEvent.Saved, event.await())
        val saved = repository.books.value.single()
        assertEquals("新书名", saved.title)
        assertEquals(null, saved.author)
        assertEquals(30, saved.currentPage)
        assertEquals(BookStatus.FINISHED, saved.status)
    }

    @Test
    fun `validation blocks blank title invalid pages and pages below current`() = runTest {
        val original = book(currentPage = 30)
        val repository = FakeBookRepository(listOf(original))
        val viewModel = viewModel(original, repository)
        runCurrent()
        viewModel.onTitleChanged(" ")
        viewModel.onTotalPagesChanged("29")
        viewModel.save()
        assertTrue(viewModel.uiState.value.titleError)
        assertEquals(EditTotalPagesError.BELOW_CURRENT_PAGE, viewModel.uiState.value.totalPagesError)
        assertEquals(0, repository.updateCalls)
    }

    @Test
    fun `failure keeps input and allows retry`() = runTest {
        val original = book()
        val repository = FakeBookRepository(listOf(original), updateFailure = IllegalStateException("disk"))
        val viewModel = viewModel(original, repository)
        runCurrent()
        viewModel.onTitleChanged("保留的输入")
        viewModel.save()
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.saveFailed)
        assertEquals("保留的输入", viewModel.uiState.value.title)
        assertFalse(viewModel.uiState.value.isSaving)
    }

    @Test
    fun `duplicate save is ignored while update is running`() = runTest {
        val gate = CompletableDeferred<Unit>()
        val original = book()
        val repository = FakeBookRepository(listOf(original), updateGate = gate)
        val viewModel = viewModel(original, repository)
        runCurrent()
        viewModel.onTitleChanged("新书名")
        viewModel.save()
        viewModel.save()
        runCurrent()
        assertEquals(1, repository.updateCalls)
        gate.complete(Unit)
        advanceUntilIdle()
        assertEquals(1, repository.updateCalls)
    }

    private fun viewModel(book: Book, repository: FakeBookRepository) = EditBookViewModel(
        SavedStateHandle(mapOf(ShiyueRoutes.BookIdArgument to book.id)),
        ObserveBookUseCase(repository),
        UpdateBookMetadataUseCase(repository),
    )

    private fun book(currentPage: Int = 10) = Book.create(
        "原书名", "原作者", 100, currentPage, BookStatus.READING, timestamp = 1,
    )
}
