package com.paulohenriquesg.fahrenheit.api

import com.google.gson.annotations.SerializedName

data class AuthorDetailResponse(
    @SerializedName("id") val id: String,
    @SerializedName("asin") val asin: String? = null,
    @SerializedName("name") val name: String,
    @SerializedName("description") val description: String? = null,
    @SerializedName("imagePath") val imagePath: String? = null,
    @SerializedName("libraryId") val libraryId: String,
    @SerializedName("addedAt") val addedAt: Long,
    @SerializedName("updatedAt") val updatedAt: Long,
    @SerializedName("libraryItems") val libraryItems: List<LibraryItem>? = null,
    /** With `include=series`: the author's series, each with its books and their sequence (#230). */
    @SerializedName("series") val series: List<AuthorSeries>? = null
)

/** One of an author's series, its books in series order; each book's `series` holds its sequence. */
data class AuthorSeries(
    @SerializedName("id") val id: String,
    @SerializedName("name") val name: String,
    @SerializedName("items") val items: List<LibraryItem>? = null
)
