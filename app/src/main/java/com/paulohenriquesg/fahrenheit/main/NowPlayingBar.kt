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
                    Text(leftLine(entry), style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
        Surface(
            onClick = { onStop(entry) },
            modifier = Modifier.testTag(NOW_PLAYING_BAR_STOP_TAG),
            shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(12.dp)),
            scale = ClickableSurfaceDefaults.scale(focusedScale = 1f)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // The label says it; the icon is decoration, not read out twice.
                Icon(Icons.Filled.Stop, contentDescription = null, modifier = Modifier.size(18.dp))
                Text(stringResource(R.string.rail_stop), style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

/**
 * The bar for what [player] - the screen's controller - has queued; nothing
 * when nothing is, or there is no controller yet. Stop moves focus down into
 * the screen first: the focused button goes with the bar, and a TV with
 * nothing focused ignores the remote (#53).
 */
@Composable
fun NowPlayingBarSlot(player: Player?, chaptersOf: suspend (String) -> List<Chapter>?, onOpen: (RailEntry) -> Unit) {
    val entry = rememberRailEntry(player, chaptersOf) ?: return
    val focus = LocalFocusManager.current
    NowPlayingBar(entry, onOpen, onStop = {
        focus.moveFocus(FocusDirection.Down)
        player?.let(Playback::end)
    })
}

private val BAR_WIDTH = 320.dp
