package com.paulohenriquesg.fahrenheit.detail

import com.paulohenriquesg.fahrenheit.api.LibraryItemResponse
import com.paulohenriquesg.fahrenheit.api.MediaProgressResponse
import com.paulohenriquesg.fahrenheit.podcast.Fact
import com.paulohenriquesg.fahrenheit.utils.formatDuration
import kotlin.math.roundToInt

/** What the shared detail header shows; see [DetailHeaderModel]. */
data class DetailHeaderContent(
    val title: String,
    val byline: String?,
    val chips: List<Fact>,
    /** The one action that takes focus on arrival, or null when there is none. */
    val primary: String?,
    val description: String?
)

/**
 * The header a book and a podcast share (frame 3 of docs/mocks/screens.html):
 * title, a by-line, facts as chips rather than a paragraph of labels, one
 * primary action, then the description. One model for both, so the two
 * screens cannot drift apart.
 */
object DetailHeaderModel {

    /** How far in: "34% in", "Just started" or "Finished"; null when not started (#134: also a fact on the book screen). */
    fun progressOf(progress: MediaProgressResponse?): String? {
        if (progress?.isFinished == true) return "Finished"
        progress?.currentTime?.takeIf { it > 0 } ?: return null
        val percent = ((progress.progress ?: return null) * 100).roundToInt()
        return if (percent < 1) "Just started" else "$percent% in"
    }

    /** @param progress the book's, from the shared store (#207): the item's own copy is from when the page opened. */
    fun book(item: LibraryItemResponse, progress: MediaProgressResponse?): DetailHeaderContent {
        val metadata = item.media.metadata
        val finished = progress?.isFinished == true
        val resumeAt = progress?.currentTime?.takeIf { !finished && it > 0 }

        val authors = metadata.authors.orEmpty().joinToString(", ") { it.name }
        val narrators = metadata.narrators.orEmpty().joinToString(", ")
        val byline = listOfNotNull(
            authors.takeIf { it.isNotEmpty() },
            narrators.takeIf { it.isNotEmpty() }?.let { "narrated by $it" }
        ).joinToString(" · ").takeIf { it.isNotEmpty() }

        return DetailHeaderContent(
            title = metadata.title,
            byline = byline,
            chips = listOfNotNull(
                item.media.duration?.takeIf { it > 0 }?.let { Fact(formatDuration(it)) },
                metadata.publishedYear?.takeIf { it.isNotBlank() }?.let { Fact(it) },
                metadata.genres?.firstOrNull { it.isNotBlank() }?.let { Fact(it) },
                progressOf(progress)?.let { Fact(it) }
            ),
            // Says where it resumes, rather than a bare "Play".
            // Under a minute in, a position would read "0m".
            primary = resumeAt?.let { if (it < 60) "Resume" else "Resume at ${formatDuration(it)}" } ?: "Play",
            description = metadata.description
        )
    }

    /**
     * @param resumeTitle the episode in progress, played most recently (#78).
     * @param facts the feed's facts, from [com.paulohenriquesg.fahrenheit.podcast.PodcastScreenModel].
     */
    fun podcast(item: LibraryItemResponse, facts: List<Fact>, resumeTitle: String? = null): DetailHeaderContent {
        val metadata = item.media.metadata
        // The server's "author" for a podcast is usually its own name again, so
        // the by-line says what it is instead.
        val genre = metadata.genres?.firstOrNull()
        return DetailHeaderContent(
            title = metadata.title,
            byline = listOfNotNull("Podcast", genre).joinToString(" · "),
            chips = facts,
            primary = when {
                resumeTitle != null -> "Resume $resumeTitle"
                item.media.episodes.isNullOrEmpty() -> null
                else -> "Play newest episode"
            },
            description = metadata.description
        )
    }
}
