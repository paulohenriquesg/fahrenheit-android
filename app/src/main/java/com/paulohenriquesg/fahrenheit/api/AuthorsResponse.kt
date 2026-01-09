package com.paulohenriquesg.fahrenheit.api

import com.google.gson.annotations.SerializedName

data class AuthorsResponse(
    @SerializedName("authors") val authors: List<Author>
)
