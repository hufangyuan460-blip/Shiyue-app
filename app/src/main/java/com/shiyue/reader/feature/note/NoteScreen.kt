package com.shiyue.reader.feature.note

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.shiyue.reader.R
import com.shiyue.reader.core.model.Note
import java.io.File
import java.text.DateFormat
import java.util.Date

@Composable
fun NoteRoute(
    onNoteClick: (String) -> Unit,
    viewModel: NoteListViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    NoteScreen(
        uiState = state,
        onNoteClick = onNoteClick,
        onBookFilterSelected = viewModel::onBookFilterSelected,
    )
}

@Composable
fun NoteScreen(
    uiState: NoteListUiState,
    onNoteClick: (String) -> Unit,
    onBookFilterSelected: (String?) -> Unit,
) {
    when {
        uiState.isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        uiState.failed -> NoteMessage(stringResource(R.string.note_list_failed))
        else -> LazyColumn(
            Modifier.fillMaxSize().padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Text(
                    stringResource(R.string.note_list_title),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 24.dp),
                )
                Spacer(Modifier.height(8.dp))
                if (uiState.bookFilters.isNotEmpty()) {
                    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = uiState.selectedBookId == null,
                            onClick = { onBookFilterSelected(null) },
                            label = { Text(stringResource(R.string.note_filter_all)) },
                            modifier = Modifier.heightIn(min = 48.dp),
                        )
                        uiState.bookFilters.forEach { (bookId, title) ->
                            FilterChip(
                                selected = uiState.selectedBookId == bookId,
                                onClick = { onBookFilterSelected(bookId) },
                                label = { Text(title) },
                                modifier = Modifier.heightIn(min = 48.dp),
                            )
                        }
                    }
                }
            }
            if (uiState.notes.isEmpty()) {
                item { NoteMessage(stringResource(R.string.note_list_empty)) }
            } else {
                items(uiState.notes, key = { it.note.id }) { item ->
                    NoteRow(item) { onNoteClick(item.note.id) }
                }
            }
        }
    }
}

@Composable
fun NoteRow(item: NoteListItem, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Row(Modifier.padding(14.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            NoteImageThumbnail(item.note)
            Column(Modifier.weight(1f)) {
                Text(
                    item.bookTitle,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    item.note.content,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(2.dp))
                NoteMetaLine(item.note)
            }
        }
    }
}

@Composable
fun BookNotesSection(onNoteClick: (String) -> Unit, viewModel: BookNotesViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    Column(Modifier.fillMaxWidth().padding(top = 28.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(stringResource(R.string.note_list_title), style = MaterialTheme.typography.titleLarge)
        if (state.notes.isEmpty()) {
            Text(stringResource(R.string.note_no_notes_in_book), color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            state.notes.forEach { note ->
                BookNoteRow(note) { onNoteClick(note.id) }
            }
        }
    }
}

@Composable
private fun BookNoteRow(note: Note, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Row(Modifier.padding(14.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            NoteImageThumbnail(note)
            Column(Modifier.weight(1f)) {
                Text(
                    note.content,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(2.dp))
                NoteMetaLine(note)
            }
        }
    }
}

@Composable
private fun NoteImageThumbnail(note: Note) {
    if (note.imagePath == null) return
    val file = File(LocalContext.current.filesDir, note.imagePath)
    AsyncImage(
        model = file,
        contentDescription = null,
        modifier = Modifier.size(56.dp).clip(RoundedCornerShape(10.dp)),
    )
}

@Composable
private fun NoteMetaLine(note: Note) {
    val time = DateFormat.getDateTimeInstance().format(Date(note.createdAt))
    val page = note.pageNumber?.let { stringResource(R.string.note_page_value, it) }
    Text(
        listOfNotNull(page, time).joinToString(" · "),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}

@Composable
private fun NoteMessage(message: String) {
    Column(
        Modifier.fillMaxWidth().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(message, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
