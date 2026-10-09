package com.paulohenriquesg.fahrenheit.player

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Button
import androidx.tv.material3.ListItem
import androidx.tv.material3.LocalContentColor
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.paulohenriquesg.fahrenheit.ui.requestFocusWhenAttached

const val SIDE_PANEL_SCRIM_TAG = "side_panel_scrim"

/** The panels the player's action chips open (#107). */
enum class PlayerPanel { Chapters, Speed, Sleep, About }

/**
 * Which panel is open, and the chip each was opened from, so focus can go
 * back to it when the panel closes.
 */
@Stable
class PlayerPanels {
    var open by mutableStateOf<PlayerPanel?>(null)
        private set
    internal var closed by mutableStateOf<PlayerPanel?>(null)
    private val openers = PlayerPanel.entries.associateWith { FocusRequester() }

    /** Attach to the chip that opens [panel]. */
    fun opener(panel: PlayerPanel): FocusRequester = openers.getValue(panel)

    fun open(panel: PlayerPanel) {
        open = panel
    }

    fun close() {
        closed = open
        open = null
    }
}

/** Closed again whenever [keys] change: a panel left open must not come back over a new connection. */
@Composable
fun rememberPlayerPanels(vararg keys: Any?): PlayerPanels = remember(*keys) { PlayerPanels() }

/** Draws the open panel over the screen, and gives focus back to its chip when it closes. */
@Composable
fun PlayerPanelHost(panels: PlayerPanels, panel: @Composable (PlayerPanel) -> Unit) {
    panels.open?.let { panel(it) }
    LaunchedEffect(panels.open) {
        if (panels.open != null) return@LaunchedEffect
        val from = panels.closed ?: return@LaunchedEffect
        panels.closed = null
        panels.opener(from).requestFocusWhenAttached()
    }
}

/**
 * The one panel every action opens: from the right, over a scrim, with the
 * player still visible behind it (frame "A, with a panel open: Speed").
 *
 * Back closes it before it can leave the player, and focus cannot wander out
 * of it to the controls behind.
 *
 * @param width 320dp for a list of choices; wider for rows of them (#182).
 */
@OptIn(ExperimentalComposeUiApi::class) // focusProperties.exit
@Composable
fun SidePanel(title: String, onClose: () -> Unit, width: Dp = 320.dp, content: @Composable ColumnScope.() -> Unit) {
    BackHandler(onBack = onClose)
    val shown = remember { MutableTransitionState(false) }.apply { targetState = true }
    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.55f)).testTag(SIDE_PANEL_SCRIM_TAG)) {
        AnimatedVisibility(
            visibleState = shown,
            modifier = Modifier.align(Alignment.CenterEnd),
            enter = slideInHorizontally { it } + fadeIn()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(width)
                    .background(MaterialTheme.colorScheme.surface)
                    .focusProperties { exit = { FocusRequester.Cancel } }
                    .focusGroup()
                    .padding(horizontal = 28.dp, vertical = 32.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(title, style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onSurface)
                Spacer(Modifier.height(14.dp))
                content()
            }
        }
    }
}

/** One choice in a panel, ticked when it is the current one; [detail] under it, quieter. */
@Composable
fun PanelOption(label: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier, detail: String? = null) {
    ListItem(
        selected = selected,
        onClick = onClick,
        modifier = modifier,
        // Chapter and playlist names come from the server, and run long.
        headlineContent = { Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        supportingContent = detail?.let { { Text(it, maxLines = 1, overflow = TextOverflow.Ellipsis) } },
        trailingContent = if (selected) {
            { Icon(Icons.Filled.Check, contentDescription = null, tint = LocalContentColor.current) }
        } else null
    )
}

/** A chip beside the transport that opens a panel (frame C's actions). */
@Composable
fun ActionChip(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, icon: ImageVector? = null, enabled: Boolean = true) {
    // Disabled TV buttons still answer a semantics click: the guard is the press itself.
    Button(onClick = { if (enabled) onClick() }, modifier = modifier, enabled = enabled) {
        icon?.let {
            Icon(it, contentDescription = null, tint = LocalContentColor.current, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(text)
    }
}
