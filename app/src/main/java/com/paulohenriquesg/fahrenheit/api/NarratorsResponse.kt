package com.paulohenriquesg.fahrenheit.api

import com.google.gson.annotations.SerializedName

/** `GET /api/libraries/:id/narrators`: the server keeps no record of a narrator but their name on books. */
data class NarratorsResponse(
    @SerializedName("narrators") val narrators: List<Narrator>? = null
)

/** [id] is the name, base64- and URL-encoded by the server. */
data class Narrator(
    @SerializedName("id") val id: String,
    @SerializedName("name") val name: String,
    @SerializedName("numBooks") val numBooks: Int? = null
)
