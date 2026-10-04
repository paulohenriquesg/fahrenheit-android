package com.paulohenriquesg.fahrenheit.api

import com.google.gson.annotations.SerializedName
import com.paulohenriquesg.fahrenheit.player.LatestSession

/**
 * One item's listening sessions (`api/me/item/listening-sessions`). Progress
 * records do not say which device wrote them; sessions do (#90).
 */
data class ItemListeningSessions(
    @SerializedName("sessions") val sessions: List<ItemSession>? = null
) {
    /** The most recently updated session, when it says which device wrote it, and when. */
    fun latest(): LatestSession? {
        val newest = sessions.orEmpty().maxByOrNull { it.updatedAt ?: 0L } ?: return null
        val device = newest.deviceInfo?.deviceId ?: return null
        return LatestSession(device, newest.updatedAt ?: return null)
    }
}

data class ItemSession(
    @SerializedName("updatedAt") val updatedAt: Long? = null,
    @SerializedName("deviceInfo") val deviceInfo: ItemSessionDevice? = null
)

data class ItemSessionDevice(@SerializedName("deviceId") val deviceId: String? = null)
