package com.paulohenriquesg.fahrenheit.player

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Bedtime
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.paulohenriquesg.fahrenheit.R
import com.paulohenriquesg.fahrenheit.ui.rememberInitialFocus

/** "Speed 1.25×". */
@Composable
fun SpeedChip(speed: Float, panels: PlayerPanels) = ActionChip(
    text = stringResource(R.string.speed_chip, ListeningSpeed.label(speed)),
    onClick = { panels.open(PlayerPanel.Speed) },
    modifier = Modifier.focusRequester(panels.opener(PlayerPanel.Speed))
)

/** "Sleep", or "Sleep 12 min" while a timer runs. */
@Composable
fun SleepChip(sleep: SleepState?, panels: PlayerPanels) = ActionChip(
    text = sleep?.let { stringResource(R.string.sleep_chip, it.minutesLeft) } ?: stringResource(R.string.sleep),
    onClick = { panels.open(PlayerPanel.Sleep) },
    modifier = Modifier.focusRequester(panels.opener(PlayerPanel.Sleep)),
    icon = Icons.Outlined.Bedtime
)

/**
 * The seven speeds; focus lands on the one playing. Choosing one closes the panel.
 *
 * @param forShow an episode: its speed is remembered for the whole show.
 */
@Composable
fun SpeedPanel(current: Float, onChoose: (Float) -> Unit, onClose: () -> Unit, forShow: Boolean = false) {
    SidePanel(stringResource(R.string.speed), onClose) {
        val landing = rememberInitialFocus(enabled = true)
        val focusAt = current.takeIf { it in ListeningSpeed.STEPS } ?: ListeningSpeed.NORMAL
        ListeningSpeed.STEPS.forEach { speed ->
            PanelOption(
                label = ListeningSpeed.label(speed),
                selected = speed == current,
                onClick = { onChoose(speed); onClose() },
                modifier = if (speed == focusAt) Modifier.focusRequester(landing) else Modifier
            )
        }
        Text(
            stringResource(if (forShow) R.string.speed_note_show else R.string.speed_note_book),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 14.dp)
        )
    }
}

/**
 * Off, End of chapter (only with chapters), 15, 30, 60 min; focus lands on
 * the running choice, or Off. Choosing one closes the panel.
 */
@Composable
fun SleepPanel(sleep: SleepState?, chapters: Boolean, onChoose: (SleepChoice) -> Unit, onClose: () -> Unit) {
    SidePanel(stringResource(R.string.sleep), onClose) {
        val landing = rememberInitialFocus(enabled = true)
        val offered = SleepChoice.OFFERED.filter { chapters || it != SleepChoice.EndOfChapter }
        val current = sleep?.choice ?: SleepChoice.Off
        val focusAt = current.takeIf { it in offered } ?: SleepChoice.Off
        offered.forEach { choice ->
            PanelOption(
                label = when (choice) {
                    SleepChoice.Off -> stringResource(R.string.sleep_off)
                    SleepChoice.EndOfChapter -> stringResource(R.string.sleep_end_of_chapter)
                    is SleepChoice.Minutes -> stringResource(R.string.sleep_minutes, choice.minutes)
                },
                selected = choice == current,
                onClick = { onChoose(choice); onClose() },
                modifier = if (choice == focusAt) Modifier.focusRequester(landing) else Modifier
            )
        }
    }
}
