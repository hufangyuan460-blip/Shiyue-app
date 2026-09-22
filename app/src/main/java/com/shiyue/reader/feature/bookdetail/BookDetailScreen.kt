package com.shiyue.reader.feature.bookdetail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.shiyue.reader.R
import com.shiyue.reader.core.model.Book
import com.shiyue.reader.feature.bookshelf.DefaultBookCover
import com.shiyue.reader.feature.bookshelf.BookCover
import com.shiyue.reader.feature.bookshelf.statusLabel
import com.shiyue.reader.feature.note.BookNotesSection
import com.shiyue.reader.feature.reading.BookReadingHistorySection
import java.text.DateFormat
import java.util.Date
import kotlin.math.roundToInt

object BookDetailTestTags {
    const val Edit = "book_detail_edit"
    const val UpdateProgress = "book_detail_update_progress"
}

@Composable
fun BookDetailRoute(
    onBack: () -> Unit,
    onEdit: (String) -> Unit,
    onUpdateProgress: (String) -> Unit,
    onStartReading: (String) -> Unit,
    onSessionClick: (String) -> Unit,
    onNoteClick: (String) -> Unit = {},
    viewModel: BookDetailViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val owner = LocalLifecycleOwner.current
    LaunchedEffect(viewModel, owner) {
        owner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            viewModel.events.collect { if (it == BookDetailEvent.Deleted) onBack() }
        }
    }
    BookDetailScreen(
        uiState = uiState,
        onBack = onBack,
        onRetry = viewModel::retry,
        onEdit = onEdit,
        onUpdateProgress = onUpdateProgress,
        onStartReading = onStartReading,
        onSessionClick = onSessionClick,
        onNoteClick = onNoteClick,
        onDelete = viewModel::delete,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookDetailScreen(
    uiState: BookDetailUiState,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onEdit: (String) -> Unit,
    onUpdateProgress: (String) -> Unit,
    onStartReading: (String) -> Unit = {},
    onSessionClick: (String) -> Unit = {},
    onNoteClick: (String) -> Unit = {},
    onDelete: () -> Unit = {},
) {
    val loadingDescription = stringResource(R.string.book_detail_loading)
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.book_detail_title)) },
                navigationIcon = { BackButton(onBack) },
            )
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
            when (uiState) {
                BookDetailUiState.Loading -> CircularProgressIndicator(
                    Modifier.semantics { contentDescription = loadingDescription },
                )
                BookDetailUiState.NotFound -> MessageState(
                    message = stringResource(R.string.book_not_found),
                    action = stringResource(R.string.back_to_bookshelf),
                    onAction = onBack,
                )
                BookDetailUiState.Error -> MessageState(
                    message = stringResource(R.string.book_detail_load_failed),
                    action = stringResource(R.string.retry),
                    onAction = onRetry,
                )
                is BookDetailUiState.Content -> DetailContent(
                    libraryBook = uiState.libraryBook,
                    isDeleting = uiState.isDeleting,
                    deleteFailed = uiState.deleteFailed,
                    onEdit = { onEdit(uiState.libraryBook.book.id) },
                    onUpdateProgress = { onUpdateProgress(uiState.libraryBook.book.id) },
                    onStartReading = { onStartReading(uiState.libraryBook.book.id) },
                    onSessionClick = onSessionClick,
                    onNoteClick = onNoteClick,
                    onDelete = onDelete,
                )
            }
        }
    }
}

