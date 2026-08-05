package com.shiyue.reader.feature.bookdetail

import androidx.lifecycle.SavedStateHandle
import com.shiyue.reader.app.navigation.ShiyueRoutes
import com.shiyue.reader.core.model.Book
import com.shiyue.reader.domain.usecase.ObserveLibraryBookUseCase
import com.shiyue.reader.domain.usecase.DeleteBookUseCase
import com.shiyue.reader.testutil.FakeBookRepository
import com.shiyue.reader.testutil.FakeCoverStorage
import com.shiyue.reader.testutil.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class BookDetailViewModelTest {
    @get:Rule val dispatcherRule = MainDispatcherRule()

    @Test
    fun `loads content and follows repository updates`() = runTest {
        val original = Book.create("原书名", null, 100, timestamp = 1)
        val repository = FakeBookRepository(listOf(original))
        val viewModel = viewModel(original.id, repository)
        runCurrent()
        assertEquals(original, (viewModel.uiState.value as BookDetailUiState.Content).libraryBook.book)

        val updated = original.updated(title = "新书名", updatedAt = 2)
        repository.updateBook(updated)
        runCurrent()
        assertEquals(updated, (viewModel.uiState.value as BookDetailUiState.Content).libraryBook.book)
    }

    @Test
    fun `missing book becomes not found`() = runTest {
        val viewModel = viewModel("missing", FakeBookRepository())
        runCurrent()
        assertSame(BookDetailUiState.NotFound, viewModel.uiState.value)
    }

    @Test
    fun `repository failure becomes error`() = runTest {
        val viewModel = viewModel(
            "broken",
            FakeBookRepository(observeFailure = IllegalStateException("read failed")),
        )
        runCurrent()
        assertSame(BookDetailUiState.Error, viewModel.uiState.value)
    }

    private fun viewModel(id: String, repository: FakeBookRepository) = BookDetailViewModel(
        SavedStateHandle(mapOf(ShiyueRoutes.BookIdArgument to id)),
        ObserveLibraryBookUseCase(repository),
        DeleteBookUseCase(repository, FakeCoverStorage()),
    )
}
