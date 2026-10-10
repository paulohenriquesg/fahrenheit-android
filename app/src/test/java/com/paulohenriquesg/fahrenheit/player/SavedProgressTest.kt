package com.paulohenriquesg.fahrenheit.player

import com.paulohenriquesg.fahrenheit.api.MediaProgressResponse
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Response
import java.io.IOException

/**
 * "Never started" and "could not tell" used to be the same null, so a failed
 * read started the book from 0:00 without a word. They are different now: the
 * first is normal, the second is said out loud.
 */
class SavedProgressTest {

    private fun error(code: Int): Response<MediaProgressResponse> = Response.error(code, "".toResponseBody())

    @Test
    fun `a saved position is found`() = runBlocking {
        val saved = MediaProgressResponse(currentTime = 900.0)

        assertEquals(SavedProgress.Found(saved), SavedProgress.read(episodeId = null) { Response.success(saved) })
    }

    @Test
    fun `a book never started has no position, and that is not a failure`() = runBlocking {
        assertEquals(SavedProgress.NeverStarted, SavedProgress.read(episodeId = null) { error(404) })
    }

    @Test
    fun `the server answering for the podcast instead of the episode is never started`() = runBlocking {
        val forThePodcast = MediaProgressResponse(currentTime = 50.0, episodeId = null)

        assertEquals(SavedProgress.NeverStarted, SavedProgress.read(episodeId = "e1") { Response.success(forThePodcast) })
    }

    @Test
    fun `an episode's own position is found`() = runBlocking {
        val saved = MediaProgressResponse(currentTime = 50.0, episodeId = "e1")

        assertEquals(SavedProgress.Found(saved), SavedProgress.read(episodeId = "e1") { Response.success(saved) })
    }

    @Test
    fun `a server error is unreadable`() = runBlocking {
        assertEquals(SavedProgress.Unreadable, SavedProgress.read(episodeId = null) { error(500) })
    }

    @Test
    fun `no connection is unreadable`() = runBlocking {
        assertEquals(SavedProgress.Unreadable, SavedProgress.read(episodeId = null) { throw IOException("timeout") })
    }

    @Test
    fun `no connection is reported, so a device log can say what failed`() = runBlocking {
        val reported = mutableListOf<Throwable>()
        val timeout = IOException("timeout")

        SavedProgress.read(episodeId = null, whyUnreadable = { reported += it }) { throw timeout }

        assertEquals(listOf<Throwable>(timeout), reported)
    }

    // #217: the player's read restarts when playback connects or starts, and the
    // cancelled one said "couldn't read where you left off" although nothing failed.
    @Test
    fun `a cancelled read is cancelled, not unreadable, and nothing is reported`() {
        val reported = mutableListOf<Throwable>()

        assertThrows(CancellationException::class.java) {
            runBlocking {
                SavedProgress.read(episodeId = null, whyUnreadable = { reported += it }) {
                    throw CancellationException("restarted")
                }
            }
        }
        assertEquals(emptyList<Throwable>(), reported)
    }

    @Test
    fun `a read cancelled mid-fetch never comes back unreadable`() = runBlocking {
        var result: SavedProgress? = null

        val read = launch(start = CoroutineStart.UNDISPATCHED) {
            result = SavedProgress.read(episodeId = null) { awaitCancellation() }
        }
        read.cancel()
        read.join()

        assertTrue(read.isCancelled)
        assertEquals(null, result)
    }

    @Test
    fun `a success with nothing in it is unreadable`() = runBlocking {
        assertEquals(SavedProgress.Unreadable, SavedProgress.read(episodeId = null) { Response.success(null) })
    }

    @Test
    fun `unreadable starts from the beginning, and says so`() {
        var told = 0

        val start = SavedProgress.Unreadable.progressOr { told++ }

        assertEquals(null, start)
        assertEquals(1, told)
    }

    @Test
    fun `a found position is used, with nothing to say`() {
        var told = 0
        val saved = MediaProgressResponse(currentTime = 900.0)

        assertEquals(saved, SavedProgress.Found(saved).progressOr { told++ })
        assertEquals(0, told)
    }

    @Test
    fun `never started starts from the beginning quietly`() {
        var told = 0

        assertEquals(null, SavedProgress.NeverStarted.progressOr { told++ })
        assertEquals(0, told)
    }
}
