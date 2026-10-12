package com.paulohenriquesg.fahrenheit.player

import android.os.Bundle
import android.os.Looper
import androidx.compose.runtime.snapshots.Snapshot
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionResult
import androidx.media3.test.utils.TestExoPlayerBuilder
import androidx.media3.test.utils.robolectric.TestPlayerRunHelper.run
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.SettableFuture
import com.google.gson.Gson
import com.paulohenriquesg.fahrenheit.TestFixtures
import com.paulohenriquesg.fahrenheit.api.LibraryItem
import com.paulohenriquesg.fahrenheit.api.LibraryItemResponse
import com.paulohenriquesg.fahrenheit.api.MediaProgressResponse
import com.paulohenriquesg.fahrenheit.api.PlaylistItem
import com.paulohenriquesg.fahrenheit.favourites.FakePlaylists
import com.paulohenriquesg.fahrenheit.favourites.FavouriteHeart
import com.paulohenriquesg.fahrenheit.favourites.Favourites
import com.paulohenriquesg.fahrenheit.favourites.HeartChange
import com.paulohenriquesg.fahrenheit.favourites.MemoryChoice
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import retrofit2.Response

/**
 * The player screen's wiring, away from the Activity (#221): what opening an
 * item queues, when progress is read and what a failed read says, the episode
 * shown following the queue, when the resume question is asked, Mark
 * finished, the queue's end, the sleep timer, the series and the heart.
 *
 * The player is a real one with fake hour-long media behind a fake
 * [PlayerLink]; the scope is unconfined, so each event has settled by the
 * time it returns, save the player's own callbacks, which [idle] delivers.
 */
@RunWith(AndroidJUnit4::class)
class PlayerViewModelTest {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
    private val context = ApplicationProvider.getApplicationContext<android.content.Context>()
    private val player: ExoPlayer = TestExoPlayerBuilder(context).setMediaSourceFactory(hourLongFiles()).build()
    private val knowledge = ServerKnowledge()

    @After
    fun stop() {
        scope.cancel()
        player.release()
        // State written outside any composition (ResumePrompt, the heart); left
        // pending, it keeps later Compose tests in this JVM from going idle.
        Snapshot.sendApplyNotifications()
    }

    // The server and the service, faked.

    private val reads = mutableListOf<Pair<String, String?>>()
    private val saved = mutableMapOf<String?, SavedProgress>()
    private var read: suspend (String, String?) -> SavedProgress = { _, episodeId -> saved[episodeId] ?: SavedProgress.NeverStarted }
    private var serverNow: MediaProgressResponse? = null
    private var checks = 0
    private val sent = mutableListOf<SessionCommand>()
    private var answer: SettableFuture<SessionResult> = SettableFuture.create()
    private var extras = Bundle()
    private var autoAdvance = false
    private var series: Result<List<LibraryItem>>? = null
    private var heart: FavouriteHeart? = null

    private val link = object : PlayerLink {
        override val player: Player get() = this@PlayerViewModelTest.player
        override val sessionExtras: Bundle get() = extras
        override fun send(command: SessionCommand, args: Bundle): ListenableFuture<SessionResult> {
            sent += command
            return answer
        }
    }

    private fun model(
        item: LibraryItemResponse? = book(),
        episodeId: String? = null,
        start: PlayerStart = PlayerStart(autoPlay = false, knowledge = knowledge),
        askThenPlay: Boolean = false,
        scope: CoroutineScope? = this.scope
    ) = PlayerViewModel(
        itemId = item?.id ?: "b1",
        episodeId = episodeId,
        start = start,
        askThenPlay = askThenPlay,
        item = { item },
        savedProgress = { itemId, episode -> reads += itemId to episode; read(itemId, episode) },
        resumeCheck = ResumeCheck(
            progress = { _, _ -> checks++; serverNow },
            latestSession = { _, _ -> LatestSession("phone-1", updatedAt = 9_000L) },
            thisDevice = "tv-1",
            knowledge = knowledge
        ),
        speeds = SpeedMemory(context),
        resolveUrl = { "https://abs.test$it" },
        playNextEpisode = { autoAdvance },
        seriesBooks = { _, _ -> series },
        heartOf = { _, _, _ -> heart },
        scope = scope
    )

