package com.paulohenriquesg.fahrenheit.player

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.performSemanticsAction
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.test.utils.TestExoPlayerBuilder
import androidx.media3.test.utils.robolectric.TestPlayerRunHelper.run
import com.paulohenriquesg.fahrenheit.ui.theme.FahrenheitTheme
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * The transport works the player it is given, in whole-book time (#16).
 *
 * Buttons are pressed through their click action: they are TV buttons, which
 * act on keys and semantics, not on injected touch.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w960dp-h540dp")
class TransportTest {

    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private lateinit var player: ExoPlayer

    private val twoParts = TrackTimeline(
        listOf(
            TimelineTrack(index = 1, startOffset = 0.0, duration = 3600.0, contentUrl = "/part1"),
            TimelineTrack(index = 2, startOffset = 3600.0, duration = 1800.0, contentUrl = "/part2")
        )
    )

    @After
    fun tearDown() = player.release()

    private fun show(player: ExoPlayer, timeline: TrackTimeline = twoParts) {
        this.player = player
        compose.setContent {
            FahrenheitTheme {
                MediaPlayerController(player = player, playback = BookPlayback(player, timeline), totalTime = timeline.totalDuration)
            }
        }
        compose.waitForIdle()
    }

    /** Queued but not prepared: an unprepared player keeps the position it is given, exactly. */
    private fun queuedAt(startAt: Double): ExoPlayer {
        val p = TestExoPlayerBuilder(compose.activity).setMediaSourceFactory(hourLongFiles()).build()
        val nowPlaying = NowPlaying("b1", "t", twoParts, null, null, null, false, null) { "" }
        val queue = PlaybackQueue.of(nowPlaying, startAt) { "https://abs.test$it" }!!
        p.setMediaItems(queue.items, queue.index, queue.positionMs)
        return p
    }

    @Test
    fun `play plays and pause pauses`() {
        show(queuedAt(0.0))

        compose.onNodeWithContentDescription("Play").performSemanticsAction(SemanticsActions.OnClick)
        compose.waitForIdle()
        assertTrue(player.playWhenReady)

        compose.onNodeWithContentDescription("Pause").performSemanticsAction(SemanticsActions.OnClick)
        compose.waitForIdle()
        assertFalse(player.playWhenReady)
    }

    @Test
    fun `skipping forward near the end of a file crosses into the next`() {
        show(queuedAt(3590.0))

        compose.onNodeWithContentDescription(compose.activity.getString(com.paulohenriquesg.fahrenheit.R.string.skip_forward_30_seconds)).performSemanticsAction(SemanticsActions.OnClick)
        compose.waitForIdle()

        assertEquals(1, player.currentMediaItemIndex)
        assertEquals(20_000L, player.currentPosition)
    }

    @Test
    fun `the times are whole-book times`() {
        show(queuedAt(3900.0))

        compose.onNodeWithText("Current Time: 01:05:00").assertIsDisplayed()
        compose.onNodeWithText("Total Time: 01:30:00").assertIsDisplayed()
    }

    @Test
    fun `stop pauses and keeps the place`() {
        show(queuedAt(3900.0))
        compose.onNodeWithContentDescription("Play").performSemanticsAction(SemanticsActions.OnClick)
        compose.waitForIdle()

        compose.onNodeWithContentDescription(compose.activity.getString(com.paulohenriquesg.fahrenheit.R.string.stop)).performSemanticsAction(SemanticsActions.OnClick)
        compose.waitForIdle()

        assertFalse(player.playWhenReady)
        assertEquals(1, player.currentMediaItemIndex)
    }

    // Review Focus 5.
    @Test
    fun `after an error, play retries where it was`() {
        // The real media source: an unresolvable host fails to load.
        val failing = TestExoPlayerBuilder(compose.activity).build()
        failing.setMediaItems(
            listOf(MediaItem.fromUri("http://abs.invalid/1.mp3"), MediaItem.fromUri("http://abs.invalid/2.mp3")),
            1, 300_000L
        )
        failing.prepare()
        run(failing).untilPlayerError()
        show(failing)

        compose.onNodeWithText("Couldn't play this").assertIsDisplayed()
        compose.onNodeWithContentDescription("Play").performSemanticsAction(SemanticsActions.OnClick)
        compose.waitForIdle()

        assertNull(player.playerError)
        assertEquals(1, player.currentMediaItemIndex)
        assertEquals(300_000L, player.currentPosition)
        assertTrue(player.playbackState != Player.STATE_IDLE)
    }
}
