package com.paulohenriquesg.fahrenheit.player

import androidx.compose.runtime.Composable

/**
 * Frame C's actions beside the transport: Go to podcast for an episode where
 * a book has Chapters, the heart while there is a Favourites playlist (#180),
 * then Speed, Sleep and About.
 */
@Composable
fun PlayerActions(
    panels: PlayerPanels,
    speed: Float,
    sleep: SleepState?,
    chapters: Boolean,
    onGoToPodcast: (() -> Unit)?,
    favourite: (@Composable () -> Unit)?
) {
    onGoToPodcast?.let { GoToPodcastButton(it) }
    if (chapters) ChaptersChip(panels)
    favourite?.invoke()
    SpeedChip(speed, panels)
    SleepChip(sleep, panels)
    AboutChip(panels)
}
