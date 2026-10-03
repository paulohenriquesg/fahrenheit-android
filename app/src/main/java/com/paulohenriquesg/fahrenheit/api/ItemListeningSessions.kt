package com.paulohenriquesg.fahrenheit.api

import com.google.gson.annotations.SerializedName

data class ItemListeningSessions(
    @SerializedName("sessions") val sessions: List<ItemSession>? = null
) {
    fun latestDeviceId(): String? = null
}

data class ItemSession(
    @SerializedName("updatedAt") val updatedAt: Long? = null,
    @SerializedName("deviceInfo") val deviceInfo: ItemSessionDevice? = null
)

data class ItemSessionDevice(@SerializedName("deviceId") val deviceId: String? = null)
