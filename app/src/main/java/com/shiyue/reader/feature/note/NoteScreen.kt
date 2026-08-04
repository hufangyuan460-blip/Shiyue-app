package com.shiyue.reader.feature.note

import androidx.compose.runtime.Composable
import com.shiyue.reader.R
import com.shiyue.reader.core.ui.PlaceholderScreen

@Composable
fun NoteScreen() {
    PlaceholderScreen(
        titleRes = R.string.note_title,
        descriptionRes = R.string.note_placeholder,
    )
}
