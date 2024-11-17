package com.paulohenriquesg.fahrenheit.api

data class MediaProgressRequest(
    val duration: Float? = null,
    val progress: Float? = null,
    val currentTime: Float? = null,
    val isFinished: Boolean = false,
    val hideFromContinueListening: Boolean = false,
    val finishedAt: Long? = null,
    val startedAt: Long? = null
)