package com.tripnext.app

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import org.junit.Rule
import org.junit.Test

class TripNextSmokeTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    @Test fun appShowsMainNavigation() { compose.onNodeWithText("Início").assertIsDisplayed(); compose.onNodeWithText("Itinerário").assertIsDisplayed(); compose.onNodeWithText("Checklist").assertIsDisplayed() }
}
