package com.shiyue.reader.app

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.waitUntilAtLeastOneExists
import com.shiyue.reader.core.data.TestBookRepository
import com.shiyue.reader.core.data.TestBookshelfPreferences
import com.shiyue.reader.core.data.TestCategoryRepository
import com.shiyue.reader.feature.bookdetail.BookDetailTestTags
import com.shiyue.reader.feature.bookedit.AddBookTestTags
import com.shiyue.reader.feature.bookedit.EditBookTestTags
import com.shiyue.reader.feature.bookprogress.UpdateProgressTestTags
import com.shiyue.reader.feature.bookshelf.BookshelfTestTags
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import javax.inject.Inject
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalTestApi::class)
@HiltAndroidTest
class BookDetailProgressFlowTest {
    @get:Rule(order = 0) val hiltRule = HiltAndroidRule(this)
    @get:Rule(order = 1) val composeRule = createAndroidComposeRule<MainActivity>()

    @Inject lateinit var repository: TestBookRepository
    @Inject lateinit var categories: TestCategoryRepository
    @Inject lateinit var preferences: TestBookshelfPreferences

    @Before
    fun setUp() {
        hiltRule.inject()
        repository.reset()
        categories.reset()
        preferences.reset()
    }

    @After
    fun tearDown() {
        repository.reset()
        categories.reset()
        preferences.reset()
    }

    @Test
    fun productionNavigationEditsBookAndUpdatesProgressEndToEnd() {
        addBook("活着", "余华", "100")
        val id = repository.booksSnapshot().single().id

        composeRule.onNodeWithTag(BookshelfTestTags.bookCard(id)).performClick()
        composeRule.waitUntilAtLeastOneExists(hasText("书籍详情"))
        composeRule.onNodeWithText("活着").assertIsDisplayed()
        composeRule.onNodeWithText("第 0 / 100 页").assertIsDisplayed()

        composeRule.onNodeWithTag(BookDetailTestTags.Edit).performScrollTo().performClick()
        composeRule.onNodeWithTag(EditBookTestTags.Title).performTextReplacement("许三观卖血记")
        composeRule.onNodeWithTag(EditBookTestTags.Author).performTextReplacement(" 余华 ")
        composeRule.onNodeWithTag(EditBookTestTags.Save).performScrollTo().performClick()
        composeRule.waitUntilAtLeastOneExists(hasText("许三观卖血记"))

        composeRule.onNodeWithTag(BookDetailTestTags.UpdateProgress).performScrollTo().performClick()
        composeRule.onNodeWithTag(UpdateProgressTestTags.Page).performTextReplacement("50")
        composeRule.onNodeWithText("更新后进度：50%").assertIsDisplayed()
        composeRule.onNodeWithTag(UpdateProgressTestTags.Save).performScrollTo().performClick()
        composeRule.waitUntilAtLeastOneExists(hasText("第 50 / 100 页"))
        composeRule.onNodeWithText("50%").assertIsDisplayed()

        composeRule.activityRule.scenario.onActivity { activity ->
            activity.onBackPressedDispatcher.onBackPressed()
        }
        composeRule.waitUntilAtLeastOneExists(hasText("第 50 / 100 页"))
        composeRule.onNodeWithText("许三观卖血记").assertIsDisplayed()
    }

    @Test
    fun rewindAndLastPageRequireExplicitChoices() {
        addBook("城南旧事", "林海音", "100")
        val id = repository.booksSnapshot().single().id
        composeRule.onNodeWithTag(BookshelfTestTags.bookCard(id)).performClick()

        updatePage("50")
        composeRule.onNodeWithTag(BookDetailTestTags.UpdateProgress).performScrollTo().performClick()
        composeRule.onNodeWithTag(UpdateProgressTestTags.Page).performTextReplacement("20")
        composeRule.onNodeWithTag(UpdateProgressTestTags.Save).performScrollTo().performClick()
        composeRule.onNodeWithText("确认回退阅读进度？").assertIsDisplayed()
        composeRule.onNodeWithText("取消").performClick()
        composeRule.onNodeWithText("当前第 50 页，共 100 页").assertIsDisplayed()

        composeRule.onNodeWithTag(UpdateProgressTestTags.Page).performTextReplacement("100")
        composeRule.onNodeWithTag(UpdateProgressTestTags.Save).performScrollTo().performClick()
        composeRule.onNodeWithText("读到最后一页了").assertIsDisplayed()
        composeRule.onNodeWithText("标记为已读").performClick()
        composeRule.waitUntilAtLeastOneExists(hasText("第 100 / 100 页"))
        composeRule.onNodeWithText("已读").assertIsDisplayed()
    }

    @Test
    fun dirtyEditShowsDiscardConfirmation() {
        addBook("边城", "沈从文", "120")
        val id = repository.booksSnapshot().single().id
        composeRule.onNodeWithTag(BookshelfTestTags.bookCard(id)).performClick()
        composeRule.onNodeWithTag(BookDetailTestTags.Edit).performScrollTo().performClick()
        composeRule.onNodeWithTag(EditBookTestTags.Title).performTextReplacement("未保存书名")
        composeRule.activityRule.scenario.onActivity { activity ->
            activity.onBackPressedDispatcher.onBackPressed()
        }
        composeRule.onNodeWithText("放弃修改？").assertIsDisplayed()
        composeRule.onNodeWithText("继续编辑").performClick()
        composeRule.onNodeWithTag(EditBookTestTags.Title).assertIsDisplayed()
    }

    @Test
    fun totalPagesBelowCurrentPageIsRejectedInProductionEditForm() {
        addBook("朝花夕拾", "鲁迅", "100")
        val id = repository.booksSnapshot().single().id
        composeRule.onNodeWithTag(BookshelfTestTags.bookCard(id)).performClick()
        updatePage("50")

        composeRule.onNodeWithTag(BookDetailTestTags.Edit).performScrollTo().performClick()
        composeRule.onNodeWithTag(EditBookTestTags.TotalPages).performTextReplacement("40")
        composeRule.onNodeWithTag(EditBookTestTags.Save).performScrollTo().performClick()
        composeRule.onNodeWithText("总页数不能少于当前已读的第 50 页").assertIsDisplayed()
        composeRule.onNodeWithTag(EditBookTestTags.TotalPages).assertIsDisplayed()
    }

    private fun addBook(title: String, author: String, pages: String) {
        composeRule.waitUntilAtLeastOneExists(hasText("添加第一本书"))
        composeRule.onNodeWithText("添加第一本书").performClick()
        composeRule.onNodeWithTag(AddBookTestTags.Title).performTextInput(title)
        composeRule.onNodeWithTag(AddBookTestTags.Author).performTextInput(author)
        composeRule.onNodeWithTag(AddBookTestTags.TotalPages).performTextInput(pages)
        composeRule.onNodeWithTag(AddBookTestTags.Save).performScrollTo().performClick()
        composeRule.waitUntilAtLeastOneExists(hasText(title))
    }

    private fun updatePage(page: String) {
        composeRule.onNodeWithTag(BookDetailTestTags.UpdateProgress).performScrollTo().performClick()
        composeRule.onNodeWithTag(UpdateProgressTestTags.Page).performTextReplacement(page)
        composeRule.onNodeWithTag(UpdateProgressTestTags.Save).performScrollTo().performClick()
        composeRule.waitUntilAtLeastOneExists(hasText("第 $page / 100 页"))
    }
}
