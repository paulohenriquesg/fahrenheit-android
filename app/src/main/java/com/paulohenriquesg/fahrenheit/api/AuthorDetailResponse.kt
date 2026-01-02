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
    @SerializedName("libraryItems") val libraryItems: List<LibraryItem>? = null
)
