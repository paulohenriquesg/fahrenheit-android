package com.paulohenriquesg.fahrenheit.player

import com.google.common.util.concurrent.Futures
import androidx.media3.session.SessionError
import androidx.media3.session.SessionResult
import android.content.Context
import android.os.Bundle
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.SessionCommand
import androidx.media3.test.utils.TestExoPlayerBuilder
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.paulohenriquesg.fahrenheit.api.Chapter
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

/** What the player screen does when Speed or Sleep is chosen (#107). */
@RunWith(AndroidJUnit4::class)
class ListeningControlsTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val player: ExoPlayer = TestExoPlayerBuilder(context).build()
    private val sent = mutableListOf<Pair<SessionCommand, Bundle>>()
    private var answer = SessionResult(SessionResult.RESULT_SUCCESS)
    private val memory = SpeedMemory(context)

    private val chapters = listOf(
        Chapter(start = 0.0, end = 600.0, title = "One"),
        Chapter(start = 600.0, title = "Two")
    )

    private fun controls(itemId: String = "b1", chapters: List<Chapter>? = this.chapters) =
        ListeningControls(player, itemId, chapters, total = 1800.0, memory) { command, args ->
            sent += command to args
            Futures.immediateFuture(answer)
        }

    @After
    fun tearDown() = player.release()

    @Test fun `a chosen speed plays at once and is remembered for the book`() {
        controls().chooseSpeed(1.5f)
        assertEquals(1.5f, player.playbackParameters.speed)
        assertEquals(1.5f, memory.of("b1"))
    }

    // Rule 7: the service's player keeps the last book's speed.
    @Test fun `a book starts at its own speed, not the last book's`() {
        player.setPlaybackSpeed(1.75f)
        controls(itemId = "b2").applyRememberedSpeed()
        assertEquals(1f, player.playbackParameters.speed)
    }

    @Test fun `a book comes back at its remembered speed`() {
        memory.remember("b1", 1.25f)
        controls().applyRememberedSpeed()
        assertEquals(1.25f, player.playbackParameters.speed)
    }

    @Test fun `end of chapter sends the chapter ends in book time`() {
        controls().chooseSleep(SleepChoice.EndOfChapter)
        val (command, args) = sent.single()
        assertEquals(SleepCommand.COMMAND, command)
        assertEquals(SleepChoice.EndOfChapter, SleepCommand.choiceOf(args))
        assertEquals(listOf(600.0, 1800.0), SleepCommand.chapterEndsOf(args))
    }

    @Test fun `minutes send just the minutes`() {
        controls().chooseSleep(SleepChoice.Minutes(30))
        assertEquals(SleepChoice.Minutes(30), SleepCommand.choiceOf(sent.single().second))
    }

    @Test fun `end of chapter is offered only with chapters`() {
        assertEquals(true, controls().hasChapters)
        assertEquals(false, controls(chapters = null).hasChapters)
    }

    // Review: the service marks it, after the closing report (FinishMarker).
    @Test fun `mark finished asks the service, and hears whether it worked`() {
        val heard = mutableListOf<Boolean>()
        controls().markFinished(true) { heard += it }
        val (command, args) = sent.single()
        assertEquals(FinishCommand.COMMAND, command)
        assertEquals(true, FinishCommand.finishedOf(args))
        assertEquals(listOf(true), heard)
    }

    // #179: at the end the service empties the queue, with the screen still
    // showing; a mark for "whatever is queued" found nothing and failed.
    @Test fun `mark finished names the book or episode the screen shows`() {
        ListeningControls(player, "p1", null, total = 1800.0, memory, episodeId = "e1") { command, args ->
            sent += command to args
            Futures.immediateFuture(answer)
        }.markFinished(true) {}
        val args = sent.single().second
        assertEquals("p1", FinishCommand.itemOf(args))
        assertEquals("e1", FinishCommand.episodeOf(args))
    }

    @Test fun `a mark the service could not make is said`() {
        answer = SessionResult(SessionError.ERROR_UNKNOWN)
        val heard = mutableListOf<Boolean>()
        controls().markFinished(false) { heard += it }
        assertEquals(false, FinishCommand.finishedOf(sent.single().second))
        assertEquals(listOf(false), heard)
    }
}
