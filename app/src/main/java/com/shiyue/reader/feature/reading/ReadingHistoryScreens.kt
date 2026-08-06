package com.shiyue.reader.feature.reading

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.shiyue.reader.R
import com.shiyue.reader.core.model.ReadingSession
import java.text.DateFormat
import java.util.Date

@Composable
fun BookReadingHistorySection(onSessionClick: (String) -> Unit, viewModel: ReadingHistoryViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    Column(Modifier.fillMaxWidth().padding(top = 28.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(stringResource(R.string.reading_history), style = MaterialTheme.typography.titleLarge)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(stringResource(R.string.history_session_count, state.summary.completedCount))
            Text(stringResource(R.string.history_total_duration, historyDuration(state.summary.totalDurationMs)))
            Text(stringResource(R.string.history_day_count, state.summary.readingDays))
        }
        if (state.sessions.isEmpty()) Text(stringResource(R.string.no_reading_history), color = MaterialTheme.colorScheme.onSurfaceVariant)
        state.sessions.forEach { session -> SessionRow(session) { onSessionClick(session.id) } }
    }
}

@Composable
private fun SessionRow(session: ReadingSession, onClick: () -> Unit) {
    Column(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 8.dp)) {
        Text(DateFormat.getDateTimeInstance().format(Date(session.startedAtEpochMs)), style = MaterialTheme.typography.titleSmall)
        Text(stringResource(R.string.session_summary, historyDuration(session.activeDurationMs), session.startPage, session.endPage ?: session.startPage))
    }
}

@Composable
fun ReadingSessionDetailRoute(
    onBack: () -> Unit, onEdit: (String) -> Unit,
    viewModel: ReadingSessionDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    CollectStarted(viewModel.events) { onBack() }
    var confirmDelete by remember { mutableStateOf(false) }
    SimpleHistoryPage(R.string.reading_session_detail, onBack) {
        state.session?.let { session ->
            Text(DateFormat.getDateTimeInstance().format(Date(session.startedAtEpochMs)), style = MaterialTheme.typography.headlineSmall)
            Text(stringResource(R.string.history_total_duration, historyDuration(session.activeDurationMs)))
            Text(stringResource(R.string.session_pages, session.startPage, session.endPage ?: session.startPage))
            Button(onClick = { onEdit(session.id) }, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) { Text(stringResource(R.string.edit_session)) }
            OutlinedButton(onClick = { confirmDelete = true }, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) { Text(stringResource(R.string.delete_session)) }
            if (state.failed) Text(stringResource(R.string.reading_action_failed), color = MaterialTheme.colorScheme.error)
        } ?: Text(stringResource(if (state.isLoading) R.string.reading_loading else R.string.session_not_found))
    }
    if (confirmDelete) AlertDialog(
        onDismissRequest = { confirmDelete = false }, title = { Text(stringResource(R.string.delete_session_title)) }, text = { Text(stringResource(R.string.delete_session_message)) },
        confirmButton = { TextButton(onClick = { confirmDelete = false; viewModel.delete() }) { Text(stringResource(R.string.delete)) } },
        dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text(stringResource(R.string.cancel)) } },
    )
}

@Composable
fun EditReadingSessionRoute(onBack: () -> Unit, viewModel: EditReadingSessionViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    CollectStarted(viewModel.events) { onBack() }
    SimpleHistoryPage(R.string.edit_session, onBack) {
        if (state.isLoading) Text(stringResource(R.string.reading_loading))
        else if (state.notFound) Text(stringResource(R.string.session_not_found))
        else {
            SessionField(state.startedAt, viewModel::updateStartedAt, R.string.session_started_at)
            SessionField(state.endedAt, viewModel::updateEndedAt, R.string.session_ended_at)
            SessionField(state.durationMinutes, viewModel::updateDuration, R.string.duration_minutes)
            SessionField(state.startPage, viewModel::updateStartPage, R.string.start_page)
            SessionField(state.endPage, viewModel::updateEndPage, R.string.end_page)
            Text(stringResource(R.string.session_time_format_hint), color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (state.hasError) Text(stringResource(R.string.session_edit_validation_error), color = MaterialTheme.colorScheme.error)
            if (state.failed) Text(stringResource(R.string.reading_action_failed), color = MaterialTheme.colorScheme.error)
            Button(onClick = viewModel::save, enabled = !state.isSaving, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) { Text(stringResource(R.string.save_changes)) }
        }
    }
}

@Composable private fun SessionField(value: String, onChange: (String) -> Unit, label: Int) = OutlinedTextField(
    value = value, onValueChange = onChange, label = { Text(stringResource(label)) }, modifier = Modifier.fillMaxWidth(), singleLine = true,
)

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun SimpleHistoryPage(title: Int, onBack: () -> Unit, content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit) {
    androidx.compose.material3.Scaffold(topBar = { androidx.compose.material3.TopAppBar(title = { Text(stringResource(title)) }, navigationIcon = { com.shiyue.reader.feature.bookdetail.BackButton(onBack) }) }) { padding ->
        Column(Modifier.padding(padding).padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp), content = content)
    }
}

@Composable private fun historyDuration(value: Long): String {
    val minutes = value / 60_000L
    return if (minutes >= 60) stringResource(R.string.duration_hours_minutes, minutes / 60, minutes % 60)
    else stringResource(R.string.duration_minutes_value, minutes)
}
