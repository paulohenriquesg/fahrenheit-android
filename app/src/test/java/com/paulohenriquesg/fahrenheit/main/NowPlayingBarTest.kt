package com.paulohenriquesg.fahrenheit.main

import androidx.activity.ComponentActivity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performSemanticsAction
import com.paulohenriquesg.fahrenheit.player.RailEntry
import com.paulohenriquesg.fahrenheit.ui.theme.FahrenheitTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** The book and podcast screens' Now playing, with Stop beside it (#159). */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w960dp-h540dp")
class NowPlayingBarTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val opened = mutableListOf<RailEntry>()
    private val stopped = mutableListOf<RailEntry>()
    private val book = RailEntry("b1", null, "A Long Drift", playing = true, progress = 0.4f, chapter = "Chapter 12", chapterNumber = 12, leftSeconds = 1210.0)

    private fun show(entry: RailEntry) {
        compose.setContent { FahrenheitTheme { NowPlayingBar(entry, onOpen = { opened += it }, onStop = { stopped += it }) } }
        compose.waitForIdle()
    }

    @Test fun `the title, the chapter's time left and the play state`() {
        show(book)
        compose.onNodeWithText("A Long Drift").assertIsDisplayed()
        compose.onNodeWithText("Chapter 12 · 20 min left").assertIsDisplayed()
        compose.onNodeWithContentDescription("Playing").assertExists()
        // The state, as the rail shows it (#170): bars, not a play icon.
        compose.onNodeWithTag(NOW_PLAYING_EQUALISER_TAG, useUnmergedTree = true).assertExists()
    }

    @Test fun `an episode says what is left of it, and paused says so`() {
        show(RailEntry("p1", "e1", "An Episode", playing = false, progress = 0.5f, chapter = null, chapterNumber = null, leftSeconds = 600.0))
        compose.onNodeWithText("10 min left").assertIsDisplayed()
        compose.onNodeWithContentDescription("Paused").assertExists()
        compose.onNodeWithTag(NOW_PLAYING_PAUSED_TAG, useUnmergedTree = true).assertExists()
    }

    @Test fun `choosing the bar opens what is queued`() {
        show(book)
        compose.onNodeWithTag(NOW_PLAYING_BAR_TAG).performSemanticsAction(SemanticsActions.OnClick)
        compose.waitForIdle()
        assertEquals(listOf(book), opened)
        assertEquals(emptyList<RailEntry>(), stopped)
    }

    @Test fun `Stop beside it stops, and does not open`() {
        show(book)
        compose.onNodeWithText("Stop").assertIsDisplayed()
        compose.onNodeWithTag(NOW_PLAYING_BAR_STOP_TAG).performSemanticsAction(SemanticsActions.OnClick)
        compose.waitForIdle()
        assertEquals(listOf(book), stopped)
        assertEquals(emptyList<RailEntry>(), opened)
    }
}
