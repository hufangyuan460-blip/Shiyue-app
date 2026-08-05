package com.shiyue.reader.feature.category

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shiyue.reader.core.model.CategorySummary
import com.shiyue.reader.domain.usecase.CreateCategoryUseCase
import com.shiyue.reader.domain.usecase.DeleteCategoryUseCase
import com.shiyue.reader.domain.usecase.MoveCategoryUseCase
import com.shiyue.reader.domain.usecase.ObserveCategoriesUseCase
import com.shiyue.reader.domain.usecase.RenameCategoryUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface CategoryDialog {
    data object Create : CategoryDialog
    data class Rename(val id: String, val currentName: String) : CategoryDialog
    data class Delete(val id: String, val name: String) : CategoryDialog
}

data class CategoryManagerUiState(
    val isLoading: Boolean = true,
    val categories: List<CategorySummary> = emptyList(),
    val dialog: CategoryDialog? = null,
    val nameInput: String = "",
    val nameError: Boolean = false,
    val conflictError: Boolean = false,
    val actionFailed: Boolean = false,
    val isSaving: Boolean = false,
)

@HiltViewModel
class CategoryManagerViewModel @Inject constructor(
    observeCategories: ObserveCategoriesUseCase,
    private val createCategory: CreateCategoryUseCase,
    private val renameCategory: RenameCategoryUseCase,
    private val deleteCategory: DeleteCategoryUseCase,
    private val moveCategory: MoveCategoryUseCase,
) : ViewModel() {
    private val _uiState = MutableStateFlow(CategoryManagerUiState())
    val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            observeCategories().catch {
                _uiState.update { it.copy(isLoading = false, actionFailed = true) }
            }.collect { categories ->
                _uiState.update { it.copy(isLoading = false, categories = categories) }
            }
        }
    }

    fun showCreate() = _uiState.update { it.copy(dialog = CategoryDialog.Create, nameInput = "", actionFailed = false) }
    fun showRename(item: CategorySummary) = _uiState.update {
        it.copy(dialog = CategoryDialog.Rename(item.category.id, item.category.name), nameInput = item.category.name)
    }
    fun showDelete(item: CategorySummary) = _uiState.update {
        it.copy(dialog = CategoryDialog.Delete(item.category.id, item.category.name))
    }
    fun dismissDialog() = _uiState.update { it.copy(dialog = null, isSaving = false) }
    fun onNameChanged(value: String) = _uiState.update {
        it.copy(nameInput = value, nameError = false, conflictError = false, actionFailed = false)
    }

    fun confirmName() {
        val current = _uiState.value
        if (current.isSaving) return
        if (current.nameInput.trim().isEmpty() || current.nameInput.trim().codePointCount(0, current.nameInput.trim().length) > 20) {
            _uiState.update { it.copy(nameError = true) }
            return
        }
        val dialog = current.dialog ?: return
        _uiState.update { it.copy(isSaving = true, actionFailed = false, conflictError = false) }
        viewModelScope.launch {
            runCatching {
                when (dialog) {
                    CategoryDialog.Create -> createCategory(current.nameInput, current.categories.size)
                    is CategoryDialog.Rename -> renameCategory(dialog.id, current.nameInput)
                    is CategoryDialog.Delete -> error("Delete does not use name confirmation")
                }
            }.onSuccess {
                _uiState.update { it.copy(dialog = null, isSaving = false) }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        conflictError = error is IllegalStateException && error.message?.contains("already exists") == true,
                        actionFailed = error !is IllegalArgumentException && error.message?.contains("already exists") != true,
                        nameError = error is IllegalArgumentException,
                    )
                }
            }
        }
    }

    fun confirmDelete() {
        val dialog = _uiState.value.dialog as? CategoryDialog.Delete ?: return
        if (_uiState.value.isSaving) return
        _uiState.update { it.copy(isSaving = true, actionFailed = false) }
        viewModelScope.launch {
            runCatching { deleteCategory(dialog.id) }
                .onSuccess { _uiState.update { it.copy(dialog = null, isSaving = false) } }
                .onFailure { _uiState.update { it.copy(isSaving = false, actionFailed = true) } }
        }
    }

    fun move(id: String, offset: Int) {
        if (_uiState.value.isSaving) return
        viewModelScope.launch {
            runCatching { moveCategory(id, offset) }
                .onFailure { _uiState.update { it.copy(actionFailed = true) } }
        }
    }
}
