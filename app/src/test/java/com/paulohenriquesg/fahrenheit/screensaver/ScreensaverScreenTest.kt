package com.paulohenriquesg.fahrenheit.screensaver

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithText
import com.google.gson.Gson
import com.paulohenriquesg.fahrenheit.api.LibraryItem
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

    private fun item(id: String): LibraryItem = Gson().fromJson(
        """{"id":"$id","ino":"1","libraryId":"lib","folderId":"f","path":"/p","relPath":"p",
            "isFile":false,"mtimeMs":0,"ctimeMs":0,"birthtimeMs":0,"addedAt":0,"updatedAt":0,
            "isMissing":false,"isInvalid":false,"mediaType":"book",
            "media":{"metadata":{"title":"Book $id"},"tags":[],"numTracks":0,"numAudioFiles":0,
            "numChapters":0,"duration":0.0,"size":0}}""",
        LibraryItem::class.java
    )

    private fun render(style: ScreensaverStyle, covers: List<LibraryItem> = emptyList()) {
        compose.setContent {
            FahrenheitTheme {
                ScreensaverScreen(style = style, listening = Listening(playing = true, line = line, covers = covers, wash = null), shownForMs = 0L)
            }
        }
        compose.waitForIdle()
    }

    @Test
    fun `the wall of covers fills with covers, and says what is playing`() {
        render(ScreensaverStyle.Wall, covers = List(3) { item("w$it") })

        assertTrue(compose.onAllNodesWithTag(ScreensaverTags.WALL_COVER, useUnmergedTree = true).fetchSemanticsNodes().size >= 3)
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
