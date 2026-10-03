package com.paulohenriquesg.fahrenheit.player

import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.test.utils.TestExoPlayerBuilder
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The remote's rewind and fast-forward keys reach the session as seekBack and
 * seekForward; they jump by the lengths set in Settings (#107), read at the
 * moment of the press.
 */
@RunWith(AndroidJUnit4::class)
class SkipLengthsTest {
    private val exo: ExoPlayer = TestExoPlayerBuilder(ApplicationProvider.getApplicationContext())
        .setMediaSourceFactory(hourLongFiles())
        .build()
    private var back = 10
    private var forward = 60
    private val player = SkipLengths(exo, backSeconds = { back }, forwardSeconds = { forward })

    @After
    fun tearDown() = exo.release()

    private fun at(seconds: Long) {
        val book = NowPlaying("b1", "b1", TrackTimeline(listOf(TimelineTrack(1, 0.0, 3600.0, "/b1"))), null, null, null, false, null)
        val queue = PlaybackQueue.of(book, seconds.toDouble()) { "https://abs.test$it" }!!
        exo.setMediaItems(queue.items, queue.index, queue.positionMs)
    }

    // Review Focus 1.
    @Test
    fun `back and forward jump by the lengths set`() {
        at(600)
        player.seekBack()
        assertEquals(590_000L, exo.currentPosition)
        player.seekForward()
        assertEquals(650_000L, exo.currentPosition)
    }

    @Test
    fun `a change in Settings applies to the next press`() {
        at(600)
        back = 15
        player.seekBack()
        assertEquals(585_000L, exo.currentPosition)
    }

    @Test
    fun `the session is told the lengths`() {
        assertEquals(10_000L, player.seekBackIncrement)
        assertEquals(60_000L, player.seekForwardIncrement)
    }

    @Test
    fun `back near the start stops at the start`() {
        at(4)
        player.seekBack()
        assertEquals(0L, exo.currentPosition)
    }
}
