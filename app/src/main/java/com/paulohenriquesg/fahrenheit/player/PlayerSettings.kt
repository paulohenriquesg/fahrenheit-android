package com.paulohenriquesg.fahrenheit.player

import android.content.Context
import androidx.core.content.edit

/**
 * The player's own settings, on this device: "Play the next episode
 * automatically" (#108), off by default, and the skip lengths (#107), 30 s
 * each way by default. Kept apart from the account's preferences, as
 * [SpeedMemory] is.
 */
class PlayerSettings(context: Context) {
    private val prefs = context.getSharedPreferences("player_settings", Context.MODE_PRIVATE)

    var playNextEpisode: Boolean
        get() = prefs.getBoolean(PLAY_NEXT_EPISODE, false)
        set(value) = prefs.edit { putBoolean(PLAY_NEXT_EPISODE, value) }

    /** How far Skip back jumps, in seconds: one of [SKIP_LENGTHS] (#107). */
    var skipBackSeconds: Int
        get() = lengthOf(SKIP_BACK)
        set(value) = prefs.edit { putInt(SKIP_BACK, value) }

    /** How far Skip forward jumps, in seconds: one of [SKIP_LENGTHS]. */
    var skipForwardSeconds: Int
        get() = lengthOf(SKIP_FORWARD)
        set(value) = prefs.edit { putInt(SKIP_FORWARD, value) }

    /** A length not on offer - stored by an older build, say - reads as the default. */
    private fun lengthOf(key: String): Int =
        prefs.getInt(key, DEFAULT_SKIP).takeIf { it in SKIP_LENGTHS } ?: DEFAULT_SKIP

    companion object {
        val SKIP_LENGTHS = listOf(10, 15, 30, 60)
        private const val DEFAULT_SKIP = 30
        private const val PLAY_NEXT_EPISODE = "play_next_episode"
        private const val SKIP_BACK = "skip_back_seconds"
        private const val SKIP_FORWARD = "skip_forward_seconds"
    }
}
