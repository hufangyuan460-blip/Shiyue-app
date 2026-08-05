package com.shiyue.reader.feature.bookshelf

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.shiyue.reader.R
import com.shiyue.reader.core.model.Book
import com.shiyue.reader.core.model.BookSortMode
import com.shiyue.reader.core.model.BookStatus
import com.shiyue.reader.core.model.CategoryFilter
import com.shiyue.reader.core.model.LibraryBook
import java.io.File
import kotlin.math.roundToInt

object BookshelfTestTags {
    const val Search = "bookshelf_search"
    const val StatusFilter = "bookshelf_status_filter"
    const val Sort = "bookshelf_sort"
    fun bookCard(bookId: String) = "bookshelf_book_$bookId"
}

@Composable
fun BookshelfRoute(
    onAddBook: () -> Unit,
    onBookClick: (String) -> Unit,
    onManageCategories: () -> Unit,
    viewModel: BookshelfViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    BookshelfScreen(
        uiState = state,
        onAddBook = onAddBook,
        onBookClick = onBookClick,
        onManageCategories = onManageCategories,
        onCategoryFilterChanged = viewModel::onCategoryFilterChanged,
        onStatusFilterChanged = viewModel::onStatusFilterChanged,
        onSearchQueryChanged = viewModel::onSearchQueryChanged,
        onSortModeChanged = viewModel::onSortModeChanged,
    )
}

@Composable
fun BookshelfScreen(
    uiState: BookshelfUiState,
    onAddBook: () -> Unit,
    modifier: Modifier = Modifier,
    onBookClick: (String) -> Unit = {},
    onManageCategories: () -> Unit = {},
    onCategoryFilterChanged: (CategoryFilter) -> Unit = {},
    onStatusFilterChanged: (BookStatus?) -> Unit = {},
    onSearchQueryChanged: (String) -> Unit = {},
    onSortModeChanged: (BookSortMode) -> Unit = {},
) {
    Box(modifier.fillMaxSize()) {
        when {
            uiState.isLoading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
            uiState.loadFailed -> Text(
                stringResource(R.string.bookshelf_load_failed),
                modifier = Modifier.align(Alignment.Center).padding(32.dp),
            )
            else -> LibraryGrid(
                uiState,
                onBookClick,
                onAddBook,
                onManageCategories,
                onCategoryFilterChanged,
                onStatusFilterChanged,
                onSearchQueryChanged,
                onSortModeChanged,
            )
        }
        if (!uiState.isLoading && !uiState.loadFailed && uiState.totalBookCount > 0) {
            ExtendedFloatingActionButton(
                onClick = onAddBook,
                modifier = Modifier.align(Alignment.BottomEnd).padding(20.dp),
                icon = { Icon(painterResource(R.drawable.ic_add), contentDescription = null) },
                text = { Text(stringResource(R.string.add_book)) },
            )
        }
    }
}

@Composable
private fun LibraryGrid(
    state: BookshelfUiState,
    onBookClick: (String) -> Unit,
    onAddBook: () -> Unit,
    onManageCategories: () -> Unit,
    onCategoryFilterChanged: (CategoryFilter) -> Unit,
    onStatusFilterChanged: (BookStatus?) -> Unit,
    onSearchQueryChanged: (String) -> Unit,
    onSortModeChanged: (BookSortMode) -> Unit,
) {
    val singleColumn = LocalDensity.current.fontScale >= 1.6f
    LazyVerticalGrid(
        columns = GridCells.Adaptive(if (singleColumn) 260.dp else 150.dp),
        contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 104.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            BookshelfControls(
                state,
                onManageCategories,
                onCategoryFilterChanged,
                onStatusFilterChanged,
                onSearchQueryChanged,
                onSortModeChanged,
            )
        }
        if (state.books.isEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                BookshelfEmpty(state, onAddBook)
            }
        } else {
            items(state.books, key = { it.book.id }) { libraryBook ->
                BookCard(libraryBook, onClick = { onBookClick(libraryBook.book.id) })
            }
        }
    }
}

@Composable
private fun BookshelfControls(
    state: BookshelfUiState,
    onManageCategories: () -> Unit,
    onCategoryFilterChanged: (CategoryFilter) -> Unit,
    onStatusFilterChanged: (BookStatus?) -> Unit,
    onSearchQueryChanged: (String) -> Unit,
    onSortModeChanged: (BookSortMode) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(stringResource(R.string.bookshelf_title), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.SemiBold)
        OutlinedTextField(
            value = state.searchQuery,
            onValueChange = onSearchQueryChanged,
            modifier = Modifier.fillMaxWidth().testTag(BookshelfTestTags.Search),
            label = { Text(stringResource(R.string.search_books)) },
            singleLine = true,
        )
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CategoryChip(stringResource(R.string.category_all), state.categoryFilter == CategoryFilter.All) {
                onCategoryFilterChanged(CategoryFilter.All)
            }
            CategoryChip(stringResource(R.string.category_uncategorized), state.categoryFilter == CategoryFilter.Uncategorized) {
                onCategoryFilterChanged(CategoryFilter.Uncategorized)
            }
            state.categories.forEach { summary ->
                val filter = CategoryFilter.CategoryId(summary.category.id)
                CategoryChip(summary.category.name, state.categoryFilter == filter) { onCategoryFilterChanged(filter) }
            }
            OutlinedButton(onClick = onManageCategories, modifier = Modifier.heightIn(min = 48.dp)) {
                Text(stringResource(R.string.manage_categories))
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            SelectionMenu(
                modifier = Modifier.weight(1f).testTag(BookshelfTestTags.StatusFilter),
                label = state.statusFilter?.let { statusLabel(it) } ?: stringResource(R.string.status_all),
                options = listOf(null) + BookStatus.entries,
                optionLabel = { it?.let { status -> statusLabel(status) } ?: stringResource(R.string.status_all) },
                onSelected = onStatusFilterChanged,
            )
            SelectionMenu(
                modifier = Modifier.weight(1f).testTag(BookshelfTestTags.Sort),
                label = sortLabel(state.sortMode),
                options = BookSortMode.entries,
                optionLabel = { sortLabel(it) },
                onSelected = onSortModeChanged,
            )
        }
        Spacer(Modifier.height(2.dp))
    }
}

