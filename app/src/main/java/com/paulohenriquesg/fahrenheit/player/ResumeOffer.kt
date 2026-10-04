package com.paulohenriquesg.fahrenheit.player

import com.paulohenriquesg.fahrenheit.api.MediaProgressResponse
import kotlin.math.abs

/**
 * An item's latest listening session: who wrote it, and when (server clock,
 * ms); [deviceName] is what the question calls that device (#158).
 */
data class LatestSession(val deviceId: String, val updatedAt: Long, val deviceName: String? = null)

/**
 * A question worth asking (#90): this player is at [here], and the server
 * holds [there], heard elsewhere at [listenedAt] (the server's clock, ms).
 * [device] names where, when a session says so; null is "elsewhere" (#158).
 */
data class ResumeOffer(val here: Double, val there: Double, val listenedAt: Long, val device: String? = null) {
    companion object {
        /** Decided on #90: a flat 30 seconds, whatever the length of the book. */
        private const val THRESHOLD_SECONDS = 30.0

        /** A server position this close to what this player wrote is that write. */
        private const val SAME_WRITE_SECONDS = 1.0

        /** For the TV's clock against the server's: room for them to disagree. */
        private const val CLOCK_MARGIN_MS = 60_000L

        /** A sync writes its session and the progress together, a moment apart. */
        private const val SESSION_SLACK_MS = 10_000L

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
         * @param latestSession the item's latest listening session; null when
         *   the sessions could not be read, which asks.
         */
        fun of(
            here: Double,
            server: MediaProgressResponse?,
            known: KnownProgress?,
            latestSession: LatestSession?,
            thisDevice: String
        ): ResumeOffer? {
            // Unreadable, never started or finished: nothing to choose between.
            if (server == null || server.isFinished == true) return null
            val there = server.currentTime ?: return null
            val listenedAt = server.lastUpdate ?: return null
            if (!writtenSince(known, there, listenedAt)) return null
            if (abs(there - here) <= THRESHOLD_SECONDS) return null
            if (writtenHere(latestSession, thisDevice, listenedAt)) return null
            // Named only when that session moved it: one older than the copy
            // did not, whoever's it is.
            val device = latestSession
                ?.takeIf { it.deviceId != thisDevice && wroteCopy(it, listenedAt) }
                ?.deviceName
            return ResumeOffer(here, there, listenedAt, device)
        }

        /**
         * Whether this device wrote the server's copy: its session is the
         * latest, and as recent as the copy. Progress written without a
         * session - a web-UI edit, an app using the plain PATCH - leaves this
         * TV's older session the latest, and is someone else's (#144).
         */
        private fun writtenHere(latest: LatestSession?, thisDevice: String, listenedAt: Long): Boolean =
            latest != null && latest.deviceId == thisDevice && wroteCopy(latest, listenedAt)

        /** Whether [session] is as recent as the copy, so wrote it. */
        private fun wroteCopy(session: LatestSession, listenedAt: Long): Boolean =
            session.updatedAt >= listenedAt - SESSION_SLACK_MS

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
