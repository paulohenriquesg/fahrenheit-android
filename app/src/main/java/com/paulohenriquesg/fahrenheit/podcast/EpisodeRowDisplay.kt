package com.paulohenriquesg.fahrenheit.podcast

import com.paulohenriquesg.fahrenheit.api.RecentPodcastEpisode

object EpisodeRowDisplay {

    /**
     * What a screen reader should say for the cover on an episode row.
     *
     * The row already shows the episode's own title, so the cover names the
     * podcast it belongs to; a list of recent episodes is a list of different
     * podcasts, and that is the part a cover tells you at a glance.
     */
    fun coverDescription(episode: RecentPodcastEpisode): String =
        episode.podcast?.metadata?.title?.takeIf { it.isNotBlank() }
            ?: episode.title?.takeIf { it.isNotBlank() }
            ?: "Podcast cover"
}
