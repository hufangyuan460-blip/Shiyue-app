package com.shiyue.reader.core.database

import com.shiyue.reader.core.model.ReadingSession

fun ReadingSessionEntity.asExternalModel(): ReadingSession = ReadingSession.restore(
    id, bookId, state, startedAtEpochMs, endedAtEpochMs, startPage, endPage,
    activeDurationMs, activeSegmentStartedAtEpochMs, activeSegmentStartedAtElapsedRealtimeMs,
    updateBookProgress, createdAt, updatedAt,
)

fun ReadingSession.asEntity() = ReadingSessionEntity(
    id, bookId, state, startedAtEpochMs, endedAtEpochMs, startPage, endPage,
    activeDurationMs, activeSegmentStartedAtEpochMs, activeSegmentStartedAtElapsedRealtimeMs,
    updateBookProgress, createdAt, updatedAt,
)
