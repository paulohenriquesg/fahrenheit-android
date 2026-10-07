package com.paulohenriquesg.fahrenheit.api

import com.google.gson.Gson
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * A playlist as the server sends it (#180): from GET /api/libraries/:id/playlists,
 * and back from create, batch add and batch remove. Shape read from the server's
 * Playlist.toOldJSONExpanded; values made up.
 */
class PlaylistPayloadShapeTest {

    private val episodes = """
        { "results": [ {
            "id": "pl_1", "name": "Favourites", "libraryId": "lib_1", "userId": "u_1",
            "description": null, "lastUpdate": 1789812000000, "createdAt": 1789812000000,
            "items": [
              { "episodeId": "ep_2", "libraryItemId": "li_1",
                "episode": { "libraryItemId": "li_1", "podcastId": "pod_1", "id": "ep_2", "index": 2,
                  "title": "An Invented Episode", "duration": 1800.5, "publishedAt": 1789812000000 },
                "libraryItem": { "id": "li_1", "libraryId": "lib_1", "mediaType": "podcast",
                  "media": { "metadata": { "title": "An Invented Show" }, "tags": [], "numEpisodes": 4 } } },
              { "episodeId": "ep_1", "libraryItemId": "li_1",
                "episode": { "libraryItemId": "li_1", "id": "ep_1", "title": "An Older Episode" },
                "libraryItem": { "id": "li_1", "libraryId": "lib_1", "mediaType": "podcast",
                  "media": { "metadata": { "title": "An Invented Show" }, "tags": [] } } }
            ] } ],
          "total": 1, "limit": 0, "page": 0 }
    """.trimIndent()

    private val books = """
        { "id": "pl_2", "name": "Bedtime", "libraryId": "lib_2", "userId": "u_1",
          "items": [ { "libraryItemId": "b_1",
            "libraryItem": { "id": "b_1", "libraryId": "lib_2", "mediaType": "book",
              "media": { "metadata": { "title": "An Invented Book" }, "tags": [] } } } ] }
    """.trimIndent()

    @Test
    fun `a library's playlists parse, items in playlist order`() {
        val playlist = Gson().fromJson(episodes, PlaylistsResponse::class.java).results!!.single()

        assertEquals("pl_1", playlist.id)
        assertEquals("Favourites", playlist.name)
        assertEquals("lib_1", playlist.libraryId)
        assertEquals(listOf("ep_2", "ep_1"), playlist.items!!.map { it.episodeId })
        assertEquals("An Invented Episode", playlist.items!!.first().episode?.title)
        assertEquals("An Invented Show", playlist.items!!.first().libraryItem?.media?.metadata?.title)
    }

    @Test
    fun `a book in a playlist has no episode`() {
        val item = Gson().fromJson(books, Playlist::class.java).items!!.single()

        assertEquals("b_1", item.libraryItemId)
        assertNull(item.episodeId)
        assertEquals("An Invented Book", item.libraryItem?.media?.metadata?.title)
    }

    @Test
    fun `what add and remove send names the item, and the episode only when there is one`() {
        val gson = Gson()

        assertEquals(
            """{"items":[{"libraryItemId":"li_1","episodeId":"ep_1"}]}""",
            gson.toJson(PlaylistItems(listOf(PlaylistItemRef("li_1", "ep_1"))))
        )
        assertEquals(
            """{"items":[{"libraryItemId":"b_1"}]}""",
            gson.toJson(PlaylistItems(listOf(PlaylistItemRef("b_1"))))
        )
        assertEquals("""{"libraryId":"lib_1","name":"Favourites"}""", gson.toJson(NewPlaylist("lib_1", "Favourites")))
    }
}
