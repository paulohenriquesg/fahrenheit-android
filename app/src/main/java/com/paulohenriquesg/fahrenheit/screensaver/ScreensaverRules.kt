package com.paulohenriquesg.fahrenheit.screensaver

import kotlin.math.abs

/** What the screensaver shows (#156): the default is the wall. */
enum class ScreensaverStyle { Wall, Bouncing }

/**
 * @property delayMinutes minutes without a key before it shows; null for Off.
 */
data class ScreensaverSettings(val delayMinutes: Int?, val style: ScreensaverStyle)

/**
 * When the listening screensaver runs (#156).
 *
 * Fire OS will not let an app turn off or replace its own screensaver, but an
 * app may keep the screen on, which stops it starting. So: while something
 * plays, keep the screen on and show ours after the chosen minutes without a
 * key. Once playback stops, keep it on for the same delay more, then let go:
 * the Fire TV screensaver and sleep run as when nothing plays. Off is today's
 * behaviour throughout.
 */
object ScreensaverPolicy {
    data class Decision(val keepScreenOn: Boolean, val show: Boolean)

    /**
     * @param idleMs since the last key.
     * @param notPlayingMs since playback stopped, or null when nothing has
     *   played while this screen was up.
     */
    fun decide(delayMinutes: Int?, playing: Boolean, idleMs: Long, notPlayingMs: Long?): Decision {
        val delayMs = (delayMinutes ?: return Decision(false, false)) * 60_000L
        val keepOn = playing || (notPlayingMs != null && notPlayingMs < delayMs)
        return Decision(keepScreenOn = keepOn, show = keepOn && idleMs >= delayMs)
    }

    /**
     * How long until [decide] answers differently with no key pressed, or null
     * when it will not: the screensaver waits for that once, rather than
     * checking on a timer (a loop that never ends keeps the app from idling).
     */
    fun nextChangeMs(delayMinutes: Int?, playing: Boolean, idleMs: Long, notPlayingMs: Long?): Long? {
        val delayMs = (delayMinutes ?: return null) * 60_000L
        val toShow = (delayMs - idleMs).takeIf { it > 0 }
        if (playing) return toShow
        val toLetGo = notPlayingMs?.let { delayMs - it }?.takeIf { it > 0 } ?: return null
        return listOfNotNull(toShow, toLetGo).min()
    }
}

/** When someone last pressed a key, for the whole app (old behaviour: per gate). */
class LastKey(val clock: () -> Long) {
    var at: Long = clock()

    companion object {
        /** The app's. */
        val app = LastKey { android.os.SystemClock.uptimeMillis() }
    }
}

/**
 * The remote's keys and the screensaver (#156). Any key resets the idle time;
 * while the screensaver shows, the first key only wakes the screen - it and
 * its release are eaten, not acted on.
 */
class KeyGate(private val lastKey: LastKey) {
    private var ownLast = lastKey.clock()
    private var eatRelease = false

    var showing: Boolean = false

    /** @return true when the key is eaten. */
    fun key(down: Boolean, keyCode: Int): Boolean {
        ownLast = lastKey.clock()
        if (!down && eatRelease) {
            eatRelease = false
            return true
        }
        if (down && showing) {
            showing = false
            eatRelease = true
            return true
        }
        return false
    }

    fun idleMs(): Long = lastKey.clock() - ownLast
}

/** Where the now-playing line sits; it moves to the next corner every minute. */
enum class NowPlayingCorner {
    TopStart, TopEnd, BottomEnd, BottomStart;

    companion object {
        private const val MINUTE = 60_000L

        fun at(shownForMs: Long): NowPlayingCorner = entries[((shownForMs / MINUTE) % entries.size).toInt()]
    }
}

/**
 * The bouncing cover's place (#156), the DVD logo: across and down at
 * different speeds, turning at each edge, so it covers the screen slowly.
 */
object Bounce {
    private const val ACROSS_PX_PER_S = 60f
    private const val DOWN_PX_PER_S = 40f

    /** The cover's top-left corner after [timeMs], inside [width] x [height]. */
    fun at(timeMs: Long, width: Float, height: Float, size: Float): Pair<Float, Float> {
        val seconds = timeMs / 1000f
        return pingPong(seconds * ACROSS_PX_PER_S, width - size) to pingPong(seconds * DOWN_PX_PER_S, height - size)
    }

    /** [distance] travelled back and forth along 0..[span]. */
    private fun pingPong(distance: Float, span: Float): Float {
        if (span <= 0f) return 0f
        val lap = distance % (2 * span)
        return span - abs(lap - span)
    }
}

/**
 * The covers on the wall (#156): the series being played first, then the
 * library, each once. The wall repeats them to fill the screen.
 */
object WallCovers {
    fun <T> pick(series: List<T>, library: List<T>, key: (T) -> Any): List<T> = (series + library).distinctBy(key)
}
