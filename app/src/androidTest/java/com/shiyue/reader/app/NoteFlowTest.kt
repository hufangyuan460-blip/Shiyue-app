package com.shiyue.reader.app

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.waitUntilAtLeastOneExists
import com.shiyue.reader.core.data.TestBookRepository
import com.shiyue.reader.core.data.TestNoteRepository
import com.shiyue.reader.core.data.TestReadingSessionRepository
import com.shiyue.reader.core.model.Book
import com.shiyue.reader.core.model.BookStatus
import com.shiyue.reader.feature.note.NoteFormTestTags
import com.shiyue.reader.feature.reading.ReadingFlowTestTags
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import javax.inject.Inject
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalTestApi::class)
@HiltAndroidTest
class NoteFlowTest {
    @get:Rule(order = 0) val hiltRule = HiltAndroidRule(this)
    @get:Rule(order = 1) val composeRule = createAndroidComposeRule<MainActivity>()
    @Inject lateinit var books: TestBookRepository
    @Inject lateinit var sessions: TestReadingSessionRepository
    @Inject lateinit var notes: TestNoteRepository

    @Before fun setup() {
        hiltRule.inject(); books.reset(); sessions.reset(); notes.reset()
        books.seed(Book.create("纸上星河", "拾页测试", 200, currentPage = 12, status = BookStatus.READING))
    }
    @After fun cleanup() { notes.reset(); sessions.reset(); books.reset() }

    @Test fun readingQuickNoteIsSavedAndVisibleInBookDetail() {
        composeRule.onNodeWithText("阅读").performClick()
        composeRule.waitUntilAtLeastOneExists(hasText("纸上星河"))
        composeRule.onAllNodesWithText("纸上星河").onFirst().performClick()
        composeRule.onNodeWithTag(ReadingFlowTestTags.StartPage).performTextReplacement("12")
        composeRule.onNodeWithTag(ReadingFlowTestTags.Start).performClick()

        composeRule.waitUntilAtLeastOneExists(hasText("阅读中"))
        composeRule.waitUntilAtLeastOneExists(hasTestTag(ReadingFlowTestTags.WriteNote))
        composeRule.onNodeWithTag(ReadingFlowTestTags.WriteNote).performScrollTo().performClick()
        composeRule.waitUntilAtLeastOneExists(hasTestTag(NoteFormTestTags.Screen), 5_000)
        composeRule.waitUntilAtLeastOneExists(hasTestTag(NoteFormTestTags.Content), 5_000)
        composeRule.onNodeWithTag(NoteFormTestTags.Content).performScrollTo().performTextReplacement("第 12 页的灵感")
        composeRule.onNodeWithTag(NoteFormTestTags.Save).performScrollTo().performClick()

        composeRule.waitUntilAtLeastOneExists(hasText("阅读中"), 5_000)
        assertEquals(1, runBlocking { notes.observeAll().first().size })

        composeRule.activity.onBackPressedDispatcher.onBackPressed()
        composeRule.waitForIdle()
        composeRule.activity.onBackPressedDispatcher.onBackPressed()
        composeRule.waitForIdle()

        composeRule.waitUntilAtLeastOneExists(hasText("随记"))
        composeRule.onNodeWithText("随记").performClick()
        composeRule.waitUntilAtLeastOneExists(hasText("第 12 页的灵感"), 5_000)
        composeRule.onNodeWithText("第 12 页的灵感").assertIsDisplayed()
    }
}
