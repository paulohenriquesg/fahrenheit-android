package com.paulohenriquesg.fahrenheit.player

import android.app.Application
import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.media3.test.utils.robolectric.RobolectricUtil.runMainLooperUntil
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.Shadows.shadowOf
import org.robolectric.android.controller.ServiceController

/**
 * The wiring: a real MediaController, connected to the real service, the way
 * the player screen will use it. The pieces are tested on their own elsewhere;
 * this is what breaks when they are put together.
 */
@RunWith(AndroidJUnit4::class)
class PlaybackServiceTest {

    private val context = ApplicationProvider.getApplicationContext<Application>()
    private lateinit var service: ServiceController<PlaybackService>
    private var controller: MediaController? = null

    private val twoParts = NowPlaying(
        itemId = "b1", title = "A Book in Parts",
        timeline = TrackTimeline(
            listOf(
                TimelineTrack(index = 1, startOffset = 0.0, duration = 3600.0, contentUrl = "/part1"),
                TimelineTrack(index = 2, startOffset = 3600.0, duration = 1800.0, contentUrl = "/part2")
            )
        ),
        mediaDuration = null, chapters = null, episodeId = null, goToPodcast = false, description = null
    )

    @Before
    fun setUp() {
        service = Robolectric.buildService(PlaybackService::class.java).create()
        val bind = Intent(MediaSessionService.SERVICE_INTERFACE).setClass(context, PlaybackService::class.java)
        shadowOf(context).setComponentNameAndServiceForBindService(
            ComponentName(context, PlaybackService::class.java),
            service.get().onBind(bind)
        )
    }

    @After
    fun tearDown() {
        controller?.release()
        service.destroy()
    }

    private fun connect(): MediaController {
        val future = Playback.connect(context)
        runMainLooperUntil { future.isDone }
        return future.get().also { controller = it }
    }

    private fun queued(controller: MediaController, startAt: Double) {
        val queue = PlaybackQueue.of(twoParts, startAt) { "https://abs.test$it" }!!
        controller.setMediaItems(queue.items, queue.index, queue.positionMs)
        runMainLooperUntil { service.get().sessionPlayer!!.mediaItemCount == 2 }
    }

    @Test
    fun `a queue sent through a controller arrives playable, in whole-book time`() {
        queued(connect(), startAt = 3900.0)

        val player = service.get().sessionPlayer!!
        assertEquals("https://abs.test/part2", player.getMediaItemAt(1).localConfiguration?.uri.toString())
        val file = QueuedFile.of(player.currentMediaItem)!!
        assertEquals(3900.0, file.bookTime(player.currentPosition / 1000.0), 0.001)
    }

    @Test
    fun `the controller sees which book is queued, so a screen can reattach`() {
        val controller = connect()
        queued(controller, startAt = 0.0)
        runMainLooperUntil { controller.mediaItemCount == 2 }

        assertTrue(QueuedFile.of(controller.currentMediaItem)!!.isFor("b1", null))
    }

    @Test
    fun `leaving the screen by Back empties the queue`() {
        val controller = connect()
        queued(controller, startAt = 0.0)

        Playback.leave(controller, finishing = true)

        runMainLooperUntil { service.get().sessionPlayer!!.mediaItemCount == 0 }
    }

    @Test
    fun `leaving the screen for Home keeps the queue`() {
        val controller = connect()
        queued(controller, startAt = 0.0)

        Playback.leave(controller, finishing = false)
        // Let anything that would have been sent arrive.
        shadowOf(android.os.Looper.getMainLooper()).idle()

        assertEquals(2, service.get().sessionPlayer!!.mediaItemCount)
    }

    @Test
    fun `stopping from elsewhere empties the queue`() {
        queued(connect(), startAt = 0.0)

        Playback.stop(context)

        runMainLooperUntil { service.get().sessionPlayer!!.mediaItemCount == 0 }
    }

    // Review: a screen coming back reattaches through a new controller, so the
    // queued book's facts must reach a controller that was not the one to set them.
    @Test
    fun `a controller connecting later sees which book is queued`() {
        queued(connect(), startAt = 0.0)

        val later = Playback.connect(context).let { future ->
            runMainLooperUntil { future.isDone }
            future.get()
        }
        runMainLooperUntil { later.mediaItemCount == 2 }

        assertEquals(QueuedFile("b1", null, 0.0, 5400.0), QueuedFile.of(later.currentMediaItem))
        later.release()
    }

    // Review: audio is fetched with the listener's token, so another app must
    // not be able to queue a URL of its own. Media3's default connection gives
    // an untrusted controller read-only commands; this pins that we keep it.
    @Test
    fun `an app that is not trusted cannot queue anything`() {
        val session = service.get().sessionPlayer!!.let { MediaSession.Builder(context, it).setId("pin").build() }
        val stranger = MediaSession.ControllerInfo.createTestOnlyControllerInfo(
            "com.example.stranger", 0, 0, 0, 0, false, Bundle.EMPTY, true
        )

        val commands = PlaybackSessionCallback.onConnectAsync(session, stranger).get().availablePlayerCommands

        assertFalse(commands.contains(Player.COMMAND_SET_MEDIA_ITEM))
        assertFalse(commands.contains(Player.COMMAND_CHANGE_MEDIA_ITEMS))
        session.release()
    }

    @Test
    fun `the app itself can queue`() {
        val session = service.get().sessionPlayer!!.let { MediaSession.Builder(context, it).setId("pin-own").build() }
        val own = MediaSession.ControllerInfo.createTestOnlyControllerInfo(
            context.packageName, 0, 0, 0, 0, true, Bundle.EMPTY, true
        )

        val commands = PlaybackSessionCallback.onConnectAsync(session, own).get().availablePlayerCommands

        assertTrue(commands.contains(Player.COMMAND_SET_MEDIA_ITEM))
        session.release()
    }
}
