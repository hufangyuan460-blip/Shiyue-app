package com.shiyue.reader.feature.bookedit

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shiyue.reader.app.navigation.ShiyueRoutes
import com.shiyue.reader.core.model.BookStatus
import com.shiyue.reader.core.model.CategorySummary
import com.shiyue.reader.domain.repository.CoverTransform
import com.shiyue.reader.domain.usecase.CoverChange
import com.shiyue.reader.domain.usecase.CreateCaptureTargetUseCase
import com.shiyue.reader.domain.usecase.CreateCategoryUseCase
import com.shiyue.reader.domain.usecase.DeleteTemporaryCoverUseCase
import com.shiyue.reader.domain.usecase.ObserveCategoriesUseCase
import com.shiyue.reader.domain.usecase.ObserveLibraryBookUseCase
import com.shiyue.reader.domain.usecase.PendingCover
import com.shiyue.reader.domain.usecase.UpdateBookWithCoverUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class EditBookUiState(
    val isLoading: Boolean = true,
    val notFound: Boolean = false,
    val loadFailed: Boolean = false,
    val title: String = "",
    val author: String = "",
    val totalPages: String = "",
    val status: BookStatus = BookStatus.READING,
    val currentPage: Int = 0,
    val coverPath: String? = null,
    val categories: List<CategorySummary> = emptyList(),
    val selectedCategoryIds: Set<String> = emptySet(),
    val newCategoryName: String = "",
    val categoryError: Boolean = false,
    val pendingCover: PendingCover? = null,
    val removeCover: Boolean = false,
    val cropSourceUri: String? = null,
    val cropTransform: CoverTransform = CoverTransform(),
    val captureToken: String? = null,
    val captureUri: String? = null,
    val titleError: Boolean = false,
    val totalPagesError: EditTotalPagesError? = null,
    val saveFailed: Boolean = false,
    val isSaving: Boolean = false,
    val isDirty: Boolean = false,
    val initialized: Boolean = false,
)

enum class EditTotalPagesError { INVALID, BELOW_CURRENT_PAGE }

sealed interface EditBookEvent {
    data object Saved : EditBookEvent
    data class LaunchCamera(val uri: String) : EditBookEvent
}

