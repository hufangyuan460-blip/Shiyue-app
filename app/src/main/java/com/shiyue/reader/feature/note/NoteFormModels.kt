package com.shiyue.reader.feature.note

sealed interface NoteImageSource {
    data object None : NoteImageSource
    data class Uri(val value: String) : NoteImageSource
    data class Stored(val relativePath: String) : NoteImageSource
}

data class NoteFormUiState(
    val isLoading: Boolean = true,
    val notFound: Boolean = false,
    val bookTitle: String = "",
    val pageText: String = "",
    val content: String = "",
    val imageSource: NoteImageSource = NoteImageSource.None,
    val captureToken: String? = null,
    val captureUri: String? = null,
    val isSaving: Boolean = false,
    val failed: Boolean = false,
)

sealed interface NoteFormEvent {
    data object Saved : NoteFormEvent
    data object Deleted : NoteFormEvent
    data class LaunchCamera(val uri: String) : NoteFormEvent
}
