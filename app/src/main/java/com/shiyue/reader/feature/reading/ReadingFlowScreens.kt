package com.shiyue.reader.feature.reading

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.shiyue.reader.R
import com.shiyue.reader.core.model.ReadingClockAnomaly
import com.shiyue.reader.core.model.ReadingSessionState
import com.shiyue.reader.feature.bookdetail.BackButton
import com.shiyue.reader.feature.bookshelf.BookCover
import com.shiyue.reader.feature.bookshelf.statusLabel
import java.text.DateFormat
import java.util.Date
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow

object ReadingFlowTestTags {
    const val StartPage = "reading_start_page"
    const val Start = "reading_start"
    const val Finish = "reading_finish"
    const val EndPage = "reading_end_page"
    const val SaveSummary = "reading_save_summary"
}

@Composable
internal fun <T> CollectStarted(flow: Flow<T>, onEvent: (T) -> Unit) {
    val owner = LocalLifecycleOwner.current
    LaunchedEffect(flow, owner) {
        owner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) { flow.collect(onEvent) }
    }
}

@Composable
fun StartReadingRoute(onBack: () -> Unit, onOpenActive: () -> Unit, viewModel: StartReadingViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    CollectStarted(viewModel.events) { onOpenActive() }
    SimplePage(R.string.start_reading, onBack) {
        state.book?.let { book ->
            BookCover(book, Modifier.size(width = 96.dp, height = 144.dp).align(Alignment.CenterHorizontally))
            Text(book.title, style = MaterialTheme.typography.headlineSmall)
            Text(statusLabel(book.status), color = MaterialTheme.colorScheme.primary)
            Text(stringResource(R.string.book_page_progress, book.currentPage, book.totalPages))
            OutlinedTextField(
                value = state.startPage, onValueChange = viewModel::onPageChanged,
                label = { Text(stringResource(R.string.start_page)) }, isError = state.pageError,
                supportingText = { if (state.pageError) Text(stringResource(R.string.reading_page_error, book.totalPages)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth().testTag(ReadingFlowTestTags.StartPage),
            )
            state.existingSessionBookId?.let { Text(stringResource(R.string.another_session_active), color = MaterialTheme.colorScheme.error) }
            if (state.failed) ErrorText(R.string.start_reading_failed)
            Button(onClick = viewModel::begin, enabled = !state.isStarting, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp).testTag(ReadingFlowTestTags.Start)) { Text(stringResource(R.string.start_reading)) }
        } ?: if (state.isLoading) CircularProgressIndicator() else Text(stringResource(R.string.book_not_found))
    }
    if (state.showStatusConfirmation) AlertDialog(
        onDismissRequest = viewModel::dismissStatusConfirmation,
        title = { Text(stringResource(R.string.switch_to_reading_title)) },
        text = { Text(stringResource(R.string.switch_to_reading_message)) },
        confirmButton = { TextButton(onClick = viewModel::confirmSwitchStatus) { Text(stringResource(R.string.switch_status_and_start)) } },
        dismissButton = { TextButton(onClick = viewModel::startWithoutStatusChange) { Text(stringResource(R.string.keep_status_and_start)) } },
    )
}

@Composable
fun ActiveReadingRoute(
    onBack: () -> Unit, onFinish: (String) -> Unit, onRecover: (String) -> Unit,
    viewModel: ActiveReadingViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var confirmDiscard by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
    val owner = LocalLifecycleOwner.current
    LaunchedEffect(viewModel, owner) {
        owner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            while (true) { viewModel.refreshTicker(); delay(1_000) }
        }
    }
    CollectStarted(viewModel.events) {
        when (it) {
            is ActiveReadingEvent.Finish -> onFinish(it.sessionId)
            is ActiveReadingEvent.Recover -> onRecover(it.sessionId)
            ActiveReadingEvent.Discarded -> onBack()
        }
    }
    SimplePage(R.string.reading_in_progress, onBack) {
        val session = state.session
        if (state.isLoading) CircularProgressIndicator()
        else if (session == null) Text(stringResource(R.string.no_active_reading))
        else {
            Text(state.book?.title.orEmpty(), style = MaterialTheme.typography.headlineSmall)
            state.book?.let { BookCover(it, Modifier.size(width = 96.dp, height = 144.dp).align(Alignment.CenterHorizontally)) }
            Text(stringResource(if (session.state == ReadingSessionState.ACTIVE) R.string.reading_state_active else R.string.reading_state_paused))
            Text(formatDuration(state.displayedDurationMs), style = MaterialTheme.typography.displayMedium)
            Text(stringResource(R.string.session_start_page_value, session.startPage))
            Text(stringResource(R.string.session_started_value, DateFormat.getDateTimeInstance().format(Date(session.startedAtEpochMs))))
            state.anomaly?.let { ErrorText(R.string.reading_clock_anomaly) }
            if (state.failed) ErrorText(R.string.reading_action_failed)
            Button(onClick = viewModel::togglePause, enabled = !state.isWorking, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) {
                Text(stringResource(if (session.state == ReadingSessionState.ACTIVE) R.string.pause_reading else R.string.resume_reading))
            }
            Button(onClick = viewModel::finish, enabled = !state.isWorking, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp).testTag(ReadingFlowTestTags.Finish)) { Text(stringResource(R.string.finish_reading)) }
            OutlinedButton(onClick = { confirmDiscard = true }, enabled = !state.isWorking, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) { Text(stringResource(R.string.discard_session)) }
        }
    }
    if (confirmDiscard) AlertDialog(
        onDismissRequest = { confirmDiscard = false }, title = { Text(stringResource(R.string.discard_session_title)) }, text = { Text(stringResource(R.string.discard_session_message)) },
        confirmButton = { TextButton(onClick = { confirmDiscard = false; viewModel.discard() }) { Text(stringResource(R.string.discard_session)) } },
        dismissButton = { TextButton(onClick = { confirmDiscard = false }) { Text(stringResource(R.string.cancel)) } },
    )
}

