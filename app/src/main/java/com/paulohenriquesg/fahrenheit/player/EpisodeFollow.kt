package com.paulohenriquesg.fahrenheit.player

/**
 * Which episode the player screen shows once the queue has moved on by
 * itself (#108: auto-advance): the one now playing, when it is another
 * episode of the same show; otherwise the one it was showing. A book never
 * follows.
 */
object EpisodeFollow {
    fun shown(playing: QueuedFile?, itemId: String, episodeId: String?): String? {
        if (episodeId == null || playing == null) return episodeId
        val moved = playing.itemId == itemId && playing.episodeId != null && playing.episodeId != episodeId
        return if (moved) playing.episodeId else episodeId
    }
}
