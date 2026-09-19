package com.paulohenriquesg.fahrenheit.api

import com.google.gson.Gson
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * /api/libraries/{id}/recent-episodes returns episodes flat, with the podcast
 * media object (not a library item) embedded. `episode` is the episode-number
 * string, often empty. Shape taken from a live server, values made up.
 */
class RecentEpisodesPayloadShapeTest {

    private val json = """
        { "episodes": [ {
            "libraryItemId": "li_1", "podcastId": "pod_1", "id": "ep_1",
            "oldEpisodeId": null, "index": null, "season": "", "episode": "",
            "episodeType": "full", "title": "Episode title", "subtitle": "",
            "description": "<p>About it</p>",
            "enclosure": { "url": "https://example.com/a.mp3", "type": "audio/mpeg", "length": "1" },
            "guid": "g", "pubDate": "Fri, 19 Sep 2026 10:00:00 GMT", "chapters": [],
            "audioFile": { "index": null, "ino": "1" }, "publishedAt": 1789812000000,
            "addedAt": 1789812000000, "updatedAt": 1789812000000,
            "audioTrack": { "index": 1 }, "size": 123, "duration": 1800.5, "libraryId": "lib_1",
            "podcast": {
                "id": "pod_1", "libraryItemId": "li_1", "coverPath": null, "tags": [], "episodes": [],
                "autoDownloadEpisodes": false, "autoDownloadSchedule": "0 * * * *",
                "lastEpisodeCheck": 0, "maxEpisodesToKeep": 0, "maxNewEpisodesToDownload": 3,
                "metadata": { "title": "Podcast title", "author": "Someone", "description": null,
                    "releaseDate": null, "genres": [], "feedUrl": null, "imageUrl": null,
                    "itunesPageUrl": null, "itunesId": null, "itunesArtistId": null,
                    "explicit": false, "language": null, "type": "episodic" }
            }
        } ], "limit": 50, "page": 0 }
    """.trimIndent()

    @Test
    fun `a recent episode parses with its podcast's title`() {
        val episode = Gson().fromJson(json, RecentEpisodesResponse::class.java).episodes.single()

        assertEquals("li_1", episode.libraryItemId)
        assertEquals("ep_1", episode.id)
        assertEquals("Episode title", episode.title)
        assertEquals("<p>About it</p>", episode.description)
        assertEquals("Podcast title", episode.podcast?.metadata?.title)
    }
}
