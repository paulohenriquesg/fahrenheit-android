package com.paulohenriquesg.fahrenheit.screensaver

import com.paulohenriquesg.fahrenheit.api.LibraryItem

enum class ScreensaverStyle { Wall, Bouncing }

data class ScreensaverSettings(val delayMinutes: Int?, val style: ScreensaverStyle)

object ScreensaverPolicy {
    data class Decision(val keepScreenOn: Boolean, val show: Boolean)

    fun decide(delayMinutes: Int?, playing: Boolean, idleMs: Long, notPlayingMs: Long?): Decision = Decision(false, false)
}

class KeyGate(private val clock: () -> Long) {
    var showing: Boolean = false
    fun key(down: Boolean): Boolean = false
    fun idleMs(): Long = 0L
}

enum class NowPlayingCorner {
    TopStart, TopEnd, BottomEnd, BottomStart;

    companion object {
        fun at(shownForMs: Long): NowPlayingCorner = TopStart
    }
}

object Bounce {
    fun at(timeMs: Long, width: Float, height: Float, size: Float): Pair<Float, Float> = 0f to 0f
}

object WallCovers {
    fun pick(series: List<LibraryItem>, library: List<LibraryItem>, tiles: Int): List<LibraryItem> = emptyList()
}
