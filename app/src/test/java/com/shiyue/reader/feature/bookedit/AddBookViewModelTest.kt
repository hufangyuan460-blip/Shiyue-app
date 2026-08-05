package com.shiyue.reader.feature.bookedit

import com.shiyue.reader.core.model.BookStatus
import com.shiyue.reader.domain.usecase.AddBookWithCoverUseCase
import com.shiyue.reader.domain.usecase.CreateCaptureTargetUseCase
import com.shiyue.reader.domain.usecase.CreateCategoryUseCase
import com.shiyue.reader.domain.usecase.DeleteTemporaryCoverUseCase
import com.shiyue.reader.domain.usecase.ObserveCategoriesUseCase
import com.shiyue.reader.testutil.FakeBookRepository
import com.shiyue.reader.testutil.FakeCategoryRepository
import com.shiyue.reader.testutil.FakeCoverStorage
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
class AddBookViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `invalid form shows field errors without saving`() = runTest {
        val repository = FakeBookRepository()
        val viewModel = viewModel(repository)

        viewModel.onTitleChanged("   ")
        viewModel.onTotalPagesChanged("0")
        viewModel.save()

        assertTrue(viewModel.uiState.value.titleError)
        assertTrue(viewModel.uiState.value.totalPagesError)
        assertEquals(0, repository.addCalls)
    }

    @Test
    fun `successful save normalizes values and emits saved event`() = runTest {
        val repository = FakeBookRepository()
        val viewModel = viewModel(repository)
        val event = async { viewModel.events.first() }

        viewModel.onTitleChanged("  活着  ")
        viewModel.onAuthorChanged("   ")
        viewModel.onTotalPagesChanged("191")
        viewModel.save()
        advanceUntilIdle()

        assertEquals(AddBookEvent.Saved, event.await())
        assertEquals(1, repository.addCalls)
        assertEquals("活着", repository.books.value.single().title)
        assertEquals(null, repository.books.value.single().author)
        assertEquals(BookStatus.READING, repository.books.value.single().status)
        assertFalse(viewModel.uiState.value.isSaving)
    }

    @Test
    fun `failed save exposes retryable error`() = runTest {
        val repository = FakeBookRepository(addFailure = IllegalStateException("disk full"))
        val viewModel = viewModel(repository)

        viewModel.onTitleChanged("长安的荔枝")
        viewModel.onTotalPagesChanged("224")
        viewModel.save()
        advanceUntilIdle()

        assertEquals(1, repository.addCalls)
        assertTrue(viewModel.uiState.value.saveFailed)
        assertFalse(viewModel.uiState.value.isSaving)
    }

    @Test
    fun `second save is ignored while first save is running`() = runTest {
        val gate = CompletableDeferred<Unit>()
        val repository = FakeBookRepository(addGate = gate)
        val viewModel = viewModel(repository)

        viewModel.onTitleChanged("额尔古纳河右岸")
        viewModel.onTotalPagesChanged("368")
        viewModel.save()
        viewModel.save()
        runCurrent()

        assertEquals(1, repository.addCalls)
        assertTrue(viewModel.uiState.value.isSaving)

        gate.complete(Unit)
        advanceUntilIdle()
        assertEquals(1, repository.books.value.size)
    }

    private fun viewModel(repository: FakeBookRepository): AddBookViewModel {
        val categories = FakeCategoryRepository()
        val covers = FakeCoverStorage()
        return AddBookViewModel(
            AddBookWithCoverUseCase(repository, covers),
            ObserveCategoriesUseCase(categories),
            CreateCategoryUseCase(categories),
            CreateCaptureTargetUseCase(covers),
            DeleteTemporaryCoverUseCase(covers),
        )
    }
}