@Composable
private fun CategoryChip(label: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(selected = selected, onClick = onClick, label = { Text(label) }, modifier = Modifier.heightIn(min = 48.dp))
}

@Composable
private fun <T> SelectionMenu(
    modifier: Modifier,
    label: String,
    options: List<T>,
    optionLabel: @Composable (T) -> String,
    onSelected: (T) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier) {
        OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
            Text(label, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(optionLabel(option)) },
                    onClick = { expanded = false; onSelected(option) },
                )
            }
        }
    }
}

@Composable
private fun BookshelfEmpty(state: BookshelfUiState, onAddBook: () -> Unit) {
    val text = when {
        state.totalBookCount == 0 -> stringResource(R.string.bookshelf_empty_description)
        state.searchQuery.trim().isNotEmpty() -> stringResource(R.string.bookshelf_search_empty)
        state.categoryFilter != CategoryFilter.All -> stringResource(R.string.bookshelf_category_empty)
        state.statusFilter != null -> stringResource(R.string.bookshelf_status_empty)
        else -> stringResource(R.string.bookshelf_filter_empty)
    }
    Column(
        Modifier.fillMaxWidth().padding(vertical = 48.dp, horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        DefaultBookCover(stringResource(R.string.bookshelf_empty_cover_title), Modifier.size(96.dp, 144.dp))
        Text(text, style = MaterialTheme.typography.bodyLarge)
        if (state.totalBookCount == 0) Button(onClick = onAddBook) { Text(stringResource(R.string.add_first_book)) }
    }
}

@Composable
fun BookCard(libraryBook: LibraryBook, modifier: Modifier = Modifier, onClick: () -> Unit = {}) {
    val book = libraryBook.book
    val percentage = (book.progress * 100).roundToInt()
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth().testTag(BookshelfTestTags.bookCard(book.id)),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(Modifier.padding(12.dp)) {
            BookCover(book, Modifier.fillMaxWidth())
            Spacer(Modifier.height(10.dp))
            Text(
                book.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.semantics { contentDescription = book.title },
            )
            book.author?.let {
                Text(it, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodySmall)
            }
            Text(stringResource(R.string.book_page_progress, book.currentPage, book.totalPages), style = MaterialTheme.typography.bodySmall)
            Text(stringResource(R.string.book_progress_percent, percentage), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
            val progressDescription = stringResource(R.string.book_progress_accessibility_named, book.title, percentage)
            LinearProgressIndicator(
                progress = { book.progress.toFloat() },
                modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)).semantics {
                    contentDescription = progressDescription
                    progressBarRangeInfo = ProgressBarRangeInfo(book.progress.toFloat(), 0f..1f)
                },
            )
            Spacer(Modifier.height(6.dp))
            Text(statusLabel(book.status), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
            if (libraryBook.categories.isNotEmpty()) {
                val categoryText = if (libraryBook.categories.size == 1) libraryBook.categories.first().name
                else stringResource(R.string.book_category_summary, libraryBook.categories.first().name, libraryBook.categories.size - 1)
                Text(categoryText, style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
fun BookCard(book: Book, modifier: Modifier = Modifier, onClick: () -> Unit = {}) =
    BookCard(LibraryBook(book, emptyList()), modifier, onClick)

@Composable
fun BookCover(book: Book, modifier: Modifier = Modifier) {
    if (book.coverPath == null) {
        DefaultBookCover(book.title, modifier)
    } else {
        val file = File(LocalContext.current.filesDir, book.coverPath)
        AsyncImage(
            model = file,
            contentDescription = stringResource(R.string.book_cover_description, book.title),
            modifier = modifier.aspectRatio(2f / 3f).clip(RoundedCornerShape(8.dp)),
            placeholder = painterResource(R.drawable.ic_book_cover),
            error = painterResource(R.drawable.ic_book_cover),
        )
    }
}

@Composable
fun DefaultBookCover(title: String, modifier: Modifier = Modifier) {
    val description = stringResource(R.string.default_book_cover_description, title)
    Surface(
        modifier = modifier.aspectRatio(2f / 3f).semantics { contentDescription = description },
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(painterResource(R.drawable.ic_book_cover), null, Modifier.size(36.dp), tint = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
fun statusLabel(status: BookStatus): String = stringResource(
    when (status) {
        BookStatus.WISH -> R.string.book_status_wish
        BookStatus.READING -> R.string.book_status_reading
        BookStatus.FINISHED -> R.string.book_status_finished
        BookStatus.PAUSED -> R.string.book_status_paused
    },
)

@Composable
private fun sortLabel(mode: BookSortMode): String = stringResource(
    when (mode) {
        BookSortMode.UPDATED_DESC -> R.string.sort_updated
        BookSortMode.TITLE_ASC -> R.string.sort_title
        BookSortMode.PROGRESS_DESC -> R.string.sort_progress
    },
)
