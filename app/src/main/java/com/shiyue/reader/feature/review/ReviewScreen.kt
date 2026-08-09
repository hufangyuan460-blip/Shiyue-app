package com.shiyue.reader.feature.review

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.shiyue.reader.R
import com.shiyue.reader.core.model.Book
import java.text.DateFormat
import java.time.LocalDate
import java.util.Date

@Composable
fun ReviewRoute(viewModel: ReviewViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    ReviewScreen(state, viewModel::previousMonth, viewModel::nextMonth)
}

@Composable
fun ReviewScreen(
    state: ReviewUiState,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
) {
    when {
        state.isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        state.failed -> ReviewMessage(stringResource(R.string.reading_load_failed))
        state.statistics.totalDurationMs == 0L -> ReviewMessage(stringResource(R.string.review_empty))
        else -> LazyColumn(
            Modifier.fillMaxSize().padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                Text(
                    stringResource(R.string.review_title),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 24.dp),
                )
                Spacer(Modifier.height(4.dp))
                TotalDurationSection(state.statistics.totalDurationMs)
            }
            item { PeriodRow(state.statistics) }
            item { ReadingCalendar(state.displayedMonth, state.calendarDays, onPreviousMonth, onNextMonth) }
            item { StatsRow(state.statistics) }
            state.topBook?.let { book ->
                item { TopBookCard(book, state.statistics.topBookDurationMs) }
            }
        }
    }
}

@Composable
private fun TotalDurationSection(totalDurationMs: Long) {
    Column(Modifier.fillMaxWidth().padding(top = 8.dp)) {
        Text(
            formatDuration(totalDurationMs),
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary,
        )
        Text(
            stringResource(R.string.review_total_duration_label),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun PeriodRow(statistics: com.shiyue.reader.core.model.ReviewStatistics) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        PeriodTile(stringResource(R.string.review_today), formatDuration(statistics.todayDurationMs), Modifier.weight(1f))
        PeriodTile(stringResource(R.string.review_week), formatDuration(statistics.weekDurationMs), Modifier.weight(1f))
        PeriodTile(stringResource(R.string.review_month), formatDuration(statistics.monthDurationMs), Modifier.weight(1f))
    }
}

@Composable
private fun PeriodTile(label: String, value: String, modifier: Modifier = Modifier) {
    Card(modifier, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(Modifier.fillMaxWidth().padding(vertical = 14.dp, horizontal = 10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(2.dp))
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun ReadingCalendar(
    month: java.time.YearMonth,
    days: List<ReviewCalendarDay>,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
) {
    val today = LocalDate.now()
    val previousMonthDescription = stringResource(R.string.review_previous_month)
    val nextMonthDescription = stringResource(R.string.review_next_month)
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(Modifier.fillMaxWidth().padding(14.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onPreviousMonth,
                    modifier = Modifier.semantics { contentDescription = previousMonthDescription },
                ) { Icon(painterResource(R.drawable.ic_arrow_back), contentDescription = null) }
                Text(
                    stringResource(R.string.review_month_format, month.year, month.monthValue),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                )
                IconButton(
                    onClick = onNextMonth,
                    modifier = Modifier.semantics { contentDescription = nextMonthDescription },
                ) { Icon(painterResource(R.drawable.ic_arrow_forward), contentDescription = null) }
            }
            Spacer(Modifier.height(6.dp))
            Row(Modifier.fillMaxWidth()) {
                (1..7).forEach { index ->
                    Text(
                        stringResource(weekdayLabel(index)),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
            Spacer(Modifier.height(4.dp))
            val leadingBlanks = month.atDay(1).dayOfWeek.value - 1
            val cells: List<ReviewCalendarDay?> = List(leadingBlanks) { null } + days
            cells.chunked(7).forEach { week ->
                Row(Modifier.fillMaxWidth()) {
                    week.forEach { cell ->
                        Box(Modifier.weight(1f).padding(2.dp)) {
                            if (cell != null) CalendarDayCell(cell, isToday = cell.date == today)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun weekdayLabel(index: Int): Int = when (index) {
    1 -> R.string.review_weekday_1
    2 -> R.string.review_weekday_2
    3 -> R.string.review_weekday_3
    4 -> R.string.review_weekday_4
    5 -> R.string.review_weekday_5
    6 -> R.string.review_weekday_6
    else -> R.string.review_weekday_7
}

@Composable
private fun CalendarDayCell(day: ReviewCalendarDay, isToday: Boolean) {
    val description = stringResource(
        R.string.review_calendar_day_description,
        DateFormat.getDateInstance().format(Date(
            day.date.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli(),
        )),
        formatDuration(day.durationMs),
    )
    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(8.dp))
            .background(heatmapColor(day.durationMs))
            .then(if (isToday) Modifier.border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(8.dp)) else Modifier)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            "${day.date.dayOfMonth}",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (day.durationMs > 0) FontWeight.SemiBold else FontWeight.Normal,
            color = if (day.durationMs > 0) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun heatmapColor(durationMs: Long): Color {
    if (durationMs <= 0) return MaterialTheme.colorScheme.surfaceContainerHighest
    val minutes = durationMs / 60_000L
    val alpha = when {
        minutes < 10 -> 0.25f
        minutes < 30 -> 0.45f
        minutes < 60 -> 0.65f
        else -> 0.9f
    }
    return MaterialTheme.colorScheme.primary.copy(alpha = alpha)
}

@Composable
private fun StatsRow(statistics: com.shiyue.reader.core.model.ReviewStatistics) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        StatTile(stringResource(R.string.review_streak_days, statistics.currentStreakDays), Modifier.weight(1f))
        StatTile(stringResource(R.string.review_reading_days, statistics.readingDays), Modifier.weight(1f))
        StatTile(stringResource(R.string.review_finished_books, statistics.finishedBookCount), Modifier.weight(1f))
    }
}

@Composable
private fun StatTile(text: String, modifier: Modifier = Modifier) {
    Card(modifier, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Text(
            text,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp, horizontal = 8.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun TopBookCard(book: Book, durationMs: Long) {
    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            Text(
                stringResource(R.string.review_top_book),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(4.dp))
            Text(book.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            book.author?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.height(2.dp))
            Text(formatDuration(durationMs), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
private fun ReviewMessage(message: String) {
    Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
        Text(message, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun formatDuration(value: Long): String {
    val minutes = value / 60_000L
    return if (minutes >= 60) stringResource(R.string.duration_hours_minutes, minutes / 60, minutes % 60)
    else stringResource(R.string.duration_minutes_value, minutes)
}
