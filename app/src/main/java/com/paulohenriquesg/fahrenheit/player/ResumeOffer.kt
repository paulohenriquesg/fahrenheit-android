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

        /** A server position this close to what this player wrote is that write. */
        private const val SAME_WRITE_SECONDS = 1.0

        /** For the TV's clock against the server's: room for them to disagree. */
        private const val CLOCK_MARGIN_MS = 60_000L

        /**
         * Whether to ask, and about what.
         *
         * Since #16 the player keeps its place while the app is away, so
         * coming back reattaches to it and the next report saves it over
         * whatever was heard on a phone meanwhile. Asked only when the
         * server's copy is newer than anything this player knows, far enough
         * away to matter, and not written by this device.
         *
         * @param known what this player last knew of the server's copy; null
         *   when it never did, which counts the server's as newer.
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
            if (!writtenSince(known, there, listenedAt)) return null
            if (abs(there - here) <= THRESHOLD_SECONDS) return null
            if (latestDevice == thisDevice) return null
            return ResumeOffer(here, there, listenedAt)
        }

        /**
         * Whether the server's copy was written after what this player knows
         * of it - server time against server time where there is a time (#145).
         */
        private fun writtenSince(known: KnownProgress?, there: Double, listenedAt: Long): Boolean = when (known) {
            null -> true
            is KnownProgress.ServerCopy -> listenedAt > known.lastUpdate
            // The server keeps the last write: a different position came after
            // this player's, whatever either clock says.
            is KnownProgress.Wrote -> abs(there - known.position) > SAME_WRITE_SECONDS
            is KnownProgress.Since -> listenedAt > known.deviceTime + CLOCK_MARGIN_MS
        }
    }
}
