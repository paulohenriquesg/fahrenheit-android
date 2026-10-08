package com.paulohenriquesg.fahrenheit.main

import androidx.activity.ComponentActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performSemanticsAction
import com.google.gson.Gson
import com.paulohenriquesg.fahrenheit.api.LibraryItem
import com.paulohenriquesg.fahrenheit.api.Shelf
import com.paulohenriquesg.fahrenheit.ui.theme.FahrenheitTheme
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
                shelf("continue-listening", book("b1"), book("b2")),
                shelf("recently-added", book("b3"))
            )
        )
        compose.setContent { FahrenheitTheme { PersonalizedHomeView(shelves, "lib") } }
        compose.onNodeWithText("Book b2").performSemanticsAction(SemanticsActions.RequestFocus)
        compose.onNodeWithText("Book b2").assertIsFocused()

        // A book just started joins Continue listening ahead of it, and a shelf appears above.
        shelves = listOf(
            shelf("listen-again", book("b4")),
            shelf("continue-listening", book("b0"), book("b1"), book("b2")),
            shelf("recently-added", book("b3"))
        )
        compose.waitForIdle()

        compose.onNodeWithText("Book b2").assertIsFocused()
    }

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
