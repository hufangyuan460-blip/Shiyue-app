package com.shiyue.reader.domain.usecase

import com.shiyue.reader.core.model.Category
import com.shiyue.reader.core.model.CategorySummary
import com.shiyue.reader.domain.repository.CategoryRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

class ObserveCategoriesUseCase @Inject constructor(private val repository: CategoryRepository) {
    operator fun invoke(): Flow<List<CategorySummary>> = repository.observeCategories()
}

class CreateCategoryUseCase @Inject constructor(private val repository: CategoryRepository) {
    suspend operator fun invoke(name: String, sortOrder: Int): Category {
        val category = Category.create(name, sortOrder)
        repository.createCategory(category)
        return category
    }
}

class RenameCategoryUseCase @Inject constructor(private val repository: CategoryRepository) {
    suspend operator fun invoke(id: String, name: String) = repository.renameCategory(id, name)
}

class DeleteCategoryUseCase @Inject constructor(private val repository: CategoryRepository) {
    suspend operator fun invoke(id: String) = repository.deleteCategory(id)
}

class MoveCategoryUseCase @Inject constructor(private val repository: CategoryRepository) {
    suspend operator fun invoke(id: String, offset: Int) = repository.moveCategory(id, offset)
}
