package com.paulohenriquesg.fahrenheit.main

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.lifecycle.Lifecycle
import com.google.gson.Gson
import com.paulohenriquesg.fahrenheit.api.LibraryItem
import com.paulohenriquesg.fahrenheit.api.MediaProgressResponse
import com.paulohenriquesg.fahrenheit.api.Shelf
import com.paulohenriquesg.fahrenheit.ui.theme.FahrenheitTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Home's covers say how far in each started book is (#104). */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w960dp-h540dp")
class HomeCoverProgressTest {

    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val shelf = Shelf(
        id = "continue-listening", label = "Continue Listening", labelStringKey = "LabelContinueListening",
        type = "book", bookEntities = listOf(book("book-1", "First Writer"), book("book-2", "Second Writer"))
    )

    @Test
    fun `a started book on a shelf shows its time left, an unstarted one its author`() {
        val progress = listOf(MediaProgressResponse(libraryItemId = "book-1", currentTime = 600.0, duration = 3480.0))

        compose.setContent { FahrenheitTheme { PersonalizedHomeView(listOf(shelf), "lib", fetchProgress = { progress }) } }

        compose.onNodeWithText("48 min left").assertIsDisplayed()
        compose.onNodeWithText("Second Writer").assertIsDisplayed()
        compose.onNodeWithText("First Writer").assertDoesNotExist()
    }

    @Test
    fun `coming back to Home, say from the player, reads progress again`() {
        var position = 600.0
        compose.setContent {
            FahrenheitTheme {
                PersonalizedHomeView(listOf(shelf), "lib", fetchProgress = {
                    listOf(MediaProgressResponse(libraryItemId = "book-1", currentTime = position, duration = 3480.0))
                })
            }
        }
        compose.onNodeWithText("48 min left").assertIsDisplayed()

        position = 1200.0
        compose.activityRule.scenario.moveToState(Lifecycle.State.STARTED)
        compose.activityRule.scenario.moveToState(Lifecycle.State.RESUMED)
        compose.waitForIdle()

        compose.onNodeWithText("38 min left").assertIsDisplayed()
    }

    @Test
    fun `progress that cannot be read leaves the covers as they were`() {
        compose.setContent {
            FahrenheitTheme { PersonalizedHomeView(listOf(shelf), "lib", fetchProgress = { error("offline") }) }
        }

        compose.onNodeWithText("First Writer").assertIsDisplayed()
    }

    private fun book(id: String, author: String): LibraryItem = Gson().fromJson(
        """{"id":"$id","ino":"1","libraryId":"lib","folderId":"f","path":"/p","relPath":"p",
            "isFile":false,"mtimeMs":0,"ctimeMs":0,"birthtimeMs":0,"addedAt":0,"updatedAt":0,
            "isMissing":false,"isInvalid":false,"mediaType":"book",
            "media":{"metadata":{"title":"Book $id","authorName":"$author"},"tags":[],
            "numTracks":0,"numAudioFiles":0,"numChapters":0,"duration":3480.0,"size":0}}""",
        LibraryItem::class.java
    )
}
