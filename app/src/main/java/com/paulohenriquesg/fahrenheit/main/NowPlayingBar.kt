package com.paulohenriquesg.fahrenheit.main

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Stop
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.media3.common.Player
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import com.paulohenriquesg.fahrenheit.R
import com.paulohenriquesg.fahrenheit.api.Chapter
import com.paulohenriquesg.fahrenheit.player.Playback
import com.paulohenriquesg.fahrenheit.player.RailEntry
import com.paulohenriquesg.fahrenheit.player.rememberRailEntry

const val NOW_PLAYING_BAR_TAG = "now_playing_bar"
const val NOW_PLAYING_BAR_STOP_TAG = "now_playing_bar_stop"

/**
 * Now playing on the screens without a rail - a book's, a podcast's (#159).
 * What the rail's open entry says, in a bar: choosing it opens the player, and
 * Stop beside it ends the listening session, as the rail's Stop does.
 */
@Composable
fun NowPlayingBar(entry: RailEntry, onOpen: (RailEntry) -> Unit, onStop: (RailEntry) -> Unit, modifier: Modifier = Modifier) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Surface(
            onClick = { onOpen(entry) },
            modifier = Modifier.width(BAR_WIDTH).testTag(NOW_PLAYING_BAR_TAG),
            shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(12.dp)),
            scale = ClickableSurfaceDefaults.scale(focusedScale = 1f)
        ) {
            Row(
                modifier = Modifier.padding(6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                CoverWithRing(entry)
                Column(Modifier.weight(1f)) {
                    Text(entry.title, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    LeftLine(entry)
                }
            }
        }
        StopButton(onClick = { onStop(entry) }, modifier = Modifier.testTag(NOW_PLAYING_BAR_STOP_TAG))
    }
}

/**
 * The bar for what [player] - the screen's controller - has queued; nothing
 * when nothing is. Between leaving the screen and connecting again there is no
 * controller: the bar stays as it was rather than going and coming back, which
 * moved the screen and took focus with it.
 *
 * Stop moves focus down into the screen first: the focused button goes with
 * the bar, and a TV with nothing focused ignores the remote (#53). A bar that
 * goes by itself - the end of the queue (#179) - while focused stays until
 * focus has moved down the same way.
 */
@Composable
fun NowPlayingBarSlot(player: Player?, chaptersOf: suspend (String) -> List<Chapter>?, onOpen: (RailEntry) -> Unit) {
    val live = rememberRailEntry(player, chaptersOf)
    val kept = remember { KeptEntry() }
    var focused by remember { mutableStateOf(false) }
    // The end of the queue ends it as Stop does (#179): focus moves down first here too.
    val leaving = player != null && live == null && focused
    if (player != null && !leaving) kept.entry = live
    val entry = kept.entry ?: return
    val focus = LocalFocusManager.current
    if (leaving) {
        LaunchedEffect(Unit) {
            focus.moveFocus(FocusDirection.Down)
            // Gone either way: a bar with nowhere to send focus would stay for good.
            focused = false
        }
    }
    NowPlayingBar(
        entry,
        onOpen,
        onStop = {
            focus.moveFocus(FocusDirection.Down)
            player?.let(Playback::end)
        },
        modifier = Modifier.onFocusChanged { focused = it.hasFocus }
    )
}

private val BAR_WIDTH = 320.dp
