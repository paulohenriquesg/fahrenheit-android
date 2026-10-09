package com.paulohenriquesg.fahrenheit.favourites

import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performSemanticsAction
import com.google.gson.Gson
import com.paulohenriquesg.fahrenheit.api.LibraryItem
import com.paulohenriquesg.fahrenheit.api.Playlist
import com.paulohenriquesg.fahrenheit.api.PlaylistEpisode
import com.paulohenriquesg.fahrenheit.api.PlaylistItem
import com.paulohenriquesg.fahrenheit.api.Shelf
import com.paulohenriquesg.fahrenheit.detail.DetailActivity
import com.paulohenriquesg.fahrenheit.main.PersonalizedHomeView
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

/** Home's Favourites shelf (#180; frame 4 of docs/mocks/podcast-actions.html). */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w960dp-h540dp")
class FavouritesShelfTest {

    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private fun item(id: String, title: String, mediaType: String): LibraryItem = Gson().fromJson(
        """{"id":"$id","libraryId":"lib","mediaType":"$mediaType",
            "media":{"metadata":{"title":"$title"},"tags":[],"numTracks":1,"numAudioFiles":1,
            "numChapters":0,"duration":0.0,"size":0}}""",
        LibraryItem::class.java
    )

    private fun episode(id: String, title: String, show: String = "pod-1") =
        PlaylistItem(show, id, PlaylistEpisode(id, title), item(show, "An Invented Show", "podcast"))

    private fun book(id: String, title: String) = PlaylistItem(id, libraryItem = item(id, title, "book"))

    private fun shelf(id: String) = Shelf(id = id, label = id, labelStringKey = "", type = "book")

    @Test
    fun `no playlist chosen, no shelf`() {
        assertNull(FavouritesShelf.of(null))
    }

    @Test
    fun `an empty playlist, no shelf`() {
        assertNull(FavouritesShelf.of(Playlist("pl_1", "Favourites", "lib", emptyList())))
    }

    @Test
    fun `the shelf is named after the playlist and keeps its order`() {
        val shelf = FavouritesShelf.of(
            Playlist("pl_1", "Bedtime", "lib", listOf(episode("ep-2", "Second"), episode("ep-1", "First")))
        )!!

        assertEquals("Bedtime", shelf.label)
        assertEquals(listOf("ep-2", "ep-1"), shelf.bookEntities!!.map { it.recentEpisode?.id })
    }

    @Test
    fun `it goes after Continue Listening`() {
        val favourites = FavouritesShelf.of(Playlist("pl_1", "Favourites", "lib", listOf(book("b1", "A Book"))))

        val placed = FavouritesShelf.placed(listOf(shelf("continue-listening"), shelf("recently-added")), favourites)

        assertEquals(listOf("continue-listening", FavouritesShelf.ID, "recently-added"), placed.map { it.id })
    }

    @Test
    fun `without Continue Listening it comes first`() {
        val favourites = FavouritesShelf.of(Playlist("pl_1", "Favourites", "lib", listOf(book("b1", "A Book"))))

        val placed = FavouritesShelf.placed(listOf(shelf("recently-added")), favourites)

        assertEquals(listOf(FavouritesShelf.ID, "recently-added"), placed.map { it.id })
    }

    @Test
    fun `no shelf leaves Home as it was`() {
        val shelves = listOf(shelf("continue-listening"))

        assertEquals(shelves, FavouritesShelf.placed(shelves, null))
    }

    private fun press(playlist: Playlist, text: String): Intent {
        compose.setContent { FahrenheitTheme { PersonalizedHomeView(listOfNotNull(FavouritesShelf.of(playlist)), "lib") } }
        compose.waitForIdle()
        compose.onNodeWithText(playlist.name).assertExists()
        compose.onNodeWithText(text).performSemanticsAction(SemanticsActions.OnClick)
        val started: Intent? = shadowOf(compose.activity).nextStartedActivity
        assertNotNull("nothing was started", started)
        return started!!
    }

    @Test
    fun `an episode on it plays from where it was left`() {
        val started = press(Playlist("pl_1", "Bedtime", "lib", listOf(episode("ep-1", "An Invented Episode"))), "An Invented Episode")

        assertEquals(PlayerActivity::class.java.name, started.component?.className)
        assertEquals("pod-1", started.getStringExtra("item_id"))
        assertEquals("ep-1", started.getStringExtra("episode_id"))
        assertTrue(started.getBooleanExtra("auto_play", false))
    }

    @Test
    fun `a book on it opens its screen`() {
        val started = press(Playlist("pl_1", "Bedtime", "lib", listOf(book("b1", "An Invented Book"))), "An Invented Book")

        assertEquals(DetailActivity::class.java.name, started.component?.className)
        assertEquals("b1", started.getStringExtra("item_id"))
    }
}
