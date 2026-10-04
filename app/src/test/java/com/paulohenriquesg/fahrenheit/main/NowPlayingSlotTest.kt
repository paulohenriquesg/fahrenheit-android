package com.paulohenriquesg.fahrenheit.main

import androidx.activity.ComponentActivity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performSemanticsAction
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.test.utils.TestExoPlayerBuilder
import androidx.media3.test.utils.robolectric.TestPlayerRunHelper.run
import com.paulohenriquesg.fahrenheit.player.NowPlaying
import com.paulohenriquesg.fahrenheit.player.PlaybackQueue
import com.paulohenriquesg.fahrenheit.player.TimelineTrack
import com.paulohenriquesg.fahrenheit.player.TrackTimeline
import com.paulohenriquesg.fahrenheit.player.hourLongFiles
import com.paulohenriquesg.fahrenheit.ui.theme.FahrenheitTheme
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Stop on the rail's entry ends what the main screen's player has queued (#155). */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w960dp-h540dp")
class NowPlayingSlotTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private lateinit var player: ExoPlayer
    private var stops = 0

    @After
    fun tearDown() = player.release()

    @Test
    fun `Stop empties the queue, and the entry goes`() {
        player = TestExoPlayerBuilder(compose.activity).setMediaSourceFactory(hourLongFiles()).build()
        val book = NowPlaying("b1", "A Book", TrackTimeline(listOf(TimelineTrack(1, 0.0, 3600.0, "/b1"))), null, null, null, false, null)
        val queue = PlaybackQueue.of(book, 600.0) { "https://abs.test$it" }!!
        player.setMediaItems(queue.items, queue.index, queue.positionMs)
        player.prepare()
        player.play()
        compose.setContent {
            FahrenheitTheme {
                NowPlayingSlot(player, open = true, chaptersOf = { null }, onOpen = {}, onStopped = { stops++ })
            }
        }
        run(player).untilPendingCommandsAreFullyHandled()
        compose.waitForIdle()

        compose.onNodeWithTag(NOW_PLAYING_STOP_TAG).performSemanticsAction(SemanticsActions.OnClick)
        run(player).untilPendingCommandsAreFullyHandled()
        compose.waitForIdle()

        assertEquals(0, player.mediaItemCount)
        assertEquals(1, stops)
        compose.onNodeWithTag(NOW_PLAYING_TAG).assertDoesNotExist()
    }
}
