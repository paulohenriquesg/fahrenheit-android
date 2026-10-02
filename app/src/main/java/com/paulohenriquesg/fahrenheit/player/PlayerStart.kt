package com.paulohenriquesg.fahrenheit.player

import androidx.media3.common.Player
import com.paulohenriquesg.fahrenheit.api.MediaProgressResponse

/** What the player screen does with the player it connects to. */
object PlayerStart {

    /**
     * Reattaches if [player] is already on this book or episode - leaving it
     * where it is, since what is playing is newer than the server's copy -
     * and otherwise queues it at the saved position, playing if [autoPlay].
     *
     * @return false when there is nothing to play; the queue is then untouched.
     */
    fun begin(
        player: Player,
        nowPlaying: NowPlaying,
        progress: MediaProgressResponse?,
        autoPlay: Boolean,
        resolveUrl: (String) -> String?
    ): Boolean {
        if (QueuedFile.of(player.currentMediaItem)?.isFor(nowPlaying.itemId, nowPlaying.episodeId) == true) return true
        val start = ResumePoint.decide(progress, nowPlaying.trackTotal, nowPlaying.mediaDuration)
        val queue = PlaybackQueue.of(nowPlaying, start.positionSeconds, resolveUrl) ?: return false
        player.setMediaItems(queue.items, queue.index, queue.positionMs)
        player.prepare()
        if (autoPlay) player.play()
        return true
    }
}
