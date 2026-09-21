package com.paulohenriquesg.fahrenheit.api

import com.google.gson.Gson
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Real-shaped playback payloads must parse.
 *
 * The track below mirrors a multi-file audiobook from a live 2.36.0 server:
 * values and types are real, identifying strings are not. Multi-file books
 * routinely have fractional start offsets (every file after the first starts at
 * the cumulative duration of those before it), so any Int-typed offset fails on
 * them the same way #1 failed on maxBackupSize.
 */
class PlaybackPayloadShapeTest {
    private val gson = Gson()

    private val secondTrackOfMultiFileBook = """
        {
          "index": 2,
          "ino": "177924",
          "metadata": {
            "filename": "part2.m4b", "ext": ".m4b", "path": "part2.m4b", "relPath": "part2.m4b",
            "size": 357792270, "mtimeMs": 1730442922000, "ctimeMs": 1730442922000,
            "birthtimeMs": 1754712821510
          },
          "addedAt": 1767191717658,
          "updatedAt": 1767191717658,
          "trackNumFromMeta": 2,
          "discNumFromMeta": 1,
          "trackNumFromFilename": 2,
          "discNumFromFilename": null,
          "manuallyVerified": false,
          "exclude": false,
          "error": null,
          "format": "QuickTime / MOV",
          "duration": 43027.318322,
          "bitRate": 64069,
          "language": "eng",
          "codec": "aac",
          "timeBase": "1/22050",
          "channels": 2,
          "channelLayout": "stereo",
          "chapters": [
            { "start": 0, "end": 2398.942993, "title": "Chapter 1", "id": 0 },
            { "start": 2398.942993, "end": 5068.444989, "title": "Chapter 2", "id": 1 }
          ],
          "embeddedCoverArt": "mjpeg",
          "mimeType": "audio/mp4",
          "title": "Part 2",
          "startOffset": 31124.073333,
          "contentUrl": "/api/items/li_test/file/1"
        }
    """.trimIndent()

    @Test
    fun `a play session with a fractional track offset parses`() {
        // audioTracks in a play session have the same shape as an item's tracks.
        val response = gson.fromJson(
            """{ "audioTracks": [ $secondTrackOfMultiFileBook ] }""",
            PlayLibraryItemResponse::class.java
        )

        assertEquals(31124.073333, response.audioTracks[0].startOffset, 1e-6)
    }

    @Test
    fun `a play session survives a file larger than 2 GB`() {
        // Single-file m4b audiobooks can exceed Int.MAX_VALUE bytes.
        val bigFile = secondTrackOfMultiFileBook.replace("\"size\": 357792270", "\"size\": 3000000000")

        val response = gson.fromJson(
            """{ "audioTracks": [ $bigFile ] }""",
            PlayLibraryItemResponse::class.java
        )

        assertEquals(3_000_000_000L, response.audioTracks[0].metadata.size)
    }

    @Test
    fun `an episode enclosure length arrives as a string`() {
        // ABS serialises it as String(enclosureSize), not a number.
        val episode = gson.fromJson(
            """{ "enclosure": { "url": "https://example.test/e.mp3", "type": "audio/mpeg", "length": "45678901" } }""",
            Episode::class.java
        )

        assertEquals("45678901", episode.enclosure?.length)
    }

    @Test
    fun `an episode enclosure with no size parses as null, not zero`() {
        // ABS sends null when the feed gave no size. Typed as a primitive Long,
        // Gson silently turned that into 0 - a size that looks real but is not.
        val episode = gson.fromJson(
            """{ "enclosure": { "url": "https://example.test/e.mp3", "type": "audio/mpeg", "length": null } }""",
            Episode::class.java
        )

        assertEquals(null, episode.enclosure?.length)
    }

