package com.shiyue.reader.feature.note

import android.content.Intent
import android.net.Uri
import android.provider.MediaStore
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import coil3.compose.AsyncImage
import com.shiyue.reader.R
import com.shiyue.reader.feature.bookdetail.BackButton
import java.io.File
import kotlinx.coroutines.flow.Flow

object NoteFormTestTags {
    const val Screen = "note_form_screen"
    const val Content = "note_form_content"
    const val Save = "note_form_save"
}

@Composable
fun CreateNoteRoute(
    onBack: () -> Unit,
    viewModel: CreateNoteViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    NoteFormLaunchers(
        events = viewModel.events,
        state = state,
        titleRes = R.string.note_write_title,
        onBack = onBack,
        onPageChanged = viewModel::onPageChanged,
        onContentChanged = viewModel::onContentChanged,
        onImagePicked = viewModel::onImagePicked,
        onTakePhoto = viewModel::requestCamera,
        onRemoveImage = viewModel::onRemoveImage,
        onSave = viewModel::save,
        onDiscard = viewModel::discard,
        onCameraResult = viewModel::onCameraResult,
        showDelete = false,
        onDelete = null,
    )
}

@Composable
fun EditNoteRoute(
    onBack: () -> Unit,
    viewModel: EditNoteViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var confirmDelete by remember { mutableStateOf(false) }
    NoteFormLaunchers(
        events = viewModel.events,
        state = state,
        titleRes = R.string.note_edit_title,
        onBack = onBack,
        onPageChanged = viewModel::onPageChanged,
        onContentChanged = viewModel::onContentChanged,
        onImagePicked = viewModel::onImagePicked,
        onTakePhoto = viewModel::requestCamera,
        onRemoveImage = viewModel::onRemoveImage,
        onSave = viewModel::save,
        onDiscard = viewModel::discard,
        onCameraResult = viewModel::onCameraResult,
        showDelete = true,
        onDelete = { confirmDelete = true },
    )
    if (confirmDelete) AlertDialog(
        onDismissRequest = { confirmDelete = false },
        title = { Text(stringResource(R.string.note_delete_confirm_title)) },
        text = { Text(stringResource(R.string.note_delete_confirm_message)) },
        confirmButton = {
            TextButton(onClick = { confirmDelete = false; viewModel.delete() }) {
                Text(stringResource(R.string.delete), color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = {
            TextButton(onClick = { confirmDelete = false }) { Text(stringResource(R.string.cancel)) }
        },
    )
}

@Composable
private fun NoteFormLaunchers(
    events: Flow<NoteFormEvent>,
    state: NoteFormUiState,
    @StringRes titleRes: Int,
    onBack: () -> Unit,
    onPageChanged: (String) -> Unit,
    onContentChanged: (String) -> Unit,
    onImagePicked: (String) -> Unit,
    onTakePhoto: () -> Unit,
    onRemoveImage: () -> Unit,
    onSave: () -> Unit,
    onDiscard: () -> Unit,
    onCameraResult: (Boolean) -> Unit,
    showDelete: Boolean,
    onDelete: (() -> Unit)?,
) {
    val owner = LocalLifecycleOwner.current
    val context = LocalContext.current
    val cameraAvailable = remember {
        Intent(MediaStore.ACTION_IMAGE_CAPTURE).resolveActivity(context.packageManager) != null
    }
    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture(), onCameraResult)
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri?.let { onImagePicked(it.toString()) }
    }
    val discardAndBack = { onDiscard(); onBack() }
    BackHandler(onBack = discardAndBack)
    LaunchedEffect(events, owner) {
        owner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            events.collect { event ->
                when (event) {
                    NoteFormEvent.Saved -> onBack()
                    NoteFormEvent.Deleted -> onBack()
                    is NoteFormEvent.LaunchCamera -> camera.launch(Uri.parse(event.uri))
                }
            }
        }
    }
    NoteFormScreen(
        state = state,
        titleRes = titleRes,
        onBack = discardAndBack,
        onPageChanged = onPageChanged,
        onContentChanged = onContentChanged,
        onPickGallery = { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
        onTakePhoto = onTakePhoto,
        onRemoveImage = onRemoveImage,
        onSave = onSave,
        cameraAvailable = cameraAvailable,
        showDelete = showDelete,
        onDelete = onDelete,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteFormScreen(
    state: NoteFormUiState,
    @StringRes titleRes: Int,
    onBack: () -> Unit,
    onPageChanged: (String) -> Unit,
    onContentChanged: (String) -> Unit,
    onPickGallery: () -> Unit,
    onTakePhoto: () -> Unit,
    onRemoveImage: () -> Unit,
    onSave: () -> Unit,
    cameraAvailable: Boolean,
    showDelete: Boolean,
    onDelete: (() -> Unit)?,
) {
    Scaffold(
        modifier = Modifier.testTag(NoteFormTestTags.Screen),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(titleRes)) },
                navigationIcon = { BackButton(onBack) },
            )
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
            when {
                state.isLoading -> CircularProgressIndicator()
                state.notFound -> Text(stringResource(R.string.note_not_found))
                else -> NoteFormBody(state, onPageChanged, onContentChanged, onPickGallery, onTakePhoto, onRemoveImage, onSave, cameraAvailable, showDelete, onDelete)
            }
        }
    }
}

