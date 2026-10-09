package com.paulohenriquesg.fahrenheit.podcast

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.unit.dp
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.FilterChip
import androidx.tv.material3.LocalContentColor
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.foundation.layout.size
import androidx.tv.material3.ListItem
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Switch
import androidx.tv.material3.Text
import com.paulohenriquesg.fahrenheit.R
import com.paulohenriquesg.fahrenheit.player.SidePanel
import com.paulohenriquesg.fahrenheit.ui.rememberInitialFocus

private val scheduleNames = mapOf(
    ScheduleChoice.Hourly to "Every hour",
    ScheduleChoice.Daily to "Every day",
    ScheduleChoice.Weekly to "Every week",
    ScheduleChoice.Custom to "Custom"
)

/**
 * The show's own auto-download settings, as the server keeps them (#182, mock
 * frame 3), saved as each is chosen. The same side panel as Speed and Sleep;
 * wider, so each setting's choices sit in a row.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DownloadsPanel(settings: DownloadSettings, failed: Boolean, onChange: (DownloadChange) -> Unit, onClose: () -> Unit) {
    val first = rememberInitialFocus(enabled = true)
    SidePanel(title = "Downloads", onClose = onClose, width = 430.dp) {
        if (failed) {
            Text(
                text = stringResource(R.string.downloads_save_failed),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }
        // Thirteen choices do not fit in 540dp: focus scrolls them into view.
        Column(
            modifier = Modifier.verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            ListItem(
                selected = false,
                onClick = { onChange(DownloadChange.Enabled(!settings.enabled)) },
                headlineContent = { Text("Download new episodes") },
                supportingContent = { Text("The server fetches them as they come out") },
                trailingContent = { Switch(checked = settings.enabled, onCheckedChange = null) },
                modifier = Modifier
                    .focusRequester(first)
                    .testTag("downloads_enabled")
                    .semantics { toggleableState = ToggleableState(settings.enabled) }
            )
            val current = DownloadSchedule.choiceOf(settings.schedule)
            Setting("Check for new episodes") {
                listOf(ScheduleChoice.Hourly, ScheduleChoice.Daily, ScheduleChoice.Weekly).forEach { choice ->
                    Choice(scheduleNames.getValue(choice), choice == current, "downloads_schedule_${choice.name}") {
                        onChange(DownloadChange.Schedule(choice))
                    }
                }
                // Set in the web app: shown, and left alone until a choice is picked.
                if (current == ScheduleChoice.Custom) {
                    Text(
                        text = scheduleNames.getValue(ScheduleChoice.Custom),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .testTag("downloads_schedule_Custom")
                            .semantics { selected = true }
                            .border(BorderStroke(1.dp, MaterialTheme.colorScheme.onSurfaceVariant), RoundedCornerShape(8.dp))
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
            }
            Setting("Keep", detail = "Older downloaded episodes are deleted from the server") {
                DownloadSettings.keepOptions(settings.keep).forEach { count ->
                    Choice(DownloadSettings.keepLabel(count), count == settings.keep, "downloads_keep_$count") {
                        onChange(DownloadChange.Keep(count))
                    }
                }
            }
            Setting("New episodes per check") {
                DownloadSettings.perCheckOptions(settings.perCheck).forEach { count ->
                    Choice(DownloadSettings.perCheckLabel(count), count == settings.perCheck, "downloads_per_check_$count") {
                        onChange(DownloadChange.PerCheck(count))
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Setting(label: String, detail: String? = null, choices: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(label, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
        detail?.let {
            Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            choices()
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun Choice(label: String, chosen: Boolean, tag: String, onClick: () -> Unit) {
    FilterChip(
        selected = chosen,
        onClick = onClick,
        modifier = Modifier.testTag(tag).semantics { selected = chosen },
        // Ticked, as the mock and the other panels show the current choice.
        leadingIcon = if (chosen) {
            { Icon(Icons.Filled.Check, contentDescription = null, tint = LocalContentColor.current, modifier = Modifier.size(16.dp)) }
        } else null
    ) {
        Text(label)
    }
}
