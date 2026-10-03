package com.paulohenriquesg.fahrenheit.player

/**
 * When this player last knew the server's state of each item (#90): it queued
 * the item from the server's position, or a report of its own reached the
 * server. A server position newer than that was written by someone else.
 *
 * In memory, for the process: the queue it describes lives no longer.
 */
sealed interface KnownProgress {
    data class ServerCopy(val lastUpdate: Long) : KnownProgress
    data class Wrote(val position: Double) : KnownProgress
    data class Since(val deviceTime: Long) : KnownProgress
}

class ServerKnowledge {
    private val known = mutableMapOf<String, Long>()

    /** [at] in milliseconds since the epoch; an earlier time than one known changes nothing. */
    @Synchronized
    fun saw(itemId: String, episodeId: String?, at: Long) {
        val key = key(itemId, episodeId)
        if (at > (known[key] ?: Long.MIN_VALUE)) known[key] = at
    }

    @Synchronized
    fun knownAt(itemId: String, episodeId: String?): Long? = known[key(itemId, episodeId)]

    fun read(itemId: String, episodeId: String?, lastUpdate: Long) {}

    fun wrote(itemId: String, episodeId: String?, position: Double) {}

    fun since(itemId: String, episodeId: String?, deviceTime: Long) {}

    fun known(itemId: String, episodeId: String?): KnownProgress? = null

    private fun key(itemId: String, episodeId: String?) = "$itemId/${episodeId.orEmpty()}"

    companion object {
        /** The player screen and the playback service share one. */
        val process = ServerKnowledge()
    }
}
