package com.paulohenriquesg.fahrenheit.player

import com.paulohenriquesg.fahrenheit.api.MediaProgressResponse

/**
 * Asks [ResumeOffer.of] about one item with what it needs, fetched only as
 * far as needed: the sessions are read only when everything else says ask.
 *
 * @param progress the server's progress for an item; null when unreadable.
 * @param latestDevice the device behind its latest listening session.
 * @param thisDevice this install's device id ([PlaybackDevice]).
 */
class ResumeCheck(
    private val progress: suspend (itemId: String, episodeId: String?) -> MediaProgressResponse?,
    private val latestDevice: suspend (itemId: String, episodeId: String?) -> String?,
    private val thisDevice: String,
    private val knowledge: ServerKnowledge = ServerKnowledge.process
) {
    /**
     * @param here where this player is, in whole-book seconds.
     * @param playing a player already playing is not checked: its own
     *   reports are the newest there are.
     */
    suspend fun offer(itemId: String, episodeId: String?, here: Double, playing: Boolean): ResumeOffer? {
        if (playing) return null
        val server = progress(itemId, episodeId)
        val known = knowledge.known(itemId, episodeId)
        // Unknown device asks; so this is the most that can be offered.
        ResumeOffer.of(here, server, known, latestDevice = null, thisDevice) ?: return null
        return ResumeOffer.of(here, server, known, latestDevice(itemId, episodeId), thisDevice)
    }

    /** Either answer settles that server position: it is not asked about again. */
    fun answered(itemId: String, episodeId: String?, offer: ResumeOffer) {
        knowledge.read(itemId, episodeId, offer.listenedAt)
    }
}
