package com.shiyue.reader.feature

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.shiyue.reader.core.data.TestBookRepository
import com.shiyue.reader.core.data.TestCategoryRepository
import com.shiyue.reader.core.data.TestCoverStorage
import com.shiyue.reader.core.model.Book
import com.shiyue.reader.core.model.BookStatus
import com.shiyue.reader.core.model.LibraryBook
import com.shiyue.reader.core.ui.theme.ShiyueTheme
import com.shiyue.reader.domain.usecase.AddBookWithCoverUseCase
import com.shiyue.reader.domain.usecase.CreateCaptureTargetUseCase
import com.shiyue.reader.domain.usecase.CreateCategoryUseCase
import com.shiyue.reader.domain.usecase.DeleteTemporaryCoverUseCase
import com.shiyue.reader.domain.usecase.ObserveCategoriesUseCase
import com.shiyue.reader.feature.bookedit.AddBookRoute
import com.shiyue.reader.feature.bookedit.AddBookScreen
import com.shiyue.reader.feature.bookedit.AddBookUiState
import com.shiyue.reader.feature.bookedit.AddBookViewModel
import com.shiyue.reader.feature.bookshelf.BookshelfScreen
import com.shiyue.reader.feature.bookshelf.BookshelfUiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
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
    fun savedEventWaitsUntilLifecycleIsStarted() {
        val lifecycleOwner = MutableTestLifecycleOwner(Lifecycle.State.CREATED)
        val categories = TestCategoryRepository()
        val covers = TestCoverStorage()
        val viewModel = AddBookViewModel(
            addBook = AddBookWithCoverUseCase(TestBookRepository(categories), covers),
            observeCategories = ObserveCategoriesUseCase(categories),
            createCategory = CreateCategoryUseCase(categories),
            createCaptureTarget = CreateCaptureTargetUseCase(covers),
            deleteTemporaryCover = DeleteTemporaryCoverUseCase(covers),
        )
        var backCount = 0

        composeRule.setContent {
            CompositionLocalProvider(LocalLifecycleOwner provides lifecycleOwner) {
                ShiyueTheme {
                    AddBookRoute(
                        onBack = { backCount += 1 },
                        viewModel = viewModel,
                    )
                }
            }
        }

        composeRule.runOnIdle {
            viewModel.onTitleChanged("测试书籍")
            viewModel.onTotalPagesChanged("100")
            viewModel.save()
        }
        composeRule.waitForIdle()
        assertEquals(0, backCount)

        composeRule.runOnIdle {
            lifecycleOwner.moveTo(Lifecycle.State.STARTED)
        }
        composeRule.waitUntil { backCount == 1 }
        assertEquals(1, backCount)
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
                    uiState = BookshelfUiState(isLoading = false, books = listOf(LibraryBook(book, emptyList()))),
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

    @Test
    fun largeFontAndNarrowCardStacksPageAndPercentage() {
        val book = Book.create(
            title = "窄屏测试书籍",
            author = "作者",
            totalPages = 320,
            currentPage = 80,
            status = BookStatus.READING,
        )
        composeRule.setContent {
            CompositionLocalProvider(LocalDensity provides Density(density = 1f, fontScale = 2f)) {
                ShiyueTheme {
                    Box(modifier = androidx.compose.ui.Modifier.width(280.dp)) {
                        com.shiyue.reader.feature.bookshelf.BookCard(book = book)
                    }
                }
            }
        }

        val pageBounds = composeRule.onNodeWithText("第 80 / 320 页")
            .assertIsDisplayed()
            .fetchSemanticsNode()
            .boundsInRoot
        val percentageBounds = composeRule.onNodeWithText("25%")
            .assertIsDisplayed()
            .fetchSemanticsNode()
            .boundsInRoot

        assertTrue(pageBounds.bottom <= percentageBounds.top)
    }

    @Test
    fun largeFontAndNarrowFormKeepsStatusAndSaveControlsVisible() {
        composeRule.setContent {
            CompositionLocalProvider(LocalDensity provides Density(density = 1f, fontScale = 2f)) {
                ShiyueTheme {
                    Box(
                        modifier = androidx.compose.ui.Modifier
                            .width(280.dp)
                            .height(900.dp),
                    ) {
                        AddBookScreen(
                            uiState = AddBookUiState(),
                            onTitleChanged = {},
                            onAuthorChanged = {},
                            onTotalPagesChanged = {},
                            onStatusChanged = {},
                            onSave = {},
                            onBack = {},
                        )
                    }
                }
            }
        }

        composeRule.onNodeWithText("在读").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("展开").assertIsDisplayed()
        composeRule.onNodeWithText("保存到书架").performScrollTo().assertIsDisplayed()
    }

    private class MutableTestLifecycleOwner(initialState: Lifecycle.State) : LifecycleOwner {
        private val registry = LifecycleRegistry(this).apply {
            currentState = initialState
        }

        override val lifecycle: Lifecycle = registry

        fun moveTo(state: Lifecycle.State) {
            registry.currentState = state
        }
    }
}
