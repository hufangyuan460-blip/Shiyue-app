package com.shiyue.reader.core.model

data class LibraryBook(
    val book: Book,
    val categories: List<Category>,
) {
    val categoryIds: Set<String> = categories.mapTo(linkedSetOf(), Category::id)
}

data class CategorySummary(
    val category: Category,
    val bookCount: Int,
)

sealed interface CategoryFilter {
    data object All : CategoryFilter
    data object Uncategorized : CategoryFilter
    data class CategoryId(val id: String) : CategoryFilter
}

enum class BookSortMode { UPDATED_DESC, TITLE_ASC, PROGRESS_DESC }
