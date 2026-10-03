package com.paulohenriquesg.fahrenheit.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Button
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.paulohenriquesg.fahrenheit.R
import com.paulohenriquesg.fahrenheit.ui.Border
import com.paulohenriquesg.fahrenheit.ui.Radius
import com.paulohenriquesg.fahrenheit.ui.Space
import com.paulohenriquesg.fahrenheit.ui.components.ScreenTitle
import com.paulohenriquesg.fahrenheit.ui.theme.ThemePreference

/** Where an update check has got to, said in the row that asked rather than in a Toast. */
sealed interface UpdateCheck {
    data object Idle : UpdateCheck
    data object Checking : UpdateCheck
    data object UpToDate : UpdateCheck
    data class Available(val version: String) : UpdateCheck
    data class Failed(val reason: String) : UpdateCheck
}

@Composable
fun SettingsView(
    theme: ThemePreference,
    onTheme: (ThemePreference) -> Unit,
    rowLayout: Boolean,
    onLayout: (Boolean) -> Unit,
    version: String,
    update: UpdateCheck,
    onCheckUpdates: () -> Unit,
    username: String,
    server: String,
    onSignOut: () -> Unit,
    deviceName: String,
    onDeviceName: (String) -> Unit,
    modifier: Modifier = Modifier,
    deviceIsDark: Boolean = isSystemInDarkTheme()
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = Space.screenH, vertical = Space.gap),
        verticalArrangement = Arrangement.spacedBy(Space.gap)
    ) {
        // Outside the scroller, so it stays put while the settings scroll under it.
        ScreenTitle(stringResource(R.string.settings))

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(Space.gap)
        ) {
            Group(stringResource(R.string.settings_appearance)) {
                SettingRow(
                    title = stringResource(R.string.settings_theme),
                    subtitle = stringResource(
                        if (deviceIsDark) R.string.settings_theme_device_dark else R.string.settings_theme_device_light
                    )
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        ThemePreference.entries.forEach { option ->
                            Choice(
                                label = option.name,
                                selected = option == theme,
                                tag = "theme_${option.name}",
                                onClick = { onTheme(option) }
                            ) {
                                ThemeSwatch(option)
                            }
                        }
                    }
                }
                SettingRow(
                    title = stringResource(R.string.settings_shelves),
                    subtitle = stringResource(R.string.settings_shelves_subtitle)
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Choice(
                            label = stringResource(R.string.settings_rows),
                            selected = rowLayout,
                            tag = "layout_rows",
                            onClick = { onLayout(true) }
                        ) { LayoutPreview(rows = true) }
                        Choice(
                            label = stringResource(R.string.settings_grid),
                            selected = !rowLayout,
                            tag = "layout_grid",
                            onClick = { onLayout(false) }
                        ) { LayoutPreview(rows = false) }
                    }
                }
            }

            Group(stringResource(R.string.settings_updates)) {
                SettingRow(
                    title = stringResource(R.string.check_for_updates),
                    subtitle = updateLine(update, version)
                ) {
                    Button(
                        onClick = onCheckUpdates,
                        enabled = update != UpdateCheck.Checking,
                        modifier = Modifier.testTag("check_for_updates")
                    ) {
                        Text(stringResource(R.string.settings_check_now))
                    }
                }
            }

            Group(stringResource(R.string.settings_account)) {
                SettingRow(title = username, subtitle = server) {
                    Button(onClick = onSignOut, modifier = Modifier.testTag("sign_out")) {
                        Text(stringResource(R.string.settings_sign_out))
                    }
                }
                DeviceNameRow(deviceName, onDeviceName)
            }
        }
    }
}

@Composable
private fun updateLine(update: UpdateCheck, version: String): String = when (update) {
    UpdateCheck.Idle -> version
    UpdateCheck.Checking -> stringResource(R.string.settings_update_checking)
    UpdateCheck.UpToDate -> stringResource(R.string.settings_update_up_to_date, version)
    is UpdateCheck.Available -> stringResource(R.string.settings_update_available, update.version)
    is UpdateCheck.Failed -> update.reason
}

/**
 * How the server lists this TV's listening sessions. The text field appears
 * only when asked for: one passed on the way down the page would bring up the
 * keyboard.
 */
@Composable
private fun DeviceNameRow(name: String, onRename: (String) -> Unit) {
    var editing by remember { mutableStateOf(false) }
    if (!editing) {
        SettingRow(title = stringResource(R.string.settings_device_name), subtitle = name) {
            Button(onClick = { editing = true }, modifier = Modifier.testTag("device_name_rename")) {
                Text(stringResource(R.string.settings_device_name_rename))
            }
        }
        return
    }

    var draft by remember { mutableStateOf(TextFieldValue(name, TextRange(name.length))) }
    val field = remember { FocusRequester() }
    val save = {
        onRename(draft.text)
        editing = false
    }
    LaunchedEffect(Unit) { field.requestFocus() }
    SettingRow(
        title = stringResource(R.string.settings_device_name),
        subtitle = stringResource(R.string.settings_device_name_hint)
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = draft,
                onValueChange = { draft = it },
                singleLine = true,
                modifier = Modifier
                    .width(280.dp)
                    .focusRequester(field)
                    .testTag("device_name_field"),
                keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { save() })
            )
            Button(onClick = save, modifier = Modifier.testTag("device_name_save")) {
                Text(stringResource(R.string.settings_device_name_save))
            }
        }
    }
}

@Composable
private fun Group(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        content()
    }
}

@Composable
private fun SettingRow(title: String, subtitle: String, trailing: @Composable () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(Radius.panel)
            .background(MaterialTheme.colorScheme.surface)
            .border(Border.rest, MaterialTheme.colorScheme.surfaceVariant, Radius.panel)
            .padding(Space.inset),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.width(Space.gap))
        trailing()
    }
}

/** An option you can see rather than only read. */
@Composable
private fun Choice(
    label: String,
    selected: Boolean,
    tag: String,
    onClick: () -> Unit,
    preview: @Composable () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = Modifier.testTag(tag)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            preview()
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = if (selected) "$label ✓" else label,
                style = MaterialTheme.typography.labelMedium
            )
        }
    }
}

@Composable
private fun ThemeSwatch(option: ThemePreference) {
    val light = Color(0xFFF5F5F5)
    val dark = Color(0xFF1E1E1E)
    Row(modifier = Modifier.size(width = 40.dp, height = 22.dp).clip(Radius.bar)) {
        when (option) {
            ThemePreference.System -> {
                Box(modifier = Modifier.weight(1f).fillMaxSize().background(light))
                Box(modifier = Modifier.weight(1f).fillMaxSize().background(dark))
            }
            ThemePreference.Light -> Box(modifier = Modifier.fillMaxSize().background(light))
            ThemePreference.Dark -> Box(modifier = Modifier.fillMaxSize().background(dark))
        }
    }
}

@Composable
private fun LayoutPreview(rows: Boolean) {
    val tile = MaterialTheme.colorScheme.onSurfaceVariant
    Column(
        modifier = Modifier.size(width = 40.dp, height = 22.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        repeat(2) {
            Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                repeat(if (rows) 4 else 3) {
                    Box(
                        modifier = Modifier
                            .size(if (rows) 7.dp else 10.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(tile)
                    )
                }
            }
        }
    }
}
