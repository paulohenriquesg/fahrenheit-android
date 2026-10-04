package com.paulohenriquesg.fahrenheit.player

import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Timeline
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.source.ForwardingTimeline
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.exoplayer.source.WrappingMediaSource

/**
 * When playback moves on to the next episode by itself (#108), it starts
 * where that episode was left ([QueuedFile.startAt]) from its first rendered
 * moment (#171) - in the service, so with no screen open too.
 *
 * ExoPlayer starts an item it moves on to at its window's default position,
 * so each such file gets the saved place as that: nothing is clipped, so
 * positions, reports and seeking back read as for any other item. A place
 * past a file's end ends it at once, so an episode left in a later file
 * passes straight through the earlier ones.
 */
@OptIn(UnstableApi::class)
class StartWhereLeft(private val delegate: MediaSource.Factory) : MediaSource.Factory by delegate {
    override fun createMediaSource(mediaItem: MediaItem): MediaSource {
        val source = delegate.createMediaSource(mediaItem)
        val file = QueuedFile.of(mediaItem) ?: return source
        val inFileUs = ((file.startAt - file.startOffset) * 1_000_000).toLong()
        if (file.startAt <= 0.0 || inFileUs <= 0) return source
        return StartingAt(source, inFileUs)
    }

    private class StartingAt(source: MediaSource, private val startUs: Long) : WrappingMediaSource(source) {
        override fun getInitialTimeline(): Timeline? = super.getInitialTimeline()?.let(::startingAt)

        override fun onChildSourceInfoRefreshed(newTimeline: Timeline) = refreshSourceInfo(startingAt(newTimeline))

        private fun startingAt(timeline: Timeline): Timeline = object : ForwardingTimeline(timeline) {
            override fun getWindow(windowIndex: Int, window: Window, defaultPositionProjectionUs: Long): Window {
                super.getWindow(windowIndex, window, defaultPositionProjectionUs)
                window.defaultPositionUs =
                    if (window.durationUs == C.TIME_UNSET) startUs else minOf(startUs, window.durationUs)
                return window
            }
        }
    }
}