    private val said = mutableListOf<PlayerNotice>()

    private fun PlayerViewModel.heard() = also { model -> scope.launch { model.notices.collect { said += it } } }

    /**
     * Lets the test player handle what it was told, on its own thread, and
     * deliver what it has to say to its listeners on the main looper.
     */
    private fun idle() {
        run(player).untilPendingCommandsAreFullyHandled()
        shadowOf(Looper.getMainLooper()).idle()
    }

    // A book in two files, and a show with two episodes, the second newer.

    private fun book(finished: Boolean = false, inSeries: Boolean = false): LibraryItemResponse = Gson().fromJson(
        """{"id":"b1","libraryId":"lib_1","mediaType":"book","media":{"duration":5400.0,
            "metadata":{"title":"A Book in Parts","authorName":"An Author","explicit":false
              ${if (inSeries) ""","series":[{"id":"s1","name":"A Series","sequence":"2"}]""" else ""}},
            "tracks":[
              {"index":1,"startOffset":0.0,"duration":3600.0,"title":"t1","contentUrl":"/api/items/b1/file/1","mimeType":"audio/mpeg",
               "metadata":{"filename":"a","ext":"mp3","path":"/a","relPath":"a","size":1,"mtimeMs":0,"ctimeMs":0,"birthtimeMs":0}},
              {"index":2,"startOffset":3600.0,"duration":1800.0,"title":"t2","contentUrl":"/api/items/b1/file/2","mimeType":"audio/mpeg",
               "metadata":{"filename":"b","ext":"mp3","path":"/b","relPath":"b","size":1,"mtimeMs":0,"ctimeMs":0,"birthtimeMs":0}}]}
            ${if (finished) ""","userMediaProgress":{"isFinished":true,"currentTime":5400.0}""" else ""}}""",
        LibraryItemResponse::class.java
    )

    private val show: LibraryItemResponse = Gson().fromJson(
        """{"id":"p1","libraryId":"lib_1","mediaType":"podcast","media":{"metadata":{"title":"A Show","explicit":false},
            "episodes":[
              {"libraryItemId":"p1","id":"e1","index":1,"title":"One","publishedAt":1000,"addedAt":0,"updatedAt":0,
               "audioTrack":{"index":1,"startOffset":0.0,"duration":1800.0,"title":"t","contentUrl":"/api/items/p1/file/1","mimeType":"audio/mpeg","codec":"mp3",
                             "metadata":{"filename":"a","ext":"mp3","path":"/a","relPath":"a","size":1,"mtimeMs":0,"ctimeMs":0,"birthtimeMs":0}}},
              {"libraryItemId":"p1","id":"e2","index":2,"title":"Two","publishedAt":2000,"addedAt":0,"updatedAt":0,
               "audioTrack":{"index":1,"startOffset":0.0,"duration":1800.0,"title":"t","contentUrl":"/api/items/p1/file/2","mimeType":"audio/mpeg","codec":"mp3",
                             "metadata":{"filename":"b","ext":"mp3","path":"/b","relPath":"b","size":1,"mtimeMs":0,"ctimeMs":0,"birthtimeMs":0}}}]}}""",
        LibraryItemResponse::class.java
    )

    private fun savedAt(seconds: Double, lastUpdate: Long = 1_000L, episodeId: String? = null) =
        SavedProgress.Found(MediaProgressResponse(currentTime = seconds, lastUpdate = lastUpdate, episodeId = episodeId))

    /** The screen opened, played a while elsewhere, and is back: the queue holds b1, paused at 4500 s. */
    private fun queuedElsewhereSince() {
        saved[null] = savedAt(4500.0)
        model().apply { connect(link) }
        // Heard elsewhere since: the server is ahead, and newer.
        serverNow = MediaProgressResponse(currentTime = 5000.0, lastUpdate = 9_000L)
    }

    // Opening an item

