package com.shiyue.reader.feature.category

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.shiyue.reader.R
import com.shiyue.reader.feature.bookdetail.BackButton

@Composable
fun CategoryManagerRoute(onBack: () -> Unit, viewModel: CategoryManagerViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    CategoryManagerScreen(
        state, onBack, viewModel::showCreate, viewModel::showRename, viewModel::showDelete,
        viewModel::move, viewModel::onNameChanged, viewModel::confirmName,
        viewModel::confirmDelete, viewModel::dismissDialog,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryManagerScreen(
    state: CategoryManagerUiState,
    onBack: () -> Unit,
    onCreate: () -> Unit,
    onRename: (com.shiyue.reader.core.model.CategorySummary) -> Unit,
    onDelete: (com.shiyue.reader.core.model.CategorySummary) -> Unit,
    onMove: (String, Int) -> Unit,
    onNameChanged: (String) -> Unit,
    onConfirmName: () -> Unit,
    onConfirmDelete: () -> Unit,
    onDismiss: () -> Unit,
) {
    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.manage_categories)) }, navigationIcon = { BackButton(onBack) }) },
        floatingActionButton = { Button(onClick = onCreate) { Text(stringResource(R.string.create_category)) } },
    ) { padding ->
        when {
            state.isLoading -> Column(Modifier.fillMaxSize().padding(padding), horizontalAlignment = Alignment.CenterHorizontally) { CircularProgressIndicator() }
            state.categories.isEmpty() -> Column(
                Modifier.fillMaxSize().padding(padding).padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) { Text(stringResource(R.string.no_categories)); Button(onClick = onCreate) { Text(stringResource(R.string.create_category)) } }
            else -> LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                itemsIndexed(state.categories, key = { _, item -> item.category.id }) { index, item ->
                    androidx.compose.material3.Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(item.category.name, style = MaterialTheme.typography.titleMedium)
                            Text(stringResource(R.string.category_book_count, item.bookCount))
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                val moveUp = stringResource(R.string.move_category_up, item.category.name)
                                val moveDown = stringResource(R.string.move_category_down, item.category.name)
                                IconButton(
                                    onClick = { onMove(item.category.id, -1) },
                                    enabled = index > 0,
                                    modifier = Modifier.semantics { contentDescription = moveUp },
                                ) { Text(stringResource(R.string.move_up_symbol)) }
                                IconButton(
                                    onClick = { onMove(item.category.id, 1) },
                                    enabled = index < state.categories.lastIndex,
                                    modifier = Modifier.semantics { contentDescription = moveDown },
                                ) { Text(stringResource(R.string.move_down_symbol)) }
                                TextButton(onClick = { onRename(item) }) { Text(stringResource(R.string.rename)) }
                                TextButton(onClick = { onDelete(item) }) { Text(stringResource(R.string.delete), color = MaterialTheme.colorScheme.error) }
                            }
                        }
                    }
                }
            }
        }
        if (state.actionFailed) Text(
            stringResource(R.string.category_action_failed),
            color = MaterialTheme.colorScheme.error,
            modifier = Modifier.padding(padding).padding(16.dp),
        )
    }

    when (val dialog = state.dialog) {
        CategoryDialog.Create, is CategoryDialog.Rename -> AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(stringResource(if (dialog == CategoryDialog.Create) R.string.create_category else R.string.rename_category)) },
            text = {
                OutlinedTextField(
                    value = state.nameInput,
                    onValueChange = onNameChanged,
                    label = { Text(stringResource(R.string.category_name)) },
                    isError = state.nameError || state.conflictError,
                    supportingText = {
                        when {
                            state.conflictError -> Text(stringResource(R.string.category_name_conflict))
                            state.nameError -> Text(stringResource(R.string.category_name_error))
                        }
                    },
                )
            },
            confirmButton = { TextButton(onClick = onConfirmName, enabled = !state.isSaving) { Text(stringResource(R.string.save_changes)) } },
            dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
        )
        is CategoryDialog.Delete -> AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(stringResource(R.string.delete_category_title, dialog.name)) },
            text = { Text(stringResource(R.string.delete_category_message)) },
            confirmButton = { TextButton(onClick = onConfirmDelete, enabled = !state.isSaving) { Text(stringResource(R.string.delete), color = MaterialTheme.colorScheme.error) } },
            dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
        )
        null -> Unit
    }
}
