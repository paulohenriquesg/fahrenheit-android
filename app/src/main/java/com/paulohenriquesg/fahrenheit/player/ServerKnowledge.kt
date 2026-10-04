package com.paulohenriquesg.fahrenheit.player

/**
 * What this player last knew of the server's copy of an item, in a form that
 * can be compared without setting the TV's clock against the server's (#145).
 */
sealed interface KnownProgress {
    /** The server's copy was read: its own `lastUpdate`, on the server's clock. */
    data class ServerCopy(val lastUpdate: Long) : KnownProgress

    /** A report of this player's reached the server, at this position (whole-book seconds). */
    data class Wrote(val position: Double) : KnownProgress

    /** Neither read nor written - a start chosen on the details screen - as of this device's clock. */
    data class Since(val deviceTime: Long) : KnownProgress
}

/**
 * What this player last knew of the server's copy of each item (#90): it
 * queued the item from the server's position, a report of its own reached
 * the server, or it started where it was asked. A server position written
 * after that was written by someone else.
 *
 * The latest recorded event wins, whatever its kind: the times of different
 * kinds do not compare. The service and the screen record independently, so
 * a read answered late can follow a report it predates; that errs towards a
 * later lastUpdate looking newer, and the 30 s threshold and the device check
 * still stand. In memory, for the process: the queue it describes lives no
 * longer.
 */
class ServerKnowledge {
    private val known = mutableMapOf<String, KnownProgress>()

    @Synchronized
    fun read(itemId: String, episodeId: String?, lastUpdate: Long) {
        known[key(itemId, episodeId)] = KnownProgress.ServerCopy(lastUpdate)
    }

    @Synchronized
    fun wrote(itemId: String, episodeId: String?, position: Double) {
        known[key(itemId, episodeId)] = KnownProgress.Wrote(position)
    }

    @Synchronized
    fun since(itemId: String, episodeId: String?, deviceTime: Long) {
        known[key(itemId, episodeId)] = KnownProgress.Since(deviceTime)
    }

    @Synchronized
    fun known(itemId: String, episodeId: String?): KnownProgress? = known[key(itemId, episodeId)]

    private fun key(itemId: String, episodeId: String?) = "$itemId/${episodeId.orEmpty()}"

    companion object {
        /** The player screen and the playback service share one. */
        val process = ServerKnowledge()
    }
}
