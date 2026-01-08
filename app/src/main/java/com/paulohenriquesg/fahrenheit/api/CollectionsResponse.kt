package com.paulohenriquesg.fahrenheit.api

import com.google.gson.annotations.SerializedName

data class CollectionsResponse(
    @SerializedName("collections") val collections: List<Collection>
)

data class Collection(
    @SerializedName("id") val id: String,
    @SerializedName("libraryId") val libraryId: String,
    @SerializedName("name") val name: String,
    @SerializedName("description") val description: String? = null,
    @SerializedName("cover") val cover: String? = null,
    @SerializedName("coverFullPath") val coverFullPath: String? = null,
    @SerializedName("books") val books: List<LibraryItem>? = null,
    @SerializedName("lastUpdate") val lastUpdate: Long,
    @SerializedName("createdAt") val createdAt: Long
)
