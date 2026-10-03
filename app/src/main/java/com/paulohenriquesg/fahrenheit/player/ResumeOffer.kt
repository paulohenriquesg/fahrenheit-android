package com.paulohenriquesg.fahrenheit.player

import com.paulohenriquesg.fahrenheit.api.MediaProgressResponse

data class ResumeOffer(val here: Double, val there: Double, val listenedAt: Long) {
    companion object {
        fun of(
            here: Double,
            server: MediaProgressResponse?,
            knownAt: Long?,
            latestDevice: String?,
            thisDevice: String
        ): ResumeOffer? = null
    }
}