@Composable
private fun DetailContent(
    libraryBook: com.shiyue.reader.core.model.LibraryBook,
    isDeleting: Boolean,
    deleteFailed: Boolean,
    onEdit: () -> Unit,
    onUpdateProgress: () -> Unit,
    onStartReading: () -> Unit,
    onSessionClick: (String) -> Unit,
    onNoteClick: (String) -> Unit,
    onDelete: () -> Unit,
) {
    val book = libraryBook.book
    var confirmDelete by remember { mutableStateOf(false) }
    val percentage = (book.progress * 100).roundToInt()
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        BookCover(book, Modifier.size(width = 128.dp, height = 192.dp))
        Spacer(Modifier.height(20.dp))
        Text(book.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
        book.author?.let {
            Spacer(Modifier.height(6.dp))
            Text(it, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.height(12.dp))
        Text(statusLabel(book.status), color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Medium)
        if (libraryBook.categories.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            Text(libraryBook.categories.joinToString(" · ") { it.name }, style = MaterialTheme.typography.bodyMedium)
        }
        Spacer(Modifier.height(20.dp))
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.book_page_progress, book.currentPage, book.totalPages))
            Text(stringResource(R.string.book_progress_percent, percentage), color = MaterialTheme.colorScheme.secondary)
            val description = stringResource(R.string.book_progress_accessibility, percentage)
            LinearProgressIndicator(
                progress = { book.progress.toFloat() },
                modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)).semantics {
                    contentDescription = description
                    progressBarRangeInfo = ProgressBarRangeInfo(book.progress.toFloat(), 0f..1f)
                },
            )
        }
        Spacer(Modifier.height(24.dp))
        DetailDate(R.string.book_created_at, book.createdAt)
        DetailDate(R.string.book_updated_at, book.updatedAt)
        Spacer(Modifier.height(28.dp))
        Button(
            onClick = onStartReading,
            modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
        ) { Text(stringResource(R.string.start_reading)) }
        Spacer(Modifier.height(12.dp))
        Button(
            onClick = onUpdateProgress,
            modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp).testTag(BookDetailTestTags.UpdateProgress),
        ) {
            Text(stringResource(R.string.update_reading_progress))
        }
        Spacer(Modifier.height(12.dp))
        OutlinedButton(
            onClick = onEdit,
            modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp).testTag(BookDetailTestTags.Edit),
        ) {
            Text(stringResource(R.string.edit_book))
        }
        Spacer(Modifier.height(12.dp))
        OutlinedButton(
            onClick = { confirmDelete = true },
            enabled = !isDeleting,
            modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
        ) { Text(stringResource(R.string.delete_book), color = MaterialTheme.colorScheme.error) }
        if (deleteFailed) Text(stringResource(R.string.delete_book_failed), color = MaterialTheme.colorScheme.error)
        BookReadingHistorySection(onSessionClick)
        BookNotesSection(onNoteClick)
    }
    if (confirmDelete) androidx.compose.material3.AlertDialog(
        onDismissRequest = { confirmDelete = false },
        title = { Text(stringResource(R.string.delete_book_title)) },
        text = { Text(stringResource(R.string.delete_book_message)) },
        confirmButton = { androidx.compose.material3.TextButton(onClick = { confirmDelete = false; onDelete() }) { Text(stringResource(R.string.delete), color = MaterialTheme.colorScheme.error) } },
        dismissButton = { androidx.compose.material3.TextButton(onClick = { confirmDelete = false }) { Text(stringResource(R.string.cancel)) } },
    )
}

@Composable
private fun DetailDate(labelRes: Int, timestamp: Long) {
    val value = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(timestamp))
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(stringResource(labelRes), color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value)
    }
}

@Composable
private fun MessageState(message: String, action: String, onAction: () -> Unit) {
    Column(
        Modifier.fillMaxWidth().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Text(message, style = MaterialTheme.typography.bodyLarge)
        Button(onClick = onAction, modifier = Modifier.heightIn(min = 48.dp)) { Text(action) }
    }
}

@Composable
fun BackButton(onBack: () -> Unit) {
    val description = stringResource(R.string.navigate_back_generic)
    IconButton(onClick = onBack, modifier = Modifier.semantics { contentDescription = description }) {
        Icon(painterResource(R.drawable.ic_arrow_back), contentDescription = null)
    }
}
