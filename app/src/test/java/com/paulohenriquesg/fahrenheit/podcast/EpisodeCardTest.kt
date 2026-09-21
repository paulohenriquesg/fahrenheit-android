package com.paulohenriquesg.fahrenheit.podcast

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import com.paulohenriquesg.fahrenheit.api.RecentEpisodePodcast
import com.paulohenriquesg.fahrenheit.api.RecentEpisodePodcastMetadata
import com.paulohenriquesg.fahrenheit.api.RecentPodcastEpisode
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class EpisodeCardTest {

    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val episode = RecentPodcastEpisode(
        id = "e1",
        libraryItemId = "li1",
        title = "Episode 42",
        description = "<p>About it</p>",
        podcast = RecentEpisodePodcast(metadata = RecentEpisodePodcastMetadata(title = "The Show"))
    )

    @Test
    fun `an episode row carries its podcast's cover`() {
        compose.setContent { EpisodeCard(episode = episode, onClick = {}) }

        compose.onNodeWithContentDescription("The Show").assertIsDisplayed()
        compose.onNodeWithText("Episode 42").assertIsDisplayed()
    }
}
