package com.paulohenriquesg.fahrenheit.player

import androidx.media3.session.SessionError
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
import androidx.media3.session.SessionResult
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

        val commands = PlaybackSessionCallback {}.onConnectAsync(session, stranger).get().availablePlayerCommands

        assertFalse(commands.contains(Player.COMMAND_SET_MEDIA_ITEM))
        assertFalse(commands.contains(Player.COMMAND_CHANGE_MEDIA_ITEMS))
        session.release()
    }

    // #144: Play from outside the player screen - the remote on Home, the
    // system - is held while paused and checked; the app's own Play passes.
    @Test
    fun `a play from outside the app, while paused, is held for the check`() {
        val session = service.get().sessionPlayer!!.let { MediaSession.Builder(context, it).setId("pin-outside").build() }
        val system = MediaSession.ControllerInfo.createTestOnlyControllerInfo(
            "com.example.launcher", 0, 0, 0, 0, true, Bundle.EMPTY, true
        )
        var held = 0
        val callback = PlaybackSessionCallback(onSleep = {}, ownPackage = context.packageName, outsidePlay = { held++; true })

        val result = callback.onPlayerCommandRequest(session, system, Player.COMMAND_PLAY_PAUSE)

        assertEquals(1, held)
        assertTrue(result != SessionResult.RESULT_SUCCESS)
        session.release()
    }

    @Test
    fun `the app's own play passes`() {
        val session = service.get().sessionPlayer!!.let { MediaSession.Builder(context, it).setId("pin-own-play").build() }
        val own = MediaSession.ControllerInfo.createTestOnlyControllerInfo(
            context.packageName, 0, 0, 0, 0, true, Bundle.EMPTY, true
        )
        var held = 0
        val callback = PlaybackSessionCallback(onSleep = {}, ownPackage = context.packageName, outsidePlay = { held++; true })

        val result = callback.onPlayerCommandRequest(session, own, Player.COMMAND_PLAY_PAUSE)

        assertEquals(0, held)
        assertEquals(SessionResult.RESULT_SUCCESS, result)
        session.release()
    }

    @Test
    fun `a pause from outside passes`() {
        val player = service.get().sessionPlayer!!
        val session = MediaSession.Builder(context, player).setId("pin-outside-pause").build()
        player.playWhenReady = true
        val system = MediaSession.ControllerInfo.createTestOnlyControllerInfo(
            "com.example.launcher", 0, 0, 0, 0, true, Bundle.EMPTY, true
        )
        var held = 0
        val callback = PlaybackSessionCallback(onSleep = {}, ownPackage = context.packageName, outsidePlay = { held++; true })

        val result = callback.onPlayerCommandRequest(session, system, Player.COMMAND_PLAY_PAUSE)

        assertEquals(0, held)
        assertEquals(SessionResult.RESULT_SUCCESS, result)
        session.release()
    }

    // Review Focus 5: only the app sets a timer.
    @Test
    fun `an app that is not trusted cannot set a sleep timer`() {
        val session = service.get().sessionPlayer!!.let { MediaSession.Builder(context, it).setId("pin-sleep").build() }
        val stranger = MediaSession.ControllerInfo.createTestOnlyControllerInfo(
            "com.example.stranger", 0, 0, 0, 0, false, Bundle.EMPTY, true
        )

        val commands = PlaybackSessionCallback {}.onConnectAsync(session, stranger).get().availableSessionCommands

        assertFalse(commands.contains(SleepCommand.COMMAND))
        session.release()
    }

    @Test
    fun `the app can set a sleep timer, and hears what is left`() {
        val heard = mutableListOf<Bundle>()
        val future = Playback.connect(context, object : MediaController.Listener {
            override fun onExtrasChanged(controller: MediaController, extras: Bundle) { heard += extras }
        })
        runMainLooperUntil { future.isDone }
        val controller = future.get().also { this.controller = it }
        queued(controller, startAt = 0.0)

        controller.sendCustomCommand(SleepCommand.COMMAND, SleepCommand.args(SleepChoice.Minutes(30)))

        runMainLooperUntil { heard.isNotEmpty() }
        assertEquals(SleepState(SleepChoice.Minutes(30), minutesLeft = 30), SleepCommand.state(heard.last()))
        assertEquals(SleepState(SleepChoice.Minutes(30), minutesLeft = 30), SleepCommand.state(controller.sessionExtras))
    }

    @Test
    fun `the app itself can queue`() {
        val session = service.get().sessionPlayer!!.let { MediaSession.Builder(context, it).setId("pin-own").build() }
        val own = MediaSession.ControllerInfo.createTestOnlyControllerInfo(
            context.packageName, 0, 0, 0, 0, true, Bundle.EMPTY, true
        )

        val commands = PlaybackSessionCallback {}.onConnectAsync(session, own).get().availablePlayerCommands

        assertTrue(commands.contains(Player.COMMAND_SET_MEDIA_ITEM))
        session.release()
    }

    // Review: the loop that runs the timer with no screen open.
    @Test
    fun `the service runs the timer on its own, and pauses when it is up`() {
        val controller = connect()
        queued(controller, startAt = 0.0)
        controller.play()
        runMainLooperUntil { service.get().sessionPlayer!!.playWhenReady }

        controller.sendCustomCommand(SleepCommand.COMMAND, SleepCommand.args(SleepChoice.Minutes(0)))
        shadowOf(android.os.Looper.getMainLooper()).idleFor(java.time.Duration.ofSeconds(2))

        runMainLooperUntil { !service.get().sessionPlayer!!.playWhenReady }
        runMainLooperUntil { SleepCommand.state(controller.sessionExtras) == null }
    }

    @Test
    fun `a new queue turns the timer off`() {
        val controller = connect()
        queued(controller, startAt = 0.0)
        controller.sendCustomCommand(SleepCommand.COMMAND, SleepCommand.args(SleepChoice.Minutes(30)))
        runMainLooperUntil { SleepCommand.state(controller.sessionExtras) != null }

        val queue = PlaybackQueue.of(twoParts, 0.0) { "https://abs.test$it" }!!
        controller.setMediaItems(queue.items, queue.index, queue.positionMs)

        runMainLooperUntil { SleepCommand.state(controller.sessionExtras) == null }
    }

    // Review: defence in depth behind Media3's own check.
    @Test
    fun `a sleep command from an untrusted app is refused`() {
        val session = service.get().sessionPlayer!!.let { MediaSession.Builder(context, it).setId("pin-sleep-cmd").build() }
        val stranger = MediaSession.ControllerInfo.createTestOnlyControllerInfo(
            "com.example.stranger", 0, 0, 0, 0, false, Bundle.EMPTY, true
        )
        var set = 0

        val result = PlaybackSessionCallback { set++ }
            .onCustomCommand(session, stranger, SleepCommand.COMMAND, SleepCommand.args(SleepChoice.Minutes(15))).get()

        assertEquals(SessionError.ERROR_PERMISSION_DENIED, result.resultCode)
        assertEquals(0, set)
        session.release()
    }

    @Test
    fun `only the app may mark an item finished`() {
        val session = service.get().sessionPlayer!!.let { MediaSession.Builder(context, it).setId("pin-finish").build() }
        val stranger = MediaSession.ControllerInfo.createTestOnlyControllerInfo(
            "com.example.stranger", 0, 0, 0, 0, false, Bundle.EMPTY, true
        )
        val own = MediaSession.ControllerInfo.createTestOnlyControllerInfo(
            context.packageName, 0, 0, 0, 0, true, Bundle.EMPTY, true
        )
        val callback = PlaybackSessionCallback {}

        assertFalse(callback.onConnectAsync(session, stranger).get().availableSessionCommands.contains(FinishCommand.COMMAND))
        assertTrue(callback.onConnectAsync(session, own).get().availableSessionCommands.contains(FinishCommand.COMMAND))
        assertEquals(
            SessionError.ERROR_PERMISSION_DENIED,
            callback.onCustomCommand(session, stranger, FinishCommand.COMMAND, FinishCommand.args(true)).get().resultCode
        )
        session.release()
    }

    // #105: the details screen marks through the service too; signed out, it hears no.
    @Test
    fun `marking from elsewhere is answered`() {
        val heard = mutableListOf<Boolean>()

        Playback.markFinished(context, itemId = "b9", finished = true) { heard += it }

        runMainLooperUntil { heard.isNotEmpty() }
        assertEquals(listOf(false), heard)
    }

    // Review (#107): the remote's rewind and fast-forward reach the session's
    // player, which must follow the lengths set in Settings.
    @Test
    fun `the session's player skips by the lengths set in Settings`() {
        PlayerSettings(context).skipBackSeconds = 10
        PlayerSettings(context).skipForwardSeconds = 60
        queued(connect(), startAt = 600.0)
        val player = service.get().sessionPlayer!!

        assertEquals(10_000L, player.seekBackIncrement)
        player.seekBack()
        assertEquals(590_000L, player.currentPosition)
        player.seekForward()
        assertEquals(650_000L, player.currentPosition)
    }
}
