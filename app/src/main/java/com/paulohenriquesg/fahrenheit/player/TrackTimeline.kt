package com.paulohenriquesg.fahrenheit.player

/** One file of a book, placed on the book's timeline. */
data class TimelineTrack(
    val index: Int,
    /** Seconds from the start of the book where this file begins. */
    val startOffset: Double,
    /** Seconds. */
    val duration: Double,
    val contentUrl: String
)

/** A whole-book position expressed as a file and a position within it. */
data class TrackPosition(val trackIndex: Int, val positionInTrack: Double)

/**
 * Translates between whole-book time and (file, position within that file).
 *
 * Audiobookshelf reports a multi-file book as tracks whose startOffset is the
 * summed duration of the files before it, and keeps chapters, the resume
 * position and progress in whole-book time. A player loads one file at a time,
 * so every position that crosses a file boundary needs translating. Getting it
 * wrong is silent: seeking past the end of the loaded file just plays nothing
 * (#16).
 *
 * The server's startOffset is trusted rather than recomputed from durations:
 * summing floating-point durations drifts from the offsets the server uses.
 */
class TrackTimeline(tracks: List<TimelineTrack>) {
    private val ordered = tracks.sortedBy { it.startOffset }

    init {
        require(ordered.isNotEmpty()) { "A book needs at least one track" }
    }

    val size: Int get() = ordered.size

    val totalDuration: Double = ordered.last().let { it.startOffset + it.duration }

    /**
     * Where a whole-book time falls. A time exactly on a file boundary belongs
     * to the later file. Out-of-range times clamp to the book's ends.
     */
    fun locate(bookTime: Double): TrackPosition {
        if (bookTime <= 0.0) return TrackPosition(0, 0.0)
        if (bookTime >= totalDuration) return TrackPosition(ordered.lastIndex, ordered.last().duration)
        val index = ordered.indexOfLast { it.startOffset <= bookTime }.coerceAtLeast(0)
        return TrackPosition(index, bookTime - ordered[index].startOffset)
    }

    /** The whole-book time for a position within a file - what progress sync must report. */
    fun bookTime(trackIndex: Int, positionInTrack: Double): Double =
        ordered[trackIndex].startOffset + positionInTrack

    fun track(trackIndex: Int): TimelineTrack = ordered[trackIndex]

    /** The file after [afterTrackIndex], or null at the end of the book. */
    fun next(afterTrackIndex: Int): TimelineTrack? = ordered.getOrNull(afterTrackIndex + 1)
}

/**
 * The timeline for a book's API tracks, or null when there are none - items
 * that are still being scanned can come back with an empty track list, and a
 * missing timeline is easier to handle than an exception mid-composition.
 */
fun timelineOf(tracks: List<com.paulohenriquesg.fahrenheit.api.Track>): TrackTimeline? =
    tracks.takeIf { it.isNotEmpty() }?.let { apiTracks ->
        TrackTimeline(
            apiTracks.map {
                TimelineTrack(
                    index = it.index,
                    startOffset = it.startOffset,
                    duration = it.duration,
                    contentUrl = it.contentUrl
                )
            }
        )
    }
