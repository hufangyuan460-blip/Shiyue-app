package com.shiyue.reader.feature.reading

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shiyue.reader.app.navigation.ShiyueRoutes
import com.shiyue.reader.domain.repository.ActiveSessionInspection
import com.shiyue.reader.domain.usecase.DiscardReadingUseCase
import com.shiyue.reader.domain.usecase.InspectActiveReadingUseCase
import com.shiyue.reader.domain.usecase.RecoverReadingUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class RecoverReadingUiState(
    val isLoading: Boolean = true,
    val inspection: ActiveSessionInspection? = null,
    val manualHours: String = "",
    val manualMinutes: String = "",
    val inputError: Boolean = false,
    val isWorking: Boolean = false,
    val failed: Boolean = false,
)

sealed interface RecoverReadingEvent {
    data object Active : RecoverReadingEvent
    data object Finish : RecoverReadingEvent
    data object Discarded : RecoverReadingEvent
}

@HiltViewModel
class RecoverReadingViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val inspect: InspectActiveReadingUseCase,
    private val recover: RecoverReadingUseCase,
    private val discard: DiscardReadingUseCase,
) : ViewModel() {
    private val sessionId: String? = savedStateHandle[ShiyueRoutes.SessionIdArgument]
    private val _uiState = MutableStateFlow(RecoverReadingUiState())
    val uiState = _uiState.asStateFlow()
    private val _events = Channel<RecoverReadingEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    init { refresh() }

    fun refresh() = viewModelScope.launch {
        runCatching { inspect() }.onSuccess { result ->
            _uiState.value = RecoverReadingUiState(isLoading = false, inspection = result)
        }.onFailure { _uiState.update { it.copy(isLoading = false, failed = true) } }
    }

    fun onHoursChanged(value: String) = _uiState.update { it.copy(manualHours = value, inputError = false) }
    fun onMinutesChanged(value: String) = _uiState.update { it.copy(manualMinutes = value, inputError = false) }

    fun continueReliable() = continueWith(0)
    fun continueWithEstimate() = continueWith(_uiState.value.inspection?.duration?.estimatedSegmentMs ?: 0)

    private fun continueWith(segmentMs: Long) = work {
        recover.continueWith(requireNotNull(sessionId), segmentMs)
        _events.send(RecoverReadingEvent.Active)
    }

    fun useManualDuration() {
        val hours = _uiState.value.manualHours.toLongOrNull() ?: 0
        val minutes = _uiState.value.manualMinutes.toLongOrNull() ?: 0
        if (hours < 0 || minutes !in 0..59 || (hours == 0L && minutes == 0L && _uiState.value.manualHours.isBlank() && _uiState.value.manualMinutes.isBlank())) {
            _uiState.update { it.copy(inputError = true) }
            return
        }
        work {
            recover.replaceDuration(requireNotNull(sessionId), (hours * 60 + minutes) * 60_000L)
            _events.send(RecoverReadingEvent.Finish)
        }
    }

    fun discard() = work {
        discard(requireNotNull(sessionId))
        _events.send(RecoverReadingEvent.Discarded)
    }

    private fun work(block: suspend () -> Unit) {
        if (_uiState.value.isWorking) return
        _uiState.update { it.copy(isWorking = true, failed = false) }
        viewModelScope.launch {
            runCatching { block() }.onFailure { _uiState.update { it.copy(isWorking = false, failed = true) } }
        }
    }
}
