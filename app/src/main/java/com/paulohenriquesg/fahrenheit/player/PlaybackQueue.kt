package com.paulohenriquesg.fahrenheit.player

import androidx.core.net.toUri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata

/** The items to queue, which one to start in, and how far into it. */
data class QueueStart(val items: List<MediaItem>, val index: Int, val positionMs: Long)

/**
 * A book or an episode as a Media3 queue: one item per file, in book order.
 *
 * Each item carries its file's URI twice. As the item's own URI it plays in a
 * player in this process; as request metadata it survives a MediaController,
 * which strips the first (see PlayableItems).
 */
object PlaybackQueue {

    /**
     * @param startAt where to begin, in whole-book seconds.
     * @param resolveUrl a track's server path to a full URL; null when there is
     *   no server to resolve against.
     * @param next an episode to play after this one (#108), queued as itself.
     * @return null when there is nothing to play.
     */
    // next before resolveUrl: callers pass resolveUrl as a trailing lambda.
    fun of(nowPlaying: NowPlaying, startAt: Double, next: NowPlaying? = null, resolveUrl: (String) -> String?): QueueStart? {
        val timeline = nowPlaying.timeline ?: return null
        val items = itemsOf(nowPlaying, resolveUrl) ?: return null
        val after = next?.let { itemsOf(it, resolveUrl) }.orEmpty()
        val at = timeline.locate(startAt)
        return QueueStart(items + after, at.trackIndex, (at.positionInTrack * 1000).toLong())
    }

    /** One item per file of [nowPlaying], each carrying its own [QueuedFile]; null when there is nothing to play. */
    fun itemsOf(nowPlaying: NowPlaying, resolveUrl: (String) -> String?): List<MediaItem>? {
        val timeline = nowPlaying.timeline ?: return null
        return (0 until timeline.size).map { index ->
            val track = timeline.track(index)
            val url = resolveUrl(track.contentUrl) ?: return null
            val file = QueuedFile(
                itemId = nowPlaying.itemId,
                episodeId = nowPlaying.episodeId,
                startOffset = track.startOffset,
                bookTotal = timeline.totalDuration
            )
            MediaItem.Builder()
                .setMediaId("${nowPlaying.itemId}/${nowPlaying.episodeId.orEmpty()}/$index")
                .setUri(url)
                .setRequestMetadata(
                    MediaItem.RequestMetadata.Builder()
                        .setMediaUri(url.toUri())
                        .setExtras(file.toBundle())
                        .build()
                )
                .setMediaMetadata(MediaMetadata.Builder().setTitle(nowPlaying.title).build())
                .build()
        }
    }
}
