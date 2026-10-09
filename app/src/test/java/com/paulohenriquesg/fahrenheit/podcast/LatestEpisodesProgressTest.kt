package com.paulohenriquesg.fahrenheit.podcast

import androidx.activity.ComponentActivity
import androidx.compose.runtime.getValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.paulohenriquesg.fahrenheit.api.RecentPodcastEpisode
import com.paulohenriquesg.fahrenheit.main.HomeViewModel
import com.paulohenriquesg.fahrenheit.progress.ProgressStore
import com.paulohenriquesg.fahrenheit.ui.theme.FahrenheitTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Latest Episodes says how far in each episode is, from the shared store (#207). */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w960dp-h540dp")
class LatestEpisodesProgressTest {

    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val episode = RecentPodcastEpisode(
        id = "e1", libraryItemId = "pod", title = "Episode One", description = null, podcast = null, duration = 1800.0
    )

    @Test
    fun `an episode the player reports on shows how far in it is, with no read of its own`() {
        val store = ProgressStore()
        val model = HomeViewModel(store)
        compose.setContent {
            val state by model.uiState.collectAsStateWithLifecycle()
            FahrenheitTheme { LatestEpisodesView("lib", heard = state.episodes, load = { Result.success(listOf(episode)) }) }
        }
        compose.waitForIdle()
        compose.onNodeWithText("50% in").assertDoesNotExist()

        compose.runOnIdle { store.played("pod", "e1", position = 900.0, duration = 1800.0) }
        compose.waitForIdle()

        compose.onNodeWithText("50% in").assertIsDisplayed()
    }
}
