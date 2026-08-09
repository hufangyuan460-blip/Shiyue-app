package com.shiyue.reader.core.model

import com.shiyue.reader.domain.time.TimeReading
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test

class ReadingSessionTest {
    private val bookId = "00000000-0000-0000-0000-000000000001"

    @Test fun activeDurationUsesElapsedRealtimeAndPauseFreezesIt() {
        val session = ReadingSession.start(bookId, 10, TimeReading(1_000, 5_000))
        assertEquals(2_000, session.durationAt(TimeReading(3_000, 7_000)).displayedDurationMs)
        val paused = session.pause(TimeReading(3_000, 7_000))
        assertEquals(ReadingSessionState.PAUSED, paused.state)
        assertEquals(2_000, paused.durationAt(TimeReading(20_000, 30_000)).displayedDurationMs)
    }

    @Test fun resumeAccumulatesOnlyActiveSegments() {
        val first = ReadingSession.start(bookId, 0, TimeReading(1_000, 1_000)).pause(TimeReading(2_000, 2_000))
        val resumed = first.resume(TimeReading(5_000, 5_000)).pause(TimeReading(8_000, 8_000))
        assertEquals(4_000, resumed.activeDurationMs)
    }

    @Test fun detectsRebootClockRollbackDivergenceAndLongSegment() {
        val session = ReadingSession.start(bookId, 0, TimeReading(100_000, 100_000))
        assertEquals(ReadingClockAnomaly.DEVICE_REBOOTED, session.durationAt(TimeReading(101_000, 99_000)).anomaly)
        assertEquals(ReadingClockAnomaly.WALL_CLOCK_MOVED_BACKWARD, session.durationAt(TimeReading(99_000, 101_000)).anomaly)
        assertEquals(ReadingClockAnomaly.CLOCKS_DIVERGED, session.durationAt(TimeReading(300_001, 101_000)).anomaly)
        assertEquals(ReadingClockAnomaly.UNUSUALLY_LONG_SEGMENT, session.durationAt(TimeReading(46_000_001, 46_000_001)).anomaly)
    }

    @Test fun normalDurationHasNoAnomaly() {
        val duration = ReadingSession.start(bookId, 0, TimeReading(10, 20)).durationAt(TimeReading(1_010, 1_020))
        assertNull(duration.anomaly)
        assertEquals(1_000, duration.displayedDurationMs)
    }

    @Test fun invalidPagesAndNegativeRecoveryAreRejected() {
        assertThrows(IllegalArgumentException::class.java) { ReadingSession.start(bookId, -1, TimeReading(0, 0)) }
        val session = ReadingSession.start(bookId, 0, TimeReading(0, 0))
        assertThrows(IllegalArgumentException::class.java) { session.recover(TimeReading(1, 1), -1) }
    }

    @Test fun completedSessionRejectsImpossibleDuration() {
        val paused = ReadingSession.start(bookId, 0, TimeReading(0, 0)).pause(TimeReading(2_000, 2_000))
        assertThrows(IllegalArgumentException::class.java) { paused.complete(10, true, 500) }
    }

    @Test fun aggregateReadingTimesCountsOnlyCompletedAndTodayByEndDay() {
        val zone = ZoneId.of("Asia/Shanghai")
        val nowEpochMs = LocalDate.of(2026, 8, 9).atStartOfDay(zone).toInstant().toEpochMilli() + 12 * 3_600_000L
        val todayStart = LocalDate.of(2026, 8, 9).atStartOfDay(zone).toInstant().toEpochMilli()
        val sessions = listOf(
            completed(todayStart - 2 * 3_600_000L, todayStart - 3_600_000L, 120_000L),
            completed(todayStart + 3_600_000L - 90_000L, todayStart + 3_600_000L, 90_000L),
            completed(todayStart - 3_600_000L, todayStart + 30 * 60_000L, 3_600_000L),
        )
        val summary = aggregateReadingTimes(sessions, nowEpochMs, zone).getValue(bookId)
        assertEquals(120_000L + 90_000L + 3_600_000L, summary.totalDurationMs)
        assertEquals(90_000L + 3_600_000L, summary.todayDurationMs)
    }

    @Test fun aggregateReadingTimesExcludesUnfinishedAndEmptyInputs() {
        val zone = ZoneId.systemDefault()
        val nowEpochMs = System.currentTimeMillis()
        val paused = ReadingSession.start(bookId, 0, TimeReading(1_000, 1_000)).pause(TimeReading(60_000, 60_000))
        val active = ReadingSession.start(bookId, 0, TimeReading(1_000, 1_000))
        assertEquals(emptyMap<String, BookReadingSummary>(), aggregateReadingTimes(emptyList(), nowEpochMs, zone))
        assertEquals(emptyMap<String, BookReadingSummary>(), aggregateReadingTimes(listOf(paused, active), nowEpochMs, zone))
    }

    @Test fun aggregateReadingTimesGroupsByBook() {
        val zone = ZoneId.systemDefault()
        val otherBookId = "00000000-0000-0000-0000-000000000002"
        val nowEpochMs = System.currentTimeMillis()
        val todayStart = LocalDate.ofInstant(java.time.Instant.ofEpochMilli(nowEpochMs), zone)
            .atStartOfDay(zone).toInstant().toEpochMilli()
        val first = completed(todayStart + 3_600_000L - 60_000L, todayStart + 3_600_000L, 60_000L)
        val second = completed(todayStart + 3_600_000L - 30_000L, todayStart + 3_600_000L, 30_000L, otherBookId)
        val result = aggregateReadingTimes(listOf(first, second), nowEpochMs, zone)
        assertEquals(60_000L, result.getValue(bookId).totalDurationMs)
        assertEquals(30_000L, result.getValue(otherBookId).totalDurationMs)
    }

