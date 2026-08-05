package com.shiyue.reader.app

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@HiltAndroidTest
class ShiyueNavigationTest {
    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun setUp() {
        hiltRule.inject()
    }

    @Test
    fun appStartsOnBookshelfAndNavigatesToReading() {
        composeRule.onNodeWithText("书架").assertIsDisplayed()

        composeRule.onNodeWithText("阅读")
            .performClick()

        composeRule.onNodeWithText("拿起一本书，专注于此刻的阅读。")
            .assertIsDisplayed()
    }
}
