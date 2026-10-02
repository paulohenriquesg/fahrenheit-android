package com.paulohenriquesg.fahrenheit.player

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith

/** A book or episode turned into what a Media3 player queues (#16). */
@RunWith(AndroidJUnit4::class)
class PlaybackQueueTest {

    private fun nowPlaying(timeline: TrackTimeline?, episodeId: String? = null) = NowPlaying(
        itemId = "b1", title = "A Book in Parts", timeline = timeline, mediaDuration = null,
        chapters = null, episodeId = episodeId, goToPodcast = episodeId != null, description = null, line = { "" }
    )

    private val threeParts = TrackTimeline(
        listOf(
            TimelineTrack(index = 1, startOffset = 0.0, duration = 3600.0, contentUrl = "/part1"),
            TimelineTrack(index = 2, startOffset = 3600.0, duration = 1800.0, contentUrl = "/part2"),
            TimelineTrack(index = 3, startOffset = 5400.0, duration = 600.0, contentUrl = "/part3")
        )
    )

    private val resolve: (String) -> String? = { "https://abs.test$it" }

    @Test
    fun `every file is queued, in order, with its full URL`() {
        val queue = PlaybackQueue.of(nowPlaying(threeParts), startAt = 0.0, resolve)!!

        assertEquals(
            listOf("https://abs.test/part1", "https://abs.test/part2", "https://abs.test/part3"),
            queue.items.map { it.requestMetadata.mediaUri.toString() }
        )
        // Also playable as is, by a player in the same process.
        assertEquals("https://abs.test/part2", queue.items[1].localConfiguration?.uri.toString())
    }

    @Test
    fun `a start in a later file starts in that file, that far in`() {
        val queue = PlaybackQueue.of(nowPlaying(threeParts), startAt = 4500.0, resolve)!!

        assertEquals(1, queue.index)
        assertEquals(900_000L, queue.positionMs)
    }

    @Test
    fun `each file knows where it sits in the book, and the book's length`() {
        val queue = PlaybackQueue.of(nowPlaying(threeParts), startAt = 0.0, resolve)!!

        assertEquals(
            listOf(QueuedFile("b1", null, 0.0, 6000.0), QueuedFile("b1", null, 3600.0, 6000.0), QueuedFile("b1", null, 5400.0, 6000.0)),
            queue.items.map { QueuedFile.of(it) }
        )
    }

    @Test
    fun `the title travels with every file`() {
        val queue = PlaybackQueue.of(nowPlaying(threeParts), startAt = 0.0, resolve)!!

        assertEquals("A Book in Parts", queue.items[2].mediaMetadata.title.toString())
    }

    @Test
    fun `an episode is one file that knows its episode`() {
        val one = TrackTimeline(listOf(TimelineTrack(index = 1, startOffset = 0.0, duration = 1800.0, contentUrl = "/ep")))

        val queue = PlaybackQueue.of(nowPlaying(one, episodeId = "e1"), startAt = 60.0, resolve)!!

        assertEquals(1, queue.items.size)
        assertEquals(QueuedFile("b1", "e1", 0.0, 1800.0), QueuedFile.of(queue.items[0]))
        assertEquals(60_000L, queue.positionMs)
    }

    @Test
    fun `nothing to play is no queue`() =
        assertNull(PlaybackQueue.of(nowPlaying(timeline = null), startAt = 0.0, resolve))

    @Test
    fun `no server to resolve against is no queue`() =
        assertNull(PlaybackQueue.of(nowPlaying(threeParts), startAt = 0.0) { null })
}