@Composable
private fun NoteFormBody(
    state: NoteFormUiState,
    onPageChanged: (String) -> Unit,
    onContentChanged: (String) -> Unit,
    onPickGallery: () -> Unit,
    onTakePhoto: () -> Unit,
    onRemoveImage: () -> Unit,
    onSave: () -> Unit,
    cameraAvailable: Boolean,
    showDelete: Boolean,
    onDelete: (() -> Unit)?,
) {
    val contentBlank = state.content.trim().isEmpty()
    val pageInvalid = state.pageText.isNotBlank() && state.pageText.trim().toIntOrNull() == null
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        if (state.bookTitle.isNotBlank()) {
            Text(
                stringResource(R.string.note_book_header, state.bookTitle),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
            )
        }
        OutlinedTextField(
            value = state.pageText,
            onValueChange = onPageChanged,
            label = { Text(stringResource(R.string.note_page_label)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        if (pageInvalid) Text(
            stringResource(R.string.note_page_error),
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodySmall,
        )
        OutlinedTextField(
            value = state.content,
            onValueChange = onContentChanged,
            label = { Text(stringResource(R.string.note_content_label)) },
            minLines = 5,
            modifier = Modifier.fillMaxWidth().testTag(NoteFormTestTags.Content),
        )
        NoteImagePicker(
            imageSource = state.imageSource,
            onPickGallery = onPickGallery,
            onTakePhoto = onTakePhoto,
            onRemoveImage = onRemoveImage,
            cameraAvailable = cameraAvailable,
        )
        if (state.failed) Text(
            stringResource(R.string.note_save_failed),
            color = MaterialTheme.colorScheme.error,
        )
        Button(
            onClick = onSave,
            enabled = !state.isSaving && !contentBlank && !pageInvalid,
            modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp).testTag(NoteFormTestTags.Save),
        ) {
            Text(stringResource(if (state.isSaving) R.string.note_saving else R.string.note_save))
        }
        if (showDelete && onDelete != null) {
            OutlinedButton(
                onClick = onDelete,
                enabled = !state.isSaving,
                modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
            ) {
                Text(stringResource(R.string.note_delete), color = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
private fun NoteImagePicker(
    imageSource: NoteImageSource,
    onPickGallery: () -> Unit,
    onTakePhoto: () -> Unit,
    onRemoveImage: () -> Unit,
    cameraAvailable: Boolean,
) {
    val context = LocalContext.current
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        when (imageSource) {
            NoteImageSource.None -> Unit
            is NoteImageSource.Uri -> AsyncImage(
                model = Uri.parse(imageSource.value),
                contentDescription = stringResource(R.string.note_image_preview_description),
                modifier = Modifier.fillMaxWidth().height(200.dp).clip(RoundedCornerShape(12.dp)),
            )
            is NoteImageSource.Stored -> AsyncImage(
                model = File(context.filesDir, imageSource.relativePath),
                contentDescription = stringResource(R.string.note_image_preview_description),
                modifier = Modifier.fillMaxWidth().height(200.dp).clip(RoundedCornerShape(12.dp)),
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedButton(onClick = onPickGallery, modifier = Modifier.weight(1f).heightIn(min = 48.dp)) {
                Text(stringResource(R.string.note_choose_from_gallery))
            }
            if (cameraAvailable) {
                OutlinedButton(onClick = onTakePhoto, modifier = Modifier.weight(1f).heightIn(min = 48.dp)) {
                    Text(stringResource(R.string.note_take_photo))
                }
            }
            if (imageSource != NoteImageSource.None) {
                OutlinedButton(onClick = onRemoveImage, modifier = Modifier.weight(1f).heightIn(min = 48.dp)) {
                    Text(stringResource(R.string.note_remove_image))
                }
            }
        }
    }
}
