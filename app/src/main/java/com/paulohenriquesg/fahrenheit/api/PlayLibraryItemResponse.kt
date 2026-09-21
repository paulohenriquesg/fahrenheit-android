package com.paulohenriquesg.fahrenheit.api

import com.google.gson.annotations.SerializedName

data class PlayLibraryItemResponse(
    @SerializedName("id") val id: String,
    @SerializedName("userId") val userId: String,
    @SerializedName("libraryId") val libraryId: String,
    @SerializedName("libraryItemId") val libraryItemId: String,
    @SerializedName("episodeId") val episodeId: String?,
    @SerializedName("mediaType") val mediaType: String,
    @SerializedName("mediaMetadata") val mediaMetadata: LibraryItemMetadata,
    @SerializedName("chapters") val chapters: List<Any>,
    @SerializedName("displayTitle") val displayTitle: String,
    @SerializedName("displayAuthor") val displayAuthor: String,
    @SerializedName("coverPath") val coverPath: String?,
    @SerializedName("duration") val duration: Double,
    @SerializedName("playMethod") val playMethod: Int,
    @SerializedName("mediaPlayer") val mediaPlayer: String,
    @SerializedName("deviceInfo") val deviceInfo: PlayLibraryItemDeviceInfo,
    @SerializedName("date") val date: String,
    @SerializedName("dayOfWeek") val dayOfWeek: String,
    // Seconds, fractional. A session starts at the saved position, so resuming a
    // partly heard episode sends values like 19141.9355 - as Int, parsing failed
    // and the episode would not start.
    @SerializedName("timeListening") val timeListening: Double,
    @SerializedName("startTime") val startTime: Double,
    @SerializedName("currentTime") val currentTime: Double,
    @SerializedName("startedAt") val startedAt: Long,
    @SerializedName("updatedAt") val updatedAt: Long,
    @SerializedName("audioTracks") val audioTracks: List<AudioTrack>,
    @SerializedName("videoTrack") val videoTrack: Any?,
    @SerializedName("libraryItem") val libraryItem: LibraryItemResponse
)

data class PlayLibraryItemDeviceInfo(
    @SerializedName("deviceId") val deviceId: String?,
    @SerializedName("clientName") val clientName: String?,
    @SerializedName("clientVersion") val clientVersion: String?,
    @SerializedName("manufacturer") val manufacturer: String?,
    @SerializedName("model") val model: String?,
    @SerializedName("sdkVersion") val sdkVersion: Int?
)
