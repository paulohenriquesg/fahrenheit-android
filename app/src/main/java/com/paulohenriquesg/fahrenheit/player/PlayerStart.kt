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
 * @param startAt where the screen that opened the player asked to start, in
 *   whole-book seconds - a chapter chosen on the details screen (#105). It wins
 *   over the saved position, and moves a book already queued. Honoured once.
 */
class PlayerStart(private val autoPlay: Boolean, private val startAt: Double? = null) {

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
    /**
     * @param next with auto-advance on (#108), the episode to queue after this
     *   one - and, reattaching to this one, behind it if it is not there yet.
     */
    suspend fun begin(
        player: Player,
        nowPlaying: NowPlaying,
        progress: suspend () -> MediaProgressResponse?,
        resolveUrl: (String) -> String?,
        next: NowPlaying? = null
    ): Boolean {
        val firstTime = !started
        started = true
        val asked = startAt?.takeIf { firstTime }
        if (QueuedFile.of(player.currentMediaItem)?.isFor(nowPlaying.itemId, nowPlaying.episodeId) == true) {
            dropWhatHasPlayed(player, nowPlaying)
            val timeline = nowPlaying.timeline
            if (asked != null && timeline != null) {
                BookPlayback(player, timeline).seekToBookTime(asked)
                if (autoPlay) player.play()
            }
            if (next != null) queueBehind(player, next, resolveUrl)
            return true
        }
        val start = asked ?: ResumePoint.decide(progress(), nowPlaying.trackTotal, nowPlaying.mediaDuration).positionSeconds
        val queue = PlaybackQueue.of(nowPlaying, start, resolveUrl, next) ?: return false
        player.setMediaItems(queue.items, queue.index, queue.positionMs)
        player.prepare()
        // Explicitly either way: the player keeps playWhenReady across a new
        // queue, so what replaced a playing book would otherwise start too.
        if (autoPlay && firstTime) player.play() else player.pause()
        return true
    }

    /**
     * After a move to the next episode (#108), what played before it is still
     * at the front of the queue: drop it, so this one is first again and the
     * screen's timeline maps onto the queue.
     */
    private fun dropWhatHasPlayed(player: Player, nowPlaying: NowPlaying) {
        val first = (0 until player.mediaItemCount).firstOrNull {
            QueuedFile.of(player.getMediaItemAt(it))?.isFor(nowPlaying.itemId, nowPlaying.episodeId) == true
        } ?: return
        if (first > 0) player.removeMediaItems(0, first)
    }

    /** Puts [next] after what plays, unless it is queued already: the screen follows a move and asks again. */
    private fun queueBehind(player: Player, next: NowPlaying, resolveUrl: (String) -> String?) {
        val queued = (0 until player.mediaItemCount).any {
            QueuedFile.of(player.getMediaItemAt(it))?.isFor(next.itemId, next.episodeId) == true
        }
        if (queued) return
        PlaybackQueue.itemsOf(next, resolveUrl)?.let { player.addMediaItems(it) }
    }
}
