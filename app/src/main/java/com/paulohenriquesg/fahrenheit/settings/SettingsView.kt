package com.paulohenriquesg.fahrenheit.settings

import com.paulohenriquesg.fahrenheit.screensaver.ListeningScreensaver
import com.paulohenriquesg.fahrenheit.screensaver.ListeningSource
import com.paulohenriquesg.fahrenheit.screensaver.ScreensaverStyle
import com.paulohenriquesg.fahrenheit.player.PlayerSettings
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
import androidx.compose.foundation.layout.widthIn
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import androidx.tv.material3.Button
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Switch
import androidx.tv.material3.Text
import com.paulohenriquesg.fahrenheit.R
import com.paulohenriquesg.fahrenheit.favourites.FAVOURITES_ROW_TAG
import com.paulohenriquesg.fahrenheit.favourites.FavouritesPanel
import com.paulohenriquesg.fahrenheit.favourites.FavouritesSetting
import com.paulohenriquesg.fahrenheit.ui.Border
import com.paulohenriquesg.fahrenheit.ui.Radius
import com.paulohenriquesg.fahrenheit.ui.Space
import com.paulohenriquesg.fahrenheit.ui.requestFocusWhenAttached
import com.paulohenriquesg.fahrenheit.ui.components.ScreenTitle
import com.paulohenriquesg.fahrenheit.ui.theme.ThemePreference
import com.paulohenriquesg.fahrenheit.update.AvailableUpdate
import com.paulohenriquesg.fahrenheit.update.CheckResult

/** Where an update check has got to, said in the row that asked rather than in a Toast. */
sealed interface UpdateCheck {
    data object Idle : UpdateCheck
    data object Checking : UpdateCheck
    data object UpToDate : UpdateCheck
    data class Available(val update: AvailableUpdate) : UpdateCheck
    data object Failed : UpdateCheck

    companion object {
        fun from(result: CheckResult): UpdateCheck = when (result) {
            CheckResult.UpToDate -> UpToDate
            is CheckResult.Available -> Available(result.update)
            CheckResult.Failed -> Failed
        }
    }
}

const val PLAY_NEXT_EPISODE_TAG = "settings_play_next_episode"

