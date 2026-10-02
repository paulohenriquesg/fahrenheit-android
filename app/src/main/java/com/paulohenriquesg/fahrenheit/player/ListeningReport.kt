package com.paulohenriquesg.fahrenheit.player

/**
 * Where playback is, in whole-book seconds, and how many seconds were
 * listened since the last report that was delivered.
 */
data class ListeningReport(val currentTime: Double, val duration: Double, val timeListened: Double = 0.0)
