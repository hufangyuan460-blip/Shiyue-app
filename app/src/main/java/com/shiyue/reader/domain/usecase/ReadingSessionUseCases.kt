package com.shiyue.reader.domain.usecase

import com.shiyue.reader.domain.repository.BookProgressUpdate
import com.shiyue.reader.domain.repository.ReadingSessionRepository
import javax.inject.Inject

class ObserveActiveReadingUseCase @Inject constructor(private val repository: ReadingSessionRepository) {
    operator fun invoke() = repository.observeActiveSession()
}

class ObserveReadingSessionUseCase @Inject constructor(private val repository: ReadingSessionRepository) {
    operator fun invoke(id: String) = repository.observeSession(id)
}

class ObserveBookReadingHistoryUseCase @Inject constructor(private val repository: ReadingSessionRepository) {
    operator fun invoke(bookId: String) = repository.observeCompletedSessions(bookId)
}

class ObserveBookReadingSummaryUseCase @Inject constructor(private val repository: ReadingSessionRepository) {
    operator fun invoke(bookId: String) = repository.observeHistorySummary(bookId)
}

class ObserveReadingTimesUseCase @Inject constructor(private val repository: ReadingSessionRepository) {
    operator fun invoke() = repository.observeReadingTimes()
}

class ObserveReviewStatisticsUseCase @Inject constructor(private val repository: ReadingSessionRepository) {
    operator fun invoke() = repository.observeReviewStatistics()
}

class InspectActiveReadingUseCase @Inject constructor(private val repository: ReadingSessionRepository) {
    suspend operator fun invoke() = repository.inspectActiveSession()
}

class StartReadingUseCase @Inject constructor(private val repository: ReadingSessionRepository) {
    suspend operator fun invoke(bookId: String, startPage: Int, switchToReading: Boolean) =
        repository.startReading(bookId, startPage, switchToReading)
}

class PauseReadingUseCase @Inject constructor(private val repository: ReadingSessionRepository) {
    suspend operator fun invoke(sessionId: String) = repository.pause(sessionId)
}

class ResumeReadingUseCase @Inject constructor(private val repository: ReadingSessionRepository) {
    suspend operator fun invoke(sessionId: String) = repository.resume(sessionId)
}

class CompleteReadingUseCase @Inject constructor(private val repository: ReadingSessionRepository) {
    suspend operator fun invoke(sessionId: String, endPage: Int, progressUpdate: BookProgressUpdate) =
        repository.complete(sessionId, endPage, progressUpdate)
}

class DiscardReadingUseCase @Inject constructor(private val repository: ReadingSessionRepository) {
    suspend operator fun invoke(sessionId: String) = repository.discard(sessionId)
}

class RecoverReadingUseCase @Inject constructor(private val repository: ReadingSessionRepository) {
    suspend fun continueWith(sessionId: String, acceptedEstimatedMs: Long) =
        repository.recoverAndContinue(sessionId, acceptedEstimatedMs)
    suspend fun replaceDuration(sessionId: String, durationMs: Long) =
        repository.replaceReliableDuration(sessionId, durationMs)
}

class CorrectReadingSessionUseCase @Inject constructor(private val repository: ReadingSessionRepository) {
    suspend operator fun invoke(
        sessionId: String,
        startedAtEpochMs: Long,
        endedAtEpochMs: Long,
        activeDurationMs: Long,
        startPage: Int,
        endPage: Int,
    ) = repository.correctCompletedSession(
        sessionId, startedAtEpochMs, endedAtEpochMs, activeDurationMs, startPage, endPage,
    )
}

class DeleteReadingSessionUseCase @Inject constructor(private val repository: ReadingSessionRepository) {
    suspend operator fun invoke(sessionId: String) = repository.deleteCompletedSession(sessionId)
}
