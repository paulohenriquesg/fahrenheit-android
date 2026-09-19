package com.paulohenriquesg.fahrenheit.api

import com.google.gson.Gson
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * /api/me/listening-stats reports every duration as fractional seconds, and
 * `days` maps a date straight to seconds. Shape taken from a live server,
 * values made up.
 */
class ListeningStatsPayloadShapeTest {

    private val json = """
        { "totalTime": 651606.5543046716,
          "items": { "li_1": { "id": "li_1", "timeListening": 4172.25,
                               "mediaMetadata": { "title": "A book" } } },
          "days": { "2026-08-24": 49.73924505710602, "2026-07-13": 30 },
          "dayOfWeek": { "Monday": 30765.968401312828, "Tuesday": 0, "Wednesday": 1.5,
                         "Thursday": 2, "Friday": 3, "Saturday": 4, "Sunday": 5 },
          "today": 0,
          "recentSessions": [ {
              "id": "s_1", "userId": "u_1", "libraryItemId": "li_1", "episodeId": null,
              "mediaType": "book", "displayTitle": "A book", "displayAuthor": "",
              "coverPath": null, "timeListening": 29.154561968609244,
              "startTime": 538.6500963920058, "currentTime": 19141.935520695628,
              "startedAt": 1789812000000, "updatedAt": 1789812030000 } ] }
    """.trimIndent()

    @Test
    fun `listening stats with fractional seconds parse`() {
        val stats = Gson().fromJson(json, ListeningStatsResponse::class.java)

        assertEquals(651606.5543046716, stats.totalTime, 1e-9)
        assertEquals(1, stats.items.size)
        assertEquals(2, stats.days.size)
        assertEquals(7, stats.dayOfWeek.size)
        assertEquals(1, stats.recentSessions!!.size)
    }
}
