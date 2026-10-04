package com.paulohenriquesg.fahrenheit.player

import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.Timeline
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.drm.DrmSessionManagerProvider
import androidx.media3.exoplayer.source.ForwardingTimeline
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.exoplayer.source.WrappingMediaSource
import androidx.media3.exoplayer.upstream.LoadErrorHandlingPolicy

/**
 * When playback moves on to the next episode (#108), it starts where that
 * episode was left ([QueuedFile.startAt]) from its first rendered moment
 * (#171) - in the service, so with no screen open too. Next from the episode
 * before lands there as well.
 *
 * ExoPlayer starts an item it moves on to at its window's default position,
 * so each such file gets the saved place as that: nothing is clipped, so
 * positions, reports and seeking back read as for any other item. A place
 * past a file's end ends it at once, so an episode left in a later file
 * passes straight through the earlier ones. Once arrived, [SavedPlaceSpent]
 * sets the place back to the start.
 */
@OptIn(UnstableApi::class)
class StartWhereLeft(private val delegate: MediaSource.Factory) : MediaSource.Factory by delegate {
    override fun createMediaSource(mediaItem: MediaItem): MediaSource {
        val source = delegate.createMediaSource(mediaItem)
        return if (QueuedFile.of(mediaItem) == null) source else StartingAt(source, mediaItem)
    }

    // Set on the factory underneath, but still this one: chaining keeps the wrapper.
    override fun setDrmSessionManagerProvider(provider: DrmSessionManagerProvider): MediaSource.Factory =
        apply { delegate.setDrmSessionManagerProvider(provider) }

    override fun setLoadErrorHandlingPolicy(policy: LoadErrorHandlingPolicy): MediaSource.Factory =
        apply { delegate.setLoadErrorHandlingPolicy(policy) }

    private class StartingAt(source: MediaSource, private var item: MediaItem) : WrappingMediaSource(source) {
        private var childTimeline: Timeline? = null

        override fun getInitialTimeline(): Timeline? = super.getInitialTimeline()?.let(::startingAt)

        override fun onChildSourceInfoRefreshed(newTimeline: Timeline) {
            childTimeline = newTimeline
            refreshSourceInfo(startingAt(newTimeline))
        }

        override fun getMediaItem(): MediaItem = item

        /**
         * The place spent ([SavedPlaceSpent]) changes only the facts the item
         * carries, which equality leaves out: the same file, so it keeps
         * playing, from a new default - whether or not the source underneath
         * can be updated.
         */
        override fun canUpdateMediaItem(mediaItem: MediaItem): Boolean =
            mediaItem == item || super.canUpdateMediaItem(mediaItem)

        override fun updateMediaItem(mediaItem: MediaItem) {
            if (super.canUpdateMediaItem(mediaItem)) super.updateMediaItem(mediaItem)
            item = mediaItem
            childTimeline?.let { refreshSourceInfo(startingAt(it)) }
        }

        private fun startingAt(timeline: Timeline): Timeline {
            val file = QueuedFile.of(item)
            val startUs = file?.takeIf { it.startAt > 0.0 }?.let { ((it.startAt - it.startOffset) * 1_000_000).toLong() }
            val current = item
            return object : ForwardingTimeline(timeline) {
                override fun getWindow(windowIndex: Int, window: Window, defaultPositionProjectionUs: Long): Window {
                    super.getWindow(windowIndex, window, defaultPositionProjectionUs)
                    window.mediaItem = current
                    if (startUs != null && startUs > 0) {
                        window.defaultPositionUs =
                            if (window.durationUs == C.TIME_UNSET) startUs else minOf(startUs, window.durationUs)
                    }
                    return window
                }
            }
        }
    }
}

/**
 * The saved place is for arriving once: on reaching the file that holds it,
 * every file of that episode starts at its beginning again - so Play after
 * the end, Previous back into it, or playing on from an earlier file never
 * jumps to where it was once left. Files passed through on the way there
 * leave it be.
 */
class SavedPlaceSpent(private val player: Player) : Player.Listener {
    override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
        val arrived = QueuedFile.of(mediaItem)?.takeIf { it.startAt > 0.0 } ?: return
        val files = (0 until player.mediaItemCount).filter {
            QueuedFile.of(player.getMediaItemAt(it))?.let { f -> f.startAt > 0.0 && f.isFor(arrived.itemId, arrived.episodeId) } == true
        }
        val holding = files.lastOrNull { QueuedFile.of(player.getMediaItemAt(it))!!.startOffset <= arrived.startAt }
        if (holding != null && player.currentMediaItemIndex < holding) return
        for (index in files) {
            val item = player.getMediaItemAt(index)
            val spent = QueuedFile.of(item)!!.copy(startAt = 0.0)
            player.replaceMediaItem(
                index,
                item.buildUpon()
                    .setRequestMetadata(item.requestMetadata.buildUpon().setExtras(spent.toBundle()).build())
                    .build()
            )
        }
    }
}
