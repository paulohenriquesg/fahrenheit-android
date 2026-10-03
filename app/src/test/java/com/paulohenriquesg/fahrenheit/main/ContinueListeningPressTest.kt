package com.paulohenriquesg.fahrenheit.main

import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performSemanticsAction
import com.google.gson.Gson
import com.paulohenriquesg.fahrenheit.api.LibraryItem
import com.paulohenriquesg.fahrenheit.api.Shelf
import com.paulohenriquesg.fahrenheit.detail.DetailActivity
import com.paulohenriquesg.fahrenheit.player.PlayerActivity
import com.paulohenriquesg.fahrenheit.ui.theme.FahrenheitTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * A book in Continue listening plays on a press and opens its details on a
 * long press (#124), as an episode there already plays. Other book shelves
 * keep opening details.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w960dp-h540dp")
class ContinueListeningPressTest {

    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val book: LibraryItem = Gson().fromJson(
        """{"id":"book-1","ino":"1","libraryId":"lib","folderId":"f","path":"/p","relPath":"p",
            "isFile":false,"mtimeMs":0,"ctimeMs":0,"birthtimeMs":0,"addedAt":0,"updatedAt":0,
            "isMissing":false,"isInvalid":false,"mediaType":"book",
            "media":{"metadata":{"title":"An Invented Book"},"tags":[],"numTracks":0,"numAudioFiles":0,
            "numChapters":0,"duration":0.0,"size":0}}""",
        LibraryItem::class.java
    )

    private fun shelf(id: String) =
        Shelf(id = id, label = "A Shelf", labelStringKey = "", type = "book", bookEntities = listOf(book))

    private fun render(shelfId: String) {
        compose.setContent { FahrenheitTheme { PersonalizedHomeView(listOf(shelf(shelfId)), "lib") } }
        compose.waitForIdle()
    }

    private fun started(): Intent = shadowOf(compose.activity).nextStartedActivity

    @Test
    fun `pressing a book in Continue listening plays it from where it was`() {
        render("continue-listening")

        compose.onNodeWithText("An Invented Book").performSemanticsAction(SemanticsActions.OnClick)

        val intent = started()
        assertEquals(PlayerActivity::class.java.name, intent.component?.className)
        assertEquals("book-1", intent.getStringExtra("item_id"))
        assertNull(intent.getStringExtra("episode_id"))
        assertTrue(intent.getBooleanExtra("auto_play", false))
    }

    @Test
    fun `long-pressing a book in Continue listening opens its details`() {
        render("continue-listening")

        compose.onNodeWithText("An Invented Book").performSemanticsAction(SemanticsActions.OnLongClick)

        val intent = started()
        assertEquals(DetailActivity::class.java.name, intent.component?.className)
        assertEquals("book-1", intent.getStringExtra("item_id"))
    }

    @Test
    fun `pressing a book on any other shelf still opens its details`() {
        render("recently-added")

        compose.onNodeWithText("An Invented Book").performSemanticsAction(SemanticsActions.OnClick)

        assertEquals(DetailActivity::class.java.name, started().component?.className)
    }
}
