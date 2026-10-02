package com.paulohenriquesg.fahrenheit.player

import androidx.media3.common.Player
import com.paulohenriquesg.fahrenheit.api.MediaProgressResponse

/**
 * What one player screen does with the player it connects to, each time it
 * connects: on opening, and again whenever it comes back into view.
 *
 * @param autoPlay whether the screen was opened to play. Honoured once: a
 *   screen coming back to a book that is no longer queued must not start it,
 *   or the first progress report writes over wherever it was listened to since.
 */
class PlayerStart(private val autoPlay: Boolean) {

    private var started = false

    /**
     * Reattaches if [player] is already on this book or episode - leaving it
     * where it is, since what is playing is newer than the server's copy -
     * and otherwise queues it at the saved position.
     *
     * @param progress the server's saved position, asked for only when the
     *   queue is built: a copy read when the screen opened can be stale by now.
     * @return false when there is nothing to play; the queue is then untouched.
     */
    suspend fun begin(
        player: Player,
        nowPlaying: NowPlaying,
        progress: suspend () -> MediaProgressResponse?,
        resolveUrl: (String) -> String?
    ): Boolean {
        val firstTime = !started
        started = true
        if (QueuedFile.of(player.currentMediaItem)?.isFor(nowPlaying.itemId, nowPlaying.episodeId) == true) return true
        val start = ResumePoint.decide(progress(), nowPlaying.trackTotal, nowPlaying.mediaDuration)
        val queue = PlaybackQueue.of(nowPlaying, start.positionSeconds, resolveUrl) ?: return false
        player.setMediaItems(queue.items, queue.index, queue.positionMs)
        player.prepare()
        // Explicitly either way: the player keeps playWhenReady across a new
        // queue, so what replaced a playing book would otherwise start too.
        if (autoPlay && firstTime) player.play() else player.pause()
        return true
    }
}