    @Test
    fun `opening a book queues it at its saved position, waiting for play`() {
        saved[null] = savedAt(4500.0)

        val model = model()
        model.connect(link)

        val state = model.uiState.value
        assertEquals("b1", state.nowPlaying?.itemId)
        assertTrue(state.ready)
        assertEquals(2, player.mediaItemCount)
        assertEquals(1, player.currentMediaItemIndex)
        assertEquals(900_000L, player.currentPosition)
        assertFalse(player.playWhenReady)
        assertEquals(listOf("b1" to null), reads)
    }

    @Test
    fun `nothing is read or queued before the player connects`() {
        val state = model().uiState.value

        assertEquals("b1", state.nowPlaying?.itemId)
        assertFalse(state.ready)
        assertTrue(reads.isEmpty())
        assertEquals(0, player.mediaItemCount)
    }

    @Test
    fun `opened to play, it plays`() {
        model(start = PlayerStart(autoPlay = true, knowledge = knowledge)).connect(link)

        assertTrue(player.playWhenReady)
    }

    // Read when it is needed, not when the screen opened (#90): each connect asks again.
    @Test
    fun `the saved position is read again on each connect, not kept from the first`() {
        val model = model()
        model.connect(link)
        model.disconnect()
        Playback.end(player)

        model.connect(link)

        assertEquals(listOf("b1" to null, "b1" to null), reads)
    }

    @Test
    fun `an unreadable saved position says it starts from the beginning, once`() {
        saved[null] = SavedProgress.Unreadable

        model().heard().connect(link)

        assertEquals(listOf<PlayerNotice>(PlayerNotice.ProgressUnreadable), said)
        assertEquals(0L, player.currentPosition)
        assertEquals(2, player.mediaItemCount)
    }

    // #217: a read cut short by leaving is not a failed read.
    @Test
    fun `a read cancelled by leaving says nothing, and queues nothing`() {
        val never = CompletableDeferred<Response<MediaProgressResponse>>()
        read = { _, episodeId -> SavedProgress.read(episodeId) { never.await() } }
        val model = model().heard()
        model.connect(link)

        model.disconnect()

        assertTrue(said.isEmpty())
        assertEquals(0, player.mediaItemCount)
        assertFalse(model.uiState.value.ready)
    }

    @Test
    fun `an item that cannot be loaded shows the error`() {
        val state = model(item = null).uiState.value

        assertTrue(state.failed)
        assertNull(state.nowPlaying)
    }

    @Test
    fun `an episode the show no longer has shows the error`() {
        assertTrue(model(item = show, episodeId = "gone").uiState.value.failed)
    }

    @Test
    fun `a book with nothing to play shows the error once connected`() {
        val empty = Gson().fromJson("""{"id":"b9","mediaType":"book","media":{"metadata":{"title":"Empty","explicit":false}}}""", LibraryItemResponse::class.java)
        val model = model(item = empty)

        model.connect(link)

        assertTrue(model.uiState.value.failed)
        assertEquals(0, player.mediaItemCount)
    }

    @Test
    fun `a service that cannot be reached shows the error, and reaching it later clears it`() {
        val model = model()

        model.connect(null)
        assertTrue(model.uiState.value.connectFailed)

        model.connect(link)
        assertFalse(model.uiState.value.connectFailed)
        assertTrue(model.uiState.value.ready)
    }

    // The episode shown (#108)

    @Test
    fun `the episode shown follows the queue on to the next one`() {
        autoAdvance = true
        val model = model(item = show, episodeId = "e1")
        model.connect(link)
        assertEquals(2, player.mediaItemCount)

        player.seekTo(1, 0)
        idle()

        assertEquals("e2", model.uiState.value.nowPlaying?.episodeId)
        // Begun again with that one: it is first in the queue, and nothing newer follows.
        assertEquals(1, player.mediaItemCount)
        assertTrue(model.uiState.value.ready)
    }

    // The reattach path where begin hands over to the episode the queue moved on to (#108).
    @Test
    fun `coming back after the queue moved on shows the episode playing, and begins with that one`() {
        autoAdvance = true
        model(item = show, episodeId = "e1").connect(link)
        player.seekTo(1, 0)
        idle()
        reads.clear()

        // The screen recreated on e1, as the intent still says.
        val model = model(item = show, episodeId = "e1", start = PlayerStart(autoPlay = false, resumed = true, knowledge = knowledge))
        model.connect(link)

        assertEquals("e2", model.uiState.value.nowPlaying?.episodeId)
        assertTrue(model.uiState.value.ready)
        // Reattached to e2: nothing read for e1, and e1 is gone from the queue.
        assertTrue(reads.none { it.second == "e1" })
        assertEquals(1, player.mediaItemCount)
        assertEquals("e2", QueuedFile.of(player.currentMediaItem)?.episodeId)
    }