    @Test fun computeReviewStatisticsAggregatesPeriodsDailyAndTopBook() {
        val zone = ZoneId.of("Asia/Shanghai")
        val today = LocalDate.of(2026, 8, 9)
        val nowEpochMs = today.atStartOfDay(zone).toInstant().toEpochMilli() + 12 * 3_600_000L
        fun startOf(day: LocalDate) = day.atStartOfDay(zone).toInstant().toEpochMilli()
        val weekStart = today.minusDays((today.dayOfWeek.value - 1).toLong())
        val sessions = listOf(
            completed(startOf(today.minusDays(1)) + 3_600_000L - 120_000L, startOf(today.minusDays(1)) + 3_600_000L, 120_000L),
            completed(startOf(today) + 3_600_000L - 60_000L, startOf(today) + 3_600_000L, 60_000L),
            completed(startOf(today.minusDays(30)) + 3_600_000L - 90_000L, startOf(today.minusDays(30)) + 3_600_000L, 90_000L),
        )
        val stats = computeReviewStatistics(sessions, setOf(bookId), nowEpochMs, zone)
        val expectedWeek = listOf(today, today.minusDays(1))
            .filter { it >= weekStart && it < weekStart.plusWeeks(1) }
            .sumOf { if (it == today) 60_000L else 120_000L }
        assertEquals(120_000L + 60_000L + 90_000L, stats.totalDurationMs)
        assertEquals(60_000L, stats.todayDurationMs)
        assertEquals(expectedWeek, stats.weekDurationMs)
        assertEquals(180_000L, stats.monthDurationMs)
        assertEquals(3, stats.readingDays)
        assertEquals(1, stats.finishedBookCount)
        assertEquals(bookId, stats.topBookId)
        assertEquals(270_000L, stats.topBookDurationMs)
        assertEquals(3, stats.dailyDurations.size)
    }

    @Test fun computeReviewStatisticsPicksMostReadBookAndIgnoresUnfinished() {
        val zone = ZoneId.systemDefault()
        val otherBookId = "00000000-0000-0000-0000-000000000002"
        val nowEpochMs = System.currentTimeMillis()
        val todayStart = LocalDate.ofInstant(Instant.ofEpochMilli(nowEpochMs), zone)
            .atStartOfDay(zone).toInstant().toEpochMilli()
        val paused = ReadingSession.start(bookId, 0, TimeReading(1_000, 1_000)).pause(TimeReading(60_000, 60_000))
        val sessions = listOf(
            paused,
            completed(todayStart + 3_600_000L - 120_000L, todayStart + 3_600_000L, 120_000L),
            completed(todayStart + 3_600_000L - 300_000L, todayStart + 3_600_000L, 300_000L, otherBookId),
        )
        val stats = computeReviewStatistics(sessions, emptySet(), nowEpochMs, zone)
        assertEquals(otherBookId, stats.topBookId)
        assertEquals(300_000L, stats.topBookDurationMs)
        assertEquals(420_000L, stats.totalDurationMs)
    }

    @Test fun computeReviewStatisticsComputesCurrentStreak() {
        val zone = ZoneId.of("Asia/Shanghai")
        val today = LocalDate.of(2026, 8, 9)
        val nowEpochMs = today.atStartOfDay(zone).toInstant().toEpochMilli() + 12 * 3_600_000L
        fun sessionOn(day: LocalDate) = completed(day.atStartOfDay(zone).toInstant().toEpochMilli(), day.atStartOfDay(zone).toInstant().toEpochMilli() + 60_000L, 60_000L)
        val threeDays = computeReviewStatistics(
            listOf(sessionOn(today), sessionOn(today.minusDays(1)), sessionOn(today.minusDays(2))),
            emptySet(), nowEpochMs, zone,
        )
        assertEquals(3, threeDays.currentStreakDays)
        val gapToday = computeReviewStatistics(
            listOf(sessionOn(today.minusDays(1)), sessionOn(today.minusDays(2))),
            emptySet(), nowEpochMs, zone,
        )
        assertEquals(2, gapToday.currentStreakDays)
    }

    @Test fun computeReviewStatisticsEmptyWithoutCompletedSessions() {
        val zone = ZoneId.systemDefault()
        val nowEpochMs = System.currentTimeMillis()
        val stats = computeReviewStatistics(emptyList(), emptySet(), nowEpochMs, zone)
        assertEquals(0L, stats.totalDurationMs)
        assertEquals(0, stats.readingDays)
        assertEquals(0, stats.currentStreakDays)
        assertNull(stats.topBookId)
        assertEquals(emptyList<DailyReadingDuration>(), stats.dailyDurations)
    }

    private fun completed(startedAt: Long, endedAt: Long, durationMs: Long, forBookId: String = bookId): ReadingSession =
        ReadingSession.start(forBookId, 0, TimeReading(startedAt, startedAt))
            .pause(TimeReading(startedAt + durationMs, startedAt + durationMs))
            .complete(1, true, endedAt)
}
