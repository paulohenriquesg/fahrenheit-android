package com.paulohenriquesg.fahrenheit.podcast

import com.paulohenriquesg.fahrenheit.api.RecentPodcastEpisode
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * A flat list of episodes with no dates read as one undifferentiated pile, which
 * is why new ones were invisible among old ones. Days do the sorting the eye
 * wants.
 */
class EpisodeGroupsTest {

    private val now = 1_790_000_000_000L
    private val day = 24 * 60 * 60 * 1000L

    private fun episode(id: String, publishedAt: Long?) = RecentPodcastEpisode(
        id = id,
        libraryItemId = "li_$id",
        title = "Episode $id",
        description = null,
        podcast = null,
        publishedAt = publishedAt
    )

    @Test
    fun `today, yesterday and the rest are their own groups`() {
        val groups = EpisodeGroups.of(
            listOf(
                episode("a", now - 3_600_000L),
                episode("b", now - day - 3_600_000L),
                episode("c", now - 9 * day)
            ),
            now
        )

        assertEquals(listOf("Today", "Yesterday", "Earlier"), groups.map { it.label })
        assertEquals(listOf("a"), groups[0].episodes.map { it.id })
        assertEquals(listOf("b"), groups[1].episodes.map { it.id })
        assertEquals(listOf("c"), groups[2].episodes.map { it.id })
    }

    @Test
    fun `this week is its own group, between yesterday and earlier`() {
        val groups = EpisodeGroups.of(
            listOf(episode("a", now - 3 * day), episode("b", now - 30 * day)),
            now
        )

        assertEquals(listOf("This week", "Earlier"), groups.map { it.label })
    }

    @Test
    fun `an empty group is not shown`() {
        val groups = EpisodeGroups.of(listOf(episode("a", now - 40 * day)), now)

        assertEquals(listOf("Earlier"), groups.map { it.label })
    }

    @Test
    fun `the server's order is kept inside a group`() {
        val groups = EpisodeGroups.of(
            listOf(episode("newer", now - 2 * day), episode("older", now - 5 * day)),
            now
        )

        assertEquals(listOf("newer", "older"), groups.single().episodes.map { it.id })
    }

    @Test
    fun `episodes the server dated not at all go last, under their own heading`() {
        val groups = EpisodeGroups.of(
            listOf(episode("dated", now - 3_600_000L), episode("undated", null)),
            now
        )

        assertEquals(listOf("Today", "Undated"), groups.map { it.label })
    }

    @Test
    fun `nothing in, nothing out`() {
        assertEquals(emptyList<EpisodeGroup>(), EpisodeGroups.of(emptyList(), now))
    }
}
