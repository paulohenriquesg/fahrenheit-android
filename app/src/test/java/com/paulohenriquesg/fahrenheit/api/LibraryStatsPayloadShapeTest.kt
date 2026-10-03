package com.paulohenriquesg.fahrenheit.api

import com.google.gson.Gson
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * GET /api/libraries/:id/stats. A book library and a podcast library answer
 * with the same counting fields; totalDuration is a sum of fractional seconds.
 * Shape from the Audiobookshelf source (LibraryController.stats), values made up.
 */
class LibraryStatsPayloadShapeTest {

    @Test
    fun `a book library's stats parse`() {
        val json = """
            { "largestItems": [], "totalAuthors": 21, "authorsWithCount": [],
              "totalGenres": 4, "genresWithCount": [], "totalItems": 38,
              "longestItems": [], "totalSize": 9876543210,
              "totalDuration": 651606.5543046716, "numAudioTracks": 412 }
        """.trimIndent()

        val stats = Gson().fromJson(json, LibraryStats::class.java)

        assertEquals(38, stats.totalItems)
        assertEquals(651606.5543046716, stats.totalDuration!!, 1e-9)
        assertEquals(412, stats.numAudioTracks)
    }

    @Test
    fun `an empty library answers zeros, which parse`() {
        val json = """{ "largestItems": [], "totalGenres": 0, "genresWithCount": [],
            "totalItems": 0, "longestItems": [], "totalSize": 0, "totalDuration": 0, "numAudioTracks": 0 }"""

        val stats = Gson().fromJson(json, LibraryStats::class.java)

        assertEquals(0, stats.totalItems)
        assertEquals(0.0, stats.totalDuration!!, 0.0)
    }
}
