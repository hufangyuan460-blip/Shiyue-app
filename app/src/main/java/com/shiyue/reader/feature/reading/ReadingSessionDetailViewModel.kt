package com.shiyue.reader.feature.reading

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shiyue.reader.app.navigation.ShiyueRoutes
import com.shiyue.reader.core.model.ReadingSession
import com.shiyue.reader.core.model.ReadingSessionState
import com.shiyue.reader.domain.usecase.DeleteReadingSessionUseCase
import com.shiyue.reader.domain.usecase.ObserveReadingSessionUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ReadingSessionDetailUiState(
    val isLoading: Boolean = true,
    val session: ReadingSession? = null,
    val notFound: Boolean = false,
    val isDeleting: Boolean = false,
    val failed: Boolean = false,
)

sealed interface ReadingSessionDetailEvent { data object Deleted : ReadingSessionDetailEvent }

@HiltViewModel
class ReadingSessionDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    observeSession: ObserveReadingSessionUseCase,
    private val deleteSession: DeleteReadingSessionUseCase,
) : ViewModel() {
    private val id: String? = savedStateHandle[ShiyueRoutes.SessionIdArgument]
    private val _uiState = MutableStateFlow(ReadingSessionDetailUiState())
    val uiState = _uiState.asStateFlow()
    private val _events = Channel<ReadingSessionDetailEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()
    init {
        val sessionId = id
        if (sessionId == null) _uiState.value = ReadingSessionDetailUiState(isLoading = false, notFound = true)
        else viewModelScope.launch {
            observeSession(sessionId).collect { session ->
                _uiState.update { it.copy(isLoading = false, session = session, notFound = session == null) }
            }
        }
    }
    fun delete() {
        val session = _uiState.value.session ?: return
        if (_uiState.value.isDeleting || session.state != ReadingSessionState.COMPLETED) return
        _uiState.update { it.copy(isDeleting = true, failed = false) }
        viewModelScope.launch {
            runCatching { deleteSession(session.id) }.onSuccess { _events.send(ReadingSessionDetailEvent.Deleted) }
                .onFailure { _uiState.update { it.copy(isDeleting = false, failed = true) } }
        }
    }
}
