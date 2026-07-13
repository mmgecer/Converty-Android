package com.converty.app

import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onRoot
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MainActivityStartupTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun mainActivityStartsAndCreatesAComposeRoot() {
        composeRule.waitForIdle()
        composeRule.onRoot(useUnmergedTree = true).fetchSemanticsNode()
    }
}
