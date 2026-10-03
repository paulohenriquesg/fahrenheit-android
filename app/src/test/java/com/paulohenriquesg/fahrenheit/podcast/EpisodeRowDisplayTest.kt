package com.paulohenriquesg.fahrenheit.podcast

import com.paulohenriquesg.fahrenheit.api.RecentEpisodePodcast
import com.paulohenriquesg.fahrenheit.api.RecentEpisodePodcastMetadata
import com.paulohenriquesg.fahrenheit.api.RecentPodcastEpisode
import java.util.Locale
import java.util.TimeZone
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
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

    private val hour = 60 * 60 * 1000L

    // "New" is the last day or two (#109): the Today and Yesterday groups.
    @Test
    fun `an episode out this morning is new`() =
        assertTrue(EpisodeRowDisplay.isNew(episode(publishedAt = now - 3 * hour), progress = null, now = now))

    @Test
    fun `an episode from yesterday evening is still new`() =
        assertTrue(EpisodeRowDisplay.isNew(episode(publishedAt = now - 47 * hour), progress = null, now = now))

    @Test
    fun `an episode from three days ago is not`() =
        assertFalse(EpisodeRowDisplay.isNew(episode(publishedAt = now - 72 * hour), progress = null, now = now))

    @Test
    fun `an episode published ahead of time counts as new, as it groups under Today`() =
        assertTrue(EpisodeRowDisplay.isNew(episode(publishedAt = now + hour), progress = null, now = now))

    @Test
    fun `an undated episode is not new, as nothing says it is`() =
        assertFalse(EpisodeRowDisplay.isNew(episode(publishedAt = null), progress = null, now = now))

    @Test
    fun `a new episode already started is no longer news`() =
        assertFalse(
            EpisodeRowDisplay.isNew(episode(publishedAt = now - hour), EpisodeProgress.InProgress(0.2, 600.0), now)
        )

    @Test
    fun `a new episode already heard is no longer news`() =
        assertFalse(EpisodeRowDisplay.isNew(episode(publishedAt = now - hour), EpisodeProgress.Heard, now))

    @Test
    fun `how far in reads as a whole percentage`() =
        assertEquals(38, EpisodeRowDisplay.percentIn(EpisodeProgress.InProgress(0.384, 600.0)))
}
