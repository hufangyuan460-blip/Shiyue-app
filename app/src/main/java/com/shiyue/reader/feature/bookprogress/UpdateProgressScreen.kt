package com.shiyue.reader.feature.bookprogress

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
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
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.shiyue.reader.R
import com.shiyue.reader.feature.bookdetail.BackButton

object UpdateProgressTestTags {
    const val Page = "update_progress_page"
    const val Save = "update_progress_save"
}

@Composable
fun UpdateProgressRoute(
    onBack: () -> Unit,
    viewModel: UpdateProgressViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(viewModel, lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            viewModel.events.collect { event ->
                if (event == UpdateProgressEvent.Saved) onBack()
            }
        }
    }
    UpdateProgressScreen(
        uiState = uiState,
        onPageChanged = viewModel::onPageChanged,
        onSave = viewModel::save,
        onBack = onBack,
        onConfirmRewind = viewModel::confirmRewind,
        onMarkFinished = viewModel::finishAndMarkRead,
        onKeepStatus = viewModel::finishKeepingStatus,
        onCancelConfirmation = viewModel::cancelConfirmation,
        onRetry = viewModel::retry,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UpdateProgressScreen(
    uiState: UpdateProgressUiState,
    onPageChanged: (String) -> Unit,
    onSave: () -> Unit,
    onBack: () -> Unit,
    onConfirmRewind: () -> Unit,
    onMarkFinished: () -> Unit,
    onKeepStatus: () -> Unit,
    onCancelConfirmation: () -> Unit,
    onRetry: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.update_reading_progress)) },
                navigationIcon = { BackButton(onBack) },
            )
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
            when {
                uiState.isLoading -> CircularProgressIndicator()
                uiState.notFound -> ProgressMessage(stringResource(R.string.book_not_found), onBack)
                uiState.loadFailed -> ProgressMessage(
                    message = stringResource(R.string.book_detail_load_failed),
                    action = stringResource(R.string.retry),
                    onAction = onRetry,
                )
                uiState.book != null -> ProgressForm(uiState, onPageChanged, onSave, onBack)
            }
        }
    }
    when (uiState.confirmation) {
        ProgressConfirmation.REWIND -> AlertDialog(
            onDismissRequest = onCancelConfirmation,
            title = { Text(stringResource(R.string.confirm_rewind_title)) },
            text = { Text(stringResource(R.string.confirm_rewind_message, uiState.book?.currentPage ?: 0, uiState.parsedPage ?: 0)) },
            confirmButton = { TextButton(onClick = onConfirmRewind) { Text(stringResource(R.string.confirm_update)) } },
            dismissButton = { TextButton(onClick = onCancelConfirmation) { Text(stringResource(R.string.cancel)) } },
        )
        ProgressConfirmation.FINISH -> AlertDialog(
            onDismissRequest = onCancelConfirmation,
            title = { Text(stringResource(R.string.finish_book_title)) },
            text = { Text(stringResource(R.string.finish_book_message)) },
            confirmButton = { TextButton(onClick = onMarkFinished) { Text(stringResource(R.string.mark_as_finished)) } },
            dismissButton = {
                Column(horizontalAlignment = Alignment.End) {
                    TextButton(onClick = onKeepStatus) { Text(stringResource(R.string.update_page_only)) }
                    TextButton(onClick = onCancelConfirmation) { Text(stringResource(R.string.cancel)) }
                }
            },
        )
        null -> Unit
    }
}

@Composable
private fun ProgressForm(
    uiState: UpdateProgressUiState,
    onPageChanged: (String) -> Unit,
    onSave: () -> Unit,
    onBack: () -> Unit,
) {
    val book = requireNotNull(uiState.book)
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())
            .navigationBarsPadding().imePadding().padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text(book.title, style = MaterialTheme.typography.titleLarge)
        Text(stringResource(R.string.current_and_total_pages, book.currentPage, book.totalPages))
        OutlinedTextField(
            value = uiState.pageInput,
            onValueChange = onPageChanged,
            modifier = Modifier.fillMaxWidth().testTag(UpdateProgressTestTags.Page),
            label = { Text(stringResource(R.string.new_page_label)) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            isError = uiState.pageError,
            supportingText = if (uiState.pageError) {
                { Text(stringResource(R.string.new_page_error, book.totalPages)) }
            } else null,
            enabled = !uiState.isSaving,
        )
        uiState.previewPercent?.let { percent ->
            Text(stringResource(R.string.progress_preview, percent), color = MaterialTheme.colorScheme.secondary)
        }
        if (uiState.saveFailed) {
            Text(stringResource(R.string.update_progress_failed), color = MaterialTheme.colorScheme.error)
        }
        Spacer(Modifier.height(10.dp))
        Button(
            onClick = onSave,
            enabled = !uiState.isSaving,
            modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp).testTag(UpdateProgressTestTags.Save),
        ) { Text(if (uiState.isSaving) stringResource(R.string.saving_book) else stringResource(R.string.save_progress)) }
        OutlinedButton(onClick = onBack, enabled = !uiState.isSaving, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) {
            Text(stringResource(R.string.cancel))
        }
    }
}

@Composable
private fun ProgressMessage(
    message: String,
    onAction: () -> Unit,
    action: String = stringResource(R.string.back_to_book_detail),
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(message)
        Button(onClick = onAction) { Text(action) }
    }
}
