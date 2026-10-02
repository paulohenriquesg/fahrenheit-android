package com.paulohenriquesg.fahrenheit.podcast

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.google.gson.Gson
import com.paulohenriquesg.fahrenheit.api.LibraryItem
import com.paulohenriquesg.fahrenheit.ui.elements.LibraryItemCard
import com.paulohenriquesg.fahrenheit.ui.theme.FahrenheitTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w960dp-h540dp")
class EmptyPodcastViewTest {

    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun `an empty podcast shows every line, not "No Episodes"`() {
        compose.setContent {
            FahrenheitTheme {
                EmptyPodcastView(lines = listOf("Nothing downloaded yet", "Feed never checked"))
            }
        }

        compose.onNodeWithText("Nothing downloaded yet").assertIsDisplayed()
        compose.onNodeWithText("Feed never checked").assertIsDisplayed()
        compose.onNodeWithText("No Episodes").assertDoesNotExist()
    }

    @Test
    fun `a podcast tile carries its episode count`() {
        val podcast = Gson().fromJson(
            """{"id":"li1","ino":"1","libraryId":"lib","folderId":"f","path":"/p","relPath":"p",
                "isFile":false,"mtimeMs":0,"ctimeMs":0,"birthtimeMs":0,"addedAt":0,"updatedAt":0,
                "isMissing":false,"isInvalid":false,"mediaType":"podcast",
                "media":{"metadata":{"title":"APIs You Won't Hate"},"tags":[],"numTracks":0,
                "numAudioFiles":0,"numChapters":0,"duration":0.0,"size":0,"numEpisodes":0}}""",
            LibraryItem::class.java
        )

        compose.setContent { FahrenheitTheme { LibraryItemCard(podcast) {} } }

        compose.onNodeWithText("Nothing downloaded").assertIsDisplayed()
    }
}
