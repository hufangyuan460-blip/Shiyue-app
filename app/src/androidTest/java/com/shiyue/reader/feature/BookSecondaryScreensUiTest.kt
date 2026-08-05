package com.shiyue.reader.feature

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.shiyue.reader.core.model.Book
import com.shiyue.reader.core.model.BookStatus
import com.shiyue.reader.core.ui.theme.ShiyueTheme
import com.shiyue.reader.feature.bookdetail.BookDetailScreen
import com.shiyue.reader.feature.bookdetail.BookDetailUiState
import com.shiyue.reader.feature.bookedit.EditBookScreen
import com.shiyue.reader.feature.bookedit.EditBookTestTags
import com.shiyue.reader.feature.bookedit.EditBookUiState
import com.shiyue.reader.feature.bookprogress.UpdateProgressScreen
import com.shiyue.reader.feature.bookprogress.UpdateProgressTestTags
import com.shiyue.reader.feature.bookprogress.UpdateProgressUiState
import org.junit.Rule
import org.junit.Test

class BookSecondaryScreensUiTest {
    @get:Rule val composeRule = createComposeRule()

    @Test
    fun missingBookShowsNotFoundAndReturnAction() {
        composeRule.setContent {
            ShiyueTheme {
                BookDetailScreen(BookDetailUiState.NotFound, {}, {}, {}, {})
            }
        }
        composeRule.onNodeWithText("这本书不存在或已被移除。").assertIsDisplayed()
        composeRule.onNodeWithText("返回书架").assertIsDisplayed()
    }

    @Test
    fun editControlsRemainReachableAtNarrowWidthAndLargeFont() {
        composeRule.setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f, 2f)) {
                ShiyueTheme {
                    Box(Modifier.width(280.dp)) {
                        EditBookScreen(
                            uiState = EditBookUiState(
                                isLoading = false,
                                initialized = true,
                                title = "长标题书籍",
                                author = "作者",
                                totalPages = "300",
                                status = BookStatus.READING,
                            ),
                            onTitleChanged = {}, onAuthorChanged = {}, onTotalPagesChanged = {},
                            onStatusChanged = {}, onSave = {}, onRetry = {}, onBack = {},
                        )
                    }
                }
            }
        }
        composeRule.onNodeWithTag(EditBookTestTags.Title).assertIsDisplayed()
        composeRule.onNodeWithTag(EditBookTestTags.Save).performScrollTo().assertIsDisplayed()
    }

    @Test
    fun progressControlsRemainReachableAtNarrowWidthAndLargeFont() {
        val book = Book.create("书名", null, 100, 20, timestamp = 1)
        composeRule.setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f, 2f)) {
                ShiyueTheme {
                    Box(Modifier.width(280.dp)) {
                        UpdateProgressScreen(
                            uiState = UpdateProgressUiState(isLoading = false, book = book, pageInput = "20"),
                            onPageChanged = {}, onSave = {}, onBack = {}, onConfirmRewind = {},
                            onMarkFinished = {}, onKeepStatus = {}, onCancelConfirmation = {}, onRetry = {},
                        )
                    }
                }
            }
        }
        composeRule.onNodeWithTag(UpdateProgressTestTags.Page).assertIsDisplayed()
        composeRule.onNodeWithTag(UpdateProgressTestTags.Save).performScrollTo().assertIsDisplayed()
    }
}
