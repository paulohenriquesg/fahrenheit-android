package com.paulohenriquesg.fahrenheit.podcast

import com.google.gson.Gson
import com.paulohenriquesg.fahrenheit.api.MediaUpdate
import com.paulohenriquesg.fahrenheit.api.PodcastSettingsApi
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

/** Each change is saved as it is chosen, and only it (#182). */
class PodcastDownloadsTest {

    private class Api(private val answer: suspend () -> Unit = {}) : PodcastSettingsApi {
        val sent = mutableListOf<Pair<String, String>>()
        override suspend fun updateMedia(itemId: String, update: MediaUpdate) {
            // As the app's Gson writes it, which leaves nulls out.
            sent += itemId to Gson().toJson(update)
            answer()
        }
    }

    private val start = DownloadSettings(enabled = false, schedule = "30 4 * * *", keep = 0, perCheck = 3)

    @Test
    fun `each change sends only its own field`() = runBlocking {
        val api = Api()
        val downloads = PodcastDownloads("p1", start, api)

        downloads.change(DownloadChange.Enabled(true))
        downloads.change(DownloadChange.Schedule(ScheduleChoice.Weekly))
        downloads.change(DownloadChange.Keep(10))
        downloads.change(DownloadChange.PerCheck(0))

        assertEquals(
            listOf(
                "p1" to """{"autoDownloadEpisodes":true}""",
                "p1" to """{"autoDownloadSchedule":"0 0 * * 0"}""",
                "p1" to """{"maxEpisodesToKeep":10}""",
                "p1" to """{"maxNewEpisodesToDownload":0}"""
            ),
            api.sent
        )
        assertEquals(DownloadSettings(enabled = true, schedule = "0 0 * * 0", keep = 10, perCheck = 0), downloads.settings)
    }

    @Test
    fun `the choice shows before the server answers`() = runBlocking {
        val answer = CompletableDeferred<Unit>()
        val downloads = PodcastDownloads("p1", start, Api { answer.await() })

        val done = async { downloads.change(DownloadChange.Keep(5)) }
        yield()
        assertEquals(5, downloads.settings.keep)

        answer.complete(Unit)
        assertTrue(done.await())
    }

    // A daily schedule at 04:30 already reads as Every day; picking it must
    // not move it to midnight.
    @Test
    fun `picking what is already chosen sends nothing`() = runBlocking {
        val api = Api()
        val downloads = PodcastDownloads("p1", start, api)

        assertTrue(downloads.change(DownloadChange.Schedule(ScheduleChoice.Daily)))
        assertTrue(downloads.change(DownloadChange.PerCheck(3)))

        assertEquals(emptyList<Pair<String, String>>(), api.sent)
        assertEquals(start, downloads.settings)
    }

    @Test
    fun `a failed save puts that choice back and says so`() = runBlocking {
        val downloads = PodcastDownloads("p1", start, Api { throw IOException("offline") })

        val saved = downloads.change(DownloadChange.Keep(25))

        assertFalse(saved)
        assertEquals(start, downloads.settings)
        assertTrue(downloads.failed)
    }

    @Test
    fun `a failure puts back only its own field, and the next change clears the message`() = runBlocking {
        var refuse = true
        val keepAnswer = CompletableDeferred<Unit>()
        val downloads = PodcastDownloads("p1", start, Api { if (refuse) { keepAnswer.await(); throw IOException("offline") } })

        val keep = async { downloads.change(DownloadChange.Keep(25)) }
        yield()
        refuse = false
        val enabled = async { downloads.change(DownloadChange.Enabled(true)) }
        yield()
        keepAnswer.complete(Unit)
        keep.await()
        enabled.await()

        assertEquals(start.copy(enabled = true), downloads.settings)
        assertTrue(downloads.failed)
        downloads.change(DownloadChange.PerCheck(10))
        assertFalse(downloads.failed)
    }

    // Review: two quick presses on one setting. Saves go one at a time, in
    // order, and a failure the newer choice has replaced undoes nothing.
    @Test
    fun `saves go in order, and a superseded failure leaves the newer choice`() = runBlocking {
        val first = CompletableDeferred<Unit>()
        var calls = 0
        val api = Api { if (++calls == 1) { first.await(); throw IOException("offline") } }
        val downloads = PodcastDownloads("p1", start, api)

        val five = async { downloads.change(DownloadChange.Keep(5)) }
        yield()
        val ten = async { downloads.change(DownloadChange.Keep(10)) }
        yield()
        assertEquals(1, api.sent.size)

        first.complete(Unit)
        five.await()
        assertTrue(ten.await())

        assertEquals(listOf("""{"maxEpisodesToKeep":5}""", """{"maxEpisodesToKeep":10}"""), api.sent.map { it.second })
        assertEquals(10, downloads.settings.keep)
        assertFalse(downloads.failed)
    }

    @Test
    fun `a failure goes back to what the server last took`() = runBlocking {
        var calls = 0
        val downloads = PodcastDownloads("p1", start, Api { if (++calls == 2) throw IOException("offline") })

        downloads.change(DownloadChange.Keep(5))
        downloads.change(DownloadChange.Keep(10))

        assertEquals(5, downloads.settings.keep)
        assertTrue(downloads.failed)
    }

    // Review: reopening the panel should not show an old failure.
    @Test
    fun `the message is let go once seen`() = runBlocking {
        val downloads = PodcastDownloads("p1", start, Api { throw IOException("offline") })
        downloads.change(DownloadChange.Keep(5))

        downloads.seen()

        assertFalse(downloads.failed)
    }
}
