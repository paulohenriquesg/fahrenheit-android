package com.paulohenriquesg.fahrenheit.api

data class MediaProgressRequest(
    val duration: Double? = null,
    val progress: Double? = null,
    val currentTime: Double? = null,
    val isFinished: Boolean = false,
    val hideFromContinueListening: Boolean = false,
    val finishedAt: Long? = null,
    val startedAt: Long? = null
)