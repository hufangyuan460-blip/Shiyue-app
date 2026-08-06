package com.shiyue.reader.app

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.waitUntilAtLeastOneExists
import com.shiyue.reader.core.data.TestBookRepository
import com.shiyue.reader.core.data.TestReadingSessionRepository
import com.shiyue.reader.core.model.Book
import com.shiyue.reader.core.model.BookStatus
import com.shiyue.reader.feature.reading.ReadingFlowTestTags
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import javax.inject.Inject
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalTestApi::class)
@HiltAndroidTest
class ReadingSessionFlowTest {
    @get:Rule(order = 0) val hiltRule = HiltAndroidRule(this)
    @get:Rule(order = 1) val composeRule = createAndroidComposeRule<MainActivity>()
    @Inject lateinit var books: TestBookRepository
    @Inject lateinit var sessions: TestReadingSessionRepository

    @Before fun setup() {
        hiltRule.inject(); books.reset(); sessions.reset()
        books.seed(Book.create("纸上星河", "拾页测试", 200, currentPage = 12, status = BookStatus.READING))
    }
    @After fun cleanup() { sessions.reset(); books.reset() }

    @Test fun productionNavigationCompletesReadingAndShowsHistory() {
        composeRule.onNodeWithText("阅读").performClick()
        composeRule.waitUntilAtLeastOneExists(hasText("纸上星河"))
        composeRule.onNodeWithText("纸上星河").performClick()
        composeRule.onNodeWithTag(ReadingFlowTestTags.StartPage).performTextReplacement("12")
        composeRule.onNodeWithTag(ReadingFlowTestTags.Start).performClick()

        composeRule.waitUntilAtLeastOneExists(hasText("阅读中"))
        composeRule.onNodeWithTag(ReadingFlowTestTags.Finish).performClick()
        composeRule.waitUntilAtLeastOneExists(hasText("结束阅读"))
        composeRule.onNodeWithTag(ReadingFlowTestTags.EndPage).performTextReplacement("25")
        composeRule.onNodeWithTag(ReadingFlowTestTags.SaveSummary).performClick()

        composeRule.waitUntilAtLeastOneExists(hasText("阅读轨迹"))
        composeRule.onNodeWithText("第 12–25 页", substring = true).assertIsDisplayed()
        composeRule.onNodeWithText("第 25 / 200 页").assertIsDisplayed()
    }
}
