package com.shiyue.reader.feature

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.shiyue.reader.core.model.Book
import com.shiyue.reader.core.model.BookStatus
import com.shiyue.reader.core.ui.theme.ShiyueTheme
import com.shiyue.reader.feature.bookedit.AddBookScreen
import com.shiyue.reader.feature.bookedit.AddBookUiState
import com.shiyue.reader.feature.bookshelf.BookshelfScreen
import com.shiyue.reader.feature.bookshelf.BookshelfUiState
import org.junit.Rule
import org.junit.Test

class ManualBookUiTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun emptyBookshelfShowsGentleCallToAction() {
        composeRule.setContent {
            ShiyueTheme {
                BookshelfScreen(
                    uiState = BookshelfUiState(isLoading = false),
                    onAddBook = {},
                )
            }
        }

        composeRule.onNodeWithText("书架还是空的").assertIsDisplayed()
        composeRule.onNodeWithText("添加第一本书").assertIsDisplayed()
    }

    @Test
    fun firstBookButtonOpensStandaloneAddPage() {
        composeRule.setContent {
            ShiyueTheme {
                val navController = rememberNavController()
                NavHost(navController = navController, startDestination = "shelf") {
                    composable("shelf") {
                        BookshelfScreen(
                            uiState = BookshelfUiState(isLoading = false),
                            onAddBook = { navController.navigate("add") },
                        )
                    }
                    composable("add") {
                        AddBookScreen(
                            uiState = AddBookUiState(),
                            onTitleChanged = {},
                            onAuthorChanged = {},
                            onTotalPagesChanged = {},
                            onStatusChanged = {},
                            onSave = {},
                            onBack = { navController.popBackStack() },
                        )
                    }
                }
            }
        }

        composeRule.onNodeWithText("添加第一本书").performClick()
        composeRule.onNodeWithText("手动添加书籍").assertIsDisplayed()
    }

    @Test
    fun invalidFormShowsErrorsNextToFields() {
        composeRule.setContent {
            ShiyueTheme {
                AddBookScreen(
                    uiState = AddBookUiState(titleError = true, totalPagesError = true),
                    onTitleChanged = {},
                    onAuthorChanged = {},
                    onTotalPagesChanged = {},
                    onStatusChanged = {},
                    onSave = {},
                    onBack = {},
                )
            }
        }

        composeRule.onNodeWithText("请输入书名").assertIsDisplayed()
        composeRule.onNodeWithText("请输入大于 0 的整数页数").assertIsDisplayed()
    }

    @Test
    fun populatedBookshelfShowsBookCardDetails() {
        val book = Book.create(
            title = "山茶文具店",
            author = "小川糸",
            totalPages = 320,
            currentPage = 80,
            status = BookStatus.READING,
        )
        composeRule.setContent {
            ShiyueTheme {
                BookshelfScreen(
                    uiState = BookshelfUiState(isLoading = false, books = listOf(book)),
                    onAddBook = {},
                )
            }
        }

        composeRule.onNodeWithText("山茶文具店").assertIsDisplayed()
        composeRule.onNodeWithText("小川糸").assertIsDisplayed()
        composeRule.onNodeWithText("第 80 / 320 页").assertIsDisplayed()
        composeRule.onNodeWithText("25%").assertIsDisplayed()
        composeRule.onNodeWithText("在读").assertIsDisplayed()
    }
}
