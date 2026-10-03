package com.paulohenriquesg.fahrenheit.player

import android.os.Bundle
import androidx.media3.common.Player
import androidx.media3.session.SessionCommand
import kotlin.math.ceil

/** What the sleep timer shows on the chip: the choice, and whole minutes of listening left. */
data class SleepState(val choice: SleepChoice, val minutesLeft: Int)

/**
 * The sleep timer's way across the session: the custom command a screen
 * sends to set it, and the session extras the service reports it in (#107).
 *
 * End of chapter carries the chapter ends in whole-book seconds, so the
 * service still needs to know nothing about books.
 */
object SleepCommand {
    val COMMAND = SessionCommand("fahrenheit.SLEEP", Bundle.EMPTY)

    private const val MINUTES = "fahrenheit.sleep.minutes"
    private const val CHAPTER_ENDS = "fahrenheit.sleep.chapterEnds"
    private const val LEFT = "fahrenheit.sleep.minutesLeft"

    fun args(choice: SleepChoice, chapterEnds: List<Double> = emptyList()): Bundle = Bundle().apply {
        when (choice) {
            SleepChoice.Off -> Unit
            SleepChoice.EndOfChapter -> putDoubleArray(CHAPTER_ENDS, chapterEnds.toDoubleArray())
            is SleepChoice.Minutes -> putInt(MINUTES, choice.minutes)
        }
    }

    fun choiceOf(args: Bundle): SleepChoice = when {
        args.containsKey(MINUTES) -> SleepChoice.Minutes(args.getInt(MINUTES))
        args.containsKey(CHAPTER_ENDS) -> SleepChoice.EndOfChapter
        else -> SleepChoice.Off
    }

    fun chapterEndsOf(args: Bundle): List<Double> = args.getDoubleArray(CHAPTER_ENDS)?.toList().orEmpty()

    fun extras(state: SleepState?): Bundle = state?.let { args(it.choice).apply { putInt(LEFT, it.minutesLeft) } } ?: Bundle()

    /** What the extras report; null when no timer runs. */
    fun state(extras: Bundle): SleepState? {
        if (!extras.containsKey(LEFT)) return null
        val choice = if (extras.containsKey(MINUTES)) SleepChoice.Minutes(extras.getInt(MINUTES)) else SleepChoice.EndOfChapter
        return SleepState(choice, extras.getInt(LEFT))
    }
}

/**
 * The sleep timer, run on the session's player so it works with no screen
 * open (#107). It counts the time [player] is playing, retargets End of
 * chapter after a seek, and pauses when the timer runs out - an ordinary
 * pause, so the closing progress report goes out as on any other.
 *
 * Install as a listener on the player; call [check] while [running], every
 * [nextCheckMs]. What is left is handed to [publish] whenever the chip's text
 * would change.
 *
 * @param now a monotonic clock in milliseconds.
 */
class SleepWatch(
    private val player: Player,
    private val now: () -> Long,
    private val publish: (Bundle) -> Unit
) : Player.Listener {

    private val timer = SleepTimer()
    private var playingSince: Long? = null
    private var shown: SleepState? = null

    val running: Boolean get() = timer.choice != SleepChoice.Off

    fun set(args: Bundle) {
        account()
        when (val choice = SleepCommand.choiceOf(args)) {
            SleepChoice.Off -> timer.off()
            SleepChoice.EndOfChapter -> timer.endOfChapter(SleepCommand.chapterEndsOf(args), position())
            is SleepChoice.Minutes -> timer.minutes(choice.minutes)
        }
        playingSince = if (player.isPlaying) now() else null
        show()
    }

    fun check() {
        account()
        // At the end of the book the file can stop a fraction short of the
        // last chapter's stored end: there is nothing left to wait for.
        if (timer.due(position()) || player.playbackState == Player.STATE_ENDED) {
            timer.off()
            player.pause()
        }
        show()
    }

    /** The queue is about to be stopped or replaced: the timer was for what was playing. */
    fun beforeLeaving() {
        timer.off()
        show()
    }

    /** Sooner than a second when the chapter's end is closer than that; paused, nothing is coming closer. */
    fun nextCheckMs(): Long {
        if (!player.isPlaying) return CHECK_MS
        val left = timer.secondsLeft(position(), player.playbackParameters.speed) ?: return CHECK_MS
        return (left * 1000).toLong().coerceIn(MIN_CHECK_MS, CHECK_MS)
    }

    override fun onIsPlayingChanged(isPlaying: Boolean) {
        account()
        playingSince = if (isPlaying) now() else null
    }

    override fun onPositionDiscontinuity(oldPosition: Player.PositionInfo, newPosition: Player.PositionInfo, reason: Int) {
        if (reason != Player.DISCONTINUITY_REASON_SEEK) return
        timer.seeked(position())
        show()
    }

    private fun account() {
        val since = playingSince ?: return
        val at = now()
        timer.listened(at - since)
        playingSince = at
    }

    private fun position(): Double =
        QueuedFile.of(player.currentMediaItem)?.bookTime(player.currentPosition / 1000.0) ?: 0.0

    private fun show() {
        val state = timer.secondsLeft(position(), player.playbackParameters.speed)?.let {
            // Under a minute still reads "1 min": the timer has not gone off.
            SleepState(timer.choice, ceil(it / 60).toInt().coerceAtLeast(1))
        }
        if (state == shown) return
        shown = state
        publish(SleepCommand.extras(state))
    }

    private companion object {
        const val CHECK_MS = 1_000L
        const val MIN_CHECK_MS = 20L
    }
}
