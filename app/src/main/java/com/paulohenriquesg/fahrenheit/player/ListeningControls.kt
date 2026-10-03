package com.paulohenriquesg.fahrenheit.player

import android.os.Bundle
import androidx.media3.common.Player
import androidx.media3.session.SessionCommand
import com.paulohenriquesg.fahrenheit.api.Chapter

/**
 * What the player screen does when Speed or Sleep is chosen (#107).
 *
 * Speed is set on [player] and remembered for [itemId]. The sleep timer
 * lives in the playback service, so a choice is [send]t to it as a
 * [SleepCommand], with the chapter ends in whole-book time for End of chapter.
 */
class ListeningControls(
    private val player: Player,
    private val itemId: String,
    chapters: List<Chapter>?,
    total: Double,
    private val memory: SpeedMemory,
    private val send: (SessionCommand, Bundle) -> Unit
) {
    private val chapterEnds = ChapterClock.spans(chapters, total).map { it.end }

    val hasChapters: Boolean get() = chapterEnds.isNotEmpty()

    /** The service's player keeps the last book's speed: each book starts at its own. */
    fun applyRememberedSpeed() = player.setPlaybackSpeed(memory.of(itemId))

    fun chooseSpeed(speed: Float) {
        memory.remember(itemId, speed)
        player.setPlaybackSpeed(speed)
    }

    fun chooseSleep(choice: SleepChoice) = send(SleepCommand.COMMAND, SleepCommand.args(choice, chapterEnds))
}
