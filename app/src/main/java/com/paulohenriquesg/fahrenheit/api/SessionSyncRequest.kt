package com.paulohenriquesg.fahrenheit.api

/**
 * A playback session sync. Nulls are left out, so an empty one is `{}`: the
 * server closes a session given that without syncing it.
 */
data class SessionSyncRequest(
    val currentTime: Double? = null,
    val timeListened: Double? = null,
    val duration: Double? = null
)
