package com.paulohenriquesg.fahrenheit.player

import androidx.media3.common.Player
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
 * A book split across files, played as one thing (#16).
 *
 * The app loads only the first file today, so a book in parts stops at the end
 * of part one and a saved position past it plays nothing at all.
 */
@RunWith(AndroidJUnit4::class)
class BookPlaybackTest {

    private lateinit var player: ExoPlayer

    private val twoParts = TrackTimeline(
        listOf(
            TimelineTrack(index = 1, startOffset = 0.0, duration = 3600.0, contentUrl = "/part1"),
            TimelineTrack(index = 2, startOffset = 3600.0, duration = 1800.0, contentUrl = "/part2")
        )
    )

    @Before
    fun setUp() {
        player = TestExoPlayerBuilder(ApplicationProvider.getApplicationContext()).build()
    }

    @After
    fun tearDown() {
        player.release()
    }

    /** Queues [timeline] at [startAt] the way the player screen does, and returns its BookPlayback. */
    private fun loaded(startAt: Double, timeline: TrackTimeline = twoParts): BookPlayback {
        val nowPlaying = NowPlaying(
            itemId = "b1", title = "t", timeline = timeline, mediaDuration = null, chapters = null,
            episodeId = null, goToPodcast = false, description = null
        )
        val queue = PlaybackQueue.of(nowPlaying, startAt) { "https://abs.test$it" }!!
        player.setMediaItems(queue.items, queue.index, queue.positionMs)
        return BookPlayback(player, timeline)
    }

    @Test
    fun `the position reported back is whole-book time, not time within a file`() {
        val playback = loaded(startAt = 4500.0)

        assertEquals(4500.0, playback.bookPosition(), 0.001)
    }

    @Test
    fun `seeking while playing crosses into the next file`() {
        val playback = loaded(startAt = 0.0)

        playback.seekToBookTime(3900.0)

        assertEquals(1, player.currentMediaItemIndex)
        // Five minutes into the second file, and 3900s into the book.
        assertEquals(300_000L, player.currentPosition)
        assertEquals(3900.0, playback.bookPosition(), 0.001)
    }

    @Test
    fun `a position past the end of the book stops at its end`() {
        val playback = loaded(startAt = 0.0)

        playback.seekToBookTime(99_999.0)

        assertEquals(1, player.currentMediaItemIndex)
        assertEquals(5400.0, playback.bookPosition(), 0.001)
    }

    @Test
    fun `a book in one file behaves as it always did`() {
        val single = TrackTimeline(
            listOf(TimelineTrack(index = 1, startOffset = 0.0, duration = 3600.0, contentUrl = "/only"))
        )

        val playback = loaded(startAt = 900.0, timeline = single)

        assertEquals(1, player.mediaItemCount)
        assertEquals(0, player.currentMediaItemIndex)
        assertEquals(900.0, playback.bookPosition(), 0.001)
    }

    @Test
    fun `the player is left ready to play, not playing`() {
        loaded(startAt = 0.0)

        assertEquals(Player.STATE_IDLE, player.playbackState)
        assertEquals(false, player.playWhenReady)
    }
}
