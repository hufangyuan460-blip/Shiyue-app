package com.shiyue.reader.app

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.waitUntilAtLeastOneExists
import com.shiyue.reader.core.data.TestBookRepository
import com.shiyue.reader.core.data.TestBookshelfPreferences
import com.shiyue.reader.core.data.TestCategoryRepository
import com.shiyue.reader.feature.bookedit.AddBookTestTags
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
class BookshelfCategoryFlowTest {
    @get:Rule(order = 0) val hiltRule = HiltAndroidRule(this)
    @get:Rule(order = 1) val composeRule = createAndroidComposeRule<MainActivity>()

    @Inject lateinit var repository: TestBookRepository
    @Inject lateinit var categories: TestCategoryRepository
    @Inject lateinit var preferences: TestBookshelfPreferences

    @Before fun setUp() {
        hiltRule.inject()
        reset()
    }

    @After fun tearDown() = reset()

    @Test
    fun productionNavigationCreatesAssignsFiltersAndDeletesCategoryWithoutDeletingBook() {
        composeRule.onNodeWithText("管理分类").performClick()
        composeRule.onAllNodesWithText("新建分类").onFirst().performClick()
        composeRule.onNodeWithText("分类名称").performTextInput("文学")
        composeRule.onNodeWithText("保存修改").performClick()
        composeRule.waitUntilAtLeastOneExists(hasText("文学"))
        composeRule.onNodeWithContentDescription("返回").performClick()

        composeRule.onNodeWithText("添加第一本书").performClick()
        composeRule.onNodeWithTag(AddBookTestTags.Title).performTextInput("百年孤独")
        composeRule.onNodeWithTag(AddBookTestTags.Author).performTextInput("马尔克斯")
        composeRule.onNodeWithTag(AddBookTestTags.TotalPages).performTextInput("360")
        composeRule.onNodeWithText("文学").performScrollTo().performClick()
        composeRule.onNodeWithTag(AddBookTestTags.Save).performScrollTo().performClick()

        composeRule.waitUntilAtLeastOneExists(hasText("百年孤独"))
        composeRule.onNodeWithText("文学").assertIsDisplayed().performClick()
        composeRule.onNodeWithText("百年孤独").assertIsDisplayed()

        composeRule.onNodeWithText("管理分类").performClick()
        composeRule.onNodeWithText("删除").performClick()
        composeRule.onNodeWithText("删除").performClick()
        composeRule.onNodeWithContentDescription("返回").performClick()
        composeRule.onNodeWithText("未分类").performClick()
        composeRule.onNodeWithText("百年孤独").assertIsDisplayed()
    }

    private fun reset() {
        repository.reset()
        categories.reset()
        preferences.reset()
    }
}
