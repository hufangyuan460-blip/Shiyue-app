package com.shiyue.reader.core.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.shiyue.reader.core.model.ReadingSessionState

@Entity(
    tableName = "reading_sessions",
    foreignKeys = [ForeignKey(
        entity = BookEntity::class,
        parentColumns = ["id"],
        childColumns = ["book_id"],
        onDelete = ForeignKey.CASCADE,
    )],
    indices = [
        Index("book_id"),
        Index("state"),
        Index("started_at_epoch_ms"),
        Index(value = ["book_id", "started_at_epoch_ms"]),
    ],
)
data class ReadingSessionEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "book_id") val bookId: String,
    val state: ReadingSessionState,
    @ColumnInfo(name = "started_at_epoch_ms") val startedAtEpochMs: Long,
    @ColumnInfo(name = "ended_at_epoch_ms") val endedAtEpochMs: Long?,
    @ColumnInfo(name = "start_page") val startPage: Int,
    @ColumnInfo(name = "end_page") val endPage: Int?,
    @ColumnInfo(name = "active_duration_ms") val activeDurationMs: Long,
    @ColumnInfo(name = "active_segment_started_at_epoch_ms") val activeSegmentStartedAtEpochMs: Long?,
    @ColumnInfo(name = "active_segment_started_at_elapsed_realtime_ms") val activeSegmentStartedAtElapsedRealtimeMs: Long?,
    @ColumnInfo(name = "update_book_progress") val updateBookProgress: Boolean,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
)

@Entity(
    tableName = "active_reading_session",
    foreignKeys = [ForeignKey(
        entity = ReadingSessionEntity::class,
        parentColumns = ["id"],
        childColumns = ["session_id"],
        onDelete = ForeignKey.CASCADE,
    )],
    indices = [Index(value = ["session_id"], unique = true)],
)
data class ActiveReadingSessionEntity(
    @PrimaryKey val slot: Int = SINGLE_ACTIVE_SLOT,
    @ColumnInfo(name = "session_id") val sessionId: String,
) {
    init { require(slot == SINGLE_ACTIVE_SLOT) }
    companion object { const val SINGLE_ACTIVE_SLOT = 1 }
}
