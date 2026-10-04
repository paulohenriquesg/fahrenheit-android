package com.paulohenriquesg.fahrenheit.player

import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import com.paulohenriquesg.fahrenheit.api.LibraryItemResponse
import com.paulohenriquesg.fahrenheit.api.MediaProgressResponse
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * With "Play the next episode automatically" on (#108), each move to the next
 * episode queues the one after it - in the service, so with no screen open
 * too (#160). The player screen queues the first one, and still does when it
 * is open; whichever comes second finds it there. Main thread only: the check
 * and the add happen together, so the two never double up.
 *
 * With the setting off, a next episode queued while it was on is dropped.
 *
 * @param enabled read at each move: the setting may change while playing.
 * @param nextOf the episode after the one in [QueuedFile], as queue items;
 *   null for none.
 */
class NextEpisodeQueue(
    private val player: Player,
    private val scope: CoroutineScope,
    private val enabled: () -> Boolean,
    private val nextOf: suspend (QueuedFile) -> List<MediaItem>?
) : Player.Listener {
    override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
        if (reason != Player.MEDIA_ITEM_TRANSITION_REASON_AUTO) return
        val file = QueuedFile.of(mediaItem)?.takeIf { it.episodeId != null } ?: return
        val behind = firstOtherBehind(file)
        if (!enabled()) {
            behind?.let { player.removeMediaItems(it, player.mediaItemCount) }
            return
        }
        if (behind != null) return
        scope.launch {
            val next = nextOf(file)?.takeIf { it.isNotEmpty() } ?: return@launch
            // The queue may have moved on, or been replaced, while asking.
            if (QueuedFile.of(player.currentMediaItem) != file || firstOtherBehind(file) != null) return@launch
            player.addMediaItems(next)
        }
    }

    /** Where another item starts after [file]'s, which is playing; null when nothing follows it. */
    private fun firstOtherBehind(file: QueuedFile): Int? =
        (player.currentMediaItemIndex + 1 until player.mediaItemCount).firstOrNull {
            QueuedFile.of(player.getMediaItemAt(it))?.isFor(file.itemId, file.episodeId) != true
        }
}

/** What plays after an episode, when playback moves on by itself (#108, #160). */
object UpNext {
    /**
     * The next newer episode the server has audio for ([EpisodeNeighbours]),
     * as queue items starting where it was left ([ResumeOnArrival] seeks
     * there on arrival); null at the newest, or for an episode the podcast no
     * longer has.
     */
    suspend fun after(
        item: LibraryItemResponse,
        episodeId: String,
        progress: suspend (itemId: String, episodeId: String) -> MediaProgressResponse?,
        resolveUrl: (String) -> String?
    ): List<MediaItem>? {
        val ref = EpisodeNeighbours.of(item.media.episodes.orEmpty(), episodeId).next ?: return null
        val next = NowPlaying.of(item, ref.id, System.currentTimeMillis()) ?: return null
        val startAt = ResumePoint.decide(progress(item.id, ref.id), next.trackTotal, next.mediaDuration).positionSeconds
        return PlaybackQueue.itemsOf(next, resolveUrl, startAt)
    }
}
