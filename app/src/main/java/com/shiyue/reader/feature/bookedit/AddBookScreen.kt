package com.shiyue.reader.feature.bookedit

import android.content.Intent
import android.net.Uri
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.BackHandler
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
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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

object AddBookTestTags {
    const val Title = "add_book_title"
    const val Author = "add_book_author"
    const val TotalPages = "add_book_total_pages"
    const val Status = "add_book_status"
    const val Save = "add_book_save"
}

@Composable
fun AddBookRoute(onBack: () -> Unit, viewModel: AddBookViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val lifecycleOwner = LocalLifecycleOwner.current
    val context = LocalContext.current
    val cameraAvailable = remember {
        Intent(MediaStore.ACTION_IMAGE_CAPTURE).resolveActivity(context.packageManager) != null
    }
    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture(), viewModel::onCameraResult)
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri?.let { viewModel.onCoverSourceSelected(it.toString()) }
    }
    val discardAndBack = { viewModel.discardChanges(); onBack() }
    BackHandler(onBack = discardAndBack)
    LaunchedEffect(viewModel, lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            viewModel.events.collect { event ->
                when (event) {
                    AddBookEvent.Saved -> onBack()
                    is AddBookEvent.LaunchCamera -> camera.launch(Uri.parse(event.uri))
                }
            }
        }
    }
    AddBookScreen(
        uiState = state,
        onTitleChanged = viewModel::onTitleChanged,
        onAuthorChanged = viewModel::onAuthorChanged,
        onTotalPagesChanged = viewModel::onTotalPagesChanged,
        onStatusChanged = viewModel::onStatusChanged,
        onSave = viewModel::save,
        onBack = discardAndBack,
        onToggleCategory = viewModel::toggleCategory,
        onNewCategoryNameChanged = viewModel::onNewCategoryNameChanged,
        onCreateCategory = viewModel::createCategory,
        onPickPhoto = { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
        onTakePhoto = viewModel::requestCamera,
        cameraAvailable = cameraAvailable,
        onRemoveCover = viewModel::removePendingCover,
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
fun AddBookScreen(
    uiState: AddBookUiState,
    onTitleChanged: (String) -> Unit,
    onAuthorChanged: (String) -> Unit,
    onTotalPagesChanged: (String) -> Unit,
    onStatusChanged: (BookStatus) -> Unit,
    onSave: () -> Unit,
    onBack: () -> Unit,
    onToggleCategory: (String) -> Unit = {},
    onNewCategoryNameChanged: (String) -> Unit = {},
    onCreateCategory: () -> Unit = {},
    onPickPhoto: () -> Unit = {},
    onTakePhoto: () -> Unit = {},
    cameraAvailable: Boolean = false,
    onRemoveCover: () -> Unit = {},
) {
    val state = uiState
    Scaffold(topBar = { TopAppBar(title = { Text(stringResource(R.string.add_book_title)) }, navigationIcon = { BackButton(onBack) }) }) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState())
                .navigationBarsPadding().imePadding().padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            state.pendingCover?.let {
                AsyncImage(it.sourceUri, stringResource(R.string.crop_cover_preview_description), Modifier.fillMaxWidth().height(210.dp))
            }
            CoverActions(state.pendingCover != null, cameraAvailable, !state.isSaving, onPickPhoto, onTakePhoto, onRemoveCover)
            OutlinedTextField(state.title, onTitleChanged, Modifier.fillMaxWidth().testTag(AddBookTestTags.Title), label = { Text(stringResource(R.string.book_title_label)) }, isError = state.titleError, supportingText = if (state.titleError) {{ Text(stringResource(R.string.book_title_error)) }} else null, singleLine = true)
            OutlinedTextField(state.author, onAuthorChanged, Modifier.fillMaxWidth().testTag(AddBookTestTags.Author), label = { Text(stringResource(R.string.book_author_label)) }, singleLine = true)
            OutlinedTextField(state.totalPages, onTotalPagesChanged, Modifier.fillMaxWidth().testTag(AddBookTestTags.TotalPages), label = { Text(stringResource(R.string.book_total_pages_label)) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), isError = state.totalPagesError, supportingText = if (state.totalPagesError) {{ Text(stringResource(R.string.book_total_pages_error)) }} else null, singleLine = true)
            BookStatusField(state.status, !state.isSaving, onStatusChanged, AddBookTestTags.Status)
            CategorySelector(state.categories, state.selectedCategoryIds, state.newCategoryName, state.categoryError, !state.isSaving, onToggleCategory, onNewCategoryNameChanged, onCreateCategory)
            if (state.saveFailed) Text(stringResource(R.string.add_book_save_failed), color = androidx.compose.material3.MaterialTheme.colorScheme.error)
            Spacer(Modifier.height(8.dp))
            Button(onSave, Modifier.fillMaxWidth().heightIn(min = 52.dp).testTag(AddBookTestTags.Save), enabled = !state.isSaving) {
                Text(if (state.isSaving) stringResource(R.string.saving_book) else stringResource(R.string.save_book))
            }
        }
    }
}
