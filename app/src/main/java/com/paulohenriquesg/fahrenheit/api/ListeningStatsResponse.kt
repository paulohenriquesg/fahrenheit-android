package com.paulohenriquesg.fahrenheit.api

import com.google.gson.annotations.SerializedName

data class ListeningStatsResponse(
    @SerializedName("totalTime") val totalTime: Long,
    @SerializedName("items") val items: Map<String, ItemStats>,
    @SerializedName("days") val days: Map<String, DayStats>,
    @SerializedName("dayOfWeek") val dayOfWeek: Map<String, Long>,
    @SerializedName("today") val today: Long,
    @SerializedName("recentSessions") val recentSessions: List<ListeningSession>? = null
)

data class ItemStats(
    @SerializedName("id") val id: String,
    @SerializedName("timeListening") val timeListening: Long,
    @SerializedName("mediaMetadata") val mediaMetadata: LibraryItemMetadata? = null
)

data class DayStats(
    @SerializedName("date") val date: String,
    @SerializedName("timeListening") val timeListening: Long
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
    @SerializedName("timeListening") val timeListening: Long,
    @SerializedName("startTime") val startTime: Long,
    @SerializedName("currentTime") val currentTime: Long,
    @SerializedName("startedAt") val startedAt: Long,
    @SerializedName("updatedAt") val updatedAt: Long
)
