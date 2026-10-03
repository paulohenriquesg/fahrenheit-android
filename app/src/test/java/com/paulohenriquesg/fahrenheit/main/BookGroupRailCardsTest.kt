package com.paulohenriquesg.fahrenheit.main

import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performSemanticsAction
import com.google.gson.Gson
import com.paulohenriquesg.fahrenheit.api.Collection
import com.paulohenriquesg.fahrenheit.api.LibraryItem
import com.paulohenriquesg.fahrenheit.api.Series
import com.paulohenriquesg.fahrenheit.group.BookGroupActivity
import com.paulohenriquesg.fahrenheit.ui.theme.FahrenheitTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * The rail's Series and Collections sections (#73): a 160dp-tall card with the
 * group's name and its book count, opening the group's screen. Their cards
 * lived inside the unreachable browse Activities; this pins them as they were.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w960dp-h540dp")
class BookGroupRailCardsTest {

    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val book: LibraryItem = Gson().fromJson(
        """{"id":"b1","ino":"1","libraryId":"lib","folderId":"f","path":"/p","relPath":"p",
            "isFile":false,"mtimeMs":0,"ctimeMs":0,"birthtimeMs":0,"addedAt":0,"updatedAt":0,
            "isMissing":false,"isInvalid":false,"mediaType":"book",
            "media":{"metadata":{"title":"T"},"tags":[],"numTracks":0,"numAudioFiles":0,
            "numChapters":0,"duration":0.0,"size":0}}""",
        LibraryItem::class.java
    )

    private fun started(): Intent {
        val intent: Intent? = shadowOf(compose.activity).nextStartedActivity
        assertNotNull("nothing was started", intent)
        return intent!!
    }

    /** 160dp tall; the grid's adaptive 200dp cells set the width. */
    private fun assertCardSize(name: String) {
        val bounds = compose.onNodeWithText(name).getUnclippedBoundsInRoot()
        assertEquals(160f, (bounds.bottom - bounds.top).value, 0.5f)
    }

    @Test
    fun `a series card names it, counts its books and opens it`() {
        val series = Series(id = "s1", name = "An Invented Series", books = listOf(book, book.copy(id = "b2")))
        compose.setContent { FahrenheitTheme { SeriesBrowseView(listOf(series), isLoading = false) } }
        compose.waitForIdle()

        compose.onNodeWithText("An Invented Series").assertIsDisplayed()
        compose.onNodeWithText("2 books").assertIsDisplayed()
        assertCardSize("An Invented Series")
        compose.onNodeWithText("An Invented Series").performSemanticsAction(SemanticsActions.OnClick)
        assertEquals(BookGroupActivity::class.java.name, started().component?.className)
    }

    @Test
    fun `a collection card names it, counts its books and opens it`() {
        val collection = Collection(
            id = "c1", libraryId = "lib", name = "An Invented Collection", books = listOf(book),
            lastUpdate = 0L, createdAt = 0L
        )
        compose.setContent { FahrenheitTheme { CollectionsBrowseView(listOf(collection), isLoading = false) } }
        compose.waitForIdle()

        compose.onNodeWithText("An Invented Collection").assertIsDisplayed()
        compose.onNodeWithText("1 book").assertIsDisplayed()
        assertCardSize("An Invented Collection")
        compose.onNodeWithText("An Invented Collection").performSemanticsAction(SemanticsActions.OnClick)
        assertEquals(BookGroupActivity::class.java.name, started().component?.className)
    }

    @Test
    fun `a card for a group the server sent without a book list shows no count`() {
        val series = Series(id = "s1", name = "An Invented Series", books = null)
        compose.setContent { FahrenheitTheme { SeriesBrowseView(listOf(series), isLoading = false) } }
        compose.waitForIdle()

        compose.onNodeWithText("book", substring = true).assertDoesNotExist()
    }
}
