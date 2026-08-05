package com.shiyue.reader.feature.bookshelf

import com.shiyue.reader.core.model.Book
import com.shiyue.reader.domain.usecase.ObserveBooksUseCase
import com.shiyue.reader.testutil.FakeBookRepository
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
class BookshelfViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `repository flow updates bookshelf in newest-first order`() = runTest {
        val repository = FakeBookRepository()
        val viewModel = BookshelfViewModel(ObserveBooksUseCase(repository))
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }
        val older = Book.create(title = "旧书", author = null, totalPages = 100, timestamp = 100)
        val newer = Book.create(title = "新书", author = null, totalPages = 200, timestamp = 200)

        repository.books.value = listOf(older, newer)
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isLoading)
        assertEquals(listOf("新书", "旧书"), viewModel.uiState.value.books.map(Book::title))
    }
}
