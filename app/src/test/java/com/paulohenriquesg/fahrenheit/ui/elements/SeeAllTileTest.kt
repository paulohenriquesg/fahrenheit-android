package com.paulohenriquesg.fahrenheit.ui.elements

import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performSemanticsAction
import com.google.gson.Gson
import com.paulohenriquesg.fahrenheit.api.Author
import com.paulohenriquesg.fahrenheit.api.LibraryItem
import com.paulohenriquesg.fahrenheit.api.Series
import com.paulohenriquesg.fahrenheit.api.Shelf
import com.paulohenriquesg.fahrenheit.ui.theme.FahrenheitTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** The "See all" tile closing a shelf row (#123). */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w960dp-h540dp")
class SeeAllTileTest {

    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private fun render(content: @Composable () -> Unit) {
        compose.setContent { FahrenheitTheme { content() } }
        compose.waitForIdle()
    }

    private val book: LibraryItem = Gson().fromJson(
        """{"id":"b1","ino":"1","libraryId":"lib","folderId":"f","path":"/p","relPath":"p",
            "isFile":false,"mtimeMs":0,"ctimeMs":0,"birthtimeMs":0,"addedAt":0,"updatedAt":0,
            "isMissing":false,"isInvalid":false,"mediaType":"book",
            "media":{"metadata":{"title":"An Invented Book"},"tags":[],"numTracks":0,"numAudioFiles":0,
            "numChapters":0,"duration":0.0,"size":0}}""",
        LibraryItem::class.java
    )

    private fun shelf(type: String) = Shelf(id = "s", label = "A Shelf", labelStringKey = "", type = type)

    /** Focusable like a cover, and pressing it does what it says. */
    private fun assertTileWorks(pressed: () -> Int) {
        val tile = compose.onNodeWithText("See all 40")
        tile.performSemanticsAction(SemanticsActions.RequestFocus)
        tile.assertIsFocused()
        tile.performSemanticsAction(SemanticsActions.OnClick)
        assertEquals(1, pressed())
    }

    @Test
    fun `a row of covers ends with the tile, after the last cover`() {
        var presses = 0
        render { ShelfRow(shelf("book").copy(bookEntities = listOf(book)), seeAllTotal = 40, onSeeAll = { presses++ }) {} }

        val cover = compose.onNodeWithText("An Invented Book").getUnclippedBoundsInRoot()
        val tile = compose.onNodeWithText("See all 40").getUnclippedBoundsInRoot()
        assertTrue("tile after the cover", tile.left > cover.right)
        assertTileWorks { presses }
    }

    @Test
    fun `a row of authors ends with the tile`() {
        var presses = 0
        render {
            AuthorShelfRow(shelf("authors"), listOf(Author(id = "a1", name = "A Writer")), seeAllTotal = 40, onSeeAll = { presses++ }) {}
        }

        assertTileWorks { presses }
    }

    @Test
    fun `a row of series ends with the tile`() {
        var presses = 0
        render {
            SeriesShelfRow(shelf("series"), listOf(Series(id = "s1", name = "A Series")), seeAllTotal = 40, onSeeAll = { presses++ }) {}
        }

        assertTileWorks { presses }
    }

    @Test
    fun `a row with nothing more to see has no tile`() {
        render { ShelfRow(shelf("book").copy(bookEntities = listOf(book))) {} }

        compose.onNodeWithText("See all", substring = true).assertDoesNotExist()
    }
}
