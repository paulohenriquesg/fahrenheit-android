package com.paulohenriquesg.fahrenheit.api

import com.google.gson.annotations.SerializedName

data class SeriesResponse(
    @SerializedName("series") val series: List<Series>
)
