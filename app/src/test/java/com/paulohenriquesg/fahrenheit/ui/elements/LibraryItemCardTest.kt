package com.paulohenriquesg.fahrenheit.ui.elements

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.google.gson.Gson
import com.paulohenriquesg.fahrenheit.api.LibraryItem
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
class LibraryItemCardTest {

    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

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
