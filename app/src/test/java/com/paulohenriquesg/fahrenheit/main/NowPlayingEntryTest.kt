package com.paulohenriquesg.fahrenheit.main

import androidx.activity.ComponentActivity
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.assertTextEquals
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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
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
    private val stopped = mutableListOf<RailEntry>()
    private val book = RailEntry("b1", null, "A Long Drift", playing = true, progress = 0.4f, chapter = "Chapter 12", chapterNumber = 12, leftSeconds = 1210.0)

    private fun show(entry: RailEntry, open: Boolean) {
        compose.setContent { FahrenheitTheme { NowPlayingEntry(entry, open = open, onOpen = { opened += it }, onStop = { stopped += it }) } }
        compose.waitForIdle()
    }

    @Test fun `closed, the cover and the play state`() {
        show(book, open = false)
        compose.onNodeWithContentDescription("A Long Drift").assertIsDisplayed()
        compose.onNodeWithContentDescription("Playing").assertExists()
        compose.onNodeWithText("Chapter 12").assertDoesNotExist()
        compose.onNodeWithText("· 20 min left").assertDoesNotExist()
    }

    @Test fun `paused says so`() {
        show(book.copy(playing = false), open = false)
        compose.onNodeWithContentDescription("Paused").assertExists()
    }

    @Test fun `open, the title and the chapter's time left`() {
        show(book, open = true)
        compose.onNodeWithText("A Long Drift").assertIsDisplayed()
        // Whole minutes, as the mock writes it: the seconds would jump with each poll.
        compose.onNodeWithText("Chapter 12").assertIsDisplayed()
        compose.onNodeWithText("· 20 min left").assertIsDisplayed()
    }

    @Test fun `an episode says what is left of it`() {
        show(RailEntry("p1", "e1", "An Episode", playing = false, progress = 0.5f, chapter = null, chapterNumber = null, leftSeconds = 600.0), open = true)
        compose.onNodeWithText("10 min left").assertIsDisplayed()
    }

    // Review Focus 3.
    @Test fun `choosing it opens what is queued`() {
        show(book, open = true)
        compose.onNodeWithTag(NOW_PLAYING_TAG).performSemanticsAction(SemanticsActions.OnClick)
        compose.waitForIdle()
        assertEquals(listOf(book), opened)
    }

    @Test fun `an untitled chapter is numbered`() {
        show(book.copy(chapter = null, chapterNumber = 3), open = true)
        compose.onNodeWithText("Chapter 3").assertIsDisplayed()
        compose.onNodeWithText("· 20 min left").assertIsDisplayed()
    }

    @Test fun `under a minute left says the seconds`() {
        show(book.copy(leftSeconds = 42.0), open = true)
        compose.onNodeWithText("Chapter 12").assertIsDisplayed()
        compose.onNodeWithText("· 42 s left").assertIsDisplayed()
    }

    // #155: Back from the player keeps playing; stopping is here.
    @Test fun `open, it offers Stop`() {
        show(book, open = true)
        compose.onNodeWithTag(NOW_PLAYING_STOP_TAG).assertIsDisplayed()
        compose.onNodeWithText("Stop").assertIsDisplayed()
    }

    @Test fun `choosing Stop stops what is queued, and does not open it`() {
        show(book, open = true)
        compose.onNodeWithTag(NOW_PLAYING_STOP_TAG).performSemanticsAction(SemanticsActions.OnClick)
        compose.waitForIdle()
        assertEquals(listOf(book), stopped)
        assertEquals(emptyList<RailEntry>(), opened)
    }

    // A closed rail holds no focus: nothing there could reach it.
    @Test fun `closed, no Stop`() {
        show(book, open = false)
        compose.onNodeWithTag(NOW_PLAYING_STOP_TAG).assertDoesNotExist()
    }

    // #198: the time left always shows; only the chapter's name is shortened.
    @Test fun `a long chapter name is shortened, and the time left shows in full`() {
        show(book.copy(chapter = "A chapter whose name runs on far past the width of the entry, and further still"), open = true)
        val chapter = layout(NOW_PLAYING_CHAPTER_TAG)
        val left = layout(NOW_PLAYING_LEFT_TAG)
        assertTrue(chapter.isLineEllipsized(0))
        assertFalse(left.isLineEllipsized(0))
        assertFalse(left.hasVisualOverflow)
        compose.onNodeWithTag(NOW_PLAYING_LEFT_TAG, useUnmergedTree = true).assertTextEquals("· 20 min left")
        val within = compose.onNodeWithTag(NOW_PLAYING_TAG).getBoundsInRoot()
        val shown = compose.onNodeWithTag(NOW_PLAYING_LEFT_TAG, useUnmergedTree = true).getBoundsInRoot()
        assertTrue("the time left ends inside the entry", shown.right <= within.right)
    }

    private fun layout(tag: String): TextLayoutResult {
        val results = mutableListOf<TextLayoutResult>()
        compose.onNodeWithTag(tag, useUnmergedTree = true).performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(results) }
        return results.single()
    }
}
