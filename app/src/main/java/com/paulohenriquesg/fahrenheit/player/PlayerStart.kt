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
 * @param resumed the screen was recreated (a configuration change) rather
 *   than opened: it has started before, and follows what plays (see [follows]).
 * @param knowledge told when an item is queued from the server's position,
 *   so a later position written elsewhere can be told apart (#90).
 */
class PlayerStart(
    private val autoPlay: Boolean,
    private val startAt: Double? = null,
    resumed: Boolean = false,
    private val knowledge: ServerKnowledge = ServerKnowledge.process
) {

    private var started = resumed

    /**
     * The episode to show instead of [nowPlaying], when this screen has started
     * before and the queue has since moved on by itself to another episode of
     * the same show (#108) - with the screen closed, say. Null to begin as
     * usual: a screen opened fresh on an episode is the listener asking for it.
     */
    fun follows(player: Player, nowPlaying: NowPlaying): String? {
        if (!started) return null
        return EpisodeFollow.shown(QueuedFile.of(player.currentMediaItem), nowPlaying.itemId, nowPlaying.episodeId)
            ?.takeIf { it != nowPlaying.episodeId }
    }

    /**
     * Reattaches if [player] is already on this book or episode - leaving it
     * where it is, since what is playing is newer than the server's copy -
     * and otherwise queues it at the saved position.
     *
     * @param progress the server's saved position, asked for only when the
     *   queue is built: a copy read when the screen opened can be stale by now.
     * @param next with auto-advance on (#108), the episode to queue after this
     *   one - and, reattaching to this one, behind it if it is not there yet.
     *   Null drops a next episode queued before (the setting was turned off).
     * @param nextStartAt where [next] starts when playback moves on to it.
     * @return false when there is nothing to play; the queue is then untouched.
     */
    suspend fun begin(
        player: Player,
        nowPlaying: NowPlaying,
        progress: suspend () -> MediaProgressResponse?,
        resolveUrl: (String) -> String?,
        next: NowPlaying? = null,
        nextStartAt: Double = 0.0
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
            if (next != null) queueBehind(player, next, nextStartAt, resolveUrl) else dropWhatFollows(player, nowPlaying)
            return true
        }
        val saved = if (asked == null) progress() else null
        saved?.lastUpdate?.let { knowledge.saw(nowPlaying.itemId, nowPlaying.episodeId, it) }
        val start = asked ?: ResumePoint.decide(saved, nowPlaying.trackTotal, nowPlaying.mediaDuration).positionSeconds
        val queue = PlaybackQueue.of(nowPlaying, start, resolveUrl, next, nextStartAt) ?: return false
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

    /** Drops another episode queued after this one: auto-advance is off now. */
    private fun dropWhatFollows(player: Player, nowPlaying: NowPlaying) {
        val after = (0 until player.mediaItemCount).firstOrNull {
            val file = QueuedFile.of(player.getMediaItemAt(it))
            it > player.currentMediaItemIndex && file != null && !file.isFor(nowPlaying.itemId, nowPlaying.episodeId)
        } ?: return
        player.removeMediaItems(after, player.mediaItemCount)
    }

    /** Puts [next] after what plays, unless it is queued already: the screen follows a move and asks again. */
    private fun queueBehind(player: Player, next: NowPlaying, startAt: Double, resolveUrl: (String) -> String?) {
        val queued = (0 until player.mediaItemCount).any {
            QueuedFile.of(player.getMediaItemAt(it))?.isFor(next.itemId, next.episodeId) == true
        }
        if (queued) return
        PlaybackQueue.itemsOf(next, resolveUrl, startAt)?.let { player.addMediaItems(it) }
    }
}
