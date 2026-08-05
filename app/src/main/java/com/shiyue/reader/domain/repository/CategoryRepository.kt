package com.shiyue.reader.domain.repository

import com.shiyue.reader.core.model.Category
import com.shiyue.reader.core.model.CategorySummary
import kotlinx.coroutines.flow.Flow

interface CategoryRepository {
    fun observeCategories(): Flow<List<CategorySummary>>
    suspend fun createCategory(category: Category)
    suspend fun renameCategory(categoryId: String, name: String, timestamp: Long = System.currentTimeMillis())
    suspend fun deleteCategory(categoryId: String)
    suspend fun moveCategory(categoryId: String, offset: Int, timestamp: Long = System.currentTimeMillis())
    suspend fun nameExists(normalizedName: String, excludingId: String? = null): Boolean
}
