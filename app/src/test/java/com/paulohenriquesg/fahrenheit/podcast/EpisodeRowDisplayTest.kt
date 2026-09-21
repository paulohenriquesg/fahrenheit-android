package com.paulohenriquesg.fahrenheit.podcast

import com.paulohenriquesg.fahrenheit.api.RecentEpisodePodcast
import com.paulohenriquesg.fahrenheit.api.RecentEpisodePodcastMetadata
import com.paulohenriquesg.fahrenheit.api.RecentPodcastEpisode
import org.junit.Assert.assertEquals
import org.junit.Test

class EpisodeRowDisplayTest {

    private fun episode(podcastTitle: String? = "The Show", episodeTitle: String? = "Episode 42") =
        RecentPodcastEpisode(
            id = "e1",
            libraryItemId = "li1",
            title = episodeTitle,
            description = null,
            podcast = podcastTitle?.let {
                RecentEpisodePodcast(metadata = RecentEpisodePodcastMetadata(title = it))
            }
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
}
