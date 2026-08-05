package com.shiyue.reader.testutil

import com.shiyue.reader.core.model.BookSortMode
import com.shiyue.reader.core.model.Category
import com.shiyue.reader.core.model.CategorySummary
import com.shiyue.reader.domain.repository.BookshelfPreferences
import com.shiyue.reader.domain.repository.CaptureTarget
import com.shiyue.reader.domain.repository.CategoryRepository
import com.shiyue.reader.domain.repository.CoverStorage
import com.shiyue.reader.domain.repository.CoverTransform
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

class FakeCategoryRepository(initial: List<Category> = emptyList()) : CategoryRepository {
    val categories = MutableStateFlow(initial.map { CategorySummary(it, 0) })
    override fun observeCategories(): Flow<List<CategorySummary>> = categories
    override suspend fun createCategory(category: Category) {
        check(!nameExists(category.normalizedName)) { "Category name already exists" }
        categories.update { it + CategorySummary(category, 0) }
    }
    override suspend fun renameCategory(categoryId: String, name: String, timestamp: Long) {
        val existing = categories.value.first { it.category.id == categoryId }
        val renamed = existing.category.renamed(name, timestamp)
        check(!nameExists(renamed.normalizedName, categoryId)) { "Category name already exists" }
        categories.update { list -> list.map { if (it.category.id == categoryId) it.copy(category = renamed) else it } }
    }
    override suspend fun deleteCategory(categoryId: String) { categories.update { it.filterNot { item -> item.category.id == categoryId } } }
    override suspend fun moveCategory(categoryId: String, offset: Int, timestamp: Long) {
        val list = categories.value.toMutableList()
        val from = list.indexOfFirst { it.category.id == categoryId }
        val to = (from + offset).coerceIn(list.indices)
        if (from != to) list.add(to, list.removeAt(from))
        categories.value = list.mapIndexed { index, item -> item.copy(category = item.category.reordered(index, timestamp)) }
    }
    override suspend fun nameExists(normalizedName: String, excludingId: String?): Boolean =
        categories.value.any { it.category.normalizedName == normalizedName && it.category.id != excludingId }
}

class FakeBookshelfPreferences(initial: BookSortMode = BookSortMode.UPDATED_DESC) : BookshelfPreferences {
    private val state = MutableStateFlow(initial)
    override val sortMode: Flow<BookSortMode> = state
    override suspend fun setSortMode(mode: BookSortMode) { state.value = mode }
}

open class FakeCoverStorage(
    private val processFailure: Throwable? = null,
) : CoverStorage {
    val deleted = mutableListOf<String>()
    var processCalls = 0
    override suspend fun createCaptureTarget() = CaptureTarget("content://test/capture", "capture.jpg")
    override suspend fun processAndStoreCover(sourceUri: String, transform: CoverTransform): String {
        processCalls++
        processFailure?.let { throw it }
        return "covers/fake-$processCalls.jpg"
    }
    open override suspend fun deleteCover(relativePath: String): Boolean { deleted += relativePath; return true }
    override suspend fun deleteTemporary(token: String) = Unit
    override suspend fun cleanupTemporaryFiles() = Unit
    override suspend fun cleanupOrphanedCovers(referencedPaths: Set<String>) = Unit
}
