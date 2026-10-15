package com.paulohenriquesg.fahrenheit.player

import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.media3.common.Player
import androidx.media3.test.utils.TestExoPlayerBuilder
import com.paulohenriquesg.fahrenheit.ui.theme.FahrenheitTheme
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Which frame the player draws (#223): the loading frame until the item is
 * ready, then the player in its place with Play focused; the failure message
 * as before.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w960dp-h540dp")
class PlayerStageTest {

    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val timeline = TrackTimeline(listOf(TimelineTrack(index = 1, startOffset = 0.0, duration = 1800.0, contentUrl = "/x")))
    private val book = NowPlaying("b1", "An Invented Book", timeline, null, null, null, false, null)
    private var player: Player? = null

    @After
    fun stop() {
        player?.release()
        // The state below is written outside any composition: drain what it
        // asked of the main looper, or later Compose tests never idle (#221).
        Snapshot.sendApplyNotifications()
        shadowOf(Looper.getMainLooper()).idle()
    }

    private fun readyState(): PlayerUiState {
        val exo = TestExoPlayerBuilder(compose.activity).setMediaSourceFactory(hourLongFiles()).build()
        player = exo
        return PlayerUiState(nowPlaying = book, player = exo, playback = BookPlayback(exo, timeline), ready = true)
    }

    private fun stage(initial: PlayerUiState, animated: Boolean = false): (PlayerUiState) -> Unit {
        var state by mutableStateOf(initial)
        compose.setContent {
            FahrenheitTheme {
                PlayerStage(state, itemId = "b1", preview = PlayerPreview("An Invented Book"), animated = animated) { playing, connected ->
                    PlayerScreen(nowPlaying = playing, currentTime = 0.0, transport = {
                        MediaPlayerController(player = connected, playback = state.playback!!, totalTime = 1800.0)
                    })
                }
            }
        }
        return { state = it }
    }

    @Test
    fun `until ready, the loading frame`() {
        stage(PlayerUiState())
        compose.waitForIdle()

        compose.onNodeWithTag(PlayerLoadingTags.FRAME).assertExists()
        compose.onNodeWithContentDescription("Play").assertDoesNotExist()
    }

    @Test
    fun `connected but not ready is still loading`() {
        stage(readyState().copy(ready = false))
        compose.waitForIdle()

        compose.onNodeWithTag(PlayerLoadingTags.FRAME).assertExists()
    }

    @Test
    fun `ready replaces the loading frame, with Play focused`() {
        val show = stage(PlayerUiState())
        compose.waitForIdle()
        val ready = readyState()

        compose.runOnIdle { show(ready) }
        compose.waitForIdle()

        compose.onNodeWithTag(PlayerLoadingTags.FRAME).assertDoesNotExist()
        compose.onNodeWithContentDescription("Play").assertIsFocused()
    }

    @Test
    fun `a failure shows the message, not the loading frame`() {
        stage(PlayerUiState(failed = true))
        compose.waitForIdle()

        compose.onNodeWithText("Could not load this item", substring = true).assertExists()
        compose.onNodeWithTag(PlayerLoadingTags.FRAME).assertDoesNotExist()
    }

    // The sweep and the spinner never end by themselves, so they live only in
    // the loading frame and go with it. They are an infinite transition, which
    // the Compose test runtime cancels ("Infinite animations are disabled on
    // tests"), so the animated frame cannot hang a test either - but nothing
    // here could tell a transition left running elsewhere: they stay inside
    // PlayerLoading, and this checks the frame and its spinner go.
    @Test
    fun `the moving frame goes once ready, and the screen goes idle`() {
        compose.mainClock.autoAdvance = false
        val show = stage(PlayerUiState(), animated = true)
        compose.mainClock.advanceTimeBy(500)
        compose.onNodeWithTag(PlayerLoadingTags.SPINNER).assertExists()
        val ready = readyState()

        compose.runOnUiThread { show(ready) }
        compose.mainClock.advanceTimeBy(500)
        compose.mainClock.autoAdvance = true
        compose.waitForIdle()

        compose.onNodeWithTag(PlayerLoadingTags.SPINNER).assertDoesNotExist()
        compose.onNodeWithContentDescription("Play").assertIsFocused()
    }
}
