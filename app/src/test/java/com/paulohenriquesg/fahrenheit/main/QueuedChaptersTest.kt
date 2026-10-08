package com.paulohenriquesg.fahrenheit.main

import com.google.gson.Gson
import com.paulohenriquesg.fahrenheit.api.LibraryItemResponse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Which chapters Now playing counts down in: the book's, or the episode's (#183). */
class QueuedChaptersTest {

    private fun item(json: String): LibraryItemResponse = Gson().fromJson(json, LibraryItemResponse::class.java)

    private val book = item(
        """{"id":"b1","media":{"metadata":{"title":"A Book","explicit":false},
            "chapters":[{"start":0.0,"end":600.0,"title":"Opening"}]}}"""
    )

    private val podcast = item(
        """{"id":"p1","media":{"metadata":{"title":"A Show","explicit":false},"episodes":[
            {"libraryItemId":"p1","id":"e1","index":1,"title":"One","publishedAt":0,"addedAt":0,"updatedAt":0,
             "chapters":[{"start":0.0,"end":900.0,"title":"Intro"},{"start":900.0,"end":1800.0,"title":"Interview"}]},
            {"libraryItemId":"p1","id":"e2","index":2,"title":"Two","publishedAt":0,"addedAt":0,"updatedAt":0,"chapters":[]}]}}"""
    )

    @Test fun `a book's are the book's`() =
        assertEquals(listOf("Opening"), chaptersIn(book, episodeId = null)?.map { it.title })

    @Test fun `an episode's are its own`() =
        assertEquals(listOf("Intro", "Interview"), chaptersIn(podcast, episodeId = "e1")?.map { it.title })

    @Test fun `an episode without chapters has none`() =
        assertEquals(emptyList<String>(), chaptersIn(podcast, episodeId = "e2")?.map { it.title }.orEmpty())

    @Test fun `an episode the podcast no longer has has none`() =
        assertNull(chaptersIn(podcast, episodeId = "gone"))
}
