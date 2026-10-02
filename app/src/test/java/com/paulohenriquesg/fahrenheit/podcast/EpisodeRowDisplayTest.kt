package com.paulohenriquesg.fahrenheit.podcast

import com.paulohenriquesg.fahrenheit.api.RecentEpisodePodcast
import com.paulohenriquesg.fahrenheit.api.RecentEpisodePodcastMetadata
import com.paulohenriquesg.fahrenheit.api.RecentPodcastEpisode
import java.util.Locale
import java.util.TimeZone
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class EpisodeRowDisplayTest {

    private val originalZone = TimeZone.getDefault()
    private val originalLocale = Locale.getDefault()
    private val now = 1_790_000_000_000L

    @Before
    fun pinTheClock() {
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"))
        Locale.setDefault(Locale.UK)
    }

    @After
    fun restore() {
        TimeZone.setDefault(originalZone)
        Locale.setDefault(originalLocale)
    }

    private fun episode(
        podcastTitle: String? = "The Show",
        episodeTitle: String? = "Episode 42",
        publishedAt: Long? = null
    ) = RecentPodcastEpisode(
        id = "e1",
        libraryItemId = "li1",
        title = episodeTitle,
        description = null,
        podcast = podcastTitle?.let {
            RecentEpisodePodcast(metadata = RecentEpisodePodcastMetadata(title = it))
        },
        publishedAt = publishedAt
    )

    // Read aloud on a row that already shows the episode title, so it names the
    // podcast the cover belongs to rather than repeating the episode.
    @Test
    fun `the cover is described by its podcast`() =
        assertEquals("The Show", EpisodeRowDisplay.coverDescription(episode()))

    @Test
    fun `without a podcast name the episode names it`() =
        assertEquals("Episode 42", EpisodeRowDisplay.coverDescription(episode(podcastTitle = null)))

    @Test
    fun `with neither, it is still described as a cover`() =
        assertEquals(
            "Podcast cover",
            EpisodeRowDisplay.coverDescription(episode(podcastTitle = null, episodeTitle = null))
        )

    // Without this the list could not say whether an episode was from today or
    // from March, which is what made it look like nothing new arrived.
    @Test
    fun `a row says when the episode came out`() =
        assertEquals(
            "13/09/2026",
            EpisodeRowDisplay.published(episode(publishedAt = 1_789_310_000_000L), now)
        )

    @Test
    fun `a fresh episode says so in words`() =
        assertEquals(
            "Today",
            EpisodeRowDisplay.published(episode(publishedAt = now - 3_600_000L), now)
        )

    @Test
    fun `a row with no date says nothing rather than guessing`() =
        assertEquals("", EpisodeRowDisplay.published(episode(), now))
}
