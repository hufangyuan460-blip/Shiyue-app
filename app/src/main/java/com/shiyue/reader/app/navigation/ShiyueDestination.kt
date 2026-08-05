package com.shiyue.reader.app.navigation

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.shiyue.reader.R

enum class ShiyueDestination(
    val route: String,
    @param:StringRes val labelRes: Int,
    @param:DrawableRes val iconRes: Int,
) {
    Bookshelf("bookshelf", R.string.nav_bookshelf, R.drawable.ic_bookshelf),
    Reading("reading", R.string.nav_reading, R.drawable.ic_reading),
    Note("note", R.string.nav_note, R.drawable.ic_note),
    Review("review", R.string.nav_review, R.drawable.ic_review),
}

object ShiyueRoutes {
    const val AddBook = "add_book"
    const val BookIdArgument = "bookId"
    const val BookDetail = "book/{$BookIdArgument}"
    const val EditBook = "book/{$BookIdArgument}/edit"
    const val UpdateProgress = "book/{$BookIdArgument}/progress"
    const val Categories = "categories"

    fun bookDetail(bookId: String) = "book/$bookId"

    fun editBook(bookId: String) = "book/$bookId/edit"

    fun updateProgress(bookId: String) = "book/$bookId/progress"
}