    // As the screen's effects did, a frame later: not inside the controller's own callback (review).
    @Test
    fun `following the queue begins again after the player's callback, not inside it`() {
        autoAdvance = true
        val model = model(item = show, episodeId = "e1", scope = null)
        idle()
        model.connect(link)
        idle()
        assertEquals(2, player.mediaItemCount)
        var queuedDuringCallback = -1
        player.addListener(object : Player.Listener {
            override fun onMediaItemTransition(mediaItem: androidx.media3.common.MediaItem?, reason: Int) {
                queuedDuringCallback = player.mediaItemCount
            }
        })

        player.seekTo(1, 0)
        assertEquals(2, queuedDuringCallback)
        idle()

        assertEquals("e2", model.uiState.value.nowPlaying?.episodeId)
        assertEquals(1, player.mediaItemCount)
    }

    @Test
    fun `the end is watched for the episode followed to`() {
        autoAdvance = true
        val model = model(item = show, episodeId = "e1").heard()
        model.connect(link)
        idle()
        player.seekTo(1, 0)
        idle()

        player.clearMediaItems()
        idle()

        assertEquals(listOf<PlayerNotice>(PlayerNotice.QueueEnded), said)
    }

    @Test
    fun `with auto-advance, the next episode is queued at its own saved position, read before this one's`() {
        autoAdvance = true
        saved["e2"] = savedAt(600.0, episodeId = "e2")

        model(item = show, episodeId = "e1").connect(link)

        assertEquals(listOf("p1" to "e2", "p1" to "e1"), reads)
        assertEquals(2, player.mediaItemCount)
        val next = QueuedFile.of(player.getMediaItemAt(1))!!
        assertEquals("e2", next.episodeId)
        assertEquals(600.0, next.startAt, 0.0)
    }

    @Test
    fun `without auto-advance, only this episode is queued and no other is read`() {
        model(item = show, episodeId = "e1").connect(link)

        assertEquals(listOf("p1" to "e1"), reads)
        assertEquals(1, player.mediaItemCount)
    }

    // Coming back to what is queued (#90)

    @Test
    fun `coming back to a paused book queued already asks where to continue`() {
        queuedElsewhereSince()

        val model = model()
        model.connect(link)

        val offer = model.uiState.value.offer
        assertNotNull(offer)
        assertEquals(5000.0, offer!!.there, 0.0)
        assertEquals(4500.0, offer.here, 0.0)
        // Reattached: nothing read for the queue, nothing moved.
        assertEquals(900_000L, player.currentPosition)
    }

    @Test
    fun `Home and back to the same screen asks too`() {
        saved[null] = savedAt(4500.0)
        val model = model()
        model.connect(link)
        model.disconnect()
        serverNow = MediaProgressResponse(currentTime = 5000.0, lastUpdate = 9_000L)

        model.connect(link)

        assertEquals(5000.0, model.uiState.value.offer!!.there, 0.0)
    }

    @Test
    fun `coming back to what is playing does not ask`() {
        queuedElsewhereSince()
        player.play()

        val model = model()
        model.connect(link)

        assertNull(model.uiState.value.offer)
        assertEquals(0, checks)
    }

    @Test
    fun `a start chosen on the opening screen is not questioned`() {
        queuedElsewhereSince()

        val model = model(start = PlayerStart(autoPlay = true, startAt = 100.0, knowledge = knowledge))
        model.connect(link)

        assertNull(model.uiState.value.offer)
        assertEquals(100_000L, player.currentPosition)
    }

    @Test
    fun `continuing there moves to the other position, and the question goes`() {
        queuedElsewhereSince()
        val model = model()
        model.connect(link)

        model.answerResume(moveThere = true)

        assertNull(model.uiState.value.offer)
        assertEquals(1, player.currentMediaItemIndex)
        assertEquals(1_400_000L, player.currentPosition)
        assertFalse(player.playWhenReady)
    }

