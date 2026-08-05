package com.shiyue.reader.feature.bookprogress

import androidx.lifecycle.SavedStateHandle
import com.shiyue.reader.app.navigation.ShiyueRoutes
import com.shiyue.reader.core.model.Book
import com.shiyue.reader.core.model.BookStatus
import com.shiyue.reader.domain.usecase.ObserveBookUseCase
import com.shiyue.reader.domain.usecase.UpdateBookProgressUseCase
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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class UpdateProgressViewModelTest {
    @get:Rule val dispatcherRule = MainDispatcherRule()

    @Test
    fun `valid input updates percentage preview and invalid input hides it`() = runTest {
        val original = book()
        val viewModel = viewModel(original, FakeBookRepository(listOf(original)))
        runCurrent()
        viewModel.onPageChanged("50")
        assertEquals(50, viewModel.uiState.value.previewPercent)
        viewModel.onPageChanged("abc")
        assertNull(viewModel.uiState.value.previewPercent)
        viewModel.save()
        assertTrue(viewModel.uiState.value.pageError)
    }

    @Test
    fun `forward progress saves and emits event`() = runTest {
        val original = book()
        val repository = FakeBookRepository(listOf(original))
        val viewModel = viewModel(original, repository)
        runCurrent()
        val event = async { viewModel.events.first() }
        viewModel.onPageChanged("40")
        viewModel.save()
        advanceUntilIdle()
        assertEquals(UpdateProgressEvent.Saved, event.await())
        assertEquals(40, repository.books.value.single().currentPage)
    }

    @Test
    fun `rewind requires confirmation and cancellation does not save`() = runTest {
        val original = book(currentPage = 40)
        val repository = FakeBookRepository(listOf(original))
        val viewModel = viewModel(original, repository)
        runCurrent()
        viewModel.onPageChanged("20")
        viewModel.save()
        assertEquals(ProgressConfirmation.REWIND, viewModel.uiState.value.confirmation)
        viewModel.cancelConfirmation()
        assertEquals(0, repository.updateCalls)
        viewModel.save()
        viewModel.confirmRewind()
        advanceUntilIdle()
        assertEquals(20, repository.books.value.single().currentPage)
    }

    @Test
    fun `last page can mark finished or preserve status`() = runTest {
        val first = book(status = BookStatus.READING)
        val firstRepo = FakeBookRepository(listOf(first))
        val firstVm = viewModel(first, firstRepo)
        runCurrent()
        firstVm.onPageChanged("100")
        firstVm.save()
        assertEquals(ProgressConfirmation.FINISH, firstVm.uiState.value.confirmation)
        firstVm.finishAndMarkRead()
        advanceUntilIdle()
        assertEquals(BookStatus.FINISHED, firstRepo.books.value.single().status)

        val second = book(status = BookStatus.PAUSED)
        val secondRepo = FakeBookRepository(listOf(second))
        val secondVm = viewModel(second, secondRepo)
        runCurrent()
        secondVm.onPageChanged("100")
        secondVm.save()
        secondVm.finishKeepingStatus()
        advanceUntilIdle()
        assertEquals(BookStatus.PAUSED, secondRepo.books.value.single().status)
    }

    @Test
    fun `failed and duplicate saves retain input and update once`() = runTest {
        val original = book()
        val failureRepo = FakeBookRepository(listOf(original), updateFailure = IllegalStateException("disk"))
        val failureVm = viewModel(original, failureRepo)
        runCurrent()
        failureVm.onPageChanged("30")
        failureVm.save()
        advanceUntilIdle()
        assertTrue(failureVm.uiState.value.saveFailed)
        assertEquals("30", failureVm.uiState.value.pageInput)

        val gate = CompletableDeferred<Unit>()
        val gatedRepo = FakeBookRepository(listOf(original), updateGate = gate)
        val gatedVm = viewModel(original, gatedRepo)
        runCurrent()
        gatedVm.onPageChanged("30")
        gatedVm.save()
        gatedVm.save()
        runCurrent()
        assertEquals(1, gatedRepo.updateCalls)
        assertTrue(gatedVm.uiState.value.isSaving)
        gate.complete(Unit)
        advanceUntilIdle()
        assertFalse(gatedVm.uiState.value.isSaving)
    }

    private fun viewModel(book: Book, repository: FakeBookRepository) = UpdateProgressViewModel(
        SavedStateHandle(mapOf(ShiyueRoutes.BookIdArgument to book.id)),
        ObserveBookUseCase(repository),
        UpdateBookProgressUseCase(repository),
    )

    private fun book(
        currentPage: Int = 20,
        status: BookStatus = BookStatus.READING,
    ) = Book.create("书名", null, 100, currentPage, status, timestamp = 1)
}
