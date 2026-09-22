package com.shiyue.reader.feature.note

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.SavedStateHandle
import com.shiyue.reader.app.navigation.ShiyueRoutes
import com.shiyue.reader.domain.usecase.CreateNoteImageCaptureTargetUseCase
import com.shiyue.reader.domain.usecase.DeleteTemporaryNoteImageUseCase
import com.shiyue.reader.domain.usecase.ObserveBookUseCase
import com.shiyue.reader.domain.usecase.SaveNoteUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class CreateNoteViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val observeBook: ObserveBookUseCase,
    private val saveNote: SaveNoteUseCase,
    private val createCaptureTarget: CreateNoteImageCaptureTargetUseCase,
    private val deleteTemporaryImage: DeleteTemporaryNoteImageUseCase,
) : ViewModel() {
    private val bookId: String = checkNotNull(savedStateHandle[ShiyueRoutes.BookIdArgument])
    private val sessionId: String? = savedStateHandle[ShiyueRoutes.SessionIdArgument]
    private val defaultPage: Int? = savedStateHandle.get<String?>(ShiyueRoutes.PageArgument)?.toIntOrNull()

    private val _uiState = MutableStateFlow(NoteFormUiState(pageText = defaultPage?.toString() ?: ""))
    val uiState = _uiState.asStateFlow()
    private val _events = Channel<NoteFormEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    init {
        viewModelScope.launch {
            observeBook(bookId).collect { book ->
                _uiState.update { it.copy(isLoading = false, bookTitle = book?.title ?: "") }
            }
        }
    }

    fun onPageChanged(value: String) = _uiState.update { it.copy(pageText = value, failed = false) }
    fun onContentChanged(value: String) = _uiState.update { it.copy(content = value, failed = false) }

    fun onImagePicked(uri: String) = _uiState.update { it.copy(imageSource = NoteImageSource.Uri(uri)) }

    fun onRemoveImage() {
        cleanupCapture()
        _uiState.update { it.copy(imageSource = NoteImageSource.None) }
    }

    fun requestCamera() {
        viewModelScope.launch {
            runCatching { createCaptureTarget() }
                .onSuccess { target ->
                    _uiState.update { it.copy(captureToken = target.token, captureUri = target.uri) }
                    _events.send(NoteFormEvent.LaunchCamera(target.uri))
                }
                .onFailure { _uiState.update { it.copy(failed = true) } }
        }
    }

    fun onCameraResult(success: Boolean) {
        val current = _uiState.value
        if (success && current.captureUri != null) {
            _uiState.update { it.copy(imageSource = NoteImageSource.Uri(current.captureUri)) }
        } else {
            cleanupCapture()
        }
    }

    fun save() {
        val current = _uiState.value
        if (current.isSaving) return
        val content = current.content.trim()
        val page = current.pageText.trim().toIntOrNull()
        if (content.isEmpty() || (current.pageText.isNotBlank() && page == null)) return
        _uiState.update { it.copy(isSaving = true, failed = false) }
        viewModelScope.launch {
            runCatching {
                saveNote(
                    noteId = null,
                    bookId = bookId,
                    sessionId = sessionId,
                    pageNumber = page,
                    content = content,
                    newImageUri = (current.imageSource as? NoteImageSource.Uri)?.value,
                    removeImage = current.imageSource is NoteImageSource.None,
                )
            }.onSuccess {
                current.captureToken?.let { deleteTemporaryImage(it) }
                _uiState.update { it.copy(isSaving = false) }
                _events.send(NoteFormEvent.Saved)
            }.onFailure { _uiState.update { it.copy(isSaving = false, failed = true) } }
        }
    }

    fun discard() {
        cleanupCapture()
    }

    private fun cleanupCapture() {
        val token = _uiState.value.captureToken ?: return
        _uiState.update { it.copy(captureToken = null, captureUri = null) }
        viewModelScope.launch { deleteTemporaryImage(token) }
    }
}
