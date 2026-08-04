package com.shiyue.reader.app.navigation

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.shiyue.reader.R

enum class ShiyueDestination(
    val route: String,
    @StringRes val labelRes: Int,
    @DrawableRes val iconRes: Int,
) {
    Bookshelf("bookshelf", R.string.nav_bookshelf, R.drawable.ic_bookshelf),
    Reading("reading", R.string.nav_reading, R.drawable.ic_reading),
    Note("note", R.string.nav_note, R.drawable.ic_note),
    Review("review", R.string.nav_review, R.drawable.ic_review),
}
