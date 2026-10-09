package com.paulohenriquesg.fahrenheit.main

import androidx.activity.ComponentActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performSemanticsAction
import com.google.gson.Gson
import com.paulohenriquesg.fahrenheit.api.LibraryItem
import com.paulohenriquesg.fahrenheit.api.Shelf
import com.paulohenriquesg.fahrenheit.ui.theme.FahrenheitTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** A reload of Home's shelves leaves focus where it was (#197). */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w960dp-h540dp")
class HomeReloadFocusTest {

    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun `new shelves that still hold the focused book keep focus on it`() {
        var shelves by mutableStateOf(
            listOf(
                shelf("continue-listening", book("b1"), book("b2"), book("b5"), book("b6"), book("b7")),
                shelf("recently-added", book("b3"))
            )
        )
        compose.setContent { FahrenheitTheme { PersonalizedHomeView(shelves, "lib") } }
        // Along the row, at the edge of what the screen holds.
        compose.onNodeWithText("Book b6").performSemanticsAction(SemanticsActions.RequestFocus)
        compose.onNodeWithText("Book b6").assertIsFocused()

        // A book just started joins Continue listening ahead of it, and a shelf appears above.
        shelves = listOf(
            shelf("listen-again", book("b4")),
            shelf("continue-listening", book("b0"), book("b1"), book("b2"), book("b5"), book("b6"), book("b7")),
            shelf("recently-added", book("b3"))
        )
        compose.waitForIdle()

        // The row holds focus: it keeps the focused card in view rather than going to its start.
        compose.onNodeWithText("Book b6").assertIsFocused()
        val focused = boundsOf("Book b6")
        assertTrue("the focused card is on screen: $focused", focused.width > 0 && focused.left >= 0)
    }

    // Device check of #214: the keyed row kept its old first card in view,
    // and the book just started sat off-screen to its left.
    @Test
    fun `a reload that puts a new book first shows it, in a row focus is not in`() {
        var shelves by mutableStateOf(
            listOf(
                shelf("continue-listening", book("b1"), book("b2"), book("b5"), book("b6"), book("b7")),
                shelf("recently-added", book("b3"))
            )
        )
        // More than the screen holds, so the row can scroll.
        compose.setContent { FahrenheitTheme { PersonalizedHomeView(shelves, "lib") } }
        compose.onNodeWithText("Book b3").performSemanticsAction(SemanticsActions.RequestFocus)
        compose.onNodeWithText("Book b1").assertIsDisplayed()

        shelves = listOf(
            shelf("continue-listening", book("b0"), book("b1"), book("b2"), book("b5"), book("b6"), book("b7")),
            shelf("recently-added", book("b3"))
        )
        compose.waitForIdle()

        // Placed, and on screen ahead of the one that was first: assertIsDisplayed
        // passes for the card left off-screen with empty bounds.
        val first = boundsOf("Book b0")
        assertTrue("the new first card is on screen: $first", first.width > 0 && first.left >= 0)
        assertTrue("ahead of the old first card", first.left < boundsOf("Book b1").left)
        compose.onNodeWithText("Book b3").assertIsFocused()
    }

    private fun boundsOf(text: String) = compose.onNodeWithText(text).fetchSemanticsNode().boundsInRoot

    private fun shelf(id: String, vararg books: LibraryItem) =
        Shelf(id = id, label = "Shelf $id", labelStringKey = "", type = "book", bookEntities = books.toList())

    private fun book(id: String): LibraryItem = Gson().fromJson(
        """{"id":"$id","ino":"1","libraryId":"lib","folderId":"f","path":"/p","relPath":"p",
            "isFile":false,"mtimeMs":0,"ctimeMs":0,"birthtimeMs":0,"addedAt":0,"updatedAt":0,
            "isMissing":false,"isInvalid":false,"mediaType":"book",
            "media":{"metadata":{"title":"Book $id","authorName":"A Writer"},"tags":[],
            "numTracks":1,"numAudioFiles":1,"numChapters":0,"duration":3480.0,"size":0}}""",
        LibraryItem::class.java
    )
}
