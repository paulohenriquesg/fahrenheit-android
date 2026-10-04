package com.paulohenriquesg.fahrenheit.main

import androidx.compose.ui.unit.dp
import com.paulohenriquesg.fahrenheit.player.RailEntry
import androidx.activity.ComponentActivity
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.text.BasicText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Star
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.pressKey
import com.paulohenriquesg.fahrenheit.navigation.MenuAction
import com.paulohenriquesg.fahrenheit.navigation.MenuItem
import com.paulohenriquesg.fahrenheit.ui.theme.FahrenheitTheme
import org.junit.Assert.assertEquals
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * The phone drawer was invisible until opened and covered everything when it
 * was. A TV rail is always there, which is what gives LEFT somewhere to go
 * (#58, and the half of #53 that needed it).
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w960dp-h540dp")
class NavigationRailTest {

    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val items = listOf(
        MenuItem("home", "Home", Icons.Filled.Home, MenuAction.HOME),
        MenuItem("library", "Library", Icons.Filled.List, MenuAction.LIBRARY),
        MenuItem("stats", "Stats", Icons.Filled.Star, MenuAction.STATS)
    )

    @OptIn(ExperimentalComposeUiApi::class)
    private fun render(onSelect: (MenuItem) -> Unit = {}, contentFocus: FocusRequester? = null) {
        compose.setContent {
            FahrenheitTheme {
                NavigationRail(items = items, selectedId = "home", onSelect = onSelect) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        BasicText(
                            text = "a book",
                            modifier = Modifier
                                .testTag("content_item")
                                .let { m -> contentFocus?.let { m.focusRequester(it) } ?: m }
                                .focusable()
                        )
                    }
                }
            }
        }
    }

    @Test
    fun `every section carries a test id a script can name`() {
        render()

        items.forEach { compose.onNodeWithTag(menuItemTestTag(it.id)).assertExists() }
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun `choosing a section with the centre button reports which one`() {
        // A remote presses centre on a focused item; it never taps.
        var chosen: MenuItem? = null
        render(onSelect = { chosen = it })

        compose.onNodeWithTag(menuItemTestTag("stats"))
            .performSemanticsAction(SemanticsActions.RequestFocus)
        compose.onNodeWithTag(menuItemTestTag("stats"))
            .performKeyInput { pressKey(Key.DirectionCenter) }
        compose.waitForIdle()

        assertEquals("stats", chosen?.id)
    }

    @Test
    fun `the rail takes room of its own, so the content does not start at the edge`() {
        render()

        val contentLeft = compose.onNodeWithTag("content_item").fetchSemanticsNode()
            .positionInRoot.x
        assert(contentLeft > 0f) { "content starts at $contentLeft, with no rail beside it" }
    }

    @Test
    fun `a focused section shows its label, however focus got there`() {
        // Arrival focus starts in the rail, which the drawer does not count as
        // focus entering it, so the labels stayed hidden and the sections were
        // navigated blind.
        render()

        compose.onNodeWithTag(menuItemTestTag("home"))
            .performSemanticsAction(SemanticsActions.RequestFocus)
        compose.waitForIdle()

        compose.onNodeWithText("Home").assertIsDisplayed()
        compose.onNodeWithText("Stats").assertIsDisplayed()
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun `left from the content lands in the rail`() {
        val contentFocus = FocusRequester()
        render(contentFocus = contentFocus)
        compose.runOnUiThread { contentFocus.requestFocus() }
        compose.waitForIdle()

        compose.onNodeWithTag("content_item").performKeyInput { pressKey(Key.DirectionLeft) }
        compose.waitForIdle()

        compose.onNodeWithTag(menuItemTestTag("home")).assertIsFocused()
    }

    // #107: Now playing sits above the sections, and knows whether the rail is open.
    private fun renderWithNowPlaying() {
        compose.setContent {
            FahrenheitTheme {
                NavigationRail(
                    items = items, selectedId = "home", onSelect = {},
                    nowPlaying = { open -> BasicText(if (open) "NOW OPEN" else "NOW CLOSED", Modifier.testTag("now_playing")) }
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        BasicText("a book", Modifier.testTag("content_item").focusable())
                    }
                }
            }
        }
        compose.waitForIdle()
    }

    @Test
    fun `now playing sits above the sections`() {
        renderWithNowPlaying()
        val entry = compose.onNodeWithTag("now_playing").getUnclippedBoundsInRoot()
        val home = compose.onNodeWithTag(menuItemTestTag("home")).getUnclippedBoundsInRoot()
        assertTrue("entry ${entry.top} above home ${home.top}", entry.bottom <= home.top)
    }

    @Test
    fun `now playing opens with the rail`() {
        renderWithNowPlaying()
        compose.onNodeWithText("NOW CLOSED").assertExists()
        compose.onNodeWithTag(menuItemTestTag("home")).performSemanticsAction(SemanticsActions.RequestFocus)
        compose.waitForIdle()
        compose.onNodeWithText("NOW OPEN").assertExists()
    }

    // Review (#107): the entry's line took the whole width, and the rail with it.
    private val longTitle = RailEntry(
        "b1", null, "A Very Long Title of a Book That Goes On and On Past Any Rail", playing = true,
        progress = 0.3f, chapter = "Chapter 12", chapterNumber = 12, leftSeconds = 1210.0
    )

    private fun renderWithEntry() {
        compose.setContent {
            FahrenheitTheme {
                NavigationRail(
                    items = items, selectedId = "home", onSelect = {},
                    nowPlaying = { open -> NowPlayingEntry(longTitle, open, onOpen = {}, onStop = {}) }
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        BasicText("a book", Modifier.testTag("content_item").focusable())
                    }
                }
            }
        }
        compose.waitForIdle()
    }

    @Test
    fun `with something playing the closed rail stays narrow`() {
        renderWithEntry()
        compose.onNodeWithTag("content_item").assertLeftPositionInRootIsAtMost(64.dp)
    }

    @Test
    fun `a long title does not widen the open rail`() {
        renderWithEntry()
        compose.onNodeWithTag(menuItemTestTag("home")).performSemanticsAction(SemanticsActions.RequestFocus)
        compose.waitForIdle()
        compose.onNodeWithTag("content_item").assertLeftPositionInRootIsAtMost(270.dp)
    }

    private fun androidx.compose.ui.test.SemanticsNodeInteraction.assertLeftPositionInRootIsAtMost(limit: androidx.compose.ui.unit.Dp) {
        val left = getUnclippedBoundsInRoot().left
        assertTrue("left at $left, at most $limit", left <= limit)
    }
}
