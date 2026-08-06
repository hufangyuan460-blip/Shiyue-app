package com.shiyue.reader.domain.time

import android.os.SystemClock
import javax.inject.Inject
import javax.inject.Singleton

interface TimeSource {
    fun wallClockMillis(): Long
    fun elapsedRealtimeMillis(): Long
}

@Singleton
class SystemTimeSource @Inject constructor() : TimeSource {
    override fun wallClockMillis(): Long = System.currentTimeMillis()
    override fun elapsedRealtimeMillis(): Long = SystemClock.elapsedRealtime()
}

data class TimeReading(val wallClockMs: Long, val elapsedRealtimeMs: Long)

fun TimeSource.read(): TimeReading = TimeReading(wallClockMillis(), elapsedRealtimeMillis())
