package com.shiyue.reader.feature.bookedit

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.shiyue.reader.R
import com.shiyue.reader.core.model.BookStatus
import com.shiyue.reader.core.ui.theme.ShiyueTheme
import com.shiyue.reader.feature.bookshelf.statusLabel

object AddBookTestTags {
    const val Title = "add_book_title"
    const val Author = "add_book_author"
    const val TotalPages = "add_book_total_pages"
    const val Status = "add_book_status"
    const val Save = "add_book_save"
}

@Composable
fun AddBookRoute(
    onBack: () -> Unit,
    viewModel: AddBookViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val lifecycleOwner = LocalLifecycleOwner.current

    LaunchedEffect(viewModel, lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            viewModel.events.collect { event ->
                when (event) {
                    AddBookEvent.Saved -> onBack()
                }
            }
        }
    }

    AddBookScreen(
        uiState = uiState,
        onTitleChanged = viewModel::onTitleChanged,
        onAuthorChanged = viewModel::onAuthorChanged,
        onTotalPagesChanged = viewModel::onTotalPagesChanged,
        onStatusChanged = viewModel::onStatusChanged,
        onSave = viewModel::save,
        onBack = onBack,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddBookScreen(
    uiState: AddBookUiState,
    onTitleChanged: (String) -> Unit,
    onAuthorChanged: (String) -> Unit,
    onTotalPagesChanged: (String) -> Unit,
    onStatusChanged: (BookStatus) -> Unit,
    onSave: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.add_book_title)) },
                navigationIcon = {
                    val backDescription = stringResource(R.string.navigate_back)
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.semantics { contentDescription = backDescription },
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_arrow_back),
                            contentDescription = null,
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = 20.dp, vertical = 12.dp),
        ) {
            Text(
                text = stringResource(R.string.add_book_intro),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(24.dp))
            OutlinedTextField(
                value = uiState.title,
                onValueChange = onTitleChanged,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(AddBookTestTags.Title),
                label = { Text(stringResource(R.string.book_title_label)) },
                supportingText = if (uiState.titleError) {
                    { Text(stringResource(R.string.book_title_error)) }
                } else {
                    null
                },
                isError = uiState.titleError,
                singleLine = true,
                enabled = !uiState.isSaving,
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = uiState.author,
                onValueChange = onAuthorChanged,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(AddBookTestTags.Author),
                label = { Text(stringResource(R.string.book_author_label)) },
                singleLine = true,
                enabled = !uiState.isSaving,
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = uiState.totalPages,
                onValueChange = onTotalPagesChanged,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(AddBookTestTags.TotalPages),
                label = { Text(stringResource(R.string.book_total_pages_label)) },
                supportingText = if (uiState.totalPagesError) {
                    { Text(stringResource(R.string.book_total_pages_error)) }
                } else {
                    null
                },
                isError = uiState.totalPagesError,
                singleLine = true,
                enabled = !uiState.isSaving,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            )
            Spacer(Modifier.height(16.dp))
            BookStatusField(
                status = uiState.status,
                enabled = !uiState.isSaving,
                onStatusChanged = onStatusChanged,
                testTag = AddBookTestTags.Status,
            )
            if (uiState.saveFailed) {
                Spacer(Modifier.height(16.dp))
                Text(
                    text = stringResource(R.string.add_book_save_failed),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                )
            }
            Spacer(Modifier.height(28.dp))
            Button(
                onClick = onSave,
                enabled = !uiState.isSaving,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 52.dp)
                    .testTag(AddBookTestTags.Save),
            ) {
                Text(
                    if (uiState.isSaving) {
                        stringResource(R.string.saving_book)
                    } else {
                        stringResource(R.string.save_book)
                    },
                )
            }
        }
    }
}

@Composable
fun BookStatusField(
    status: BookStatus,
    enabled: Boolean,
    onStatusChanged: (BookStatus) -> Unit,
    testTag: String,
) {
    var expanded by remember { mutableStateOf(false) }
    Column {
        Text(
            text = stringResource(R.string.book_status_label),
            style = MaterialTheme.typography.labelLarge,
        )
        Spacer(Modifier.height(8.dp))
        Box(modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(
                onClick = { expanded = true },
                enabled = enabled,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 52.dp)
                    .testTag(testTag),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = statusLabel(status),
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 8.dp),
                    )
                    Text(stringResource(R.string.open_status_options))
                }
            }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.fillMaxWidth(0.8f),
            ) {
                BookStatus.entries.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(statusLabel(option)) },
                        onClick = {
                            onStatusChanged(option)
                            expanded = false
                        },
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun AddBookScreenPreview() {
    ShiyueTheme {
        AddBookScreen(
            uiState = AddBookUiState(),
            onTitleChanged = {},
            onAuthorChanged = {},
            onTotalPagesChanged = {},
            onStatusChanged = {},
            onSave = {},
            onBack = {},
        )
    }
}
