package com.paulohenriquesg.fahrenheit.podcast

import com.paulohenriquesg.fahrenheit.api.Episode

/** How a podcast's episodes are listed. */
object EpisodeOrder {

    /**
     * Newest first. Episodes whose feed gave no date keep the order the server
     * sent them in, after the dated ones: at 0 they would otherwise claim the
     * oldest slot and shuffle as new episodes arrive.
     */
    fun newestFirst(episodes: List<Episode>): List<Episode> {
        val (dated, undated) = episodes.partition { it.publishedAt > 0 }
        return dated.sortedByDescending { it.publishedAt } + undated
    }
}
