package com.paulohenriquesg.fahrenheit.api

import com.google.gson.annotations.SerializedName

/**
 * GET /api/libraries/:id/stats, the counting fields only. For a podcast
 * library [totalItems] counts shows and [numAudioTracks] counts episodes.
 */
data class LibraryStats(
    @SerializedName("totalItems") val totalItems: Int?,
    /** Seconds of audio in the library, not time listened. */
    @SerializedName("totalDuration") val totalDuration: Double?,
    @SerializedName("numAudioTracks") val numAudioTracks: Int?
)
