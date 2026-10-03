package com.paulohenriquesg.fahrenheit.player

import com.paulohenriquesg.fahrenheit.api.MediaProgressResponse
import kotlin.math.abs

/**
 * A question worth asking (#90): this player is at [here], and the server
 * holds [there], heard elsewhere at [listenedAt] (the server's clock, ms).
 */
data class ResumeOffer(val here: Double, val there: Double, val listenedAt: Long) {
    companion object {
        /** Decided on #90: a flat 30 seconds, whatever the length of the book. */
        private const val THRESHOLD_SECONDS = 30.0

        /**
         * Whether to ask, and about what.
         *
         * Since #16 the player keeps its place while the app is away, so
         * coming back reattaches to it and the next report saves it over
         * whatever was heard on a phone meanwhile. Asked only when the
         * server's copy is newer than anything this player knows, far enough
         * away to matter, and not written by this device.
         *
         * @param knownAt when this player last knew the server's state of
         *   the item; null when it never did (the queue predates this process).
         * @param latestDevice the device behind the item's latest listening
         *   session; null when the sessions could not be read, which asks.
         */
        fun of(
            here: Double,
            server: MediaProgressResponse?,
            known: KnownProgress?,
            latestDevice: String?,
            thisDevice: String
        ): ResumeOffer? {
            // Unreadable, never started or finished: nothing to choose between.
            if (server == null || server.isFinished == true) return null
            val there = server.currentTime ?: return null
            val listenedAt = server.lastUpdate ?: return null
            val knownAt = (known as? KnownProgress.ServerCopy)?.lastUpdate
            if (knownAt != null && listenedAt <= knownAt) return null
            if (abs(there - here) <= THRESHOLD_SECONDS) return null
            if (latestDevice == thisDevice) return null
            return ResumeOffer(here, there, listenedAt)
        }
    }
}
