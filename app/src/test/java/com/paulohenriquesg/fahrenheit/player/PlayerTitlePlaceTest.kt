package com.paulohenriquesg.fahrenheit.player

import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.media3.common.Player
import androidx.media3.test.utils.TestExoPlayerBuilder
import com.paulohenriquesg.fahrenheit.api.Chapter
import com.paulohenriquesg.fahrenheit.ui.theme.FahrenheitTheme
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Nothing moves on ready (#223, frames 1-2 of docs/mocks/player-loading.html):
 * the title stands where the loading frame drew it, however much more the
 * ready player has around and under it - a series above the title, a second
 * bar for a chaptered book, an "Up next" line for an episode.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w960dp-h540dp")
class PlayerTitlePlaceTest {

    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val timeline = TrackTimeline(listOf(TimelineTrack(index = 1, startOffset = 0.0, duration = 1800.0, contentUrl = "/x")))
    private var player: Player? = null

    @After
    fun stop() {
        player?.release()
        // The state below is written outside any composition (#221).
        Snapshot.sendApplyNotifications()
        shadowOf(Looper.getMainLooper()).idle()
    }

    /** The title's top while loading, then once [ready] replaces the loading frame. */
    private fun titleTops(preview: PlayerPreview, ready: NowPlaying): Pair<Dp, Dp> {
        val exo = TestExoPlayerBuilder(compose.activity).setMediaSourceFactory(hourLongFiles()).build()
        player = exo
        val playback = BookPlayback(exo, timeline)
        var state by mutableStateOf(PlayerUiState())
        compose.setContent {
            FahrenheitTheme {
                PlayerStage(state, itemId = ready.itemId, preview = preview, animated = false) { playing, connected ->
                    PlayerScreen(nowPlaying = playing, currentTime = 0.0, transport = {
                        MediaPlayerController(
                            player = connected,
                            playback = playback,
                            totalTime = 1800.0,
                            chapters = playing.chapters,
                            episodes = playing.episodeId?.let { EpisodeSkip(onPrevious = null, onNext = {}) }
                        )
                    })
                }
            }
        }
        compose.waitForIdle()
        val loading = compose.onNodeWithText(ready.title).getBoundsInRoot().top

        compose.runOnIdle { state = PlayerUiState(nowPlaying = ready, player = exo, playback = playback, ready = true) }
        compose.waitForIdle()
        compose.onNodeWithTag(PlayerLoadingTags.FRAME).assertDoesNotExist()
        compose.onNodeWithContentDescription("Play").assertExists()
        return loading to compose.onNodeWithText(ready.title, useUnmergedTree = true).getBoundsInRoot().top
    }

    private fun assertSamePlace(tops: Pair<Dp, Dp>) =
        assertEquals("the title moved on ready", tops.first.value, tops.second.value, 0.5f)

    @Test
    fun `a chaptered book in a series keeps its title in place`() {
        val book = NowPlaying(
            itemId = "b1", title = "An Invented Book", timeline = timeline, mediaDuration = 1800.0,
            chapters = listOf(Chapter(0, 0.0, 900.0, "One"), Chapter(1, 900.0, 1800.0, "Two")),
            episodeId = null, goToPodcast = false, description = null,
            kicker = "An Invented Series · Book 2", byline = "An Invented Writer · read by An Invented Reader"
        )

        assertSamePlace(titleTops(PlayerPreview("An Invented Book", byline = "An Invented Writer"), book))
    }

    @Test
    fun `an episode with a next one keeps its title in place`() {
        val episode = NowPlaying(
            itemId = "p1", title = "An Invented Episode", timeline = timeline, mediaDuration = 1800.0,
            chapters = null, episodeId = "e1", goToPodcast = true, description = null,
            kicker = "An Invented Show · 03/10/2026", details = "Episode 4 · 30 min", notes = "Invented notes.",
            next = EpisodeRef("e2", "The Next Invented Episode", 1200.0)
        )

        assertSamePlace(titleTops(PlayerPreview("An Invented Episode", kicker = "An Invented Show"), episode))
    }
}