@Composable
fun FinishReadingRoute(onBack: () -> Unit, onSaved: (String) -> Unit, viewModel: FinishReadingViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    CollectStarted(viewModel.events) { event ->
        when (event) { is FinishReadingEvent.Saved -> onSaved(event.bookId) }
    }
    SimplePage(R.string.finish_reading, onBack) {
        val book = state.book
        if (state.isLoading) CircularProgressIndicator()
        else if (book == null) Text(stringResource(R.string.book_not_found))
        else {
            Text(book.title, style = MaterialTheme.typography.headlineSmall)
            state.session?.let { session ->
                Text(stringResource(R.string.session_started_value, DateFormat.getDateTimeInstance().format(Date(session.startedAtEpochMs))))
                Text(stringResource(R.string.history_total_duration, formatDuration(session.activeDurationMs)))
                Text(stringResource(R.string.session_start_page_value, session.startPage))
            }
            OutlinedTextField(
                state.endPage, viewModel::onEndPageChanged, label = { Text(stringResource(R.string.end_page)) },
                isError = state.pageError, supportingText = { if (state.pageError) Text(stringResource(R.string.reading_page_error, book.totalPages)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth().testTag(ReadingFlowTestTags.EndPage),
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.update_book_progress), Modifier.weight(1f)); Switch(state.updateProgress, viewModel::onUpdateProgressChanged)
            }
            val end = state.endPage.toIntOrNull()
            if (end != null) {
                Text(stringResource(R.string.session_page_delta, end - (state.session?.startPage ?: end)))
                Text(stringResource(R.string.progress_preview, ((end.toDouble() / book.totalPages) * 100).toInt()))
            }
            if (state.failed) ErrorText(R.string.finish_reading_failed)
            Button(onClick = viewModel::save, enabled = !state.isSaving, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp).testTag(ReadingFlowTestTags.SaveSummary)) { Text(stringResource(R.string.save_reading_summary)) }
        }
    }
    when (state.confirmation) {
        FinishConfirmation.REWIND -> ConfirmDialog(R.string.confirm_rewind_title, R.string.confirm_reading_rewind_message, viewModel::confirmRewind, viewModel::cancelConfirmation)
        FinishConfirmation.LAST_PAGE -> AlertDialog(
            onDismissRequest = viewModel::cancelConfirmation, title = { Text(stringResource(R.string.finish_book_title)) }, text = { Text(stringResource(R.string.finish_book_message)) },
            confirmButton = { TextButton(onClick = viewModel::markFinished) { Text(stringResource(R.string.mark_as_finished)) } },
            dismissButton = { TextButton(onClick = viewModel::keepStatusAtLastPage) { Text(stringResource(R.string.update_page_only)) } },
        )
        null -> Unit
    }
}

