package com.paulohenriquesg.fahrenheit.api

import com.google.gson.annotations.SerializedName

data class MediaProgressResponse(
    @SerializedName("id") val id: String? = null,
    @SerializedName("userId") val userId: String? = null,
    @SerializedName("libraryItemId") val libraryItemId: String? = null,
    @SerializedName("episodeId") val episodeId: String? = null,
    @SerializedName("mediaItemId") val mediaItemId: String? = null,
    @SerializedName("mediaItemType") val mediaItemType: String? = null,
    @SerializedName("duration") val duration: Double? = null,
    @SerializedName("progress") val progress: Double? = null,
    @SerializedName("currentTime") val currentTime: Double? = null,
    @SerializedName("isFinished") val isFinished: Boolean? = null,
    @SerializedName("hideFromContinueListening") val hideFromContinueListening: Boolean? = null,
    @SerializedName("ebookLocation") val ebookLocation: String? = null,
    @SerializedName("ebookProgress") val ebookProgress: Double? = null,
    @SerializedName("lastUpdate") val lastUpdate: Long? = null,
    @SerializedName("startedAt") val startedAt: Long? = null,
    @SerializedName("finishedAt") val finishedAt: Long? = null
)