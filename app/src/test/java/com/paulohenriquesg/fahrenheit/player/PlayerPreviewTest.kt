package com.paulohenriquesg.fahrenheit.player

import androidx.test.core.app.ApplicationProvider
import com.google.gson.Gson
import com.paulohenriquesg.fahrenheit.api.ApiClient
import com.paulohenriquesg.fahrenheit.api.LibraryItem
import com.paulohenriquesg.fahrenheit.api.LibraryItemResponse
import com.paulohenriquesg.fahrenheit.storage.SharedPreferencesHandler
import com.paulohenriquesg.fahrenheit.storage.UserPreferences
import org.junit.After
import org.junit.Before
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * What the opening screen hands the player for its loading frame (#223):
 * the title, kicker and cover it already shows.
 */
@RunWith(RobolectricTestRunner::class)
class PlayerPreviewTest {

    private val context = ApplicationProvider.getApplicationContext<android.content.Context>()

    @Before
    fun signIn() {
        SharedPreferencesHandler(context).saveUserPreferences(UserPreferences("https://abs.test", "listener", "token", false))
        ApiClient.initialize(context)
    }

    @After
    fun signOut() = ApiClient.clearSession()

    private fun roundTrip(preview: PlayerPreview?): PlayerPreview? =
        PlayerActivity.previewOf(PlayerActivity.createIntent(context, "b1", autoPlay = true, preview = preview))

    @Test
    fun `a preview with every field travels with the intent`() {
        val preview = PlayerPreview("An Invented Book", kicker = "A. Writer", coverUrl = "https://abs.test/api/items/b1/cover")

        assertEquals(preview, roundTrip(preview))
    }

    @Test
    fun `a preview with no kicker or cover travels too`() {
        assertEquals(PlayerPreview("An Invented Book"), roundTrip(PlayerPreview("An Invented Book")))
        assertEquals(PlayerPreview("T", kicker = "K"), roundTrip(PlayerPreview("T", kicker = "K")))
        assertEquals(PlayerPreview("T", coverUrl = "u"), roundTrip(PlayerPreview("T", coverUrl = "u")))
    }

    @Test
    fun `no preview reads back as none`() {
        assertNull(roundTrip(null))
        assertNull(PlayerActivity.previewOf(PlayerActivity.createIntent(context, "b1")))
    }

    @Test
    fun `a book hands over its title and author, and its cover only when it has one`() {
        assertEquals(PlayerPreview("T", kicker = "A", coverUrl = "https://abs.test/api/items/b1/cover"), PlayerPreview.book("b1", "T", "A"))
        assertEquals(PlayerPreview("T"), PlayerPreview.book("b1", "T", null, hasCover = false))
    }

    @Test
    fun `an episode hands over its title, its show, and the show's cover`() {
        assertEquals(PlayerPreview("E", kicker = "S", coverUrl = "https://abs.test/api/items/p1/cover"), PlayerPreview.episode("p1", "E", "S"))
    }

    private fun shelfItem(json: String): LibraryItem = Gson().fromJson(
        """{"id":"pod-1","ino":"1","libraryId":"lib","folderId":"f","path":"/p","relPath":"p",
            "isFile":false,"mtimeMs":0,"ctimeMs":0,"birthtimeMs":0,"addedAt":0,"updatedAt":0,
            "isMissing":false,"isInvalid":false,$json}""",
        LibraryItem::class.java
    )

    // Home's cards (#223 entry points 1 and 2): what the card shows.
    @Test
    fun `a book card hands over its title and author, and no cover when it has none`() {
        val book = shelfItem(
            """"mediaType":"book","media":{"metadata":{"title":"An Invented Book","authorName":"An Invented Writer"},"tags":[],
            "numTracks":1,"numAudioFiles":1,"numChapters":0,"duration":0.0,"size":0}"""
        )

        assertEquals(PlayerPreview("An Invented Book", kicker = "An Invented Writer"), PlayerPreview.of(book))
        assertEquals(
            "https://abs.test/api/items/pod-1/cover",
            PlayerPreview.of(book.copy(media = book.media.copy(coverPath = "/c.jpg")))?.coverUrl
        )
    }

    @Test
    fun `an episode card hands over the episode's title, its show, and the show's cover`() {
        val episode = shelfItem(
            """"mediaType":"podcast","media":{"metadata":{"title":"An Invented Show"},"coverPath":"/c.jpg","tags":[],
            "numTracks":1,"numAudioFiles":1,"numChapters":0,"duration":0.0,"size":0},
            "recentEpisode":{"id":"ep-1","libraryItemId":"pod-1","title":"An Invented Episode"}"""
        )

        assertEquals(
            PlayerPreview("An Invented Episode", kicker = "An Invented Show", coverUrl = "https://abs.test/api/items/pod-1/cover"),
            PlayerPreview.of(episode)
        )
    }

    // The book's screen (#223 entry points 4 and 5).
    @Test
    fun `a book's screen hands over its title and author`() {
        val item: LibraryItemResponse = Gson().fromJson(
            """{"id":"b1","mediaType":"book","media":{"metadata":{"title":"An Invented Book","authorName":"An Invented Writer","explicit":false}}}""",
            LibraryItemResponse::class.java
        )

        assertEquals(
            PlayerPreview("An Invented Book", kicker = "An Invented Writer", coverUrl = "https://abs.test/api/items/b1/cover"),
            PlayerPreview.of(item)
        )
    }

    // Now playing in the rail and the bar (#223 entry points 3 and 6): the title they show.
    @Test
    fun `Now playing hands over the title it shows`() {
        val entry = RailEntry("p1", "e1", "An Invented Episode", playing = true, progress = 0.5f, chapter = null, leftSeconds = 60.0)

        assertEquals(
            PlayerPreview("An Invented Episode", coverUrl = "https://abs.test/api/items/p1/cover"),
            PlayerPreview.of(entry)
        )
    }

    @Test
    fun `a blank title or kicker is none`() {
        assertNull(PlayerPreview.book("b1", " ", "A"))
        assertNull(PlayerPreview.episode("p1", null, "S"))
        assertEquals(null, PlayerPreview.book("b1", "T", " ")?.kicker)
    }
}
