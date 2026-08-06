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

fun NavHostController.navigateToStartReading(bookId: String) {
    navigate(ShiyueRoutes.startReading(bookId)) { launchSingleTop = true }
}

fun NavHostController.navigateToActiveReading() {
    navigate(ShiyueRoutes.ActiveReading) { launchSingleTop = true }
}

fun NavHostController.navigateToFinishReading(sessionId: String) {
    navigate(ShiyueRoutes.finishReading(sessionId)) { launchSingleTop = true }
}

fun NavHostController.navigateToRecoverReading(sessionId: String) {
    navigate(ShiyueRoutes.recoverReading(sessionId)) { launchSingleTop = true }
}

fun NavHostController.navigateToReadingSessionDetail(sessionId: String) {
    navigate(ShiyueRoutes.readingSessionDetail(sessionId)) { launchSingleTop = true }
}

fun NavHostController.navigateToEditReadingSession(sessionId: String) {
    navigate(ShiyueRoutes.editReadingSession(sessionId)) { launchSingleTop = true }
}
