package com.shiyue.reader.feature.review

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shiyue.reader.core.model.Book
import com.shiyue.reader.core.model.DailyReadingDuration
import com.shiyue.reader.core.model.ReviewStatistics
import com.shiyue.reader.domain.usecase.ObserveLibraryBooksUseCase
import com.shiyue.reader.domain.usecase.ObserveReviewStatisticsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class ReviewCalendarDay(
    val date: LocalDate,
    val durationMs: Long,
)

data class ReviewUiState(
    val isLoading: Boolean = true,
    val failed: Boolean = false,
    val statistics: ReviewStatistics = ReviewStatistics(),
    val displayedMonth: YearMonth = YearMonth.now(),
    val calendarDays: List<ReviewCalendarDay> = emptyList(),
    val topBook: Book? = null,
)

@HiltViewModel
class ReviewViewModel @Inject constructor(
    observeReview: ObserveReviewStatisticsUseCase,
    observeBooks: ObserveLibraryBooksUseCase,
) : ViewModel() {
    private val displayedMonth = MutableStateFlow(YearMonth.now())

    val uiState = combine(observeReview(), observeBooks(), displayedMonth) { statistics, books, month ->
        ReviewUiState(
            isLoading = false,
            statistics = statistics,
            displayedMonth = month,
            calendarDays = calendarDaysFor(month, statistics.dailyDurations),
            topBook = statistics.topBookId?.let { id -> books.firstOrNull { it.book.id == id }?.book },
        )
    }.catch {
        emit(ReviewUiState(isLoading = false, failed = true))
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ReviewUiState())

    fun previousMonth() {
        displayedMonth.value = displayedMonth.value.minusMonths(1)
    }

    fun nextMonth() {
        displayedMonth.value = displayedMonth.value.plusMonths(1)
    }

    private fun calendarDaysFor(month: YearMonth, daily: List<DailyReadingDuration>): List<ReviewCalendarDay> {
        val byDate = daily.associateBy { it.date }
        return (1..month.lengthOfMonth()).map { day ->
            val date = month.atDay(day)
            ReviewCalendarDay(date, byDate[date]?.durationMs ?: 0)
        }
    }
}
