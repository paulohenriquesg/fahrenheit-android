package com.paulohenriquesg.fahrenheit.podcast

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.paulohenriquesg.fahrenheit.api.MediaProgressResponse

/** Episodes marked finished or not on the podcast's screen, over the progress read on open (#181). */
object EpisodeMarks {

    /**
     * Marked finished reads as finished; marked unfinished is no longer
     * finished, and a half-heard episode keeps its place.
     */
    fun over(progress: Map<String, EpisodeProgress>, marks: Map<String, Boolean>): Map<String, EpisodeProgress> =
        (progress.keys + marks.keys).mapNotNull { id ->
            val shown = when (marks[id]) {
                true -> EpisodeProgress.Heard
                false -> progress[id]?.takeUnless { it == EpisodeProgress.Heard }
                null -> progress[id]
            }
            shown?.let { id to it }
        }.toMap()

    /**
     * Where an un-finished episode should stay rather than the start the
     * server puts it at: where it was, unless so near the end that the server
     * would finish it again (under 10 s left) - the book's rule.
     */
    fun keepAt(progress: MediaProgressResponse?): Double? {
        val at = progress?.currentTime ?: return null
        val length = progress.duration ?: return null
        return at.takeIf { at > 0 && length - at > 10 }
    }
}

/** The marks made here: each shows at once, and goes back if the server refuses it. */
class EpisodeMarking {
    var marks: Map<String, Boolean> by mutableStateOf(emptyMap())
        private set

    // Episodes whose mark is still on its way to the server.
    private val out = mutableSetOf<String>()

    /**
     * @param send tells the server; false is a refusal.
     * @return whether the server took it; null when the press was ignored,
     *   because that episode's last mark is still out and a second would race it.
     */
    suspend fun mark(episodeId: String, finished: Boolean, send: suspend () -> Boolean): Boolean? {
        if (!out.add(episodeId)) return null
        val before = marks[episodeId]
        marks = marks + (episodeId to finished)
        val worked = try {
            send()
        } finally {
            out.remove(episodeId)
        }
        if (!worked) marks = if (before == null) marks - episodeId else marks + (episodeId to before)
        return worked
    }

    /**
     * The server's progress has been read again: it says what is so now, so
     * only marks still out are kept over it.
     */
    fun settle() {
        marks = marks.filterKeys { it in out }
    }
}
