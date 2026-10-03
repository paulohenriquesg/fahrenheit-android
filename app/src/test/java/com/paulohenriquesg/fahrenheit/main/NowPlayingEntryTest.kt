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

/** The rail's way back to the player (#107; the two rail frames of docs/mocks/player.html). */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w960dp-h540dp")
class NowPlayingEntryTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val opened = mutableListOf<RailEntry>()
    private val book = RailEntry("b1", null, "A Long Drift", playing = true, progress = 0.4f, chapter = "Chapter 12", leftSeconds = 1210.0)

    private fun show(entry: RailEntry, open: Boolean) {
        compose.setContent { FahrenheitTheme { NowPlayingEntry(entry, open = open, onOpen = { opened += it }) } }
        compose.waitForIdle()
    }

    @Test fun `closed, the cover and the play state`() {
        show(book, open = false)
        compose.onNodeWithContentDescription("A Long Drift").assertIsDisplayed()
        compose.onNodeWithContentDescription("Playing").assertExists()
        compose.onNodeWithText("Chapter 12 · 20 min 10 s left").assertDoesNotExist()
    }

    @Test fun `paused says so`() {
        show(book.copy(playing = false), open = false)
        compose.onNodeWithContentDescription("Paused").assertExists()
    }

    @Test fun `open, the title and the chapter's time left`() {
        show(book, open = true)
        compose.onNodeWithText("A Long Drift").assertIsDisplayed()
        compose.onNodeWithText("Chapter 12 · 20 min 10 s left").assertIsDisplayed()
    }

    @Test fun `an episode says what is left of it`() {
        show(RailEntry("p1", "e1", "An Episode", playing = false, progress = 0.5f, chapter = null, leftSeconds = 600.0), open = true)
        compose.onNodeWithText("10 min 0 s left").assertIsDisplayed()
    }

    // Review Focus 3.
    @Test fun `choosing it opens what is queued`() {
        show(book, open = true)
        compose.onNodeWithTag(NOW_PLAYING_TAG).performSemanticsAction(SemanticsActions.OnClick)
        compose.waitForIdle()
        assertEquals(listOf(book), opened)
    }
}
