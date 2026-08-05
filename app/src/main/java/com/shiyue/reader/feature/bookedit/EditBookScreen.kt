package com.shiyue.reader.feature.bookedit

import android.content.Intent
import android.net.Uri
import android.provider.MediaStore
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import coil3.compose.AsyncImage
import com.shiyue.reader.R
import com.shiyue.reader.core.model.BookStatus
import com.shiyue.reader.feature.bookdetail.BackButton
import java.io.File

object EditBookTestTags {
    const val Title = "edit_book_title"
    const val Author = "edit_book_author"
    const val TotalPages = "edit_book_total_pages"
    const val Status = "edit_book_status"
    const val Save = "edit_book_save"
}

@Composable
fun EditBookRoute(onBack: () -> Unit, viewModel: EditBookViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val owner = LocalLifecycleOwner.current
    val context = LocalContext.current
    val cameraAvailable = remember { Intent(MediaStore.ACTION_IMAGE_CAPTURE).resolveActivity(context.packageManager) != null }
    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture(), viewModel::onCameraResult)
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri?.let { viewModel.onCoverSourceSelected(it.toString()) }
    }
    LaunchedEffect(viewModel, owner) {
        owner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            viewModel.events.collect { event ->
                when (event) {
                    EditBookEvent.Saved -> onBack()
                    is EditBookEvent.LaunchCamera -> camera.launch(Uri.parse(event.uri))
                }
            }
        }
    }
    EditBookScreen(
        uiState = state,
        onTitleChanged = viewModel::onTitleChanged,
        onAuthorChanged = viewModel::onAuthorChanged,
        onTotalPagesChanged = viewModel::onTotalPagesChanged,
        onStatusChanged = viewModel::onStatusChanged,
        onToggleCategory = viewModel::toggleCategory,
        onNewCategoryNameChanged = viewModel::onNewCategoryNameChanged,
        onCreateCategory = viewModel::createCategory,
        onSave = viewModel::save,
        onRetry = viewModel::retry,
        onBack = onBack,
        onDiscard = { viewModel.discardChanges(); onBack() },
        onPickPhoto = { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
        onTakePhoto = viewModel::requestCamera,
        cameraAvailable = cameraAvailable,
        onRemoveCover = viewModel::removeCover,
    )
    state.cropSourceUri?.let { source ->
        CoverCropDialog(
            source, state.cropTransform, viewModel::onCropTransformChanged, viewModel::confirmCrop,
            viewModel::cancelCrop,
            onReselect = { viewModel.cancelCrop(); picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
        )
    }
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
    onToggleCategory: (String) -> Unit = {},
    onNewCategoryNameChanged: (String) -> Unit = {},
    onCreateCategory: () -> Unit = {},
    onDiscard: () -> Unit = onBack,
    onPickPhoto: () -> Unit = {},
    onTakePhoto: () -> Unit = {},
    cameraAvailable: Boolean = false,
    onRemoveCover: () -> Unit = {},
) {
    val state = uiState
    var discardDialog by rememberSaveable { mutableStateOf(false) }
    val requestBack = { if (state.isDirty) discardDialog = true else onBack() }
    BackHandler(onBack = requestBack)
    Scaffold(topBar = { TopAppBar(title = { Text(stringResource(R.string.edit_book_title)) }, navigationIcon = { BackButton(requestBack) }) }) { padding ->
        Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
            when {
                state.isLoading -> CircularProgressIndicator()
                state.notFound -> Message(stringResource(R.string.book_not_found), stringResource(R.string.back_to_book_detail), onBack)
                state.loadFailed -> Message(stringResource(R.string.book_detail_load_failed), stringResource(R.string.retry), onRetry)
                else -> EditForm(
                    state, onTitleChanged, onAuthorChanged, onTotalPagesChanged, onStatusChanged,
                    onToggleCategory, onNewCategoryNameChanged, onCreateCategory, onSave,
                    onPickPhoto, onTakePhoto, cameraAvailable, onRemoveCover,
                )
            }
        }
    }
    if (discardDialog) AlertDialog(
        onDismissRequest = { discardDialog = false },
        title = { Text(stringResource(R.string.discard_changes_title)) },
        text = { Text(stringResource(R.string.discard_changes_message)) },
        confirmButton = { TextButton(onClick = onDiscard) { Text(stringResource(R.string.discard_changes)) } },
        dismissButton = { TextButton(onClick = { discardDialog = false }) { Text(stringResource(R.string.continue_editing)) } },
    )
}

