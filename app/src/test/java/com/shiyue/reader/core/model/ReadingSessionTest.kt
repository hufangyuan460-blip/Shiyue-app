package com.shiyue.reader.core.model

import com.shiyue.reader.domain.time.TimeReading
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
}
