package com.shiyue.reader.feature.bookedit

import androidx.activity.compose.BackHandler
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
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
import com.shiyue.reader.core.model.BookStatus
import com.shiyue.reader.feature.bookdetail.BackButton

object EditBookTestTags {
    const val Title = "edit_book_title"
    const val Author = "edit_book_author"
    const val TotalPages = "edit_book_total_pages"
    const val Status = "edit_book_status"
    const val Save = "edit_book_save"
}

@Composable
fun EditBookRoute(
    onBack: () -> Unit,
    viewModel: EditBookViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(viewModel, lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            viewModel.events.collect { event ->
                if (event == EditBookEvent.Saved) onBack()
            }
        }
    }
    EditBookScreen(
        uiState = uiState,
        onTitleChanged = viewModel::onTitleChanged,
        onAuthorChanged = viewModel::onAuthorChanged,
        onTotalPagesChanged = viewModel::onTotalPagesChanged,
        onStatusChanged = viewModel::onStatusChanged,
        onSave = viewModel::save,
        onRetry = viewModel::retry,
        onBack = onBack,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditBookScreen(
    uiState: EditBookUiState,
    onTitleChanged: (String) -> Unit,
    onAuthorChanged: (String) -> Unit,
    onTotalPagesChanged: (String) -> Unit,
    onStatusChanged: (BookStatus) -> Unit,
    onSave: () -> Unit,
    onRetry: () -> Unit,
    onBack: () -> Unit,
) {
    var showDiscardDialog by rememberSaveable { mutableStateOf(false) }
    val requestBack = { if (uiState.isDirty) showDiscardDialog = true else onBack() }
    BackHandler(onBack = requestBack)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.edit_book_title)) },
                navigationIcon = { BackButton(requestBack) },
            )
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
            when {
                uiState.isLoading -> CircularProgressIndicator()
                uiState.notFound -> EditMessage(stringResource(R.string.book_not_found), onBack)
                uiState.loadFailed -> EditMessage(
                    message = stringResource(R.string.book_detail_load_failed),
                    action = stringResource(R.string.retry),
                    onAction = onRetry,
                )
                else -> EditForm(
                    uiState = uiState,
                    onTitleChanged = onTitleChanged,
                    onAuthorChanged = onAuthorChanged,
                    onTotalPagesChanged = onTotalPagesChanged,
                    onStatusChanged = onStatusChanged,
                    onSave = onSave,
                )
            }
        }
    }
    if (showDiscardDialog) {
        AlertDialog(
            onDismissRequest = { showDiscardDialog = false },
            title = { Text(stringResource(R.string.discard_changes_title)) },
            text = { Text(stringResource(R.string.discard_changes_message)) },
            confirmButton = {
                TextButton(onClick = onBack) { Text(stringResource(R.string.discard_changes)) }
            },
            dismissButton = {
                TextButton(onClick = { showDiscardDialog = false }) {
                    Text(stringResource(R.string.continue_editing))
                }
            },
        )
    }
}

@Composable
private fun EditForm(
    uiState: EditBookUiState,
    onTitleChanged: (String) -> Unit,
    onAuthorChanged: (String) -> Unit,
    onTotalPagesChanged: (String) -> Unit,
    onStatusChanged: (BookStatus) -> Unit,
    onSave: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())
            .navigationBarsPadding().imePadding().padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        OutlinedTextField(
            value = uiState.title,
            onValueChange = onTitleChanged,
            modifier = Modifier.fillMaxWidth().testTag(EditBookTestTags.Title),
            label = { Text(stringResource(R.string.book_title_label)) },
            isError = uiState.titleError,
            supportingText = if (uiState.titleError) {{ Text(stringResource(R.string.book_title_error)) }} else null,
            enabled = !uiState.isSaving,
            singleLine = true,
        )
        OutlinedTextField(
            value = uiState.author,
            onValueChange = onAuthorChanged,
            modifier = Modifier.fillMaxWidth().testTag(EditBookTestTags.Author),
            label = { Text(stringResource(R.string.book_author_label)) },
            enabled = !uiState.isSaving,
            singleLine = true,
        )
        val pagesErrorText = when (uiState.totalPagesError) {
            EditTotalPagesError.INVALID -> stringResource(R.string.book_total_pages_error)
            EditTotalPagesError.BELOW_CURRENT_PAGE -> stringResource(
                R.string.book_total_pages_below_current,
                uiState.currentPage,
            )
            null -> null
        }
        OutlinedTextField(
            value = uiState.totalPages,
            onValueChange = onTotalPagesChanged,
            modifier = Modifier.fillMaxWidth().testTag(EditBookTestTags.TotalPages),
            label = { Text(stringResource(R.string.book_total_pages_label)) },
            isError = pagesErrorText != null,
            supportingText = pagesErrorText?.let { text -> { Text(text) } },
            enabled = !uiState.isSaving,
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        )
        Spacer(Modifier.height(4.dp))
        BookStatusField(uiState.status, !uiState.isSaving, onStatusChanged, EditBookTestTags.Status)
        if (uiState.saveFailed) {
            Text(stringResource(R.string.edit_book_save_failed), color = MaterialTheme.colorScheme.error)
        }
        Spacer(Modifier.height(12.dp))
        Button(
            onClick = onSave,
            enabled = !uiState.isSaving,
            modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp).testTag(EditBookTestTags.Save),
        ) {
            Text(if (uiState.isSaving) stringResource(R.string.saving_book) else stringResource(R.string.save_changes))
        }
    }
}

@Composable
private fun EditMessage(
    message: String,
    onAction: () -> Unit,
    action: String = stringResource(R.string.back_to_book_detail),
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(message)
        Button(onClick = onAction) { Text(action) }
    }
}
