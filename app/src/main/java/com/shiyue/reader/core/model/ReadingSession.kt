package com.shiyue.reader.core.model

import com.shiyue.reader.domain.time.TimeReading
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID
import kotlin.math.abs

enum class ReadingSessionState { ACTIVE, PAUSED, COMPLETED, DISCARDED }

enum class ReadingClockAnomaly {
    DEVICE_REBOOTED,
    WALL_CLOCK_MOVED_BACKWARD,
    CLOCKS_DIVERGED,
    UNUSUALLY_LONG_SEGMENT,
}

data class ReadingDuration(
    val reliableDurationMs: Long,
    val displayedDurationMs: Long,
    val estimatedSegmentMs: Long,
    val anomaly: ReadingClockAnomaly? = null,
)

object ReadingTimerPolicy {
    const val CLOCK_DIVERGENCE_THRESHOLD_MS = 2 * 60 * 1000L
    const val UNUSUALLY_LONG_SEGMENT_MS = 12 * 60 * 60 * 1000L
    const val DURATION_TOLERANCE_MS = 1_000L
}

@ConsistentCopyVisibility
data class ReadingSession private constructor(
    val id: String,
    val bookId: String,
    val state: ReadingSessionState,
    val startedAtEpochMs: Long,
    val endedAtEpochMs: Long?,
    val startPage: Int,
    val endPage: Int?,
    val activeDurationMs: Long,
    val activeSegmentStartedAtEpochMs: Long?,
    val activeSegmentStartedAtElapsedRealtimeMs: Long?,
    val updateBookProgress: Boolean,
    val createdAt: Long,
    val updatedAt: Long,
) {
    fun durationAt(now: TimeReading): ReadingDuration {
        if (state != ReadingSessionState.ACTIVE) {
            return ReadingDuration(activeDurationMs, activeDurationMs, 0)
        }
        val wallStart = requireNotNull(activeSegmentStartedAtEpochMs)
        val elapsedStart = requireNotNull(activeSegmentStartedAtElapsedRealtimeMs)
        val wallDelta = now.wallClockMs - wallStart
        val elapsedDelta = now.elapsedRealtimeMs - elapsedStart
        val anomaly = when {
            elapsedDelta < 0 -> ReadingClockAnomaly.DEVICE_REBOOTED
            wallDelta < 0 -> ReadingClockAnomaly.WALL_CLOCK_MOVED_BACKWARD
            abs(wallDelta - elapsedDelta) > ReadingTimerPolicy.CLOCK_DIVERGENCE_THRESHOLD_MS ->
                ReadingClockAnomaly.CLOCKS_DIVERGED
            elapsedDelta > ReadingTimerPolicy.UNUSUALLY_LONG_SEGMENT_MS ->
                ReadingClockAnomaly.UNUSUALLY_LONG_SEGMENT
            else -> null
        }
        val safeElapsed = elapsedDelta.coerceAtLeast(0)
        val estimate = wallDelta.coerceAtLeast(0)
        return ReadingDuration(
            reliableDurationMs = activeDurationMs,
            displayedDurationMs = activeDurationMs + if (anomaly == null) safeElapsed else 0,
            estimatedSegmentMs = estimate,
            anomaly = anomaly,
        )
    }

    fun pause(now: TimeReading): ReadingSession {
        if (state == ReadingSessionState.PAUSED) return this
        check(state == ReadingSessionState.ACTIVE) { "Only an active session can be paused" }
        val duration = durationAt(now)
        check(duration.anomaly == null) { "Reading clock anomaly: ${duration.anomaly}" }
        return validated(
            id, bookId, ReadingSessionState.PAUSED, startedAtEpochMs, null, startPage, null,
            duration.displayedDurationMs, null, null, false, createdAt, now.wallClockMs,
        )
    }

    fun resume(now: TimeReading): ReadingSession {
        if (state == ReadingSessionState.ACTIVE) return this
        check(state == ReadingSessionState.PAUSED) { "Only a paused session can be resumed" }
        return validated(
            id, bookId, ReadingSessionState.ACTIVE, startedAtEpochMs, null, startPage, null,
            activeDurationMs, now.wallClockMs, now.elapsedRealtimeMs, false, createdAt, now.wallClockMs,
        )
    }

    fun recover(now: TimeReading, acceptedSegmentMs: Long): ReadingSession {
        check(state == ReadingSessionState.ACTIVE) { "Only an active session can be recovered" }
        require(acceptedSegmentMs >= 0) { "Accepted duration must not be negative" }
        return validated(
            id, bookId, ReadingSessionState.ACTIVE, startedAtEpochMs, null, startPage, null,
            activeDurationMs + acceptedSegmentMs, now.wallClockMs, now.elapsedRealtimeMs,
            false, createdAt, now.wallClockMs,
        )
    }

    fun setReliableDuration(durationMs: Long, nowEpochMs: Long): ReadingSession {
        check(state == ReadingSessionState.ACTIVE || state == ReadingSessionState.PAUSED)
        require(durationMs >= 0)
        return validated(
            id, bookId, ReadingSessionState.PAUSED, startedAtEpochMs, null, startPage, null,
            durationMs, null, null, false, createdAt, nowEpochMs,
        )
    }

    fun complete(
        endPage: Int,
        updateBookProgress: Boolean,
        endedAtEpochMs: Long,
    ): ReadingSession {
        if (state == ReadingSessionState.COMPLETED) return this
        check(state == ReadingSessionState.PAUSED) { "Session must be paused before completion" }
        require(endedAtEpochMs >= startedAtEpochMs) { "End time must not precede start time" }
        require(activeDurationMs <= endedAtEpochMs - startedAtEpochMs + ReadingTimerPolicy.DURATION_TOLERANCE_MS) {
            "Active duration exceeds natural session span"
        }
        return validated(
            id, bookId, ReadingSessionState.COMPLETED, startedAtEpochMs, endedAtEpochMs,
            startPage, endPage, activeDurationMs, null, null, updateBookProgress,
            createdAt, endedAtEpochMs,
        )
    }

    fun discard(nowEpochMs: Long): ReadingSession {
        if (state == ReadingSessionState.DISCARDED) return this
        check(state == ReadingSessionState.ACTIVE || state == ReadingSessionState.PAUSED)
        return validated(
            id, bookId, ReadingSessionState.DISCARDED, startedAtEpochMs, nowEpochMs,
            startPage, null, activeDurationMs, null, null, false, createdAt, nowEpochMs,
        )
    }

    fun corrected(
        startedAtEpochMs: Long,
        endedAtEpochMs: Long,
        activeDurationMs: Long,
        startPage: Int,
        endPage: Int,
        updatedAt: Long,
    ): ReadingSession {
        check(state == ReadingSessionState.COMPLETED) { "Only completed sessions can be corrected" }
        require(endedAtEpochMs >= startedAtEpochMs)
        require(activeDurationMs >= 0)
        require(activeDurationMs <= endedAtEpochMs - startedAtEpochMs + ReadingTimerPolicy.DURATION_TOLERANCE_MS)
        return validated(
            id, bookId, state, startedAtEpochMs, endedAtEpochMs, startPage, endPage,
            activeDurationMs, null, null, updateBookProgress, createdAt, updatedAt,
        )
    }

    companion object {
        fun start(bookId: String, startPage: Int, now: TimeReading): ReadingSession = validated(
            UUID.randomUUID().toString(), bookId, ReadingSessionState.ACTIVE,
            now.wallClockMs, null, startPage, null, 0, now.wallClockMs,
            now.elapsedRealtimeMs, false, now.wallClockMs, now.wallClockMs,
        )

        internal fun restore(
            id: String,
            bookId: String,
            state: ReadingSessionState,
            startedAtEpochMs: Long,
            endedAtEpochMs: Long?,
            startPage: Int,
            endPage: Int?,
            activeDurationMs: Long,
            activeSegmentStartedAtEpochMs: Long?,
            activeSegmentStartedAtElapsedRealtimeMs: Long?,
            updateBookProgress: Boolean,
            createdAt: Long,
            updatedAt: Long,
        ) = validated(
            id, bookId, state, startedAtEpochMs, endedAtEpochMs, startPage, endPage,
            activeDurationMs, activeSegmentStartedAtEpochMs, activeSegmentStartedAtElapsedRealtimeMs,
            updateBookProgress, createdAt, updatedAt,
        )

        private fun validated(
            id: String,
            bookId: String,
            state: ReadingSessionState,
            startedAtEpochMs: Long,
            endedAtEpochMs: Long?,
            startPage: Int,
            endPage: Int?,
            activeDurationMs: Long,
            activeSegmentStartedAtEpochMs: Long?,
            activeSegmentStartedAtElapsedRealtimeMs: Long?,
            updateBookProgress: Boolean,
            createdAt: Long,
            updatedAt: Long,
        ): ReadingSession {
            require(runCatching { UUID.fromString(id) }.isSuccess)
            require(runCatching { UUID.fromString(bookId) }.isSuccess)
            require(startPage >= 0 && (endPage == null || endPage >= 0))
            require(activeDurationMs >= 0)
            when (state) {
                ReadingSessionState.ACTIVE -> {
                    require(endedAtEpochMs == null && endPage == null)
                    require(activeSegmentStartedAtEpochMs != null && activeSegmentStartedAtElapsedRealtimeMs != null)
                }
                ReadingSessionState.PAUSED -> {
                    require(endedAtEpochMs == null && endPage == null)
                    require(activeSegmentStartedAtEpochMs == null && activeSegmentStartedAtElapsedRealtimeMs == null)
                }
                ReadingSessionState.COMPLETED -> {
                    require(endedAtEpochMs != null && endPage != null)
                    require(activeSegmentStartedAtEpochMs == null && activeSegmentStartedAtElapsedRealtimeMs == null)
                }
                ReadingSessionState.DISCARDED -> {
                    require(activeSegmentStartedAtEpochMs == null && activeSegmentStartedAtElapsedRealtimeMs == null)
                }
            }
            return ReadingSession(
                id, bookId, state, startedAtEpochMs, endedAtEpochMs, startPage, endPage,
                activeDurationMs, activeSegmentStartedAtEpochMs, activeSegmentStartedAtElapsedRealtimeMs,
                updateBookProgress, createdAt, updatedAt,
            )
        }
    }
}

