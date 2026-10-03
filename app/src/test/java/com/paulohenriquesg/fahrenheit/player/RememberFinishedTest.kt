package com.paulohenriquesg.fahrenheit.player

import androidx.activity.ComponentActivity
import androidx.compose.runtime.MutableState
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.test.utils.TestExoPlayerBuilder
import androidx.media3.test.utils.robolectric.TestPlayerRunHelper.run
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * About's Mark finished / Mark unfinished follows the server: playing a
 * finished book moves its position, and the server un-finishes it (#107).
 */
@RunWith(RobolectricTestRunner::class)
class RememberFinishedTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private lateinit var player: ExoPlayer
    private lateinit var finished: MutableState<Boolean?>

    @After
    fun tearDown() = player.release()

    private fun show(initial: Boolean?) {
        player = TestExoPlayerBuilder(compose.activity).setMediaSourceFactory(hourLongFiles()).build()
        val book = NowPlaying("b1", "b1", TrackTimeline(listOf(TimelineTrack(1, 0.0, 3600.0, "/f"))), null, null, null, false, null)
        val queue = PlaybackQueue.of(book, 0.0) { "https://abs.test$it" }!!
        player.setMediaItems(queue.items, queue.index, queue.positionMs)
        player.prepare()
        compose.setContent { finished = rememberFinished(player, initial) }
        compose.waitForIdle()
    }

    @Test fun `playing a finished book makes it unfinished`() {
        show(initial = true)
        compose.runOnUiThread { player.play() }
        run(player).untilPositionAtLeast(1_000)
        compose.waitForIdle()
        assertEquals(false, finished.value)
    }

    @Test fun `an episode stays without Mark finished`() {
        show(initial = null)
        compose.runOnUiThread { player.play() }
        run(player).untilPositionAtLeast(1_000)
        compose.waitForIdle()
        assertEquals(null, finished.value)
    }

    @Test fun `marked finished while paused, it stays finished`() {
        show(initial = false)
        compose.runOnUiThread { finished.value = true }
        compose.waitForIdle()
        assertEquals(true, finished.value)
    }
}
