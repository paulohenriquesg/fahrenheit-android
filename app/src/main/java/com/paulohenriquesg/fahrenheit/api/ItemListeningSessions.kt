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
    /**
     * The most recently updated session, when it says which device wrote it,
     * and when; with the device's name for the question (#158), or its
     * client's when it sends none (a browser).
     */
    fun latest(): LatestSession? {
        val newest = sessions.orEmpty().maxByOrNull { it.updatedAt ?: 0L } ?: return null
        val info = newest.deviceInfo ?: return null
        val device = info.deviceId ?: return null
        val name = listOf(info.deviceName, info.clientName).firstNotNullOfOrNull { it?.trim()?.takeIf(String::isNotEmpty) }
        return LatestSession(device, newest.updatedAt ?: return null, name)
    }
}

data class ItemSession(
    @SerializedName("updatedAt") val updatedAt: Long? = null,
    @SerializedName("deviceInfo") val deviceInfo: ItemSessionDevice? = null
)

data class ItemSessionDevice(
    @SerializedName("deviceId") val deviceId: String? = null,
    @SerializedName("deviceName") val deviceName: String? = null,
    @SerializedName("clientName") val clientName: String? = null
)
