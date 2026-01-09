package com.paulohenriquesg.fahrenheit.api

import com.google.gson.annotations.SerializedName

data class CollectionsResponse(
    @SerializedName("results") val results: List<Collection>,
    @SerializedName("total") val total: Int? = null,
    @SerializedName("limit") val limit: Int? = null,
    @SerializedName("page") val page: Int? = null
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