@Composable
fun SettingsView(
    theme: ThemePreference,
    onTheme: (ThemePreference) -> Unit,
    rowLayout: Boolean,
    onLayout: (Boolean) -> Unit,
    version: String,
    update: UpdateCheck,
    onCheckUpdates: () -> Unit,
    onInstall: (AvailableUpdate) -> Unit,
    username: String,
    server: String,
    onSignOut: () -> Unit,
    deviceName: String,
    onDeviceName: (String) -> Unit,
    modifier: Modifier = Modifier,
    deviceIsDark: Boolean = isSystemInDarkTheme(),
    playNextEpisode: Boolean = false,
    onPlayNextEpisode: (Boolean) -> Unit = {},
    skipBack: Int = 30,
    onSkipBack: (Int) -> Unit = {},
    skipForward: Int = 30,
    onSkipForward: (Int) -> Unit = {},
    screensaverMinutes: Int? = 5,
    onScreensaverMinutes: (Int?) -> Unit = {},
    screensaverStyle: ScreensaverStyle = ScreensaverStyle.Wall,
    onScreensaverStyle: (ScreensaverStyle) -> Unit = {},
    favourites: FavouritesSetting? = null,
    screensaver: ListeningSource? = null
) {
    // The Favourites panel, over the screen; focus goes back to its row when it closes.
    var favouritesOpen by remember { mutableStateOf(false) }
    var favouritesClosed by remember { mutableStateOf(false) }
    val favouritesRow = remember { FocusRequester() }
    LaunchedEffect(favouritesClosed) {
        if (favouritesClosed) {
            favouritesRow.requestFocusWhenAttached()
            favouritesClosed = false
        }
    }
    // Try it (#190): the item queued when it was pressed, while the trial shows.
    var trying by remember { mutableStateOf<String?>(null) }
    // A key pressed during the trial; its release closes it.
    var closing by remember { mutableStateOf(false) }
    Box(modifier = modifier.fillMaxSize()) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            // The trial never takes focus, so its keys come here first: every
            // one is eaten, Back included, and Try it keeps the focus it had.
            .onPreviewKeyEvent { event ->
                if (trying == null) return@onPreviewKeyEvent false
                when (event.type) {
                    KeyEventType.KeyDown -> closing = true
                    KeyEventType.KeyUp -> if (closing) {
                        trying = null
                        closing = false
                    }
                }
                true
            }
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
            // A waiting update comes first: with Playback second (#170) the
            // Updates section is below the fold, and an update there went
            // unnoticed (#138). Check for updates stays in its section.
            if (update is UpdateCheck.Available) {
                SettingRow(
                    title = stringResource(R.string.settings_update_ready, update.update.versionName),
                    subtitle = updateSummary(update.update)
                ) {
                    Button(
                        onClick = { onInstall(update.update) },
                        modifier = Modifier.testTag("install_update")
                    ) {
                        Text(stringResource(R.string.settings_install))
                    }
                }
            }

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

            Group(stringResource(R.string.settings_playback)) {
                SettingRow(
                    title = stringResource(R.string.play_next_episode),
                    subtitle = stringResource(R.string.play_next_episode_subtitle)
                ) {
                    Switch(
                        checked = playNextEpisode,
                        onCheckedChange = onPlayNextEpisode,
                        modifier = Modifier.testTag(PLAY_NEXT_EPISODE_TAG)
                    )
                }
                // An Audiobookshelf playlist, one per library (#180; docs/mocks/podcast-actions.html, frame 0).
                favourites?.let { setting ->
                    SettingRow(
                        title = stringResource(R.string.favourites_row, setting.libraryName),
                        subtitle = stringResource(R.string.favourites_row_subtitle)
                    ) {
                        Button(
                            onClick = { favouritesOpen = true },
                            modifier = Modifier.focusRequester(favouritesRow).testTag(FAVOURITES_ROW_TAG)
                        ) {
                            Text(
                                stringResource(R.string.favourites_chosen, setting.chosen?.name ?: stringResource(R.string.favourites_none)),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.widthIn(max = 220.dp)
                            )
                        }
                    }
                }
                SkipRow(stringResource(R.string.settings_skip_back), skipBack, "skip_back", onSkipBack)
                SkipRow(stringResource(R.string.settings_skip_forward), skipForward, "skip_forward", onSkipForward)
                // Our own screensaver while something plays (#156; docs/mocks/screensaver.html).
                SettingRow(
                    title = stringResource(R.string.settings_screensaver),
                    subtitle = stringResource(R.string.settings_screensaver_subtitle)
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        (listOf<Int?>(null) + PlayerSettings.SCREENSAVER_MINUTES).forEach { minutes ->
                            Choice(
                                label = if (minutes == null) stringResource(R.string.settings_screensaver_off)
                                else stringResource(R.string.settings_screensaver_minutes, minutes),
                                selected = minutes == screensaverMinutes,
                                tag = "screensaver_${minutes ?: "off"}",
                                onClick = { onScreensaverMinutes(minutes) },
                                modifier = Modifier.widthIn(min = 76.dp)
                            )
                        }
                    }
                }
                SettingRow(
                    title = stringResource(R.string.settings_screensaver_style),
                    subtitle = stringResource(R.string.settings_screensaver_style_subtitle)
                ) {
                    Column(
                        horizontalAlignment = Alignment.End,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            ScreensaverStyle.entries.forEach { style ->
                                Choice(
                                    label = stringResource(
                                        when (style) {
                                            ScreensaverStyle.Wall -> R.string.settings_screensaver_wall
                                            ScreensaverStyle.Bouncing -> R.string.settings_screensaver_bouncing
                                        }
                                    ),
                                    selected = style == screensaverStyle,
                                    tag = "screensaver_style_${style.name}",
                                    onClick = { onScreensaverStyle(style) }
                                )
                            }
                        }
                        // The real screensaver needs something queued: a title
                        // and time left. Without it there is nothing to show.
                        if (screensaver != null) {
                            val queued = screensaver.queued()
                            // A trial of something no longer queued would mix
                            // one item's art with another's line: closed.
                            LaunchedEffect(queued?.itemId) {
                                if (trying != null && trying != queued?.itemId) {
                                    trying = null
                                    closing = false
                                }
                            }
                            // Never disabled: a disabled button gives up focus,
                            // and the queue can empty while Try it holds it. It
                            // says it is disabled, looks it, and does nothing.
                            Button(
                                onClick = { queued?.let { trying = it.itemId } },
                                modifier = Modifier
                                    .testTag("screensaver_try_it")
                                    .alpha(if (queued != null) 1f else 0.5f)
                                    .semantics { if (queued == null) disabled() }
                            ) {
                                Text(
                                    stringResource(
                                        if (queued != null) R.string.settings_screensaver_try
                                        else R.string.settings_screensaver_try_nothing
                                    )
                                )
                            }
                        }
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
    if (favouritesOpen && favourites != null) {
        FavouritesPanel(favourites) {
            favouritesOpen = false
            favouritesClosed = true
        }
    }
    val trial = trying
    if (trial != null && screensaver != null) ScreensaverTrial(screensaverStyle, trial, screensaver)
    }
}

