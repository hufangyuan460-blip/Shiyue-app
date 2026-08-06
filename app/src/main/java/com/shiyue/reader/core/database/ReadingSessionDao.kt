package com.shiyue.reader.core.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ReadingSessionDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertSession(session: ReadingSessionEntity)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertActiveSlot(slot: ActiveReadingSessionEntity)

    @Update(onConflict = OnConflictStrategy.ABORT)
    suspend fun updateSession(session: ReadingSessionEntity): Int

    @Query("SELECT * FROM reading_sessions WHERE id = :id LIMIT 1")
    suspend fun getSession(id: String): ReadingSessionEntity?

    @Query("SELECT * FROM reading_sessions WHERE id = :id LIMIT 1")
    fun observeSession(id: String): Flow<ReadingSessionEntity?>

    @Query("SELECT reading_sessions.* FROM reading_sessions INNER JOIN active_reading_session ON reading_sessions.id = active_reading_session.session_id WHERE active_reading_session.slot = 1 LIMIT 1")
    suspend fun getActiveSession(): ReadingSessionEntity?

    @Query("SELECT reading_sessions.* FROM reading_sessions INNER JOIN active_reading_session ON reading_sessions.id = active_reading_session.session_id WHERE active_reading_session.slot = 1 LIMIT 1")
    fun observeActiveSession(): Flow<ReadingSessionEntity?>

    @Query("SELECT * FROM reading_sessions WHERE state IN ('ACTIVE', 'PAUSED') ORDER BY started_at_epoch_ms DESC")
    suspend fun getUnfinishedSessions(): List<ReadingSessionEntity>

    @Query("SELECT COUNT(*) FROM reading_sessions WHERE book_id = :bookId AND state IN ('ACTIVE', 'PAUSED')")
    suspend fun unfinishedCountForBook(bookId: String): Int

    @Query("DELETE FROM active_reading_session WHERE slot = 1 AND session_id = :sessionId")
    suspend fun deleteActiveSlot(sessionId: String): Int

    @Query("SELECT * FROM reading_sessions WHERE book_id = :bookId AND state = 'COMPLETED' ORDER BY started_at_epoch_ms DESC, id ASC")
    fun observeCompletedForBook(bookId: String): Flow<List<ReadingSessionEntity>>

    @Query("SELECT * FROM reading_sessions WHERE state = 'COMPLETED' ORDER BY started_at_epoch_ms DESC, id ASC")
    fun observeAllCompleted(): Flow<List<ReadingSessionEntity>>

    @Query("DELETE FROM reading_sessions WHERE id = :id AND state = 'COMPLETED'")
    suspend fun deleteCompleted(id: String): Int
}
