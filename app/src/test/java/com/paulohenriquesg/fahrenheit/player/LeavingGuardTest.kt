package com.paulohenriquesg.fahrenheit.player

import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.test.utils.TestExoPlayerBuilder
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The last progress report must be taken from the queue as it was, so every
 * way of ending or replacing the queue has to be announced before it happens.
 */
@RunWith(AndroidJUnit4::class)
class LeavingGuardTest {

    private lateinit var player: ExoPlayer

    @Before
    fun setUp() {
        player = TestExoPlayerBuilder(ApplicationProvider.getApplicationContext()).build()
    }

    @After
    fun tearDown() = player.release()

    @Test
    fun `every way of changing or ending the queue is announced first`() {
        val seen = mutableListOf<Int>()
        val guard = LeavingGuard(player) { seen += player.mediaItemCount }
        val a = MediaItem.fromUri("https://abs.test/a")
        val b = MediaItem.fromUri("https://abs.test/b")

        guard.setMediaItems(listOf(a, b))       // was 0
        guard.setMediaItems(listOf(a), true)    // was 2
        guard.setMediaItems(listOf(a, b), 1, 0) // was 1
        guard.setMediaItem(a)                   // was 2
        guard.setMediaItem(a, 0L)               // was 1
        guard.setMediaItem(a, true)             // was 1
        guard.stop()                            // was 1
        guard.clearMediaItems()                 // was 1

        assertEquals(listOf(0, 2, 1, 2, 1, 1, 1, 1), seen)
        assertEquals(0, player.mediaItemCount)
    }

    @Test
    fun `playing and pausing are not leaving`() {
        var announced = 0
        val guard = LeavingGuard(player) { announced++ }

        guard.play()
        guard.pause()
        guard.seekTo(1_000)

        assertEquals(0, announced)
    }
}
