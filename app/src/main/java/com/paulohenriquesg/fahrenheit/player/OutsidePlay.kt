package com.paulohenriquesg.fahrenheit.player

import kotlinx.coroutines.CoroutineScope

class OutsidePlay(
    private val scope: CoroutineScope,
    private val check: suspend () -> ResumeOffer?,
    private val play: () -> Unit,
    private val openPlayer: () -> Boolean,
    private val timeoutMs: Long = 1_500
) {
    fun request() {}
}
