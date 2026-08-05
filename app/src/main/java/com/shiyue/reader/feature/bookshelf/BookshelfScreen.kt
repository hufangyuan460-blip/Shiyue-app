package com.shiyue.reader.feature.bookshelf

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.shiyue.reader.R
import com.shiyue.reader.core.model.Book
import com.shiyue.reader.core.model.BookStatus
import com.shiyue.reader.core.ui.theme.ShiyueTheme
import kotlin.math.roundToInt

@Composable
fun BookshelfRoute(
    onAddBook: () -> Unit,
    viewModel: BookshelfViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    BookshelfScreen(uiState = uiState, onAddBook = onAddBook)
}

@Composable
fun BookshelfScreen(
    uiState: BookshelfUiState,
    onAddBook: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize()) {
        when {
            uiState.isLoading -> BookshelfLoading()
            uiState.loadFailed -> BookshelfLoadError()
            uiState.books.isEmpty() -> EmptyBookshelf(onAddBook = onAddBook)
            else -> BookList(books = uiState.books)
        }

        if (!uiState.isLoading && !uiState.loadFailed && uiState.books.isNotEmpty()) {
            ExtendedFloatingActionButton(
                onClick = onAddBook,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(20.dp),
                icon = {
                    Icon(
                        painter = painterResource(R.drawable.ic_add),
                        contentDescription = null,
                    )
                },
                text = { Text(stringResource(R.string.add_book)) },
            )
        }
    }
}

@Composable
private fun BookshelfLoading() {
    val description = stringResource(R.string.bookshelf_loading)
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator(
            modifier = Modifier.semantics { contentDescription = description },
        )
    }
}

@Composable
private fun BookshelfLoadError() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = stringResource(R.string.bookshelf_load_failed),
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}

@Composable
private fun EmptyBookshelf(onAddBook: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        DefaultBookCover(
            title = stringResource(R.string.bookshelf_empty_cover_title),
            modifier = Modifier.size(width = 104.dp, height = 156.dp),
        )
        Spacer(Modifier.height(24.dp))
        Text(
            text = stringResource(R.string.bookshelf_empty_title),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.bookshelf_empty_description),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(24.dp))
        Button(onClick = onAddBook) {
            Text(stringResource(R.string.add_first_book))
        }
    }
}

@Composable
private fun BookList(books: List<Book>) {
    LazyColumn(
        contentPadding = PaddingValues(start = 20.dp, top = 24.dp, end = 20.dp, bottom = 104.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Text(
                text = stringResource(R.string.bookshelf_title),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.SemiBold,
            )
        }
        items(items = books, key = Book::id) { book ->
            BookCard(book = book)
        }
    }
}

@Composable
fun BookCard(
    book: Book,
    modifier: Modifier = Modifier,
) {
    val percentage = (book.progress * 100).roundToInt()
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(20.dp),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            DefaultBookCover(
                title = book.title,
                modifier = Modifier
                    .size(width = 88.dp, height = 132.dp),
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = book.title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                book.author?.let { author ->
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = author,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = stringResource(
                            R.string.book_page_progress,
                            book.currentPage,
                            book.totalPages,
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Text(
                        text = stringResource(R.string.book_progress_percent, percentage),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.secondary,
                        fontWeight = FontWeight.Medium,
                    )
                }
                Spacer(Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { book.progress.toFloat() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = MaterialTheme.colorScheme.secondary,
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    text = statusLabel(book.status),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

@Composable
private fun DefaultBookCover(
    title: String,
    modifier: Modifier = Modifier,
) {
    val description = stringResource(R.string.default_book_cover_description, title)
    Surface(
        modifier = modifier
            .aspectRatio(2f / 3f)
            .semantics { contentDescription = description },
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
        tonalElevation = 2.dp,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                painter = painterResource(R.drawable.ic_book_cover),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(36.dp),
            )
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

@Preview(showBackground = true)
@Composable
private fun EmptyBookshelfPreview() {
    ShiyueTheme {
        BookshelfScreen(BookshelfUiState(isLoading = false), onAddBook = {})
    }
}

@Preview(showBackground = true)
@Composable
private fun BookCardPreview() {
    ShiyueTheme {
        BookCard(
            book = Book.create(
                title = "山茶文具店",
                author = "小川糸",
                totalPages = 320,
                currentPage = 128,
                status = BookStatus.READING,
            ),
            modifier = Modifier.padding(16.dp),
        )
    }
}
