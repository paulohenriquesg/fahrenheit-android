package com.paulohenriquesg.fahrenheit.detail

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicText
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import com.paulohenriquesg.fahrenheit.api.Chapter
import com.paulohenriquesg.fahrenheit.player.ChapterClock
import com.paulohenriquesg.fahrenheit.ui.components.DESCRIPTION_BOX_TAG
import com.paulohenriquesg.fahrenheit.ui.theme.FahrenheitTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** The details screen carries Now playing, for a book and a podcast (#159). */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w960dp-h540dp")
class DetailNowPlayingTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    // The bar as wide and tall as the real one, with its Stop.
    private val bar: @androidx.compose.runtime.Composable () -> Unit = {
        BasicText("bar", Modifier.testTag("bar").width(450.dp).height(56.dp))
    }

    // In the header, which scrolls away: a bar fixed above the episodes would
    // leave even fewer rows in 540dp.
    @Test fun `on a podcast's screen the bar sits beside the cover, above the title`() {
        compose.setContent {
            FahrenheitTheme {
                DetailHeader(
                    itemId = "p1",
                    content = DetailHeaderContent("A Podcast", null, emptyList(), "Play", null),
                    onPrimary = {},
                    nowPlaying = bar
                )
            }
        }
        compose.waitForIdle()
        val top = compose.onNodeWithTag("bar").getUnclippedBoundsInRoot()
        val cover = compose.onNodeWithContentDescription("A Podcast").getUnclippedBoundsInRoot()
        val title = compose.onNodeWithText("A Podcast").getUnclippedBoundsInRoot()
        assertTrue("bar $top, cover $cover", top.left >= cover.right)
        assertTrue("bar $top, title $title", top.bottom <= title.top)
    }

    private val three = ChapterClock.spans(
        listOf(Chapter(start = 0.0, end = 600.0, title = "One"), Chapter(start = 600.0, end = 1200.0, title = "Two")),
        total = 1200.0
    )

    private fun showLongBook() {
        compose.setContent {
            FahrenheitTheme {
                DetailBody(isBook = true) { margin ->
                    BookDetailView(
                        itemId = "b1",
                        content = DetailHeaderContent(
                            "A Very Long Title of a Book, Volume Three: The Part Where Everything Happens at Once",
                            "First Author, Second Author, Third Author · read by A Reader and Another Reader",
                            emptyList(), "Resume at 1 h 2 min", "<p>${"A long blurb that goes on. ".repeat(40)}</p>"
                        ),
                        onPrimary = {},
                        chapters = three,
                        finished = false,
                        padding = margin,
                        nowPlaying = bar
                    )
                }
            }
        }
        compose.waitForIdle()
    }

    // Review: above the whole book it pushed Chapters off the screen again (#134).
    @Test
    @Config(qualifiers = "w960dp-h540dp", fontScale = 1.3f)
    fun `on a book's screen the bar leaves every action on screen`() {
        showLongBook()
        val screen = compose.onRoot().getUnclippedBoundsInRoot()
        listOf("Chapters", "Mark finished").forEach { action ->
            val bounds = compose.onNodeWithText(action).getUnclippedBoundsInRoot()
            assertTrue("$action ends at ${bounds.bottom}, the screen at ${screen.bottom}", bounds.bottom <= screen.bottom)
        }
    }

    @Test fun `on a book's screen the bar sits beside the cover, above the description`() {
        showLongBook()
        val top = compose.onNodeWithTag("bar").getUnclippedBoundsInRoot()
        val cover = compose.onNodeWithContentDescription(
            "A Very Long Title of a Book, Volume Three: The Part Where Everything Happens at Once"
        ).getUnclippedBoundsInRoot()
        val description = compose.onNodeWithTag(DESCRIPTION_BOX_TAG).getUnclippedBoundsInRoot()
        assertTrue("bar $top, cover $cover", top.left >= cover.right)
        assertTrue("bar $top, description $description", top.bottom <= description.top)
    }
}
