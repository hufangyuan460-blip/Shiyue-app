package com.shiyue.reader.feature.reading

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.shiyue.reader.R
import com.shiyue.reader.core.model.LibraryBook
import com.shiyue.reader.feature.bookshelf.BookCover

@Composable
fun ReadingRoute(
    onStartReading: (String) -> Unit,
    onOpenActive: () -> Unit,
    onRecover: (String) -> Unit,
    viewModel: ReadingHomeViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val owner = LocalLifecycleOwner.current
    LaunchedEffect(viewModel, owner) {
        owner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            viewModel.events.collect { event ->
                when (event) {
                    is ReadingHomeEvent.OpenActive -> if (event.needsRecovery) onRecover(event.sessionId) else onOpenActive()
                }
            }
        }
    }
    ReadingScreen(state, viewModel::openActive, onStartReading)
}

@Composable
fun ReadingScreen(state: ReadingHomeUiState, onOpenActive: () -> Unit, onStartReading: (String) -> Unit) {
    when {
        state.isLoading -> Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) { CircularProgressIndicator() }
        state.failed -> ReadingMessage(stringResource(R.string.reading_load_failed))
        else -> LazyColumn(
            Modifier.fillMaxSize().padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Text(stringResource(R.string.reading_title), style = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(top = 24.dp))
                Spacer(Modifier.height(12.dp))
                Text(stringResource(R.string.reading_placeholder), color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(8.dp))
                state.activeSession?.let {
                    Button(onClick = onOpenActive, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) {
                        Text(stringResource(R.string.continue_reading_session))
                    }
                } ?: Text(stringResource(R.string.no_active_reading), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (state.readingBooks.isNotEmpty()) item { Text(stringResource(R.string.currently_reading), style = MaterialTheme.typography.titleLarge) }
            items(state.readingBooks, key = { it.book.id }) { ReadingBookRow(it) { onStartReading(it.book.id) } }
            if (state.recentBooks.isNotEmpty()) item { Text(stringResource(R.string.recent_books), style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(top = 8.dp)) }
            items(state.recentBooks, key = { "recent-${it.book.id}" }) { ReadingBookRow(it) { onStartReading(it.book.id) } }
            if (state.recentBooks.isEmpty()) item { ReadingMessage(stringResource(R.string.reading_empty)) }
        }
    }
}

@Composable
private fun ReadingBookRow(book: LibraryBook, onClick: () -> Unit) {
    Card(Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            BookCover(book.book, Modifier.height(90.dp))
            Column(Modifier.weight(1f)) {
                Text(book.book.title, style = MaterialTheme.typography.titleMedium)
                book.book.author?.let { Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                Text(stringResource(R.string.start_from_page, book.book.currentPage))
            }
        }
    }
}

@Composable
private fun ReadingMessage(message: String) {
    Column(Modifier.fillMaxWidth().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) { Text(message) }
}
