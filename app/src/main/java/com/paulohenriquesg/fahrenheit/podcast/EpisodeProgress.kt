package com.paulohenriquesg.fahrenheit.podcast

import com.paulohenriquesg.fahrenheit.api.MediaProgressResponse

/**
 * Whether an episode has been heard, or how far into it the listener is (#78).
 *
 * Progress is in no part of the item or recent-episodes responses; GET /api/me
 * carries it for everything the user has touched, books and episodes alike.
 */
sealed interface EpisodeProgress {
    data object Heard : EpisodeProgress
    data class InProgress(val fraction: Double, val secondsLeft: Double) : EpisodeProgress

    companion object {

        /** This podcast's episodes, by episode id. Opened-but-unplayed ones are left out. */
        fun index(progress: List<MediaProgressResponse>, podcastId: String): Map<String, EpisodeProgress> =
            progress.filter { it.libraryItemId == podcastId && it.episodeId != null }
                .mapNotNull { p -> of(p)?.let { p.episodeId!! to it } }
                .toMap()

        /**
         * The episode to resume: the one played most recently and not finished,
         * among those the server still holds.
         */
        fun resumable(progress: List<MediaProgressResponse>, podcastId: String, onServer: Set<String>): String? =
            progress.filter { it.libraryItemId == podcastId && it.episodeId in onServer && of(it) is InProgress }
                .maxByOrNull { it.lastUpdate ?: 0L }
                ?.episodeId

        private fun of(p: MediaProgressResponse): EpisodeProgress? {
            if (p.isFinished == true) return Heard
            val at = p.currentTime ?: return null
            val duration = p.duration ?: return null
            if (at <= 0 || duration <= 0) return null
            return InProgress(fraction = at / duration, secondsLeft = (duration - at).coerceAtLeast(0.0))
        }
    }
}
