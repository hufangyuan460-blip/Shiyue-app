package com.shiyue.reader.feature.bookedit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shiyue.reader.core.model.BookStatus
import com.shiyue.reader.core.model.CategorySummary
import com.shiyue.reader.domain.repository.CoverTransform
import com.shiyue.reader.domain.usecase.AddBookWithCoverUseCase
import com.shiyue.reader.domain.usecase.CreateCaptureTargetUseCase
import com.shiyue.reader.domain.usecase.CreateCategoryUseCase
import com.shiyue.reader.domain.usecase.DeleteTemporaryCoverUseCase
import com.shiyue.reader.domain.usecase.ObserveCategoriesUseCase
import com.shiyue.reader.domain.usecase.PendingCover
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AddBookUiState(
    val title: String = "",
    val author: String = "",
    val totalPages: String = "",
    val status: BookStatus = BookStatus.READING,
    val categories: List<CategorySummary> = emptyList(),
    val selectedCategoryIds: Set<String> = emptySet(),
    val newCategoryName: String = "",
    val categoryError: Boolean = false,
    val titleError: Boolean = false,
    val totalPagesError: Boolean = false,
    val saveFailed: Boolean = false,
    val isSaving: Boolean = false,
    val cropSourceUri: String? = null,
    val cropTransform: CoverTransform = CoverTransform(),
    val pendingCover: PendingCover? = null,
    val captureToken: String? = null,
    val captureUri: String? = null,
)

sealed interface AddBookEvent {
    data object Saved : AddBookEvent
    data class LaunchCamera(val uri: String) : AddBookEvent
}

@HiltViewModel
class AddBookViewModel @Inject constructor(
    private val addBook: AddBookWithCoverUseCase,
    observeCategories: ObserveCategoriesUseCase,
    private val createCategory: CreateCategoryUseCase,
    private val createCaptureTarget: CreateCaptureTargetUseCase,
    private val deleteTemporaryCover: DeleteTemporaryCoverUseCase,
) : ViewModel() {
    private val _uiState = MutableStateFlow(AddBookUiState())
    val uiState = _uiState.asStateFlow()
    private val _events = Channel<AddBookEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    init {
        viewModelScope.launch {
            observeCategories().catch { _uiState.update { it.copy(categoryError = true) } }
                .collect { categories -> _uiState.update { it.copy(categories = categories) } }
        }
    }

    fun onTitleChanged(value: String) = _uiState.update { it.copy(title = value, titleError = false, saveFailed = false) }
    fun onAuthorChanged(value: String) = _uiState.update { it.copy(author = value, saveFailed = false) }
    fun onTotalPagesChanged(value: String) = _uiState.update { it.copy(totalPages = value, totalPagesError = false, saveFailed = false) }
    fun onStatusChanged(value: BookStatus) = _uiState.update { it.copy(status = value, saveFailed = false) }
    fun toggleCategory(id: String) = _uiState.update {
        it.copy(selectedCategoryIds = if (id in it.selectedCategoryIds) it.selectedCategoryIds - id else it.selectedCategoryIds + id)
    }
    fun onNewCategoryNameChanged(value: String) = _uiState.update { it.copy(newCategoryName = value, categoryError = false) }

    fun createCategory() {
        val current = _uiState.value
        if (current.newCategoryName.isBlank() || current.isSaving) return
        viewModelScope.launch {
            runCatching { createCategory(current.newCategoryName, current.categories.size) }
                .onSuccess { category ->
                    _uiState.update { it.copy(newCategoryName = "", selectedCategoryIds = it.selectedCategoryIds + category.id) }
                }.onFailure { _uiState.update { it.copy(categoryError = true) } }
        }
    }

    fun requestCamera() {
        viewModelScope.launch {
            runCatching { createCaptureTarget() }.onSuccess { target ->
                _uiState.update { it.copy(captureToken = target.token, captureUri = target.uri) }
                _events.send(AddBookEvent.LaunchCamera(target.uri))
            }.onFailure { _uiState.update { it.copy(saveFailed = true) } }
        }
    }

    fun onCameraResult(success: Boolean) {
        val current = _uiState.value
        if (success && current.captureUri != null) onCoverSourceSelected(current.captureUri)
        else cleanupCapture()
    }

    fun onCoverSourceSelected(uri: String) = _uiState.update {
        it.copy(cropSourceUri = uri, cropTransform = CoverTransform())
    }
    fun onCropTransformChanged(value: CoverTransform) = _uiState.update { it.copy(cropTransform = value) }
    fun confirmCrop() = _uiState.update {
        val source = it.cropSourceUri ?: return@update it
        it.copy(pendingCover = PendingCover(source, it.cropTransform), cropSourceUri = null)
    }
    fun cancelCrop() {
        _uiState.update { it.copy(cropSourceUri = null) }
        cleanupCapture()
    }
    fun removePendingCover() {
        _uiState.update { it.copy(pendingCover = null) }
        cleanupCapture()
    }

    fun discardChanges() {
        cleanupCapture()
    }

    fun save() {
        val current = _uiState.value
        if (current.isSaving) return
        val pages = current.totalPages.toIntOrNull()
        val titleInvalid = current.title.trim().isEmpty()
        val pagesInvalid = pages == null || pages <= 0
        if (titleInvalid || pagesInvalid) {
            _uiState.update { it.copy(titleError = titleInvalid, totalPagesError = pagesInvalid) }
            return
        }
        _uiState.update { it.copy(isSaving = true, saveFailed = false) }
        viewModelScope.launch {
            runCatching {
                addBook(
                    current.title,
                    current.author,
                    requireNotNull(pages),
                    current.status,
                    current.selectedCategoryIds,
                    current.pendingCover,
                )
            }.onSuccess {
                current.captureToken?.let { deleteTemporaryCover(it) }
                _uiState.update { it.copy(isSaving = false) }
                _events.send(AddBookEvent.Saved)
            }.onFailure { _uiState.update { it.copy(isSaving = false, saveFailed = true) } }
        }
    }

    private fun cleanupCapture() {
        val token = _uiState.value.captureToken ?: return
        _uiState.update { it.copy(captureToken = null, captureUri = null) }
        viewModelScope.launch { deleteTemporaryCover(token) }
    }
}
