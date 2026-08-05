package com.shiyue.reader.core.data.repository

import androidx.room.withTransaction
import com.shiyue.reader.core.database.ShiyueDatabase
import com.shiyue.reader.core.database.asEntity
import com.shiyue.reader.core.database.asExternalModel
import com.shiyue.reader.core.model.Category
import com.shiyue.reader.core.model.CategorySummary
import com.shiyue.reader.domain.repository.CategoryRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Singleton
class OfflineCategoryRepository @Inject constructor(
    private val database: ShiyueDatabase,
) : CategoryRepository {
    private val dao get() = database.categoryDao()

    override fun observeCategories(): Flow<List<CategorySummary>> =
        dao.observeAllWithBookCount().map { list -> list.map { it.asExternalModel() } }

    override suspend fun createCategory(category: Category) {
        check(!nameExists(category.normalizedName)) { "Category name already exists" }
        dao.insert(category.asEntity())
    }

    override suspend fun renameCategory(categoryId: String, name: String, timestamp: Long) {
        val existing = checkNotNull(dao.getById(categoryId)) { "Category not found: $categoryId" }
            .asExternalModel()
        val renamed = existing.renamed(name, timestamp)
        check(!nameExists(renamed.normalizedName, categoryId)) { "Category name already exists" }
        check(dao.update(renamed.asEntity()) == 1) { "Category not found: $categoryId" }
    }

    override suspend fun deleteCategory(categoryId: String) {
        val existing = checkNotNull(dao.getById(categoryId)) { "Category not found: $categoryId" }
        check(dao.delete(existing) == 1) { "Category not found: $categoryId" }
        normalizeOrder()
    }

    override suspend fun moveCategory(categoryId: String, offset: Int, timestamp: Long) {
        require(offset == -1 || offset == 1) { "Category offset must be -1 or 1" }
        database.withTransaction {
            val all = dao.getAll().map { it.asExternalModel() }
            val from = all.indexOfFirst { it.id == categoryId }
            check(from >= 0) { "Category not found: $categoryId" }
            val to = (from + offset).coerceIn(all.indices)
            if (from == to) return@withTransaction
            val reordered = all.toMutableList().apply { add(to, removeAt(from)) }
                .mapIndexed { index, category -> category.reordered(index, timestamp).asEntity() }
            dao.updateAll(reordered)
        }
    }

    override suspend fun nameExists(normalizedName: String, excludingId: String?): Boolean =
        dao.getByNormalizedName(normalizedName)?.id?.let { it != excludingId } == true

    private suspend fun normalizeOrder() {
        database.withTransaction {
            val normalized = dao.getAll().mapIndexed { index, entity ->
                entity.asExternalModel().reordered(index).asEntity()
            }
            if (normalized.isNotEmpty()) dao.updateAll(normalized)
        }
    }
}