data class ReadingHistorySummary(
    val totalDurationMs: Long = 0,
    val completedCount: Int = 0,
    val readingDays: Int = 0,
    val lastReadAtEpochMs: Long? = null,
)

data class BookReadingSummary(
    val bookId: String,
    val totalDurationMs: Long,
    val todayDurationMs: Long,
)

/**
 * 按书聚合已完成场次的有效阅读时长。
 * 累计时长只统计 COMPLETED 场次；今日时长同样只统计 COMPLETED 场次，并按场次结束时间归入当天。
 */
fun aggregateReadingTimes(
    sessions: List<ReadingSession>,
    nowEpochMs: Long,
    zone: ZoneId = ZoneId.systemDefault(),
): Map<String, BookReadingSummary> {
    val todayStart = Instant.ofEpochMilli(nowEpochMs).atZone(zone).toLocalDate()
        .atStartOfDay(zone).toInstant().toEpochMilli()
    val tomorrowStart = todayStart + Duration.ofDays(1).toMillis()
    return sessions.asSequence()
        .filter { it.state == ReadingSessionState.COMPLETED }
        .groupBy { it.bookId }
        .mapValues { (bookId, bookSessions) ->
            BookReadingSummary(
                bookId = bookId,
                totalDurationMs = bookSessions.sumOf { it.activeDurationMs },
                todayDurationMs = bookSessions
                    .filter { session ->
                        session.endedAtEpochMs?.let { it >= todayStart && it < tomorrowStart } == true
                    }
                    .sumOf { it.activeDurationMs },
            )
        }
}

