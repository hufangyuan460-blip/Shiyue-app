package com.shiyue.reader.app

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Rule
import org.junit.Test

class ShiyueNavigationTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun appStartsOnBookshelfAndNavigatesToReading() {
        composeRule.onNodeWithText("你的实体书阅读档案将从这里开始。")
            .assertIsDisplayed()

        composeRule.onNodeWithText("阅读")
            .performClick()

        composeRule.onNodeWithText("拿起一本书，专注于此刻的阅读。")
            .assertIsDisplayed()
    }
}
