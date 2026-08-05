package com.shiyue.reader.app

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.waitUntilAtLeastOneExists
import com.shiyue.reader.core.data.TestBookRepository
import com.shiyue.reader.feature.bookedit.AddBookTestTags
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import javax.inject.Inject
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalTestApi::class)
@HiltAndroidTest
class ManualBookFlowTest {
    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Inject
    lateinit var repository: TestBookRepository

    @Before
    fun setUp() {
        hiltRule.inject()
        repository.reset()
    }

    @After
    fun tearDown() {
        repository.reset()
    }

    @Test
    fun manuallyAddedBookReturnsToProductionBookshelf() {
        composeRule.waitUntilAtLeastOneExists(hasText("添加第一本书"))
        composeRule.onNodeWithText("添加第一本书").performClick()
        composeRule.onNodeWithText("手动添加书籍").assertIsDisplayed()

        composeRule.onNodeWithTag(AddBookTestTags.Title).performTextInput("置身事内")
        composeRule.onNodeWithTag(AddBookTestTags.Author).performTextInput("兰小欢")
        composeRule.onNodeWithTag(AddBookTestTags.TotalPages).performTextInput("321")

        composeRule.onNodeWithTag(AddBookTestTags.Status).performScrollTo().performClick()
        composeRule.onNodeWithText("想读").performClick()
        composeRule.onNodeWithTag(AddBookTestTags.Save)
            .performScrollTo()
            .performClick()

        composeRule.waitUntilAtLeastOneExists(hasText("第 0 / 321 页"))
        composeRule.onNodeWithText("置身事内").assertIsDisplayed()
        composeRule.onNodeWithText("兰小欢").assertIsDisplayed()
        composeRule.onNodeWithText("第 0 / 321 页").assertIsDisplayed()
        composeRule.onNodeWithText("想读").assertIsDisplayed()
    }
}