    @Test
    fun `staying keeps the position`() {
        queuedElsewhereSince()
        val model = model()
        model.connect(link)

        model.answerResume(moveThere = false)

        assertNull(model.uiState.value.offer)
        assertEquals(900_000L, player.currentPosition)
    }

    // #144
    @Test
    fun `opened by a Play from outside, it asks, then plays once answered`() {
        queuedElsewhereSince()
        val model = model(askThenPlay = true)
        model.connect(link)
        assertNotNull(model.uiState.value.offer)
        assertFalse(player.playWhenReady)

        model.answerResume(moveThere = false)

        assertTrue(player.playWhenReady)
    }

    @Test
    fun `the Play it was opened for is spent once, and coming back again asks without playing`() {
        queuedElsewhereSince()
        val model = model(askThenPlay = true)
        model.connect(link)
        model.answerResume(moveThere = false)
        player.pause()
        model.disconnect()
        serverNow = MediaProgressResponse(currentTime = 5200.0, lastUpdate = 20_000L)

        model.connect(link)
        model.answerResume(moveThere = false)

        assertFalse(player.playWhenReady)
    }

    // The focus nudge back to Play follows an answer, not a press on nothing.
    @Test
    fun `an answer is taken only while the question shows`() {
        queuedElsewhereSince()
        val model = model()
        model.connect(link)

        assertTrue(model.answerResume(moveThere = false))
        assertFalse(model.answerResume(moveThere = false))
    }

    @Test
    fun `Play from a pause asks first`() {
        saved[null] = savedAt(4500.0)
        val model = model()
        model.connect(link)
        serverNow = MediaProgressResponse(currentTime = 5000.0, lastUpdate = 9_000L)

        model.play()

        assertNotNull(model.uiState.value.offer)
        assertFalse(player.playWhenReady)
    }

    @Test
    fun `Play with nothing to ask plays`() {
        val model = model()
        model.connect(link)

        model.play()

        assertNull(model.uiState.value.offer)
        assertTrue(player.playWhenReady)
    }

    @Test
    fun `the remote's Play from a pause is taken to ask, and while playing is left to the session`() {
        saved[null] = savedAt(4500.0)
        val model = model()
        assertFalse(model.remotePlay(down = true))
        model.connect(link)
        serverNow = MediaProgressResponse(currentTime = 5000.0, lastUpdate = 9_000L)

        assertTrue(model.remotePlay(down = true))
        assertTrue(model.remotePlay(down = false))
        assertNotNull(model.uiState.value.offer)

        model.answerResume(moveThere = false)
        player.play()
        assertFalse(model.remotePlay(down = true))
    }

    // Mark finished (#107, #181)

    @Test
    fun `marking finished asks the service, and shows it once done`() {
        val model = model()
        model.connect(link)

        model.markFinished(true)
        assertTrue(model.uiState.value.marking)
        assertEquals(listOf(FinishCommand.COMMAND), sent)

        answer.set(SessionResult(SessionResult.RESULT_SUCCESS))

        assertFalse(model.uiState.value.marking)
        assertEquals(true, model.uiState.value.finished)
    }

    @Test
    fun `a mark that fails says so and leaves it as it was`() {
        val model = model().heard()
        model.connect(link)

        model.markFinished(true)
        answer.set(SessionResult(SessionResult.RESULT_ERROR_UNKNOWN))

        assertFalse(model.uiState.value.marking)
        assertEquals(false, model.uiState.value.finished)
        assertEquals(listOf<PlayerNotice>(PlayerNotice.MarkFinishedFailed), said)
    }

    @Test
    fun `a mark survives the player reconnecting`() {
        val model = model()
        model.connect(link)
        model.markFinished(true)
        answer.set(SessionResult(SessionResult.RESULT_SUCCESS))

        model.disconnect()
        model.connect(link)

        assertEquals(true, model.uiState.value.finished)
    }

