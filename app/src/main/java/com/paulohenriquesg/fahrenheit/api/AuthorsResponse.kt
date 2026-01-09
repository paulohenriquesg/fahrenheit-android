package com.paulohenriquesg.fahrenheit.api

import com.google.gson.annotations.SerializedName

data class AuthorsResponse(
    @SerializedName("results") val results: List<Author>,
    @SerializedName("total") val total: Int? = null,
    @SerializedName("limit") val limit: Int? = null,
    @SerializedName("page") val page: Int? = null
)
