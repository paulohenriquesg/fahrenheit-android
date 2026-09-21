package com.paulohenriquesg.fahrenheit.player

import androidx.media3.common.MediaItem
import androidx.media3.common.Player

/**
 * A book played as one thing, however many files it is stored in.
 *
 * Audiobookshelf reports positions, chapters and progress in whole-book time,
 * while a player works in one file at a time. Everything crossing that line
 * goes through the book's [TrackTimeline], so the two cannot drift: loading
 * only the first file is why a book in parts stops at the end of part one, and
 * why a saved position beyond it plays nothing at all (#16).
 *
 * @param resolveUrl turns a track's server path into a URL the player can
 *   fetch, including whatever authentication the server wants.
 */
class BookPlayback(
    private val player: Player,
    private val timeline: TrackTimeline,
    private val resolveUrl: (String) -> String
) {

    /** Queues every file of the book and positions it at [startAtBookTime]. */
    fun load(startAtBookTime: Double) {
        val items = (0 until timeline.size).map { index ->
            MediaItem.fromUri(resolveUrl(timeline.track(index).contentUrl))
        }
        val at = timeline.locate(startAtBookTime)
        player.setMediaItems(items, at.trackIndex, (at.positionInTrack * 1000).toLong())
    }

    /** Moves to a whole-book time, crossing into another file if it falls there. */
    fun seekToBookTime(seconds: Double) {
        val at = timeline.locate(seconds)
        player.seekTo(at.trackIndex, (at.positionInTrack * 1000).toLong())
    }

    /** Where playback is, in whole-book time - what progress sync must report. */
    fun bookPosition(): Double =
        timeline.bookTime(player.currentMediaItemIndex, player.currentPosition / 1000.0)
}