/**
 * The screensaver as it would run, over the whole screen, rail included. A
 * popup that never takes focus: keys stay with the screen's window, where the
 * screensaver's own key gate counts them, and Settings closes it.
 */
@Composable
private fun ScreensaverTrial(style: ScreensaverStyle, itemId: String, source: ListeningSource) {
    Popup(
        popupPositionProvider = FullScreen,
        properties = PopupProperties(focusable = false, clippingEnabled = false)
    ) {
        // Black until the now-playing line is known, as the screensaver itself is.
        Box(Modifier.fillMaxSize().background(Color.Black)) {
            ListeningScreensaver(style, itemId, source)
        }
    }
}

/** At the window's top left, so a full-size popup covers it all. */
private object FullScreen : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize
    ): IntOffset = IntOffset.Zero
}

@Composable
private fun updateLine(update: UpdateCheck, version: String): String = when (update) {
    UpdateCheck.Idle -> version
    UpdateCheck.Checking -> stringResource(R.string.settings_update_checking)
    UpdateCheck.UpToDate -> stringResource(R.string.settings_update_up_to_date, version)
    // Install sits at the top of Settings, out of view from here, so the row
    // that asked says where to find it.
    is UpdateCheck.Available -> stringResource(R.string.settings_update_ready_above, update.update.versionName)
    UpdateCheck.Failed -> stringResource(R.string.settings_update_failed)
}

/**
 * How the server lists this TV's listening sessions. The text field appears
 * only when asked for: one passed on the way down the page would bring up the
 * keyboard.
 */
@Composable
private fun DeviceNameRow(name: String, onRename: (String) -> Unit) {
    var editing by remember { mutableStateOf(false) }
    // The field and Save leave the screen while one of them holds focus, so
    // focus is handed back to Rename rather than left to land on the rail.
    var returnFocus by remember { mutableStateOf(false) }
    if (!editing) {
        val rename = remember { FocusRequester() }
        LaunchedEffect(returnFocus) {
            if (returnFocus) {
                rename.requestFocusWhenAttached()
                returnFocus = false
            }
        }
        SettingRow(title = stringResource(R.string.settings_device_name), subtitle = name) {
            Button(
                onClick = { editing = true },
                modifier = Modifier.focusRequester(rename).testTag("device_name_rename")
            ) {
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
        returnFocus = true
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
                // Done is asked for, but the Fire TV keyboard labels the key "Next";
                // either one saves.
                keyboardActions = KeyboardActions(onDone = { save() }, onNext = { save() })
            )
            Button(onClick = save, modifier = Modifier.testTag("device_name_save")) {
                Text(stringResource(R.string.settings_device_name_save))
            }
        }
    }
}

/** How big the download is, and the first thing it brings. */
@Composable
private fun updateSummary(update: AvailableUpdate): String {
    val megabytes = (update.sizeBytes + BYTES_PER_MB / 2) / BYTES_PER_MB
    val size = if (update.sizeBytes > 0) {
        stringResource(R.string.settings_update_size, megabytes.coerceAtLeast(1))
    } else {
        null
    }
    return listOfNotNull(size, update.changelog.firstOrNull()).joinToString(" \u00b7 ")
}

private const val BYTES_PER_MB = 1024L * 1024L

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
                color = MaterialTheme.colorScheme.onSurface,
                // Some titles carry a server string: the account, the library.
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
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
    modifier: Modifier = Modifier,
    preview: (@Composable () -> Unit)? = null
) {
    Button(
        onClick = onClick,
        modifier = modifier.testTag(tag)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            preview?.let {
                it()
                Spacer(modifier = Modifier.height(4.dp))
            }
            Text(
                text = if (selected) "$label ✓" else label,
                style = MaterialTheme.typography.labelMedium
            )
        }
    }
}

/** One skip length setting: 10, 15, 30 or 60 s, the one set ticked (#107). */
@Composable
private fun SkipRow(title: String, seconds: Int, tag: String, onChoose: (Int) -> Unit) {
    SettingRow(title = title, subtitle = stringResource(R.string.settings_skip_subtitle)) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            PlayerSettings.SKIP_LENGTHS.forEach { length ->
                Choice(
                    label = stringResource(R.string.settings_skip_length, length),
                    selected = length == seconds,
                    tag = "${tag}_$length",
                    onClick = { onChoose(length) },
                    // One width, so the columns line up from row to row for Up and Down.
                    modifier = Modifier.widthIn(min = 76.dp)
                )
            }
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
