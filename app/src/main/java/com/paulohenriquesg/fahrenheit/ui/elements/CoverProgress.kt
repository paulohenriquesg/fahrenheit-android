package com.paulohenriquesg.fahrenheit.ui.elements

import com.paulohenriquesg.fahrenheit.api.LibraryItem
import com.paulohenriquesg.fahrenheit.api.MediaProgressResponse
import com.paulohenriquesg.fahrenheit.player.PlaybackPosition

/**
 * How far into each item the listener is, for the bar along a cover (#104).
 *
 * The personalized shelves carry no progress; GET /api/me does, for books and
 * episodes alike. A card standing for an episode reads that episode's progress,
 * never its podcast's.
 */
class CoverProgress private constructor(private val started: Map<String, Started>) {

    /** Played some way into and not finished. */
    data class Started(val fraction: Float, val secondsLeft: Double)

    fun of(item: LibraryItem): Started? {
        val episode = item.recentEpisode
        val key = if (episode != null) key(episode.libraryItemId ?: item.id, episode.id) else key(item.id, null)
        return started[key]
    }

    companion object {
        val None = CoverProgress(emptyMap())

        fun index(progress: List<MediaProgressResponse>): CoverProgress = CoverProgress(
            progress.mapNotNull { p ->
                val itemId = p.libraryItemId ?: return@mapNotNull null
                started(p)?.let { key(itemId, p.episodeId) to it }
            }.toMap()
        )

        private fun key(itemId: String, episodeId: String?) = if (episodeId == null) itemId else "$itemId/$episodeId"

        private fun started(p: MediaProgressResponse): Started? {
            if (p.isFinished == true) return null
            val at = p.currentTime ?: return null
            val duration = p.duration ?: return null
            if (at <= 0 || duration <= 0) return null
            return Started(
                fraction = PlaybackPosition.fraction(at, duration),
                secondsLeft = PlaybackPosition.left(at, duration)
            )
        }
    }
}
