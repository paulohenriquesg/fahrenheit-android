package com.paulohenriquesg.fahrenheit.api

import com.google.gson.annotations.SerializedName

/**
 * One item's listening sessions (`api/me/item/listening-sessions`). Progress
 * records do not say which device wrote them; sessions do (#90).
 */
data class ItemListeningSessions(
    @SerializedName("sessions") val sessions: List<ItemSession>? = null
) {
    /** The device behind the most recently updated session, if it says. */
    fun latest(): com.paulohenriquesg.fahrenheit.player.LatestSession? = null

    fun latestDeviceId(): String? =
        sessions.orEmpty().maxByOrNull { it.updatedAt ?: 0L }?.deviceInfo?.deviceId
}

data class ItemSession(
    @SerializedName("updatedAt") val updatedAt: Long? = null,
    @SerializedName("deviceInfo") val deviceInfo: ItemSessionDevice? = null
)

data class ItemSessionDevice(@SerializedName("deviceId") val deviceId: String? = null)