@Composable
fun RecoverReadingRoute(onBack: () -> Unit, onActive: () -> Unit, onFinish: () -> Unit, viewModel: RecoverReadingViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    CollectStarted(viewModel.events) { when (it) { RecoverReadingEvent.Active -> onActive(); RecoverReadingEvent.Finish -> onFinish(); RecoverReadingEvent.Discarded -> onBack() } }
    SimplePage(R.string.recover_reading, onBack) {
        if (state.isLoading) CircularProgressIndicator() else {
            Text(stringResource(R.string.recovery_explanation))
            state.inspection?.anomaly?.let { Text(stringResource(anomalyLabel(it)), color = MaterialTheme.colorScheme.error) }
            if (!state.inspection?.additionalUnfinishedSessionIds.isNullOrEmpty()) ErrorText(R.string.multiple_unfinished_sessions)
            Button(onClick = viewModel::continueReliable, enabled = !state.isWorking, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.ignore_uncertain_time)) }
            Button(onClick = viewModel::continueWithEstimate, enabled = !state.isWorking, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.use_wall_clock_estimate)) }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = state.manualHours, onValueChange = viewModel::onHoursChanged, modifier = Modifier.weight(1f), label = { Text(stringResource(R.string.hours)) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                OutlinedTextField(value = state.manualMinutes, onValueChange = viewModel::onMinutesChanged, modifier = Modifier.weight(1f), label = { Text(stringResource(R.string.minutes)) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
            }
            if (state.inputError) ErrorText(R.string.duration_input_error)
            Button(onClick = viewModel::useManualDuration, enabled = !state.isWorking, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.use_manual_duration)) }
            OutlinedButton(onClick = viewModel::discard, enabled = !state.isWorking, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.discard_session)) }
            if (state.failed) ErrorText(R.string.reading_action_failed)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SimplePage(title: Int, onBack: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    Scaffold(topBar = { TopAppBar(title = { Text(stringResource(title)) }, navigationIcon = { BackButton(onBack) }) }) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp), content = content,
        )
    }
}

@Composable private fun ErrorText(res: Int) = Text(stringResource(res), color = MaterialTheme.colorScheme.error)
@Composable private fun ConfirmDialog(title: Int, message: Int, confirm: () -> Unit, dismiss: () -> Unit) = AlertDialog(
    onDismissRequest = dismiss, title = { Text(stringResource(title)) }, text = { Text(stringResource(message)) },
    confirmButton = { TextButton(onClick = confirm) { Text(stringResource(R.string.confirm_update)) } },
    dismissButton = { TextButton(onClick = dismiss) { Text(stringResource(R.string.cancel)) } },
)
private fun formatDuration(value: Long): String { val s = value / 1000; return "%02d:%02d:%02d".format(s / 3600, s / 60 % 60, s % 60) }
private fun anomalyLabel(value: ReadingClockAnomaly): Int = when (value) {
    ReadingClockAnomaly.DEVICE_REBOOTED -> R.string.anomaly_device_rebooted
    ReadingClockAnomaly.WALL_CLOCK_MOVED_BACKWARD -> R.string.anomaly_wall_clock_rollback
    ReadingClockAnomaly.CLOCKS_DIVERGED -> R.string.anomaly_clocks_diverged
    ReadingClockAnomaly.UNUSUALLY_LONG_SEGMENT -> R.string.anomaly_long_segment
}
