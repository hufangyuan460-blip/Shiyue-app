package com.shiyue.reader.core.data

import com.shiyue.reader.core.model.BookSortMode
import com.shiyue.reader.core.model.Category
import com.shiyue.reader.core.model.CategorySummary
import com.shiyue.reader.domain.repository.BookshelfPreferences
import com.shiyue.reader.domain.repository.CaptureTarget
import com.shiyue.reader.domain.repository.CategoryRepository
import com.shiyue.reader.domain.repository.CoverStorage
import com.shiyue.reader.domain.repository.CoverTransform
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

@Singleton
class TestCategoryRepository @Inject constructor() : CategoryRepository {
    val categories = MutableStateFlow<List<CategorySummary>>(emptyList())

    override fun observeCategories(): Flow<List<CategorySummary>> = categories

    override suspend fun createCategory(category: Category) {
        check(!nameExists(category.normalizedName))
        categories.update { (it + CategorySummary(category, 0)).sortedBy { item -> item.category.sortOrder } }
    }

    override suspend fun renameCategory(categoryId: String, name: String, timestamp: Long) {
        val current = categories.value.first { it.category.id == categoryId }
        val renamed = current.category.renamed(name, timestamp)
        check(!nameExists(renamed.normalizedName, categoryId))
        categories.update { list ->
            list.map { if (it.category.id == categoryId) it.copy(category = renamed) else it }
        }
    }

    override suspend fun deleteCategory(categoryId: String) {
        categories.update { list -> list.filterNot { it.category.id == categoryId } }
    }

    override suspend fun moveCategory(categoryId: String, offset: Int, timestamp: Long) {
        val reordered = categories.value.toMutableList()
        val from = reordered.indexOfFirst { it.category.id == categoryId }
        if (from < 0 || reordered.isEmpty()) return
        val to = (from + offset).coerceIn(reordered.indices)
        if (from != to) reordered.add(to, reordered.removeAt(from))
        categories.value = reordered.mapIndexed { index, item ->
            item.copy(category = item.category.reordered(index, timestamp))
        }
    }

    override suspend fun nameExists(normalizedName: String, excludingId: String?): Boolean =
        categories.value.any { it.category.normalizedName == normalizedName && it.category.id != excludingId }

    fun reset() {
        categories.value = emptyList()
    }
}

@Singleton
class TestBookshelfPreferences @Inject constructor() : BookshelfPreferences {
    private val state = MutableStateFlow(BookSortMode.UPDATED_DESC)
    override val sortMode: Flow<BookSortMode> = state
    override suspend fun setSortMode(mode: BookSortMode) {
        state.value = mode
    }

    fun reset() {
        state.value = BookSortMode.UPDATED_DESC
    }
}

@Singleton
class TestCoverStorage @Inject constructor() : CoverStorage {
    override suspend fun createCaptureTarget() = CaptureTarget("content://test/capture", "capture.jpg")
    override suspend fun processAndStoreCover(sourceUri: String, transform: CoverTransform) = "covers/test.jpg"
    override suspend fun deleteCover(relativePath: String) = true
    override suspend fun deleteTemporary(token: String) = Unit
    override suspend fun cleanupTemporaryFiles() = Unit
    override suspend fun cleanupOrphanedCovers(referencedPaths: Set<String>) = Unit
}
