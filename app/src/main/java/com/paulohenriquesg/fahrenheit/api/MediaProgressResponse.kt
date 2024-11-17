package com.paulohenriquesg.fahrenheit.api

import com.google.gson.annotations.SerializedName

data class MediaProgressResponse(
    @SerializedName("id") val id: String? = null,
    @SerializedName("libraryItemId") val libraryItemId: String? = null,
    @SerializedName("episodeId") val episodeId: String? = null,
    @SerializedName("duration") val duration: Float? = null,
    @SerializedName("progress") val progress: Float? = null,
    @SerializedName("currentTime") val currentTime: Float? = null,
    @SerializedName("isFinished") val isFinished: Boolean? = null,
    @SerializedName("hideFromContinueListening") val hideFromContinueListening: Boolean? = null,
    @SerializedName("lastUpdate") val lastUpdate: Long? = null,
    @SerializedName("startedAt") val startedAt: Long? = null,
    @SerializedName("finishedAt") val finishedAt: Long? = null
)