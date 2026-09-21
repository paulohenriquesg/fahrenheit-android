package com.paulohenriquesg.fahrenheit.podcast

import com.google.gson.Gson
import com.paulohenriquesg.fahrenheit.api.Episode
import org.junit.Assert.assertEquals
import org.junit.Test

class EpisodeOrderTest {

    private fun episode(id: String, publishedAt: Long?) = Gson().fromJson(
        """{"libraryItemId":"li","id":"$id","title":"$id","publishedAt":${publishedAt ?: "null"},
            "addedAt":0,"updatedAt":0}""",
        Episode::class.java
    )

    @Test
    fun `the newest episode comes first`() {
        val episodes = listOf(episode("old", 1_000L), episode("new", 3_000L), episode("middle", 2_000L))

        assertEquals(listOf("new", "middle", "old"), EpisodeOrder.newestFirst(episodes).map { it.id })
    }

    // A feed that omits the date leaves it at 0, which would otherwise claim
    // the oldest slot and jump around as other episodes arrive.
    @Test
    fun `episodes without a date keep the order the server sent, after the dated ones`() {
        val episodes = listOf(
            episode("undated-first", null),
            episode("dated", 2_000L),
            episode("undated-second", null)
        )

        assertEquals(
            listOf("dated", "undated-first", "undated-second"),
            EpisodeOrder.newestFirst(episodes).map { it.id }
        )
    }

    @Test
    fun `an empty list stays empty`() =
        assertEquals(emptyList<String>(), EpisodeOrder.newestFirst(emptyList()).map { it.id })
}
