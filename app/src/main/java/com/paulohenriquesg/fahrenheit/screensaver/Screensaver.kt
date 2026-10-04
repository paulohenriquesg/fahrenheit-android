package com.paulohenriquesg.fahrenheit.screensaver

import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.paulohenriquesg.fahrenheit.api.LibraryItem

data class NowPlayingLine(val itemId: String, val title: String, val detail: String?)

data class Listening(val playing: Boolean, val line: NowPlayingLine, val covers: List<LibraryItem>, val wash: Color?)

object ScreensaverTags {
    const val SCREEN = "screensaver"
    const val OVERLAY = "screensaver-overlay"
    const val WALL_COVER = "screensaver-wall-cover"
    const val BOUNCING_COVER = "screensaver-bouncing-cover"
}

object Screensaver {
    fun install(
        activity: ComponentActivity,
        listening: @Composable () -> Listening?,
        settings: () -> ScreensaverSettings,
        clock: () -> Long
    ) {}
}

@Composable
fun ScreensaverScreen(style: ScreensaverStyle, listening: Listening, shownForMs: Long) {}
