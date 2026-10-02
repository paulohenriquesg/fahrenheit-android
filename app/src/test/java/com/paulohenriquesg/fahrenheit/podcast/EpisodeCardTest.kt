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

    @Test
    fun `a heard episode is marked heard`() {
        compose.setContent { EpisodeCard(episode = episode, onClick = {}, progress = EpisodeProgress.Heard) }

        compose.onNodeWithText("Heard").assertIsDisplayed()
    }

    @Test
    fun `a half-heard episode says how long is left`() {
        compose.setContent {
            EpisodeCard(episode = episode, onClick = {}, progress = EpisodeProgress.InProgress(0.4, 840.0))
        }

        compose.onNodeWithText("14 min left", substring = true).assertIsDisplayed()
    }

    @Test
    fun `an unplayed episode carries neither`() {
        compose.setContent { EpisodeCard(episode = episode, onClick = {}) }

        compose.onNodeWithText("Heard").assertDoesNotExist()
        compose.onNodeWithText("left", substring = true).assertDoesNotExist()
    }
}
