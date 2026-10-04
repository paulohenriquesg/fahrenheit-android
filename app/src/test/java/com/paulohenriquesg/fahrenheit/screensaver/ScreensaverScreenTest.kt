package com.paulohenriquesg.fahrenheit.screensaver

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.graphics.ImageBitmap
import com.paulohenriquesg.fahrenheit.ui.theme.FahrenheitTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Both screensaver styles, from docs/mocks/screensaver.html (#156). */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w960dp-h540dp")
class ScreensaverScreenTest {

    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val line = NowPlayingLine(itemId = "b1", title = "An Invented Book", detail = "Chapter 3 · 12 min left in chapter")

    private fun cover(): ImageBitmap = ImageBitmap(40, 40)

    private fun render(style: ScreensaverStyle, covers: List<ImageBitmap> = emptyList()) {
        // The wall drifts for as long as it is drawn; time here is moved by hand.
        compose.mainClock.autoAdvance = false
        compose.setContent {
            FahrenheitTheme {
                ScreensaverScreen(style = style, listening = Listening(playing = true, line = line, covers = covers, wash = null), elapsedMs = 0L)
            }
        }
        compose.mainClock.advanceTimeByFrame()
    }

    @Test
    fun `the wall of covers fills with covers, and says what is playing`() {
        render(ScreensaverStyle.Wall, covers = List(3) { cover() })

        assertTrue(compose.onAllNodesWithTag(ScreensaverTags.WALL, useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty())
        compose.onNodeWithText("An Invented Book", useUnmergedTree = true).assertExists()
        compose.onNodeWithText("Chapter 3 · 12 min left in chapter", useUnmergedTree = true).assertExists()
    }

    @Test
    fun `the bouncing cover is the playing cover, with its chapter and time left under it`() {
        render(ScreensaverStyle.Bouncing)

        assertEquals(1, compose.onAllNodesWithTag(ScreensaverTags.BOUNCING_COVER, useUnmergedTree = true).fetchSemanticsNodes().size)
        compose.onNodeWithText("Chapter 3 · 12 min left in chapter", useUnmergedTree = true).assertExists()
    }
}
