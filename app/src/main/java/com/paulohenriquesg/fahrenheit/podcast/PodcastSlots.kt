package com.paulohenriquesg.fahrenheit.podcast

import com.paulohenriquesg.fahrenheit.auth.SessionRole

data class PodcastSlots(val feedCheck: Boolean, val downloads: Boolean) {
    companion object {
        const val TAG = "podcast_reserved_slot"
        val NONE = PodcastSlots(feedCheck = false, downloads = false)

        fun of(role: SessionRole?): PodcastSlots = NONE
    }
}
