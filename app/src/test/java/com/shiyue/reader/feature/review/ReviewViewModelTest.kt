package com.shiyue.reader.feature.review

import com.shiyue.reader.core.model.Book
import com.shiyue.reader.core.model.DailyReadingDuration
import com.shiyue.reader.core.model.ReviewStatistics
import com.shiyue.reader.domain.usecase.ObserveLibraryBooksUseCase
import com.shiyue.reader.domain.usecase.ObserveReviewStatisticsUseCase
import com.shiyue.reader.testutil.FakeBookRepository
import com.shiyue.reader.testutil.FakeReadingSessionRepository
import com.shiyue.reader.testutil.MainDispatcherRule
import java.time.LocalDate
import java.time.YearMonth
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ReviewViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `calendar shows current month days with daily durations`() = runTest {
        val today = LocalDate.now()
        val stats = ReviewStatistics(
            totalDurationMs = 180_000L,
            dailyDurations = listOf(
                DailyReadingDuration(today, 60_000L),
                DailyReadingDuration(today.minusDays(1), 120_000L),
            ),
        )
        val viewModel = ReviewViewModel(
            ObserveReviewStatisticsUseCase(FakeReadingSessionRepository(reviewStatistics = stats)),
            ObserveLibraryBooksUseCase(FakeBookRepository()),
        )
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect() }
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isLoading)
        assertEquals(YearMonth.now().lengthOfMonth(), viewModel.uiState.value.calendarDays.size)
        assertEquals(60_000L, viewModel.uiState.value.calendarDays.first { it.date == today }.durationMs)
        assertEquals(180_000L, viewModel.uiState.value.statistics.totalDurationMs)
    }

    @Test
    fun `month navigation changes displayed month and clears data`() = runTest {
        val today = LocalDate.now()
        val stats = ReviewStatistics(dailyDurations = listOf(DailyReadingDuration(today, 60_000L)))
        val viewModel = ReviewViewModel(
            ObserveReviewStatisticsUseCase(FakeReadingSessionRepository(reviewStatistics = stats)),
            ObserveLibraryBooksUseCase(FakeBookRepository()),
        )
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect() }
        advanceUntilIdle()

        val current = viewModel.uiState.value.displayedMonth
        viewModel.previousMonth()
        advanceUntilIdle()
        assertEquals(current.minusMonths(1), viewModel.uiState.value.displayedMonth)
        assertTrue(viewModel.uiState.value.calendarDays.none { it.durationMs > 0 })

        viewModel.nextMonth()
        advanceUntilIdle()
        assertEquals(current, viewModel.uiState.value.displayedMonth)
        assertTrue(viewModel.uiState.value.calendarDays.any { it.durationMs > 0 })
    }

    @Test
    fun `top book resolved from library`() = runTest {
        val book = Book.create("最久的一本", null, 100)
        val stats = ReviewStatistics(topBookId = book.id, topBookDurationMs = 120_000L)
        val viewModel = ReviewViewModel(
            ObserveReviewStatisticsUseCase(FakeReadingSessionRepository(reviewStatistics = stats)),
            ObserveLibraryBooksUseCase(FakeBookRepository(listOf(book))),
        )
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect() }
        advanceUntilIdle()

        assertEquals("最久的一本", viewModel.uiState.value.topBook?.title)
    }
}
