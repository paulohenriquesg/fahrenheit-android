package com.paulohenriquesg.fahrenheit.podcast

import com.paulohenriquesg.fahrenheit.api.RecentPodcastEpisode
import com.paulohenriquesg.fahrenheit.utils.EpisodeDate
import com.paulohenriquesg.fahrenheit.utils.listeningLength
import com.paulohenriquesg.fahrenheit.player.PlaybackPosition
import java.util.Locale
import kotlin.math.roundToInt
import kotlin.math.roundToLong

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

    /**
     * How far in, as the whole percentage the row's "38% in" shows. Started is
     * never "0% in", which read as though nothing had happened, and unfinished
     * is never "100% in": finished episodes are marked Heard instead.
     */
    fun percentIn(progress: EpisodeProgress.InProgress): Int =
        (progress.fraction * 100).roundToInt().coerceIn(1, 99)

    /** A length or time left on the row: see [listeningLength]. */
    fun length(seconds: Double): String = listeningLength(seconds)

    private const val NEW_FOR_MS = 2 * 24 * 60 * 60 * 1000L
}
