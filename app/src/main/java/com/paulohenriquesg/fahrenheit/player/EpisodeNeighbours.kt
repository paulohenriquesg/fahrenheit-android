package com.paulohenriquesg.fahrenheit.player

import com.paulohenriquesg.fahrenheit.api.Episode

/** An episode as Previous, Next and Up next name it. */
data class EpisodeRef(val id: String, val title: String, val length: Double?)

/**
 * The episodes either side of one (#108): older and newer by when they came
 * out, among those the server has audio for - "next" is the next newer one.
 * Episodes out at the same moment go by the server's own order.
 */
object EpisodeNeighbours {
    data class Around(val previous: EpisodeRef?, val next: EpisodeRef?)

    fun of(episodes: List<Episode>, currentId: String): Around {
        val playable = episodes
            .filter { it.audioTrack != null }
            .sortedWith(compareBy<Episode> { it.publishedAt }.thenBy { it.index })
        val at = playable.indexOfFirst { it.id == currentId }
        if (at < 0) return Around(null, null)
        return Around(previous = playable.getOrNull(at - 1)?.ref(), next = playable.getOrNull(at + 1)?.ref())
    }

    private fun Episode.ref() = EpisodeRef(id, title, audioTrack?.duration)
}
