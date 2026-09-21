package com.paulohenriquesg.fahrenheit.player

import com.paulohenriquesg.fahrenheit.api.MediaProgressResponse

/** Where playback should begin, and the length to show against it. */
data class PlaybackStart(val positionSeconds: Double, val totalSeconds: Double)

object ResumePoint {

    /**
     * Decides both from what the server says.
     *
     * Three durations can disagree. [trackTotal] - the sum of the audio files
     * that will actually be played - wins, because media.duration can count
     * files the scanner later excluded and so overstate the audio. A saved
     * position measured against that longer figure can then sit past the end
     * of what exists, which plays nothing at all; starting over is the only
     * position that plays.
     */
    fun decide(
        progress: MediaProgressResponse?,
        trackTotal: Double?,
        mediaDuration: Double?
    ): PlaybackStart {
        val total = trackTotal?.takeIf { it > 0 }
            ?: progress?.duration?.takeIf { it > 0 }
            ?: mediaDuration?.takeIf { it > 0 }
            ?: 0.0

        val saved = progress?.currentTime ?: 0.0
        val position = when {
            progress?.isFinished == true -> 0.0
            saved <= 0.0 -> 0.0
            total > 0 && saved >= total -> 0.0
            else -> saved
        }

        return PlaybackStart(positionSeconds = position, totalSeconds = total)
    }
}
