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

        assertEquals(31124.073333, response.audioTracks[0].startOffset.toDouble(), 1e-6)
    }

    @Test
    fun `a play session survives a file larger than 2 GB`() {
        // Single-file m4b audiobooks can exceed Int.MAX_VALUE bytes.
        val bigFile = secondTrackOfMultiFileBook.replace("\"size\": 357792270", "\"size\": 3000000000")

        val response = gson.fromJson(
            """{ "audioTracks": [ $bigFile ] }""",
            PlayLibraryItemResponse::class.java
        )

        assertEquals(3_000_000_000L, response.audioTracks[0].metadata.size.toLong())
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

        assertEquals(19141.935520695628, response.currentTime.toDouble(), 1e-9)
        assertEquals(538.6500963920058, response.startTime.toDouble(), 1e-9)
        assertEquals(29.154561968609244, response.timeListening.toDouble(), 1e-9)
    }
}