    @Test
    fun `a play session resuming part-way through an episode parses`() {
        // ABS starts a session at the saved position, which is almost always
        // fractional - 8 of 10 real sessions sampled. Typed as Int, resuming any
        // partly heard podcast episode failed to parse.
        val response = gson.fromJson(
            """{ "currentTime": 19141.935520695628, "startTime": 538.6500963920058,
                 "timeListening": 29.154561968609244, "audioTracks": [] }""",
            PlayLibraryItemResponse::class.java
        )

        assertEquals(19141.935520695628, response.currentTime, 1e-9)
        assertEquals(538.6500963920058, response.startTime, 1e-9)
        assertEquals(29.154561968609244, response.timeListening, 1e-9)
    }

    /**
     * The play session embeds a whole library item, and it parses into the
     * same classes as /api/items now that the duplicated family is gone.
     * A podcast session, because its metadata is the shape that differed.
     */
    @Test
    fun `a podcast play session parses into the shared item classes`() {
        val response = Gson().fromJson(
            """
            { "id": "play_1", "userId": "u1", "libraryItemId": "li1", "episodeId": "ep1",
              "mediaType": "podcast", "mediaMetadata": { "title": "The Show", "author": "Someone",
                "feedUrl": "https://example.invalid/feed.xml", "itunesId": "123456",
                "itunesArtistId": 789, "explicit": false, "genres": ["News"] },
              "chapters": [], "displayTitle": "Episode 42", "displayAuthor": "Someone",
              "coverPath": null, "duration": 1800.5, "playMethod": 0, "mediaPlayer": "unknown",
              "deviceInfo": { "deviceId": "d1", "clientName": "Fahrenheit", "sdkVersion": 25 },
              "timeListening": 0.0, "startTime": 0.0, "currentTime": 12.5,
              "startedAt": 1789812000000, "updatedAt": 1789812000000,
              "audioTracks": [ { "index": 1, "startOffset": 0.0, "duration": 1800.5,
                "title": "ep.mp3", "contentUrl": "/audio/ep.mp3", "mimeType": "audio/mpeg",
                "metadata": { "filename": "ep.mp3", "ext": ".mp3", "path": "/p/ep.mp3",
                  "relPath": "ep.mp3", "size": 30000000, "mtimeMs": 0, "ctimeMs": 0,
                  "birthtimeMs": 0 } } ],
              "videoTrack": null,
              "libraryItem": { "id": "li1", "ino": "1", "libraryId": "lib", "folderId": "f",
                "path": "/p", "relPath": "p", "isFile": false, "mtimeMs": 0, "ctimeMs": 0,
                "birthtimeMs": 0, "addedAt": 0, "updatedAt": 0, "lastScan": 0,
                "scanVersion": null, "isMissing": false, "isInvalid": false,
                "mediaType": "podcast",
                "media": { "id": "m1", "libraryItemId": "li1",
                  "metadata": { "title": "The Show", "author": "Someone",
                    "itunesId": "123456", "explicit": false },
                  "coverPath": null, "tags": [], "size": 30000000,
                  "episodes": [ { "libraryItemId": "li1", "id": "ep1", "index": null,
                    "season": "", "episode": "", "episodeType": null, "title": "Episode 42",
                    "subtitle": null, "description": null, "enclosure": null,
                    "pubDate": null, "audioFile": null, "audioTrack": null,
                    "publishedAt": 1789812000000, "addedAt": 0, "updatedAt": 0 } ] },
                "libraryFiles": [ { "ino": "2", "fileType": "audio" } ], "size": 30000000 }
            }
            """.trimIndent(),
            PlayLibraryItemResponse::class.java
        )

        assertEquals("Episode 42", response.displayTitle)
        assertEquals("The Show", response.libraryItem.media.metadata.title)
        // Podcast metadata used to live on a class of its own and was dropped
        // from item responses entirely.
        assertEquals("123456", response.libraryItem.media.metadata.itunesId)
        assertEquals("https://example.invalid/feed.xml", response.mediaMetadata.feedUrl)
        // A number where the last feed sent a string.
        assertEquals("789", response.mediaMetadata.itunesArtistId)
        assertEquals("Episode 42", response.libraryItem.media.episodes!!.single().title)
        assertEquals("audio", response.libraryItem.libraryFiles.single().fileType)
        assertEquals(1, response.audioTracks.size)
    }
}