data class DailyReadingDuration(
    val date: LocalDate,
    val durationMs: Long,
)

data class ReviewStatistics(
    val totalDurationMs: Long = 0,
    val todayDurationMs: Long = 0,
    val weekDurationMs: Long = 0,
    val monthDurationMs: Long = 0,
    val readingDays: Int = 0,
    val currentStreakDays: Int = 0,
    val finishedBookCount: Int = 0,
    val dailyDurations: List<DailyReadingDuration> = emptyList(),
    val topBookId: String? = null,
    val topBookDurationMs: Long = 0,
)

/**
 * 回顾页全 App 统计。只统计 COMPLETED 场次，跨午夜按结束时间归入当天；
 * 周以周一开始，连续阅读按“今天或昨天起向前不间断有阅读”计算。
 */
fun computeReviewStatistics(
    sessions: List<ReadingSession>,
    finishedBookIds: Set<String>,
    nowEpochMs: Long,
    zone: ZoneId = ZoneId.systemDefault(),
): ReviewStatistics {
    val completed = sessions.filter { it.state == ReadingSessionState.COMPLETED }
    val today = Instant.ofEpochMilli(nowEpochMs).atZone(zone).toLocalDate()
    val weekStart = today.minusDays((today.dayOfWeek.value - 1).toLong())
    val monthStart = today.withDayOfMonth(1)

    val byDate = completed
        .filter { it.endedAtEpochMs != null }
        .groupBy { Instant.ofEpochMilli(it.endedAtEpochMs!!).atZone(zone).toLocalDate() }
    val daily = byDate
        .map { (date, list) -> DailyReadingDuration(date, list.sumOf { it.activeDurationMs }) }
        .filter { it.durationMs > 0 }
        .sortedBy { it.date }

    val todayMs = daily.filter { it.date == today }.sumOf { it.durationMs }
    val weekMs = daily.filter { it.date >= weekStart && it.date < weekStart.plusWeeks(1) }.sumOf { it.durationMs }
    val monthMs = daily.filter { it.date >= monthStart && it.date < monthStart.plusMonths(1) }.sumOf { it.durationMs }

    val byBook = completed.groupBy { it.bookId }
    val topBook = byBook.entries.maxByOrNull { it.value.sumOf { session -> session.activeDurationMs } }

    return ReviewStatistics(
        totalDurationMs = completed.sumOf { it.activeDurationMs },
        todayDurationMs = todayMs,
        weekDurationMs = weekMs,
        monthDurationMs = monthMs,
        readingDays = daily.size,
        currentStreakDays = currentStreak(daily.map { it.date }.toSet(), today),
        finishedBookCount = finishedBookIds.size,
        dailyDurations = daily,
        topBookId = topBook?.key,
        topBookDurationMs = topBook?.value?.sumOf { session -> session.activeDurationMs } ?: 0,
    )
}

private fun currentStreak(readingDates: Set<LocalDate>, today: LocalDate): Int {
    var day = today
    if (day !in readingDates) day = day.minusDays(1)
    var count = 0
    while (day in readingDates) {
        count += 1
        day = day.minusDays(1)
    }
    return count
}
