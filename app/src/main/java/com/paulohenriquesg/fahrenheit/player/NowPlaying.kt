package com.paulohenriquesg.fahrenheit.player

import com.paulohenriquesg.fahrenheit.api.Chapter
import com.paulohenriquesg.fahrenheit.api.LibraryItemMetadata
import com.paulohenriquesg.fahrenheit.api.LibraryItemResponse
import com.paulohenriquesg.fahrenheit.utils.EpisodeDate

/**
 * What is playing, for the one player that serves books and episodes (#73).
 *
 * Plain data on purpose: it is everything a player screen needs to draw, and it
 * is what a player that outlives a screen (#16, an app-wide mini-player) would
 * draw too. Frames 4 and 4b of docs/mocks/screens.html.
 *
 * @property timeline every file that will play, in order; null when there is
 *   nothing to play. Its length is [trackTotal].
 * @property chapters drawn as marks on the scrubber; null for an episode.
 * @property goToPodcast whether the player offers a way to the episode's podcast.
 */
data class NowPlaying(
    val itemId: String,
    val title: String,
    val timeline: TrackTimeline?,
    val mediaDuration: Double?,
    val chapters: List<Chapter>?,
    val episodeId: String?,
    val goToPodcast: Boolean,
    val description: String?,
    /** Above the title: the series and number, a standalone book's genre, or an episode's show and date. */
    val kicker: String? = null,
    /** Who wrote it and who reads it; null for an episode. */
    val byline: String? = null
) {
    /** The length of what will actually play; trusted over [mediaDuration] (see [ResumePoint]). */
    val trackTotal: Double? get() = timeline?.totalDuration

    companion object {

        /** "Series · Book N", or the series alone when it has no number. */
        private fun seriesLine(metadata: LibraryItemMetadata): String? {
            val series = metadata.series?.firstOrNull()
            val name = series?.name?.takeIf { it.isNotBlank() } ?: metadata.seriesName?.takeIf { it.isNotBlank() } ?: return null
            val number = series?.sequence?.takeIf { it.isNotBlank() } ?: return name
            return "$name · Book $number"
        }

        /**
         * @param episodeId the episode to play, for a podcast; null for a book.
         * @return null when there is nothing to play, e.g. an episode the
         *   podcast no longer has.
         */
        fun of(item: LibraryItemResponse, episodeId: String?, now: Long, serverFormat: String? = null): NowPlaying? {
            val metadata = item.media.metadata
            if (episodeId == null) {
                val tracks = item.media.tracks.orEmpty()
                val chapters = item.media.chapters
                return NowPlaying(
                    itemId = item.id,
                    title = metadata.title,
                    timeline = timelineOf(tracks),
                    mediaDuration = item.media.duration,
                    chapters = chapters,
                    episodeId = null,
                    goToPodcast = false,
                    description = metadata.description,
                    kicker = seriesLine(metadata) ?: metadata.genres?.firstOrNull { it.isNotBlank() },
                    byline = listOfNotNull(
                        metadata.authorName?.takeIf { it.isNotBlank() },
                        metadata.narratorName?.takeIf { it.isNotBlank() }?.let { "read by $it" }
                    ).joinToString(" · ").takeIf { it.isNotEmpty() }
                )
            }
            val episode = item.media.episodes?.firstOrNull { it.id == episodeId } ?: return null
            // Fixed once: "Yesterday" should not tick over while listening.
            val published = EpisodeDate.of(episode.publishedAt, now, serverFormat)
            return NowPlaying(
                itemId = item.id,
                title = episode.title,
                timeline = episode.audioTrack?.let {
                    // An episode is one file, starting at the start.
                    TrackTimeline(listOf(TimelineTrack(index = it.index, startOffset = 0.0, duration = it.duration, contentUrl = it.contentUrl)))
                },
                mediaDuration = null,
                chapters = null,
                episodeId = episode.id,
                goToPodcast = true,
                description = episode.description,
                kicker = listOfNotNull(metadata.title.takeIf { it.isNotBlank() }, published.takeIf { it.isNotEmpty() })
                    .joinToString(" · ").takeIf { it.isNotEmpty() },
                byline = null
            )
        }
    }
}
