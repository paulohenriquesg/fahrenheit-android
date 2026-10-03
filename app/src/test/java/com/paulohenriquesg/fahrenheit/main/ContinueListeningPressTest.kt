package com.paulohenriquesg.fahrenheit.main

import android.content.Intent
import android.os.SystemClock
import android.view.KeyEvent
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
import org.junit.Assert.assertNotNull
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
    ).let { it.copy(media = it.media.copy(numAudioFiles = 3)) }

    private val silentBook: LibraryItem = book.copy(media = book.media.copy(numAudioFiles = 0))

    private fun shelf(id: String) =
        Shelf(id = id, label = "A Shelf", labelStringKey = "", type = "book", bookEntities = listOf(book))

    private fun render(shelfId: String) {
        compose.setContent { FahrenheitTheme { PersonalizedHomeView(listOf(shelf(shelfId)), "lib") } }
        compose.waitForIdle()
    }

    private fun started(): Intent {
        val intent: Intent? = shadowOf(compose.activity).nextStartedActivity
        assertNotNull("nothing was started", intent)
        return intent!!
    }

    private fun nothingStarted() = assertNull(shadowOf(compose.activity).nextStartedActivity)

    /** The remote's centre button, held: a press, its repeat, and the release. */
    private fun holdCentre() {
        compose.onNodeWithText("An Invented Book").performSemanticsAction(SemanticsActions.RequestFocus)
        compose.waitForIdle()
        val down = SystemClock.uptimeMillis()
        compose.runOnUiThread {
            val view = compose.activity.window.decorView
            view.dispatchKeyEvent(KeyEvent(down, down, KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_CENTER, 0))
            view.dispatchKeyEvent(
                KeyEvent(down, down + 600, KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_CENTER, 1, 0, 0, 0, KeyEvent.FLAG_LONG_PRESS)
            )
            view.dispatchKeyEvent(KeyEvent(down, down + 700, KeyEvent.ACTION_UP, KeyEvent.KEYCODE_DPAD_CENTER, 0))
        }
        compose.waitForIdle()
    }

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
    fun `holding the remote's centre button on a Continue listening book opens its details, not the player`() {
        render("continue-listening")

        holdCentre()

        assertEquals(DetailActivity::class.java.name, started().component?.className)
        nothingStarted()
    }

    @Test
    fun `a Continue listening book with no audio left opens its details rather than a player that cannot play it`() {
        compose.setContent {
            FahrenheitTheme {
                PersonalizedHomeView(listOf(shelf("continue-listening").copy(bookEntities = listOf(silentBook))), "lib")
            }
        }
        compose.waitForIdle()

        compose.onNodeWithText("An Invented Book").performSemanticsAction(SemanticsActions.OnClick)

        assertEquals(DetailActivity::class.java.name, started().component?.className)
    }

    @Test
    fun `long-pressing a book on another shelf does nothing new`() {
        render("recently-added")

        compose.onNodeWithText("An Invented Book").performSemanticsAction(SemanticsActions.OnLongClick)

        nothingStarted()
    }

    @Test
    fun `pressing a book on any other shelf still opens its details`() {
        render("recently-added")

        compose.onNodeWithText("An Invented Book").performSemanticsAction(SemanticsActions.OnClick)

        assertEquals(DetailActivity::class.java.name, started().component?.className)
    }
}
