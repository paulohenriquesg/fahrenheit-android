package com.paulohenriquesg.fahrenheit.api

import com.google.gson.annotations.SerializedName

data class LibraryItemsResponse(
    @SerializedName("results") val results: List<LibraryItem>,
    @SerializedName("total") val total: Int,
    @SerializedName("limit") val limit: Int,
    @SerializedName("page") val page: Int,
    @SerializedName("sortBy") val sortBy: String,
    @SerializedName("sortDesc") val sortDesc: Boolean,
    @SerializedName("filterBy") val filterBy: String,
    @SerializedName("mediaType") val mediaType: String,
    @SerializedName("minified") val minified: Boolean,
    @SerializedName("collapseseries") val collapseseries: Boolean,
    @SerializedName("include") val include: String
)

data class LibraryItem(
    @SerializedName("id") val id: String,
    @SerializedName("ino") val ino: String,
    @SerializedName("libraryId") val libraryId: String,
    @SerializedName("folderId") val folderId: String,
    @SerializedName("path") val path: String,
    @SerializedName("relPath") val relPath: String,
    @SerializedName("isFile") val isFile: Boolean,
    @SerializedName("mtimeMs") val mtimeMs: Long,
    @SerializedName("ctimeMs") val ctimeMs: Long,
    @SerializedName("birthtimeMs") val birthtimeMs: Long,
    @SerializedName("addedAt") val addedAt: Long,
    @SerializedName("updatedAt") val updatedAt: Long,
    @SerializedName("isMissing") val isMissing: Boolean,
    @SerializedName("isInvalid") val isInvalid: Boolean,
    @SerializedName("mediaType") val mediaType: String,
    @SerializedName("media") val media: Media,
    @SerializedName("numFiles") val numFiles: Int,
    @SerializedName("size") val size: Long,
    @SerializedName("collapsedSeries") val collapsedSeries: CollapsedSeries?,
    @SerializedName("numEpisodesIncomplete") val numEpisodesIncomplete: Int?,
    // Personalized view specific fields
    @SerializedName("progressLastUpdate") val progressLastUpdate: Long? = null,
    @SerializedName("weight") val weight: Double? = null
)

data class Media(
    @SerializedName("metadata") val metadata: LibraryItemMetadata,
    @SerializedName("coverPath") val coverPath: String,
    @SerializedName("tags") val tags: List<String>,
    @SerializedName("numTracks") val numTracks: Int,
    @SerializedName("numAudioFiles") val numAudioFiles: Int,
    @SerializedName("numChapters") val numChapters: Int,
    @SerializedName("duration") val duration: Double,
    @SerializedName("size") val size: Long,
    @SerializedName("ebookFileFormat") val ebookFileFormat: String?
)

data class CollapsedSeries(
    @SerializedName("id") val id: String,
    @SerializedName("name") val name: String,
    @SerializedName("nameIgnorePrefix") val nameIgnorePrefix: String,
    @SerializedName("numBooks") val numBooks: Int
)