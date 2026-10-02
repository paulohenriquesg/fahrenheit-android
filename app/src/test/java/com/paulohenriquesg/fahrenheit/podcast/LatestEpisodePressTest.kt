package com.paulohenriquesg.fahrenheit.podcast

import androidx.test.core.app.ApplicationProvider
import com.paulohenriquesg.fahrenheit.api.RecentEpisodePodcast
import com.paulohenriquesg.fahrenheit.api.RecentEpisodePodcastMetadata
import com.paulohenriquesg.fahrenheit.api.RecentPodcastEpisode
import com.paulohenriquesg.fahrenheit.player.PlayerActivity
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Choosing an episode in Latest Episodes plays it. It used to open the
 * podcast's screen, one press and a search away from the episode the viewer
 * had just picked - while the same episode on a Home shelf went straight to
 * the player.
 */
@RunWith(RobolectricTestRunner::class)
class LatestEpisodePressTest {

    private val episode = RecentPodcastEpisode(
        id = "ep-7",
        libraryItemId = "podcast-3",
        title = "Episode 7",
        description = null,
        podcast = RecentEpisodePodcast(metadata = RecentEpisodePodcastMetadata(title = "The Show"))
    )

    @Test
    fun `an episode opens the player, on that episode, playing`() {
        val intent = latestEpisodeIntent(ApplicationProvider.getApplicationContext(), episode)

        assertEquals(PlayerActivity::class.java.name, intent.component?.className)
        assertEquals("podcast-3", intent.getStringExtra("item_id"))
        assertEquals("ep-7", intent.getStringExtra("episode_id"))
        assertEquals(true, intent.getBooleanExtra("auto_play", false))
    }
}
