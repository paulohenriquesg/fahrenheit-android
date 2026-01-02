package com.paulohenriquesg.fahrenheit.api

import com.google.gson.annotations.SerializedName

data class PersonalizedViewResponse(
    @SerializedName("id") val id: String,
    @SerializedName("shelves") val shelves: List<Shelf>,
    @SerializedName("isAuthenticated") val isAuthenticated: Boolean? = null
)

data class Shelf(
    val id: String,
    val label: String,
    val labelStringKey: String,
    val type: String,
    val bookEntities: List<LibraryItem>? = null,
    val total: Int? = null,
    val authorEntities: List<Author>? = null,
    val seriesEntities: List<Series>? = null
) {
    // Helper property for backward compatibility
    val entities: List<LibraryItem>?
        get() = bookEntities
}
