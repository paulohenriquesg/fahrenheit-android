package com.paulohenriquesg.fahrenheit.main

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.paulohenriquesg.fahrenheit.progress.ProgressStore
import com.paulohenriquesg.fahrenheit.ui.elements.CoverProgress
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

        compose.setContent { FahrenheitTheme { PersonalizedHomeView(listOf(shelf), "lib", progress = CoverProgress.index(progress)) } }

        compose.onNodeWithText("48 min left").assertIsDisplayed()
        compose.onNodeWithText("Second Writer").assertIsDisplayed()
        compose.onNodeWithText("First Writer").assertDoesNotExist()
    }

    // #207: back from the player, the covers already say where it got to.
    @Test
    fun `what the player writes to the store reaches the cover, with no read of Home's own`() {
        val store = ProgressStore()
        val model = HomeViewModel(store)
        compose.setContent {
            val state by model.uiState.collectAsStateWithLifecycle()
            FahrenheitTheme { PersonalizedHomeView(listOf(shelf), "lib", progress = state.covers) }
        }
        compose.onNodeWithText("First Writer").assertIsDisplayed()

        compose.runOnIdle { store.played("book-1", null, position = 1200.0, duration = 3480.0) }
        compose.waitForIdle()

        compose.onNodeWithText("38 min left").assertIsDisplayed()
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