    @Test
    fun `playing a finished book makes it unfinished`() {
        val model = model(item = book(finished = true))
        model.connect(link)
        assertEquals(true, model.uiState.value.finished)

        player.play()
        run(player).untilPositionAtLeast(1_000)
        idle()

        assertEquals(false, model.uiState.value.finished)
    }

    @Test
    fun `an episode has no Mark finished, playing or not`() {
        val model = model(item = show, episodeId = "e1")
        model.connect(link)

        player.play()
        run(player).untilPositionAtLeast(1_000)
        idle()

        assertNull(model.uiState.value.finished)
    }

    // The end (#179)

    @Test
    fun `the queue played to its end closes the screen`() {
        val model = model().heard()
        model.connect(link)
        idle()

        player.clearMediaItems()
        idle()

        assertEquals(listOf<PlayerNotice>(PlayerNotice.QueueEnded), said)
    }

    @Test
    fun `a queue ending after the screen let go of the player is not this screen's end`() {
        val model = model().heard()
        model.connect(link)
        idle()
        model.disconnect()

        player.clearMediaItems()
        idle()

        assertTrue(said.isEmpty())
        assertNull(model.uiState.value.player)
    }

    // The sleep timer, which the service keeps

    @Test
    fun `the sleep timer is the service's, on connecting and as it changes`() {
        extras = SleepCommand.extras(SleepState(SleepChoice.Minutes(15), minutesLeft = 15))
        val model = model()

        model.connect(link)
        assertEquals(SleepState(SleepChoice.Minutes(15), 15), model.uiState.value.sleep)

        model.extrasChanged(SleepCommand.extras(SleepState(SleepChoice.Minutes(15), minutesLeft = 14)))
        assertEquals(14, model.uiState.value.sleep?.minutesLeft)

        model.extrasChanged(SleepCommand.extras(null))
        assertNull(model.uiState.value.sleep)
    }

    @Test
    fun `choosing a sleep timer sends it to the service`() {
        val model = model()
        model.connect(link)

        model.chooseSleep(SleepChoice.Minutes(30))

        assertEquals(listOf(SleepCommand.COMMAND), sent)
    }

    @Test
    fun `a speed chosen plays at once and is remembered for the book`() {
        val model = model()
        model.connect(link)

        model.chooseSpeed(1.5f)

        assertEquals(1.5f, player.playbackParameters.speed, 0f)
        assertEquals(1.5f, SpeedMemory(context).of("b1"), 0f)
    }

    // What the screen shows beside the player

    @Test
    fun `the series is read for a book in one, for Book N of M`() {
        series = Result.success(listOf(TestFixtures.createMockLibraryItemEntry("b0"), TestFixtures.createMockLibraryItemEntry("b1")))

        val state = model(item = book(inSeries = true)).uiState.value

        assertEquals(2, state.series?.total)
    }

    @Test
    fun `a series that does not list this book is left out`() {
        series = Result.success(listOf(TestFixtures.createMockLibraryItemEntry("b0")))

        assertNull(model(item = book(inSeries = true)).uiState.value.series)
    }

    @Test
    fun `the heart shows while the library has a Favourites playlist, and a press says what it did`() {
        val server = FakePlaylists().apply { add("pl_1", "Favourites", "lib_1", PlaylistItem("p1", "e1"), PlaylistItem("b0", null)) }
        heart = FavouriteHeart(Favourites(server, MemoryChoice("lib_1" to "pl_1")), "lib_1", "p1", "e1")
        val model = model(item = show, episodeId = "e1").heard()
        assertEquals(FavouriteState(playlist = "Favourites", filled = true), model.uiState.value.favourite)

        model.toggleFavourite()

        assertEquals(FavouriteState(playlist = "Favourites", filled = false), model.uiState.value.favourite)
        assertEquals(listOf<PlayerNotice>(PlayerNotice.Favourite(HeartChange.Removed("Favourites"))), said)
    }

    @Test
    fun `with no Favourites playlist there is no heart`() {
        assertNull(model().uiState.value.favourite)
    }

    @Test
    fun `the transport's time is the screen's`() {
        val model = model()

        model.timeUpdated(42.0)

        assertEquals(42.0, model.uiState.value.currentTime, 0.0)
    }
}
