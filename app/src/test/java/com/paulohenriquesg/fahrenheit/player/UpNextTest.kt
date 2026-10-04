package com.paulohenriquesg.fahrenheit.player

import com.google.gson.Gson
import com.paulohenriquesg.fahrenheit.TestFixtures
import com.paulohenriquesg.fahrenheit.api.Episode
import com.paulohenriquesg.fahrenheit.api.LibraryItemResponse
import com.paulohenriquesg.fahrenheit.api.MediaProgressResponse
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** What the playback service queues after an episode, with or without a screen (#160). */
@RunWith(RobolectricTestRunner::class)
class UpNextTest {
    private fun episode(id: String, publishedAt: Long, audio: Boolean = true): Episode =
        Gson().fromJson(
            """{"libraryItemId":"p1","id":"$id","index":1,"title":"Episode $id","publishedAt":$publishedAt,"addedAt":0,"updatedAt":0
               ${if (audio) ""","audioTrack":{"index":1,"startOffset":0.0,"duration":1800.0,"title":"t","contentUrl":"/f/$id","mimeType":"audio/mpeg","codec":"mp3",
                 "metadata":{"filename":"a","ext":"mp3","path":"/a","relPath":"a","size":1,"mtimeMs":0,"ctimeMs":0,"birthtimeMs":0}}""" else ""}}""",
            Episode::class.java
        )

    private fun podcast(vararg episodes: Episode): LibraryItemResponse =
        TestFixtures.createMockLibraryItem(id = "p1").let { it.copy(mediaType = "podcast", media = it.media.copy(episodes = episodes.toList())) }

    private val shows = podcast(episode("e3", 3_000), episode("e1", 1_000), episode("e2b", 2_500, audio = false), episode("e2", 2_000))
    private val asked = mutableListOf<String>()
    private fun savedAt(seconds: Double?): suspend (String, String) -> MediaProgressResponse? = { _, episodeId ->
        asked += episodeId
        seconds?.let { Gson().fromJson("""{"currentTime":$it,"duration":1800.0,"progress":0.1,"isFinished":false}""", MediaProgressResponse::class.java) }
    }
    private val resolve: (String) -> String? = { "https://abs.test$it" }

    @Test fun `the next newer episode with audio, where it was left`() = runBlocking {
        val items = UpNext.after(shows, "e2", savedAt(600.0), resolve)!!

        val file = QueuedFile.of(items.single())!!
        assertEquals(QueuedFile("p1", "e3", 0.0, 1800.0, startAt = 600.0), file)
        assertEquals("https://abs.test/f/e3", items.single().localConfiguration?.uri.toString())
        assertEquals(listOf("e3"), asked)
    }

    @Test fun `never started, from the beginning`() = runBlocking {
        val items = UpNext.after(shows, "e1", savedAt(null), resolve)!!
        assertEquals(QueuedFile("p1", "e2", 0.0, 1800.0, startAt = 0.0), QueuedFile.of(items.single()))
    }

    @Test fun `the newest has nothing after it`() = runBlocking {
        assertNull(UpNext.after(shows, "e3", savedAt(600.0), resolve))
        assertEquals(emptyList<String>(), asked)
    }

    @Test fun `an episode the podcast no longer has, nothing`() = runBlocking {
        assertNull(UpNext.after(shows, "gone", savedAt(600.0), resolve))
    }
}
