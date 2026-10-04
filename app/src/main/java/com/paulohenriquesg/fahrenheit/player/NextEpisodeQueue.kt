package com.paulohenriquesg.fahrenheit.player

import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.Timeline
import com.paulohenriquesg.fahrenheit.api.LibraryItemResponse
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * With "Play the next episode automatically" on (#108), each move to the next
 * episode queues the one after it - in the service, so with no screen open
 * too (#160). The player screen queues the first one, and still queues it
 * when it is open, through its controller's view of the queue, which can lag
 * behind: so a second copy of an episode queued behind is dropped, whoever
 * added it.
 *
 * A change to the setting acts at once ([settingChanged]): off drops the
 * episode queued next, on queues it.
 *
 * @param enabled read at each move and change.
 * @param nextOf the episode after the one in [QueuedFile], as queue items;
 *   null for none, or when it cannot be told.
 */
class NextEpisodeQueue(
    private val player: Player,
    private val scope: CoroutineScope,
    private val enabled: () -> Boolean,
    private val nextOf: suspend (QueuedFile) -> List<MediaItem>?
) : Player.Listener {
    override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
        if (reason != Player.MEDIA_ITEM_TRANSITION_REASON_AUTO) return
        follow(QueuedFile.of(mediaItem))
    }

    override fun onTimelineChanged(timeline: Timeline, reason: Int) {
        if (reason == Player.TIMELINE_CHANGE_REASON_PLAYLIST_CHANGED) dropRepeats()
    }

    /** The setting changed: act on the episode playing now. */
    fun settingChanged() = follow(QueuedFile.of(player.currentMediaItem))

    private fun follow(playing: QueuedFile?) {
        val file = playing?.takeIf { it.episodeId != null } ?: return
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

    /**
     * Behind what plays, a file queued again after its first copy goes - the
     * same file whatever start each writer read for it. From the end, so the
     * indices still to look at do not move.
     */
    private fun dropRepeats() {
        val first = player.currentMediaItemIndex + 1
        for (index in player.mediaItemCount - 1 downTo first) {
            val file = sameFile(index) ?: continue
            if ((first until index).any { sameFile(it) == file }) player.removeMediaItem(index)
        }
    }

    private fun sameFile(index: Int): QueuedFile? = QueuedFile.of(player.getMediaItemAt(index))?.copy(startAt = 0.0)

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
     * as queue items starting where it was left ([StartWhereLeft] starts
     * it there); null at the newest, for an episode the podcast no
     * longer has, and when where it was left could not be read.
     */
    suspend fun after(
        item: LibraryItemResponse,
        episodeId: String,
        progress: suspend (itemId: String, episodeId: String) -> SavedProgress,
        resolveUrl: (String) -> String?
    ): List<MediaItem>? {
        val ref = EpisodeNeighbours.of(item.media.episodes.orEmpty(), episodeId).next ?: return null
        val next = NowPlaying.of(item, ref.id, System.currentTimeMillis()) ?: return null
        val saved = when (val read = progress(item.id, ref.id)) {
            is SavedProgress.Found -> read.progress
            SavedProgress.NeverStarted -> null
            // No one is watching to be told: starting from 0:00 would write over it.
            SavedProgress.Unreadable -> return null
        }
        val startAt = ResumePoint.decide(saved, next.trackTotal, next.mediaDuration).positionSeconds
        return PlaybackQueue.itemsOf(next, resolveUrl, startAt)
    }
}
