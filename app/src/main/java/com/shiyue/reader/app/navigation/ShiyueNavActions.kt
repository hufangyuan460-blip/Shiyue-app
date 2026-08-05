package com.shiyue.reader.app.navigation

import androidx.navigation.NavHostController

fun NavHostController.navigateToAddBook() {
    navigate(ShiyueRoutes.AddBook) { launchSingleTop = true }
}

fun NavHostController.navigateToBookDetail(bookId: String) {
    navigate(ShiyueRoutes.bookDetail(bookId)) { launchSingleTop = true }
}

fun NavHostController.navigateToEditBook(bookId: String) {
    navigate(ShiyueRoutes.editBook(bookId)) { launchSingleTop = true }
}

fun NavHostController.navigateToUpdateProgress(bookId: String) {
    navigate(ShiyueRoutes.updateProgress(bookId)) { launchSingleTop = true }
}

fun NavHostController.navigateToCategories() {
    navigate(ShiyueRoutes.Categories) { launchSingleTop = true }
}
