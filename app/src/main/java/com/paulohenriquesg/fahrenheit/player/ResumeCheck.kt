package com.paulohenriquesg.fahrenheit.player

import com.paulohenriquesg.fahrenheit.api.MediaProgressResponse

class ResumeCheck(
    private val progress: suspend (itemId: String, episodeId: String?) -> MediaProgressResponse?,
    private val latestDevice: suspend (itemId: String, episodeId: String?) -> String?,
    private val thisDevice: String,
    private val knowledge: ServerKnowledge = ServerKnowledge.process
) {
    suspend fun offer(itemId: String, episodeId: String?, here: Double, playing: Boolean): ResumeOffer? = null

    fun answered(itemId: String, episodeId: String?, offer: ResumeOffer) {}
}
