package com.paulohenriquesg.fahrenheit.api

import com.google.gson.annotations.SerializedName

data class LibrariesResponse(
    @SerializedName("libraries") val libraries: List<Library>
)

data class Library(
    @SerializedName("id") val id: String,
    @SerializedName("name") val name: String,
    @SerializedName("folders") val folders: List<Folder>,
    @SerializedName("displayOrder") val displayOrder: Int,
    @SerializedName("icon") val icon: String,
    @SerializedName("mediaType") val mediaType: String,
    @SerializedName("provider") val provider: String,
    @SerializedName("settings") val settings: Settings,
    @SerializedName("createdAt") val createdAt: Long,
    @SerializedName("lastUpdate") val lastUpdate: Long
)

data class Folder(
    @SerializedName("id") val id: String,
    @SerializedName("fullPath") val fullPath: String,
    @SerializedName("libraryId") val libraryId: String,
    @SerializedName("addedAt") val addedAt: Long? = null
)

data class Settings(
    @SerializedName("coverAspectRatio") val coverAspectRatio: Int,
    @SerializedName("disableWatcher") val disableWatcher: Boolean,
    @SerializedName("skipMatchingMediaWithAsin") val skipMatchingMediaWithAsin: Boolean,
    @SerializedName("skipMatchingMediaWithIsbn") val skipMatchingMediaWithIsbn: Boolean,
    @SerializedName("autoScanCronExpression") val autoScanCronExpression: String?
)