package com.paulohenriquesg.fahrenheit.player

import androidx.media3.common.Player
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
        // 500 s: the chapter row reads "8 min 20 s left in chapter", and the chip agrees.
        assertEquals(8, shown!!.minutesLeft)
    }

    private fun episode(id: String) = NowPlaying(
        itemId = "p1", title = id,
        timeline = TrackTimeline(listOf(TimelineTrack(index = 1, startOffset = 0.0, duration = 3600.0, contentUrl = "/$id"))),
        mediaDuration = null, chapters = null, episodeId = id, goToPodcast = true, description = null
    )

    private fun queue(start: QueueStart) {
        player.setMediaItems(start.items, start.index, start.positionMs)
        run(player).untilPendingCommandsAreFullyHandled()
    }

    // #183: an episode's last chapter can end, by the feed's reckoning, after
    // its file does, and with the next episode queued the player never ends.
    @Test
    fun `end of chapter stops when the queue moves on to the next episode`() {
        queue(PlaybackQueue.of(episode("e1"), 3590.0, { "https://abs.test$it" }, next = episode("e2"))!!)
        watch.set(SleepCommand.args(SleepChoice.EndOfChapter, chapterEnds = listOf(600.0, 1200.0, 3700.0)))

        player.play()
        // Into the next file: it pauses there, at its start.
        run(player).untilPositionAtLeast(1, 0)
        watch.check()

        assertFalse(player.playWhenReady)
        assertFalse(watch.running)
    }

    @Test
    fun `end of chapter carries on into a book's next file`() {
        val twoFiles = book.copy(
            timeline = TrackTimeline(
                listOf(TimelineTrack(1, 0.0, 3600.0, "/b1"), TimelineTrack(2, 3600.0, 3600.0, "/b2"))
            )
        )
        queue(PlaybackQueue.of(twoFiles, 3590.0) { "https://abs.test$it" }!!)
        watch.set(SleepCommand.args(SleepChoice.EndOfChapter, chapterEnds = listOf(1200.0, 4200.0, 7200.0)))

        player.play()
        run(player).untilPositionAtLeast(1, 1)
        watch.check()

        assertTrue(player.playWhenReady)
        assertTrue(watch.running)
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

    // Review: the file can end a fraction short of the last chapter's stored end.
    @Test
    fun `at the end of the book it turns off, even short of the last chapter's end`() {
        watch.set(SleepCommand.args(SleepChoice.EndOfChapter, chapterEnds = listOf(600.0, 3600.5)))
        player.seekTo(3_590_000L)
        player.play()
        run(player).untilState(Player.STATE_ENDED)

        watch.check()

        assertFalse(watch.running)
        assertNull(shown)
    }

    // Review: a paused player a few ms short of the end was polled 50 times a second.
    @Test
    fun `paused, it checks only once a second`() {
        watch.set(SleepCommand.args(SleepChoice.EndOfChapter, chapterEnds = listOf(600.01)))
        player.seekTo(600_000L)
        run(player).untilPendingCommandsAreFullyHandled()

        assertEquals(1_000L, watch.nextCheckMs())
    }

    // Device check: the chip read "Sleep 2 min" beside "1 min 8 s left in chapter".
    @Test
    fun `end of chapter's minutes agree with the chapter row`() {
        watch.set(SleepCommand.args(SleepChoice.EndOfChapter, chapterEnds = listOf(68.0, 3600.0)))
        assertEquals(SleepState(SleepChoice.EndOfChapter, minutesLeft = 1), shown)
    }

    @Test
    fun `under a minute to the chapter's end still reads one minute`() {
        watch.set(SleepCommand.args(SleepChoice.EndOfChapter, chapterEnds = listOf(30.0, 3600.0)))
        assertEquals(1, shown!!.minutesLeft)
    }

    @Test
    fun `a timer of minutes counts down from what was chosen`() {
        watch.set(SleepCommand.args(SleepChoice.Minutes(15)))
        player.play()
        run(player).untilPositionAtLeast(1_000)
        watch.check()
        assertEquals(15, shown!!.minutesLeft)
    }
}
