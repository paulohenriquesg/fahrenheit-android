package com.paulohenriquesg.fahrenheit.podcast

import com.paulohenriquesg.fahrenheit.api.RecentPodcastEpisode

data class EpisodeGroup(val label: String, val episodes: List<RecentPodcastEpisode>)

/**
 * The list, under day headings.
 *
 * A flat list with no dates read as one undifferentiated pile, so a new episode
 * was invisible among old ones. The server's order within a group is kept: it
 * sorts by publishedAt already.
 */
object EpisodeGroups {

    private const val DAY = 24 * 60 * 60 * 1000L

    private enum class Bucket(val label: String) {
        Today("Today"),
        Yesterday("Yesterday"),
        ThisWeek("This week"),
        Earlier("Earlier"),
        Undated("Undated")
    }

    fun of(episodes: List<RecentPodcastEpisode>, now: Long): List<EpisodeGroup> =
        episodes.groupBy { bucket(it.publishedAt, now) }
            .toSortedMap(compareBy { it.ordinal })
            .map { (bucket, list) -> EpisodeGroup(bucket.label, list) }

    private fun bucket(publishedAt: Long?, now: Long): Bucket {
        if (publishedAt == null || publishedAt <= 0) return Bucket.Undated
        // Feeds publish ahead of time, so anything not yet past counts as today.
        val age = now - publishedAt
        return when {
            age < DAY -> Bucket.Today
            age < 2 * DAY -> Bucket.Yesterday
            age < 7 * DAY -> Bucket.ThisWeek
            else -> Bucket.Earlier
        }
    }
}