@HiltViewModel
class EditBookViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val observeBook: ObserveLibraryBookUseCase,
    observeCategories: ObserveCategoriesUseCase,
    private val updateBook: UpdateBookWithCoverUseCase,
    private val createCategory: CreateCategoryUseCase,
    private val createCaptureTarget: CreateCaptureTargetUseCase,
    private val deleteTemporaryCover: DeleteTemporaryCoverUseCase,
) : ViewModel() {
    private val bookId: String? = savedStateHandle[ShiyueRoutes.BookIdArgument]
    private val _uiState = MutableStateFlow(EditBookUiState())
    val uiState = _uiState.asStateFlow()
    private val _events = Channel<EditBookEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()
    private var loadJob: Job? = null

    init {
        viewModelScope.launch {
            observeCategories().catch { _uiState.update { it.copy(categoryError = true) } }
                .collect { categories -> _uiState.update { it.copy(categories = categories) } }
        }
        retry()
    }

    fun retry() {
        val id = bookId
        if (id.isNullOrBlank()) {
            _uiState.update { it.copy(isLoading = false, notFound = true) }
            return
        }
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, loadFailed = false) }
            observeBook(id).catch { _uiState.update { it.copy(isLoading = false, loadFailed = true) } }
                .collect { libraryBook ->
                    if (libraryBook == null) {
                        _uiState.update { it.copy(isLoading = false, notFound = true) }
                    } else if (!_uiState.value.isDirty) {
                        val book = libraryBook.book
                        _uiState.update {
                            it.copy(
                                isLoading = false, notFound = false, title = book.title,
                                author = book.author.orEmpty(), totalPages = book.totalPages.toString(),
                                status = book.status, currentPage = book.currentPage, coverPath = book.coverPath,
                                selectedCategoryIds = libraryBook.categoryIds, initialized = true,
                            )
                        }
                    }
                }
        }
    }

    fun onTitleChanged(v: String) = edit { it.copy(title = v, titleError = false) }
    fun onAuthorChanged(v: String) = edit { it.copy(author = v) }
    fun onTotalPagesChanged(v: String) = edit { it.copy(totalPages = v, totalPagesError = null) }
    fun onStatusChanged(v: BookStatus) = edit { it.copy(status = v) }
    fun toggleCategory(id: String) = edit {
        it.copy(selectedCategoryIds = if (id in it.selectedCategoryIds) it.selectedCategoryIds - id else it.selectedCategoryIds + id)
    }
    fun onNewCategoryNameChanged(v: String) = _uiState.update { it.copy(newCategoryName = v, categoryError = false) }
    private fun edit(block: (EditBookUiState) -> EditBookUiState) {
        if (_uiState.value.isSaving || !_uiState.value.initialized) return
        _uiState.update { block(it).copy(isDirty = true, saveFailed = false) }
    }

    fun createCategory() {
        val current = _uiState.value
        if (current.newCategoryName.isBlank()) return
        viewModelScope.launch {
            runCatching { createCategory(current.newCategoryName, current.categories.size) }
                .onSuccess { category -> edit { it.copy(newCategoryName = "", selectedCategoryIds = it.selectedCategoryIds + category.id) } }
                .onFailure { _uiState.update { it.copy(categoryError = true) } }
        }
    }

    fun requestCamera() = viewModelScope.launch {
        runCatching { createCaptureTarget() }.onSuccess { target ->
            _uiState.update { it.copy(captureToken = target.token, captureUri = target.uri) }
            _events.send(EditBookEvent.LaunchCamera(target.uri))
        }.onFailure { _uiState.update { it.copy(saveFailed = true) } }
    }
    fun onCameraResult(success: Boolean) {
        val current = _uiState.value
        if (success && current.captureUri != null) onCoverSourceSelected(current.captureUri) else cleanupCapture()
    }
    fun onCoverSourceSelected(uri: String) = _uiState.update { it.copy(cropSourceUri = uri, cropTransform = CoverTransform()) }
    fun onCropTransformChanged(v: CoverTransform) = _uiState.update { it.copy(cropTransform = v) }
    fun confirmCrop() = edit {
        val source = it.cropSourceUri ?: return@edit it
        it.copy(pendingCover = PendingCover(source, it.cropTransform), removeCover = false, cropSourceUri = null)
    }
    fun cancelCrop() { _uiState.update { it.copy(cropSourceUri = null) }; cleanupCapture() }
    fun removeCover() = edit { it.copy(pendingCover = null, removeCover = true) }

    fun save() {
        val current = _uiState.value
        if (current.isSaving || !current.initialized) return
        val pages = current.totalPages.toIntOrNull()
        val titleInvalid = current.title.trim().isEmpty()
        val pagesError = when {
            pages == null || pages <= 0 -> EditTotalPagesError.INVALID
            pages < current.currentPage -> EditTotalPagesError.BELOW_CURRENT_PAGE
            else -> null
        }
        if (titleInvalid || pagesError != null) {
            _uiState.update { it.copy(titleError = titleInvalid, totalPagesError = pagesError) }
            return
        }
        val coverChange = when {
            current.pendingCover != null -> CoverChange.Replace(current.pendingCover)
            current.removeCover -> CoverChange.Remove
            else -> CoverChange.Keep
        }
        _uiState.update { it.copy(isSaving = true, saveFailed = false) }
        viewModelScope.launch {
            runCatching {
                updateBook(
                    requireNotNull(bookId), current.title, current.author, requireNotNull(pages), current.status,
                    current.selectedCategoryIds, coverChange,
                )
            }.onSuccess {
                current.captureToken?.let { deleteTemporaryCover(it) }
                _uiState.update { it.copy(isSaving = false, isDirty = false) }
                _events.send(EditBookEvent.Saved)
            }.onFailure { _uiState.update { it.copy(isSaving = false, saveFailed = true) } }
        }
    }

    fun discardChanges() { cleanupCapture() }

    private fun cleanupCapture() {
        val token = _uiState.value.captureToken ?: return
        _uiState.update { it.copy(captureToken = null, captureUri = null) }
        viewModelScope.launch { deleteTemporaryCover(token) }
    }
}
