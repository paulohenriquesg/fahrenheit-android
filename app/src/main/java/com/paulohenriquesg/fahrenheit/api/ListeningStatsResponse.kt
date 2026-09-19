package com.paulohenriquesg.fahrenheit.api

import com.google.gson.annotations.SerializedName

// Every duration here is fractional seconds. As Long, parsing failed on the
// first one, so the stats screens never loaded.
data class ListeningStatsResponse(
    @SerializedName("totalTime") val totalTime: Double,
    @SerializedName("items") val items: Map<String, ItemStats>,
    // Date (yyyy-MM-dd) to seconds listened that day.
    @SerializedName("days") val days: Map<String, Double>,
    @SerializedName("dayOfWeek") val dayOfWeek: Map<String, Double>,
    @SerializedName("today") val today: Double,
    @SerializedName("recentSessions") val recentSessions: List<ListeningSession>? = null
)

data class ItemStats(
    @SerializedName("id") val id: String,
    @SerializedName("timeListening") val timeListening: Double,
    @SerializedName("mediaMetadata") val mediaMetadata: LibraryItemMetadata? = null
)

data class ListeningSession(
    @SerializedName("id") val id: String,
    @SerializedName("userId") val userId: String,
    @SerializedName("libraryItemId") val libraryItemId: String,
    @SerializedName("episodeId") val episodeId: String? = null,
    @SerializedName("mediaType") val mediaType: String,
    @SerializedName("displayTitle") val displayTitle: String? = null,
    @SerializedName("displayAuthor") val displayAuthor: String? = null,
    @SerializedName("coverPath") val coverPath: String? = null,
    @SerializedName("timeListening") val timeListening: Double,
    @SerializedName("startTime") val startTime: Double,
    @SerializedName("currentTime") val currentTime: Double,
    @SerializedName("startedAt") val startedAt: Long,
    @SerializedName("updatedAt") val updatedAt: Long
)
