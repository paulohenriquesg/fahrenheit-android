package com.paulohenriquesg.fahrenheit.ui.elements

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.dp
import com.google.gson.Gson
import com.paulohenriquesg.fahrenheit.api.LibraryItem
import com.paulohenriquesg.fahrenheit.ui.theme.FahrenheitTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * A series or a collection shows its books' covers (#149), as the server's web
 * client does: a collection its first two side by side, each with its own
 * placeholder when it has no cover; a series up to three fanned, only those
 * with a cover. The books come with each group; nothing new is asked for.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w960dp-h540dp")
class BookGroupCardTest {

    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private fun book(n: Int, cover: Boolean = true): LibraryItem = Gson().fromJson(
        """{"id":"b$n","ino":"1","libraryId":"lib","folderId":"f","path":"/p","relPath":"p",
            "isFile":false,"mtimeMs":0,"ctimeMs":0,"birthtimeMs":0,"addedAt":0,"updatedAt":0,
            "isMissing":false,"isInvalid":false,"mediaType":"book",
            "media":{"metadata":{"title":"Invented Book $n","authorName":"Invented Writer $n"},
            ${if (cover) "\"coverPath\":\"/covers/b$n.jpg\"," else ""}"tags":[],"numTracks":0,
            "numAudioFiles":0,"numChapters":0,"duration":0.0,"size":0}}""",
        LibraryItem::class.java
    )

    private fun render(look: BookGroupLook, books: List<LibraryItem>) {
        compose.setContent {
            FahrenheitTheme {
                BookGroupCard(name = "An Invented Group", bookCount = books.size, books = books, look = look, onClick = {})
            }
        }
        compose.waitForIdle()
    }

    private fun covers() = compose.onAllNodesWithTag(CoverTags.GROUP_COVER, useUnmergedTree = true)

    @Test
    fun `a collection shows its first two covers`() {
        render(BookGroupLook.Collection, List(5) { book(it) })

        covers().assertCountEquals(2)
        compose.onNodeWithText("An Invented Group").assertIsDisplayed()
        compose.onNodeWithText("5 books", substring = true).assertIsDisplayed()
    }

    @Test
    fun `a collection of one book shows that one`() {
        render(BookGroupLook.Collection, listOf(book(1)))

        covers().assertCountEquals(1)
    }

    // A collection draws each book as the web client's BookCover does: a book
    // without a cover gets its own placeholder, not the group's name.
    @Test
    fun `a collection's coverless book shows its own title and author`() {
        render(BookGroupLook.Collection, listOf(book(1, cover = false)))

        compose.onAllNodesWithTag(CoverTags.PLACEHOLDER, useUnmergedTree = true)[0]
            .assert(SemanticsMatcher.expectValue(CoverPlaceholderLines, listOf("Invented Book 1", "Invented Writer 1")))
    }

    @Test
    fun `an empty collection says so instead of covers`() {
        render(BookGroupLook.Collection, emptyList())

        covers().assertCountEquals(0)
        compose.onNodeWithText("Empty collection", substring = true).assertExists()
    }

    @Test
    fun `a series fans up to three covers`() {
        render(BookGroupLook.Series, List(7) { book(it) })

        covers().assertCountEquals(3)
    }

    // The web client's series cover leaves out books without one.
    @Test
    fun `a series fans only the books that have a cover`() {
        render(BookGroupLook.Series, listOf(book(1, cover = false), book(2), book(3, cover = false), book(4)))

        covers().assertCountEquals(2)
    }

    @Test
    fun `a series with no covers is just its name`() {
        render(BookGroupLook.Series, listOf(book(1, cover = false)))

        covers().assertCountEquals(0)
        compose.onNodeWithText("An Invented Group").assertIsDisplayed()
        compose.onNodeWithText("Empty collection", substring = true).assertDoesNotExist()
    }

    // The fan used to measure as one cover wide, so it sat off-centre and its
    // last cover ran past the card's edge, clipped.
    @Test
    fun `the fan stays inside the card`() {
        render(BookGroupLook.Series, List(3) { book(it) })

        val card = compose.onNodeWithText("An Invented Group").getUnclippedBoundsInRoot()
        // Unclipped: the card clips what overflows it, so clipped bounds would
        // always look inside.
        val fanned = (0 until 3).map { covers()[it].getUnclippedBoundsInRoot() }
        assertTrue("$fanned in $card", fanned.all { it.right <= card.right + 0.5.dp && it.left >= card.left - 0.5.dp })
    }

    // The card already says its name; covers that said it too made a screen
    // reader repeat it once per cover.
    @Test
    fun `the covers do not repeat the group's name to a screen reader`() {
        render(BookGroupLook.Series, List(3) { book(it) })

        compose.onAllNodesWithContentDescription("An Invented Group", useUnmergedTree = true).assertCountEquals(0)
    }
}
