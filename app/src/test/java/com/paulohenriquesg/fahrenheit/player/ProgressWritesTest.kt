package com.paulohenriquesg.fahrenheit.player

import com.paulohenriquesg.fahrenheit.api.MediaProgressResponse
import com.paulohenriquesg.fahrenheit.api.ProgressMark
import com.paulohenriquesg.fahrenheit.progress.ProgressStore
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** What the playback service puts on the server, it puts in the store too (#207). */
class ProgressWritesTest {

    private val store = ProgressStore(now = { 0L })
    private val episode = QueuedFile("pod", "e1", 0.0, 1800.0)

    @Test
    fun `a delivered report is in the store, against the whole length`() {
        ProgressWrites(store) { _, _ -> null }.delivered(episode, 900.0)

        val seen = store.of("pod", "e1")!!
        assertEquals(900.0, seen.currentTime!!, 0.0)
        assertEquals(0.5, seen.progress!!, 0.0)
    }

    @Test
    fun `a stretch closed, the item is read back - finished or not is the server's word`() = runBlocking<Unit> {
        val writes = ProgressWrites(store) { itemId, episodeId ->
            MediaProgressResponse(libraryItemId = itemId, episodeId = episodeId, currentTime = 1795.0, duration = 1800.0, isFinished = true)
        }
        writes.delivered(episode, 1795.0)

        writes.closed(episode)

        assertEquals(true, store.of("pod", "e1")?.isFinished)
    }

    @Test
    fun `a read-back that fails leaves what the reports wrote`() = runBlocking<Unit> {
        val writes = ProgressWrites(store) { _, _ -> error("offline") }
        writes.delivered(episode, 600.0)

        writes.closed(episode)

        assertEquals(600.0, store.of("pod", "e1")?.currentTime!!, 0.0)
    }

    @Test
    fun `a mark the server took is in the store`() = runBlocking<Unit> {
        ProgressWrites(store) { _, _ -> null }.marked("book", null, ProgressMark(isFinished = true)) {}

        assertEquals(true, store.of("book")?.isFinished)
    }

    @Test
    fun `a mark the server refused is not, and the refusal still fails the mark`() = runBlocking<Unit> {
        val refused = runCatching {
            ProgressWrites(store) { _, _ -> null }.marked("book", null, ProgressMark(isFinished = true)) { error("403") }
        }

        assertEquals(true, refused.isFailure)
        assertNull(store.of("book"))
    }
}
