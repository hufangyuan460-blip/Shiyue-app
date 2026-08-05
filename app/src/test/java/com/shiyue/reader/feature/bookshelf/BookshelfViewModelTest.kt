package com.shiyue.reader.feature.bookshelf

import com.shiyue.reader.core.model.Book
import com.shiyue.reader.core.model.BookSortMode
import com.shiyue.reader.core.model.BookStatus
import com.shiyue.reader.core.model.Category
import com.shiyue.reader.core.model.CategoryFilter
import com.shiyue.reader.domain.usecase.ObserveLibraryBooksUseCase
import com.shiyue.reader.domain.usecase.ObserveCategoriesUseCase
import com.shiyue.reader.testutil.FakeBookRepository
import com.shiyue.reader.testutil.FakeCategoryRepository
import com.shiyue.reader.testutil.FakeBookshelfPreferences
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
        val viewModel = BookshelfViewModel(
            ObserveLibraryBooksUseCase(repository),
            ObserveCategoriesUseCase(FakeCategoryRepository()),
            FakeBookshelfPreferences(),
        )
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }
        val older = Book.create(title = "旧书", author = null, totalPages = 100, timestamp = 100)
        val newer = Book.create(title = "新书", author = null, totalPages = 200, timestamp = 200)

        repository.books.value = listOf(older, newer)
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isLoading)
        assertEquals(listOf("新书", "旧书"), viewModel.uiState.value.books.map { it.book.title })
    }

    @Test
    fun `category status search and sort filters combine without overriding each other`() = runTest {
        val literature = Category.create("文学", 0)
        val matching = Book.create("百年孤独", "Gabriel García Márquez", 400, currentPage = 200, status = BookStatus.READING, timestamp = 100)
        val wrongStatus = Book.create("百年历史", "Author", 200, status = BookStatus.FINISHED, timestamp = 300)
        val wrongCategory = Book.create("百年计算机", "Author", 100, currentPage = 90, status = BookStatus.READING, timestamp = 200)
        val repository = FakeBookRepository(listOf(matching, wrongStatus, wrongCategory)).apply {
            categoryAssignments.value = mapOf(matching.id to listOf(literature), wrongStatus.id to listOf(literature))
        }
        val categories = FakeCategoryRepository(listOf(literature))
        val preferences = FakeBookshelfPreferences()
        val viewModel = BookshelfViewModel(
            ObserveLibraryBooksUseCase(repository), ObserveCategoriesUseCase(categories), preferences,
        )
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect() }

        viewModel.onCategoryFilterChanged(CategoryFilter.CategoryId(literature.id))
        viewModel.onStatusFilterChanged(BookStatus.READING)
        viewModel.onSearchQueryChanged("  百年  ")
        viewModel.onSortModeChanged(BookSortMode.PROGRESS_DESC)
        advanceUntilIdle()

        assertEquals(listOf(matching.id), viewModel.uiState.value.books.map { it.book.id })
        assertEquals(BookSortMode.PROGRESS_DESC, viewModel.uiState.value.sortMode)
    }
}
