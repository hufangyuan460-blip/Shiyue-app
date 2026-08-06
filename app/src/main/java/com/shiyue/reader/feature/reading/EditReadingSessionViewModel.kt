package com.shiyue.reader.feature.reading

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shiyue.reader.app.navigation.ShiyueRoutes
import com.shiyue.reader.core.model.ReadingSession
import com.shiyue.reader.core.model.ReadingSessionState
import com.shiyue.reader.domain.usecase.CorrectReadingSessionUseCase
import com.shiyue.reader.domain.usecase.ObserveReadingSessionUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class EditReadingSessionUiState(
    val isLoading: Boolean = true,
    val session: ReadingSession? = null,
    val startedAt: String = "",
    val endedAt: String = "",
    val durationMinutes: String = "",
    val startPage: String = "",
    val endPage: String = "",
    val hasError: Boolean = false,
    val isSaving: Boolean = false,
    val failed: Boolean = false,
    val notFound: Boolean = false,
)

sealed interface EditReadingSessionEvent { data object Saved : EditReadingSessionEvent }

@HiltViewModel
class EditReadingSessionViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    observeSession: ObserveReadingSessionUseCase,
    private val correct: CorrectReadingSessionUseCase,
) : ViewModel() {
    private val id: String? = savedStateHandle[ShiyueRoutes.SessionIdArgument]
    private val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
    private val zone = ZoneId.systemDefault()
    private val _uiState = MutableStateFlow(EditReadingSessionUiState())
    val uiState = _uiState.asStateFlow()
    private val _events = Channel<EditReadingSessionEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    init {
        val sessionId = id
        if (sessionId == null) _uiState.value = EditReadingSessionUiState(isLoading = false, notFound = true)
        else viewModelScope.launch {
            observeSession(sessionId).collect { session ->
                if (session == null || session.state != ReadingSessionState.COMPLETED) {
                    _uiState.update { it.copy(isLoading = false, notFound = true) }
                } else if (_uiState.value.session == null) {
                    _uiState.value = EditReadingSessionUiState(
                        isLoading = false, session = session,
                        startedAt = format(session.startedAtEpochMs), endedAt = format(requireNotNull(session.endedAtEpochMs)),
                        durationMinutes = (session.activeDurationMs / 60_000L).toString(),
                        startPage = session.startPage.toString(), endPage = requireNotNull(session.endPage).toString(),
                    )
                }
            }
        }
    }

    fun updateStartedAt(value: String) = change { copy(startedAt = value) }
    fun updateEndedAt(value: String) = change { copy(endedAt = value) }
    fun updateDuration(value: String) = change { copy(durationMinutes = value) }
    fun updateStartPage(value: String) = change { copy(startPage = value) }
    fun updateEndPage(value: String) = change { copy(endPage = value) }
    private fun change(block: EditReadingSessionUiState.() -> EditReadingSessionUiState) =
        _uiState.update { it.block().copy(hasError = false, failed = false) }

    fun save() {
        val state = _uiState.value
        if (state.isSaving) return
        val started = parse(state.startedAt)
        val ended = parse(state.endedAt)
        val minutes = state.durationMinutes.toLongOrNull()
        val startPage = state.startPage.toIntOrNull()
        val endPage = state.endPage.toIntOrNull()
        if (started == null || ended == null || ended < started || minutes == null || minutes < 0 ||
            startPage == null || startPage < 0 || endPage == null || endPage < 0 || minutes * 60_000L > ended - started + 1_000L
        ) {
            _uiState.update { it.copy(hasError = true) }
            return
        }
        val sessionId = id ?: return
        _uiState.update { it.copy(isSaving = true, failed = false) }
        viewModelScope.launch {
            runCatching { correct(sessionId, started, ended, minutes * 60_000L, startPage, endPage) }
                .onSuccess { _events.send(EditReadingSessionEvent.Saved) }
                .onFailure { _uiState.update { it.copy(isSaving = false, failed = true) } }
        }
    }

    private fun format(value: Long): String = Instant.ofEpochMilli(value).atZone(zone).format(formatter)
    private fun parse(value: String): Long? = runCatching {
        java.time.LocalDateTime.parse(value.trim(), formatter).atZone(zone).toInstant().toEpochMilli()
    }.getOrNull()
}
