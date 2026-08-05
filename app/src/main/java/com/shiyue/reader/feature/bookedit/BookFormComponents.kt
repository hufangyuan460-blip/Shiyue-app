package com.shiyue.reader.feature.bookedit

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.layout.ContentScale
import coil3.compose.AsyncImage
import com.shiyue.reader.R
import com.shiyue.reader.core.model.BookStatus
import com.shiyue.reader.core.model.CategorySummary
import com.shiyue.reader.domain.repository.CoverTransform
import com.shiyue.reader.feature.bookshelf.statusLabel

@Composable
fun BookStatusField(
    status: BookStatus,
    enabled: Boolean,
    onStatusChanged: (BookStatus) -> Unit,
    testTag: String,
) {
    var expanded by remember { mutableStateOf(false) }
    Box(Modifier.fillMaxWidth()) {
        OutlinedButton(
            onClick = { expanded = true },
            enabled = enabled,
            modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp).testTag(testTag),
        ) { Text(statusLabel(status), modifier = Modifier.weight(1f)); Text(stringResource(R.string.open_status_options)) }
        DropdownMenu(expanded, { expanded = false }) {
            BookStatus.entries.forEach { option ->
                DropdownMenuItem(
                    text = { Text(statusLabel(option)) },
                    onClick = { expanded = false; onStatusChanged(option) },
                )
            }
        }
    }
}

@Composable
fun CategorySelector(
    categories: List<CategorySummary>,
    selectedIds: Set<String>,
    newName: String,
    hasError: Boolean,
    enabled: Boolean,
    onToggle: (String) -> Unit,
    onNewNameChanged: (String) -> Unit,
    onCreate: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stringResource(R.string.book_categories_label), style = MaterialTheme.typography.labelLarge)
        categories.forEach { item ->
            Row(
                Modifier.fillMaxWidth().heightIn(min = 48.dp).toggleable(
                    value = item.category.id in selectedIds,
                    enabled = enabled,
                    role = Role.Checkbox,
                    onValueChange = { onToggle(item.category.id) },
                ),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Checkbox(
                    checked = item.category.id in selectedIds,
                    onCheckedChange = null,
                    enabled = enabled,
                )
                Text(item.category.name, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = newName,
                onValueChange = onNewNameChanged,
                modifier = Modifier.weight(1f),
                label = { Text(stringResource(R.string.quick_create_category)) },
                isError = hasError,
                supportingText = if (hasError) {{ Text(stringResource(R.string.category_name_conflict)) }} else null,
                enabled = enabled,
            )
            Button(onClick = onCreate, enabled = enabled && newName.isNotBlank(), modifier = Modifier.heightIn(min = 48.dp)) {
                Text(stringResource(R.string.create))
            }
        }
    }
}

@Composable
fun CoverActions(
    hasCover: Boolean,
    cameraAvailable: Boolean,
    enabled: Boolean,
    onPickPhoto: () -> Unit,
    onTakePhoto: () -> Unit,
    onRemove: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stringResource(R.string.book_cover_label), style = MaterialTheme.typography.labelLarge)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onPickPhoto, enabled = enabled, modifier = Modifier.weight(1f).heightIn(min = 48.dp)) {
                Text(stringResource(R.string.choose_from_gallery))
            }
            if (cameraAvailable) OutlinedButton(
                onClick = onTakePhoto,
                enabled = enabled,
                modifier = Modifier.weight(1f).heightIn(min = 48.dp),
            ) { Text(stringResource(R.string.take_cover_photo)) }
        }
        if (hasCover) TextButton(onClick = onRemove, enabled = enabled) {
            Text(stringResource(R.string.remove_cover), color = MaterialTheme.colorScheme.error)
        }
    }
}

@Composable
fun CoverCropDialog(
    sourceUri: String,
    transform: CoverTransform,
    onTransformChanged: (CoverTransform) -> Unit,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
    onReselect: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text(stringResource(R.string.crop_cover_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(
                    Modifier.fillMaxWidth().aspectRatio(2f / 3f).clip(RoundedCornerShape(8.dp))
                        .pointerInput(transform) {
                            detectDragGestures { change, drag ->
                                change.consume()
                                onTransformChanged(
                                    transform.copy(
                                        offsetXFraction = (transform.offsetXFraction - drag.x / size.width).coerceIn(-1f, 1f),
                                        offsetYFraction = (transform.offsetYFraction - drag.y / size.height).coerceIn(-1f, 1f),
                                    ),
                                )
                            }
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    AsyncImage(
                        model = sourceUri,
                        contentDescription = stringResource(R.string.crop_cover_preview_description),
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.matchParentSize().graphicsLayer {
                            scaleX = transform.scale
                            scaleY = transform.scale
                            rotationZ = transform.quarterTurns * 90f
                            translationX = -transform.offsetXFraction * size.width / 2f
                            translationY = -transform.offsetYFraction * size.height / 2f
                        },
                    )
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    TextButton(onClick = { onTransformChanged(transform.copy(scale = (transform.scale - .25f).coerceAtLeast(1f))) }) { Text(stringResource(R.string.zoom_out)) }
                    TextButton(onClick = { onTransformChanged(transform.copy(scale = (transform.scale + .25f).coerceAtMost(4f))) }) { Text(stringResource(R.string.zoom_in)) }
                    TextButton(onClick = { onTransformChanged(transform.copy(quarterTurns = transform.quarterTurns - 1)) }) { Text(stringResource(R.string.rotate_left)) }
                    TextButton(onClick = { onTransformChanged(transform.copy(quarterTurns = transform.quarterTurns + 1)) }) { Text(stringResource(R.string.rotate_right)) }
                }
            }
        },
        confirmButton = { TextButton(onClick = onConfirm) { Text(stringResource(R.string.use_cover)) } },
        dismissButton = {
            Column(horizontalAlignment = Alignment.End) {
                TextButton(onClick = onReselect) { Text(stringResource(R.string.reselect_cover)) }
                TextButton(onClick = onCancel) { Text(stringResource(R.string.cancel)) }
            }
        },
    )
}
