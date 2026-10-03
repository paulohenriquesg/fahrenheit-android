package com.paulohenriquesg.fahrenheit.player

import android.os.Bundle
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.test.utils.FakeClock
import androidx.media3.test.utils.TestExoPlayerBuilder
import androidx.media3.test.utils.robolectric.TestPlayerRunHelper.run
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The sleep timer as the playback service runs it: on the player itself, so
 * it pauses whether or not a screen is open (#107).
 */
@RunWith(AndroidJUnit4::class)
class SleepWatchTest {

    private lateinit var player: ExoPlayer
    private lateinit var watch: SleepWatch
    private val published = mutableListOf<Bundle>()
    private val shown get() = SleepCommand.state(published.last())

    private val book = NowPlaying(
        itemId = "b1", title = "b1",
        timeline = TrackTimeline(listOf(TimelineTrack(index = 1, startOffset = 0.0, duration = 3600.0, contentUrl = "/b1"))),
        mediaDuration = null, chapters = null, episodeId = null, goToPodcast = false, description = null
    )

    @Before
    fun setUp() {
        player = TestExoPlayerBuilder(ApplicationProvider.getApplicationContext())
            .setMediaSourceFactory(hourLongFiles())
            .build()
        watch = SleepWatch(player, now = { player.clock.elapsedRealtime() }, publish = { published += it })
        player.addListener(watch)
        val queue = PlaybackQueue.of(book, 0.0) { "https://abs.test$it" }!!
        player.setMediaItems(queue.items, queue.index, queue.positionMs)
        player.prepare()
    }

    @After
    fun tearDown() = player.release()

    private fun playUntil(positionMs: Long) {
        player.play()
        run(player).untilPositionAtLeast(positionMs)
    }

    // Review Focus 1.
    @Test
    fun `fifteen minutes of listening pauses the player`() {
        watch.set(SleepCommand.args(SleepChoice.Minutes(15)))
        playUntil(14 * 60_000L)
        watch.check()
        assertTrue(player.playWhenReady)

        playUntil(15 * 60_000L + 500)
        watch.check()

        assertFalse(player.playWhenReady)
        assertFalse(watch.running)
    }

    @Test
    fun `a pause does not use the timer up`() {
        watch.set(SleepCommand.args(SleepChoice.Minutes(15)))
        playUntil(10 * 60_000L)
        player.pause()
        run(player).untilPendingCommandsAreFullyHandled()
        (player.clock as FakeClock).advanceTime(30 * 60_000L)

        watch.check()

        assertTrue(watch.running)
        assertEquals(SleepState(SleepChoice.Minutes(15), minutesLeft = 5), shown)
    }

    @Test
    fun `end of chapter pauses at the chapter's end`() {
        watch.set(SleepCommand.args(SleepChoice.EndOfChapter, chapterEnds = listOf(600.0, 1200.0, 3600.0)))
        playUntil(590_000L)
        watch.check()
        assertTrue(player.playWhenReady)

        playUntil(600_000L)
        watch.check()

        assertFalse(player.playWhenReady)
    }

    // Review Focus 2.
    @Test
    fun `a seek past the chapter's end waits for the next one`() {
        watch.set(SleepCommand.args(SleepChoice.EndOfChapter, chapterEnds = listOf(600.0, 1200.0, 3600.0)))
        playUntil(10_000L)
        player.seekTo(700_000L)
        run(player).untilPendingCommandsAreFullyHandled()

        watch.check()

        assertTrue(player.playWhenReady)
        assertEquals(SleepChoice.EndOfChapter, shown!!.choice)
        assertEquals(9, shown!!.minutesLeft)
    }

    @Test
    fun `what is left is published in whole minutes`() {
        watch.set(SleepCommand.args(SleepChoice.Minutes(30)))
        assertEquals(SleepState(SleepChoice.Minutes(30), minutesLeft = 30), shown)
    }

    @Test
    fun `turned off, nothing is shown`() {
        watch.set(SleepCommand.args(SleepChoice.Minutes(30)))
        watch.set(SleepCommand.args(SleepChoice.Off))
        assertNull(shown)
        assertFalse(watch.running)
    }

    @Test
    fun `when it fires, nothing is shown`() {
        watch.set(SleepCommand.args(SleepChoice.Minutes(15)))
        playUntil(15 * 60_000L + 500)
        watch.check()
        assertNull(shown)
    }

    @Test
    fun `a new queue turns it off`() {
        watch.set(SleepCommand.args(SleepChoice.Minutes(15)))
        watch.beforeLeaving()
        assertFalse(watch.running)
        assertNull(shown)
    }

    @Test
    fun `the command's choice survives the trip`() {
        assertEquals(SleepChoice.Minutes(60), SleepCommand.choiceOf(SleepCommand.args(SleepChoice.Minutes(60))))
        assertEquals(SleepChoice.EndOfChapter, SleepCommand.choiceOf(SleepCommand.args(SleepChoice.EndOfChapter, listOf(1.0))))
        assertEquals(SleepChoice.Off, SleepCommand.choiceOf(Bundle.EMPTY))
    }
}
