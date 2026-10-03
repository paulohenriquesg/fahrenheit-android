package com.paulohenriquesg.fahrenheit.podcast

import com.paulohenriquesg.fahrenheit.api.RecentPodcastEpisode
import com.paulohenriquesg.fahrenheit.utils.EpisodeDate
import kotlin.math.roundToInt

object EpisodeRowDisplay {

    /**
     * What a screen reader should say for the cover on an episode row.
     *
     * The row already shows the episode's own title, so the cover names the
     * podcast it belongs to; a list of recent episodes is a list of different
     * podcasts, and that is the part a cover tells you at a glance.
     */
    /** When the episode came out, as the row shows it. */
    fun published(
        episode: RecentPodcastEpisode,
        now: Long = System.currentTimeMillis(),
        serverFormat: String? = null
    ): String = EpisodeDate.of(episode.publishedAt, now, serverFormat)

    fun coverDescription(episode: RecentPodcastEpisode): String =
        episode.podcast?.metadata?.title?.takeIf { it.isNotBlank() }
            ?: episode.title?.takeIf { it.isNotBlank() }
            ?: "Podcast cover"

    /**
     * Whether the row is marked New (#109): out in the last day or two, the
     * Today and Yesterday groups, and not yet touched. Once started or heard,
     * the row says that instead.
     */
    fun isNew(episode: RecentPodcastEpisode, progress: EpisodeProgress?, now: Long): Boolean {
        if (progress != null) return false
        val publishedAt = episode.publishedAt?.takeIf { it > 0 } ?: return false
        return now - publishedAt < NEW_FOR_MS
    }

    /** How far in, as the whole percentage the row's "38% in" shows. */
    fun percentIn(progress: EpisodeProgress.InProgress): Int =
        (progress.fraction * 100).roundToInt().coerceIn(0, 100)

    private const val NEW_FOR_MS = 2 * 24 * 60 * 60 * 1000L
}
