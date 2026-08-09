package com.shiyue.reader.feature.bookshelf

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shiyue.reader.core.model.BookReadingSummary
import com.shiyue.reader.core.model.BookSortMode
import com.shiyue.reader.core.model.BookStatus
import com.shiyue.reader.core.model.CategoryFilter
import com.shiyue.reader.core.model.CategorySummary
import com.shiyue.reader.core.model.LibraryBook
import com.shiyue.reader.domain.repository.BookshelfPreferences
import com.shiyue.reader.domain.usecase.ObserveCategoriesUseCase
import com.shiyue.reader.domain.usecase.ObserveLibraryBooksUseCase
import com.shiyue.reader.domain.usecase.ObserveReadingTimesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class BookshelfUiState(
    val isLoading: Boolean = true,
    val books: List<LibraryBook> = emptyList(),
    val categories: List<CategorySummary> = emptyList(),
    val totalBookCount: Int = 0,
    val readingTimes: Map<String, BookReadingSummary> = emptyMap(),
    val categoryFilter: CategoryFilter = CategoryFilter.All,
    val statusFilter: BookStatus? = null,
    val searchQuery: String = "",
    val sortMode: BookSortMode = BookSortMode.UPDATED_DESC,
    val loadFailed: Boolean = false,
)

private data class FilterState(
    val category: CategoryFilter,
    val status: BookStatus?,
    val query: String,
    val sort: BookSortMode,
)

@HiltViewModel
class BookshelfViewModel @Inject constructor(
    observeBooks: ObserveLibraryBooksUseCase,
    observeCategories: ObserveCategoriesUseCase,
    private val observeReadingTimes: ObserveReadingTimesUseCase,
    private val preferences: BookshelfPreferences,
) : ViewModel() {
    private val categoryFilter = MutableStateFlow<CategoryFilter>(CategoryFilter.All)
    private val statusFilter = MutableStateFlow<BookStatus?>(null)
    private val searchQuery = MutableStateFlow("")

    private val filters = combine(
        categoryFilter,
        statusFilter,
        searchQuery,
        preferences.sortMode,
        ::FilterState,
    )

    val uiState = combine(observeBooks(), observeCategories(), filters, observeReadingTimes()) { allBooks, categories, filter, readingTimes ->
        val validCategory = when (val selected = filter.category) {
            is CategoryFilter.CategoryId -> if (categories.any { it.category.id == selected.id }) selected else CategoryFilter.All
            else -> selected
        }
        val normalizedQuery = filter.query.trim().lowercase(Locale.ROOT)
        val filtered = allBooks.asSequence()
            .filter { libraryBook ->
                when (validCategory) {
                    CategoryFilter.All -> true
                    CategoryFilter.Uncategorized -> libraryBook.categories.isEmpty()
                    is CategoryFilter.CategoryId -> validCategory.id in libraryBook.categoryIds
                }
            }
            .filter { validCategoryBook ->
                filter.status == null || validCategoryBook.book.status == filter.status
            }
            .filter { libraryBook ->
                normalizedQuery.isEmpty() ||
                    libraryBook.book.title.lowercase(Locale.ROOT).contains(normalizedQuery) ||
                    libraryBook.book.author?.lowercase(Locale.ROOT)?.contains(normalizedQuery) == true
            }
            .let { books ->
                when (filter.sort) {
                    BookSortMode.UPDATED_DESC -> books.sortedWith(
                        compareByDescending<LibraryBook> { it.book.updatedAt }.thenBy { it.book.id },
                    )
                    BookSortMode.TITLE_ASC -> books.sortedWith(
                        compareBy<LibraryBook> { it.book.title.lowercase(Locale.ROOT) }.thenBy { it.book.id },
                    )
                    BookSortMode.PROGRESS_DESC -> books.sortedWith(
                        compareByDescending<LibraryBook> { it.book.progress }
                            .thenByDescending { it.book.updatedAt },
                    )
                }
            }.toList()
        BookshelfUiState(
            isLoading = false,
            books = filtered,
            categories = categories,
            totalBookCount = allBooks.size,
            readingTimes = readingTimes,
            categoryFilter = validCategory,
            statusFilter = filter.status,
            searchQuery = filter.query,
            sortMode = filter.sort,
        )
    }.catch {
        emit(BookshelfUiState(isLoading = false, loadFailed = true))
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), BookshelfUiState())

    fun onCategoryFilterChanged(filter: CategoryFilter) {
        categoryFilter.value = filter
    }

    fun onStatusFilterChanged(status: BookStatus?) {
        statusFilter.value = status
    }

    fun onSearchQueryChanged(query: String) {
        searchQuery.value = query
    }

    fun onSortModeChanged(mode: BookSortMode) {
        viewModelScope.launch { preferences.setSortMode(mode) }
    }
}
