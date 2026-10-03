package com.paulohenriquesg.fahrenheit.podcast

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.assertRangeInfoEquals
import androidx.compose.ui.semantics.ProgressBarRangeInfo
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

        // Ten minutes or more: no seconds.
        compose.onNodeWithText("14 min left", substring = true).assertIsDisplayed()
    }

    @Test
    fun `a half-heard episode says how far in, with the bar on its artwork and its description kept`() {
        compose.setContent {
            EpisodeCard(episode = episode, onClick = {}, progress = EpisodeProgress.InProgress(0.4, 840.0))
        }

        compose.onNodeWithText("40% in").assertIsDisplayed()
        compose.onNodeWithTag(EpisodeCardTags.PROGRESS, useUnmergedTree = true)
            .assertIsDisplayed()
            .assertRangeInfoEquals(ProgressBarRangeInfo(0.4f, 0f..1f))
        compose.onNodeWithText("About it").assertIsDisplayed()
    }

    @Test
    fun `an episode shows how long it is, rounded to the minute when long`() {
        compose.setContent { EpisodeCard(episode = episode.copy(duration = 1729.0), onClick = {}) }

        compose.onNodeWithText("29 min", substring = true).assertIsDisplayed()
        compose.onNodeWithText(" s", substring = true).assertDoesNotExist()
    }

    @Test
    fun `an episode out in the last day or two is marked new`() {
        val fresh = episode.copy(publishedAt = System.currentTimeMillis() - 60 * 60 * 1000L)
        compose.setContent { EpisodeCard(episode = fresh, onClick = {}) }

        compose.onNodeWithText("New").assertIsDisplayed()
    }

    @Test
    fun `an older episode is not`() {
        val old = episode.copy(publishedAt = System.currentTimeMillis() - 5 * 24 * 60 * 60 * 1000L)
        compose.setContent { EpisodeCard(episode = old, onClick = {}) }

        compose.onNodeWithText("New").assertDoesNotExist()
    }

    @Test
    fun `an unplayed episode carries neither`() {
        compose.setContent { EpisodeCard(episode = episode, onClick = {}) }

        compose.onNodeWithText("Heard").assertDoesNotExist()
        compose.onNodeWithText("left", substring = true).assertDoesNotExist()
        compose.onNodeWithText("% in", substring = true).assertDoesNotExist()
        compose.onNodeWithTag(EpisodeCardTags.PROGRESS, useUnmergedTree = true).assertDoesNotExist()
    }
}
