package com.paulohenriquesg.fahrenheit.player

import com.paulohenriquesg.fahrenheit.api.Chapter
import com.paulohenriquesg.fahrenheit.api.LibraryItemResponse
import com.paulohenriquesg.fahrenheit.utils.EpisodeDate

/**
 * What is playing, for the one player that serves books and episodes (#73).
 *
 * Plain data on purpose: it is everything a player screen needs to draw, and it
 * is what a player that outlives a screen (#16, an app-wide mini-player) would
 * draw too. Frames 4 and 4b of docs/mocks/screens.html.
 *
 * @property trackTotal the length of what will actually play; trusted over
 *   [mediaDuration] where there is one (see [ResumePoint]).
 * @property chapters drawn as marks on the scrubber; null for an episode.
 * @property goToPodcast whether the player offers a way to the episode's podcast.
 */
data class NowPlaying(
    val itemId: String,
    val title: String,
    val contentUrl: String?,
    val trackTotal: Double?,
    val mediaDuration: Double?,
    val chapters: List<Chapter>?,
    val episodeId: String?,
    val goToPodcast: Boolean,
    val description: String?,
    private val line: (Double) -> String
) {
    /** The line under the title; a book's depends on where it is. */
    fun subtitle(currentTime: Double): String = line(currentTime)

    companion object {

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
                    contentUrl = tracks.firstOrNull()?.contentUrl,
                    trackTotal = timelineOf(tracks)?.totalDuration,
                    mediaDuration = item.media.duration,
                    chapters = chapters,
                    episodeId = null,
                    goToPodcast = false,
                    description = metadata.description,
                    line = { at ->
                        val chapter = chapters?.firstOrNull { (it.start ?: 0.0) <= at && at < (it.end ?: 0.0) }
                        listOfNotNull(chapter?.title, metadata.authorName).joinToString(" · ")
                    }
                )
            }
            val episode = item.media.episodes?.firstOrNull { it.id == episodeId } ?: return null
            // Fixed once: "Yesterday" should not tick over while listening.
            val published = EpisodeDate.of(episode.publishedAt, now, serverFormat)
            return NowPlaying(
                itemId = item.id,
                title = episode.title,
                contentUrl = episode.audioTrack?.contentUrl,
                trackTotal = episode.audioTrack?.duration,
                mediaDuration = null,
                chapters = null,
                episodeId = episode.id,
                goToPodcast = true,
                description = episode.description,
                line = { listOfNotNull(metadata.title, published.takeIf { it.isNotEmpty() }).joinToString(" · ") }
            )
        }
    }
}