@Composable
private fun EditForm(
    state: EditBookUiState,
    onTitleChanged: (String) -> Unit,
    onAuthorChanged: (String) -> Unit,
    onTotalPagesChanged: (String) -> Unit,
    onStatusChanged: (BookStatus) -> Unit,
    onToggleCategory: (String) -> Unit,
    onNewCategoryNameChanged: (String) -> Unit,
    onCreateCategory: () -> Unit,
    onSave: () -> Unit,
    onPickPhoto: () -> Unit,
    onTakePhoto: () -> Unit,
    cameraAvailable: Boolean,
    onRemoveCover: () -> Unit,
) {
    val context = LocalContext.current
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).navigationBarsPadding().imePadding().padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        when {
            state.pendingCover != null -> AsyncImage(state.pendingCover.sourceUri, stringResource(R.string.crop_cover_preview_description), Modifier.fillMaxWidth().height(210.dp))
            !state.removeCover && state.coverPath != null -> AsyncImage(File(context.filesDir, state.coverPath), stringResource(R.string.book_cover_label), Modifier.fillMaxWidth().height(210.dp))
        }
        CoverActions(
            hasCover = state.pendingCover != null || (!state.removeCover && state.coverPath != null),
            cameraAvailable = cameraAvailable,
            enabled = !state.isSaving,
            onPickPhoto = onPickPhoto,
            onTakePhoto = onTakePhoto,
            onRemove = onRemoveCover,
        )
        OutlinedTextField(state.title, onTitleChanged, Modifier.fillMaxWidth().testTag(EditBookTestTags.Title), label = { Text(stringResource(R.string.book_title_label)) }, isError = state.titleError, supportingText = if (state.titleError) {{ Text(stringResource(R.string.book_title_error)) }} else null, singleLine = true)
        OutlinedTextField(state.author, onAuthorChanged, Modifier.fillMaxWidth().testTag(EditBookTestTags.Author), label = { Text(stringResource(R.string.book_author_label)) }, singleLine = true)
        val pageError = when (state.totalPagesError) {
            EditTotalPagesError.INVALID -> stringResource(R.string.book_total_pages_error)
            EditTotalPagesError.BELOW_CURRENT_PAGE -> stringResource(R.string.book_total_pages_below_current, state.currentPage)
            null -> null
        }
        OutlinedTextField(state.totalPages, onTotalPagesChanged, Modifier.fillMaxWidth().testTag(EditBookTestTags.TotalPages), label = { Text(stringResource(R.string.book_total_pages_label)) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), isError = pageError != null, supportingText = pageError?.let { text -> { Text(text) } }, singleLine = true)
        BookStatusField(state.status, !state.isSaving, onStatusChanged, EditBookTestTags.Status)
        CategorySelector(state.categories, state.selectedCategoryIds, state.newCategoryName, state.categoryError, !state.isSaving, onToggleCategory, onNewCategoryNameChanged, onCreateCategory)
        if (state.saveFailed) Text(stringResource(R.string.edit_book_save_failed), color = MaterialTheme.colorScheme.error)
        Spacer(Modifier.height(8.dp))
        Button(onSave, Modifier.fillMaxWidth().heightIn(min = 52.dp).testTag(EditBookTestTags.Save), enabled = !state.isSaving) {
            Text(if (state.isSaving) stringResource(R.string.saving_book) else stringResource(R.string.save_changes))
        }
    }
}

@Composable
private fun Message(message: String, action: String, onAction: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(message)
        Button(onClick = onAction) { Text(action) }
    }
}
