package com.paulohenriquesg.fahrenheit.api

data class PlayLibraryItemRequest(
    val deviceInfo: PlayLibraryItemDeviceInfo,
    val forceDirectPlay: Boolean = false,
    val forceTranscode: Boolean = false,
    val supportedMimeTypes: List<String> = emptyList(),
    val mediaPlayer: String = "unknown"
)