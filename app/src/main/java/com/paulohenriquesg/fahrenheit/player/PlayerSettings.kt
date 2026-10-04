package com.paulohenriquesg.fahrenheit.player

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.paulohenriquesg.fahrenheit.screensaver.ScreensaverStyle

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

    /**
     * Calls [listener] when "Play the next episode automatically" changes, until
     * the returned function is called. The preferences hold their listeners
     * weakly: this one is held by the returned function.
     */
    fun onPlayNextEpisodeChanged(listener: () -> Unit): () -> Unit {
        val watch = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == PLAY_NEXT_EPISODE) listener()
        }
        prefs.registerOnSharedPreferenceChangeListener(watch)
        return { prefs.unregisterOnSharedPreferenceChangeListener(watch) }
    }

    /**
     * Minutes without a key, while listening, before the app's own
     * screensaver shows (#156): one of [SCREENSAVER_MINUTES], or null for Off.
     */
    var screensaverMinutes: Int?
        get() = when (val stored = prefs.getInt(SCREENSAVER_MINUTES_KEY, DEFAULT_SCREENSAVER_MINUTES)) {
            OFF -> null
            in SCREENSAVER_MINUTES -> stored
            // Not on offer, stored by an older build say: the default.
            else -> DEFAULT_SCREENSAVER_MINUTES
        }
        set(value) = prefs.edit { putInt(SCREENSAVER_MINUTES_KEY, value ?: OFF) }

    /** What the screensaver shows (#156). */
    var screensaverStyle: ScreensaverStyle
        get() = ScreensaverStyle.entries.find { it.name == prefs.getString(SCREENSAVER_STYLE_KEY, null) } ?: ScreensaverStyle.Wall
        set(value) = prefs.edit { putString(SCREENSAVER_STYLE_KEY, value.name) }

    /** A length not on offer - stored by an older build, say - reads as the default. */
    private fun lengthOf(key: String): Int =
        prefs.getInt(key, DEFAULT_SKIP).takeIf { it in SKIP_LENGTHS } ?: DEFAULT_SKIP

    companion object {
        val SKIP_LENGTHS = listOf(10, 15, 30, 60)
        val SCREENSAVER_MINUTES = listOf(2, 5, 10)
        private const val DEFAULT_SCREENSAVER_MINUTES = 5
        private const val OFF = 0
        private const val SCREENSAVER_MINUTES_KEY = "screensaver_minutes"
        private const val SCREENSAVER_STYLE_KEY = "screensaver_style"
        private const val DEFAULT_SKIP = 30
        private const val PLAY_NEXT_EPISODE = "play_next_episode"
        private const val SKIP_BACK = "skip_back_seconds"
        private const val SKIP_FORWARD = "skip_forward_seconds"
    }
}
