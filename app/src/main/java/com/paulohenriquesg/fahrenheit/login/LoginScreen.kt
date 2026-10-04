// LoginScreen.kt
package com.paulohenriquesg.fahrenheit.login

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.draw.alpha
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.tv.material3.Button
import androidx.tv.material3.ListItem
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Edit
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.Border
import androidx.tv.material3.LocalContentColor
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.tv.material3.Text
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import com.paulohenriquesg.fahrenheit.R
import com.paulohenriquesg.fahrenheit.storage.SharedPreferencesHandler

@Composable
fun LoginScreen(
    handleLogin: (String, String, String, MutableState<Boolean>) -> Unit,
    handleApiKeyLogin: (String, String, MutableState<Boolean>) -> Unit,
    error: LoginError? = null,
    onDismissError: () -> Unit = {},
    findServers: (suspend ((FoundServer) -> Unit) -> Unit)? = null
) {
    val context = LocalContext.current
    val sharedPreferencesHandler = SharedPreferencesHandler(context)
    val userPreferences = sharedPreferencesHandler.getUserPreferences()

    var host by remember { mutableStateOf(userPreferences.host) }
    var username by remember { mutableStateOf(userPreferences.username) }
    var password by remember { mutableStateOf("") }
    var apiKey by remember { mutableStateOf("") }
    // An account with no password signs in with the field left empty (#2);
    // the API key stays for OpenID-only servers, where local sign-in is
    // refused outright.
    var useApiKey by remember { mutableStateOf(false) }
    var isApiKeyFocused by remember { mutableStateOf(false) }
    var isLoading = remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current
    // Down from the last field goes to the button that signs in. Left to
    // geometry it went to whichever button sits under the field's centre.
    val submitFocus = remember { FocusRequester() }

    var isHostFocused by remember { mutableStateOf(false) }
    var isUsernameFocused by remember { mutableStateOf(false) }
    var isPasswordFocused by remember { mutableStateOf(false) }

    // First run is two steps (#132): the address on its own, then signing in
    // to it, with the server named as a fact rather than left as a field.
    var addressConfirmed by remember { mutableStateOf(userPreferences.host.isNotBlank()) }
    var addressError by remember { mutableStateOf<LoginError?>(null) }
    // After a sign-out the server and username are still known (#63), so a
    // return visit shows them as a fact and asks only for the password (#100).
    val remembered = remember {
        userPreferences.host.isNotBlank() && userPreferences.username.isNotBlank()
    }
    var differentServer by remember { mutableStateOf(false) }
    val returning = remembered && !differentServer

    // Frame 1 (#101): before asking for an address, offer the servers that
    // answer on this network. Typing one is the fallback.
    var manualAddress by remember { mutableStateOf(false) }
    val listing = !addressConfirmed && findServers != null && !manualAddress
    val found = remember { mutableStateListOf<FoundServer>() }
    var scanning by remember { mutableStateOf(true) }
    if (listing && findServers != null) {
        // The last scan's rows go with the list: shown again on return, they
        // would take focus and then vanish as the new scan starts.
        DisposableEffect(Unit) {
            onDispose {
                found.clear()
                scanning = true
            }
        }
        // Leaving the list cancels the scan; coming back looks again.
        LaunchedEffect(Unit) {
            found.clear()
            scanning = true
            try {
                findServers { server -> if (server !in found) found += server }
            } finally {
                scanning = false
            }
        }
    }

    // Beside the field that caused it - the band above the keyboard is the only
    // part of the screen visible while typing. If that field is not on screen
    // (the address, once it is a fact; a password error, then a switch to the
    // API key), the first one that is.
    val shownFields = when {
        !addressConfirmed -> listOf(LoginField.Host)
        useApiKey -> listOf(LoginField.ApiKey)
        returning -> listOf(LoginField.Password)
        else -> listOf(LoginField.Username, LoginField.Password)
    }
    // Back on the address step only an address problem is worth repeating.
    val shownError = if (addressConfirmed) error else addressError ?: error?.takeIf { it.field == LoginField.Host }
    val errorField = shownError?.field?.takeIf { it in shownFields } ?: shownFields.first()
    @Composable
    fun ErrorAbove(field: LoginField) {
        if (shownError != null && errorField == field) LoginErrorBand(shownError, addressShown = !addressConfirmed)
    }

    // A step change removes whatever held focus - Continue, or the button row
    // - and a TV with nothing focused ignores the remote until a blind press.
    // So the step that appears takes focus: its first field.
    val hostFocus = remember { FocusRequester() }
    val firstFieldFocus = remember { FocusRequester() }
    // The server list's first row: the first server found, or typing one.
    val listFocus = remember { FocusRequester() }
    // The list has no keyboard to fight, so it takes focus from the start; the
    // text fields wait for the remote rather than raising the keyboard.
    var stepChanged by remember { mutableStateOf(listing) }
    val step = when {
        addressConfirmed -> firstFieldFocus
        listing -> listFocus
        else -> hostFocus
    }
    LaunchedEffect(step) {
        if (stepChanged) step.requestFocus()
    }

    // Checked here, before asking for anything else: nothing is sent yet.
    val confirmAddress: () -> Unit = {
        host = host.trim()
        addressError = LoginCoordinator.hostProblem(host)
        if (addressError == null) {
            // The last attempt's error belongs to the last address.
            onDismissError()
            stepChanged = true
            addressConfirmed = true
        }
    }
    // One way in, whatever pressed it: the keyboard's Done, the remote's Play
    // or the button. Ignored while a request is out, so a second press does
    // not send a second one.
    val submit: () -> Unit = {
        if (!isLoading.value) {
            if (useApiKey) handleApiKeyLogin(host, apiKey, isLoading)
            else handleLogin(host, username, password, isLoading)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .testTag("login_screen")
            // The Fire TV keyboard owns the bottom 45% while typing, so the
            // form starts at the top rather than centring its buttons under it.
            .padding(start = 60.dp, end = 16.dp, top = 24.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.Top,
        // The login mock reads from the left, like a page.
        horizontalAlignment = Alignment.Start
    ) {
        Text(
            text = when {
                !addressConfirmed -> stringResource(R.string.login_where_is_library)
                returning -> stringResource(R.string.login_welcome_back)
                else -> stringResource(R.string.login_sign_in_to, displayHost(host))
            },
            style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.ExtraBold),
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier
                .padding(bottom = 8.dp)
                .testTag("login_title")
        )

        if (listing) {
            Text(
                stringResource(
                    when {
                        scanning -> R.string.login_looking_for_servers
                        found.isEmpty() -> R.string.login_no_servers_found
                        else -> R.string.login_choose_server
                    }
                ),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            ErrorAbove(LoginField.Host)
            // In address order: probes answer in any order, and a list that
            // reshuffles under the cursor is worse than a predictable one.
            found.sortedBy { addressOrder(it.address) }.forEachIndexed { index, server ->
                // Keyed, so a server found later slots in without the focused
                // row starting to show a different server.
                key(server.address) { ServerRow(
                    title = displayHost(server.address),
                    detail = server.version?.let { stringResource(R.string.login_server_version, it) }
                        ?: stringResource(R.string.login_server_unknown_version),
                    icon = Icons.Filled.Dns,
                    onClick = {
                        host = server.address
                        addressError = null
                        onDismissError()
                        stepChanged = true
                        addressConfirmed = true
                    },
                    modifier = Modifier
                        .then(if (index == 0) Modifier.focusRequester(listFocus) else Modifier)
                        .testTag("login_server_${displayHost(server.address)}")
                ) }
            }
            ServerRow(
                title = stringResource(R.string.login_manual_address),
                detail = stringResource(R.string.login_manual_address_detail),
                icon = Icons.Filled.Edit,
                onClick = {
                    stepChanged = true
                    manualAddress = true
                },
                modifier = Modifier
                    .then(if (found.isEmpty()) Modifier.focusRequester(listFocus) else Modifier)
                    .testTag("login_manual_address")
            )
            return@Column
        }

        if (!addressConfirmed) {
            Text(
                stringResource(R.string.login_address_hint),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            ErrorAbove(LoginField.Host)
            OutlinedTextField(
                value = host,
                onValueChange = { host = it },
                label = {
                    Text(
                        stringResource(R.string.host),
                        color = if (isHostFocused) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                leadingIcon = {
                    Icon(
                        Icons.Filled.Language,
                        contentDescription = stringResource(R.string.host_icon),
                        tint = if (isHostFocused) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                singleLine = true,
                modifier = Modifier
                    .formWidth()
                    .testTag("login_host_field")
                    .focusRequester(hostFocus)
                    .remoteKeys(focusManager, onPlay = confirmAddress)
                    .onFocusChanged {
                        isHostFocused = it.isFocused
                    },
                keyboardOptions = KeyboardOptions.Default.copy(
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = { confirmAddress() }
                ),
                colors = fieldColors()
            )
            Spacer(modifier = Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                LoginButton(
                    text = stringResource(R.string.login_continue),
                    onClick = confirmAddress,
                    primary = true,
                    modifier = Modifier.testTag("login_continue")
                )
                if (findServers != null) {
                    LoginButton(
                        text = stringResource(R.string.login_search_again),
                        onClick = {
                            addressError = null
                            stepChanged = true
                            manualAddress = false
                        },
                        modifier = Modifier.testTag("login_search_again")
                    )
                }
            }
            return@Column
        }

        if (returning) RememberedAccount(username, host)
        if (useApiKey) {
            ErrorAbove(LoginField.ApiKey)
            OutlinedTextField(
                value = apiKey,
                onValueChange = { apiKey = it },
                label = {
                    Text(
                        stringResource(R.string.api_key),
                        color = if (isApiKeyFocused) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                leadingIcon = {
                    Icon(
                        Icons.Filled.Lock,
                        contentDescription = stringResource(R.string.api_key_icon),
                        tint = if (isApiKeyFocused) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                supportingText = {
                    Text(
                        stringResource(R.string.api_key_where_to_create),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                visualTransformation = PasswordVisualTransformation(),
                singleLine = true,
                modifier = Modifier
                    .formWidth()
                    .testTag("login_api_key_field")
                    .focusRequester(firstFieldFocus)
                    .focusProperties { down = submitFocus }
                    .remoteKeys(focusManager, onPlay = submit)
                    .onFocusChanged { isApiKeyFocused = it.isFocused },
                keyboardOptions = SecretKeyboard,
                keyboardActions = KeyboardActions(
                    onDone = { submit() }
                ),
                colors = fieldColors()
            )
        } else {
            if (!returning) {
                ErrorAbove(LoginField.Username)
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = {
                        Text(
                            stringResource(R.string.username),
                            color = if (isUsernameFocused) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    leadingIcon = {
                        Icon(
                            Icons.Filled.Person,
                            contentDescription = stringResource(R.string.username_icon),
                            tint = if (isUsernameFocused) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    singleLine = true,
                    modifier = Modifier
                        .formWidth()
                        .testTag("login_username_field")
                        .focusRequester(firstFieldFocus)
                        .remoteKeys(focusManager)
                        .onFocusChanged {
                            isUsernameFocused = it.isFocused
                        },
                    keyboardOptions = KeyboardOptions.Default.copy(
                        imeAction = ImeAction.Next
                    ),
                    keyboardActions = KeyboardActions(
                        onNext = { focusManager.moveFocus(FocusDirection.Down) }
                    ),
                    colors = fieldColors()
                )
                Spacer(modifier = Modifier.height(4.dp))
            }
            ErrorAbove(LoginField.Password)
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = {
                    Text(
                        stringResource(R.string.password),
                        color = if (isPasswordFocused) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                leadingIcon = {
                    Icon(
                        Icons.Filled.Lock,
                        contentDescription = stringResource(R.string.password_icon),
                        tint = if (isPasswordFocused) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                visualTransformation = PasswordVisualTransformation(),
                singleLine = true,
                modifier = Modifier
                    .formWidth()
                    .testTag("login_password_field")
                    .then(if (returning) Modifier.focusRequester(firstFieldFocus) else Modifier)
                    .focusProperties { down = submitFocus }
                    .remoteKeys(focusManager, onPlay = submit)
                    .onFocusChanged {
                        isPasswordFocused = it.isFocused
                    },
                keyboardOptions = SecretKeyboard,
                keyboardActions = KeyboardActions(
                    onDone = { submit() }
                ),
                colors = fieldColors()
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        // The buttons stay while signing in: the spinner used to replace them,
        // taking focus along, and on the stick that looked like nothing at all.
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            LoginButton(
                text = stringResource(R.string.login_sign_in),
                onClick = submit,
                primary = true,
                loading = isLoading.value,
                modifier = Modifier
                    .focusRequester(submitFocus)
                    .testTag("login_submit_button")
            )
            LoginButton(
                text = stringResource(if (useApiKey) R.string.login_use_password else R.string.login_use_api_key),
                onClick = { useApiKey = !useApiKey },
                modifier = Modifier.testTag("login_mode_toggle")
            )
            LoginButton(
                text = stringResource(R.string.login_different_server),
                onClick = {
                    // Mid-request the answer would land on the wrong step.
                    if (!isLoading.value) {
                        onDismissError()
                        // Typed for this server; not to be sent to the next.
                        password = ""
                        apiKey = ""
                        differentServer = true
                        stepChanged = true
                        addressConfirmed = false
                        manualAddress = false
                    }
                },
                modifier = Modifier.testTag("login_different_server")
            )
        }
    }
}

/**
 * The password and the API key. Hiding the text on screen is not enough: the
 * Fire TV keyboard shows what is typed in its own preview line, in clear,
 * unless the field says it is a password, and may learn it for suggestions
 * (#163). Done still signs in.
 */
private val SecretKeyboard = KeyboardOptions(
    keyboardType = KeyboardType.Password,
    autoCorrectEnabled = false,
    imeAction = ImeAction.Done
)

/**
 * Frame 3's buttons: the one that signs in filled with the primary colour,
 * the rest outlined.
 *
 * @param loading shows progress in place of the label; the button keeps its
 *   place and its focus.
 */
@Composable
private fun LoginButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    primary: Boolean = false,
    loading: Boolean = false
) {
    val scheme = MaterialTheme.colorScheme
    Button(
        onClick = onClick,
        modifier = modifier,
        colors = if (primary) {
            // Focus inverts it. The TV default turns any button white, and
            // the dark theme's light primary to white barely changes (#117).
            ButtonDefaults.colors(
                containerColor = scheme.primary,
                contentColor = scheme.onPrimary,
                focusedContainerColor = scheme.onPrimary,
                focusedContentColor = scheme.primary
            )
        } else {
            ButtonDefaults.colors(containerColor = Color.Transparent, contentColor = scheme.onSurface)
        },
        border = if (primary) ButtonDefaults.border() else ButtonDefaults.border(
            border = Border(BorderStroke(1.dp, scheme.onSurfaceVariant))
        )
    ) {
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // The label keeps its space while the spinner shows, so the
            // buttons beside this one do not shift under the cursor.
            Box(contentAlignment = Alignment.Center) {
                Text(text, modifier = Modifier.alpha(if (loading) 0f else 1f))
                if (loading) {
                    val signingIn = stringResource(R.string.login_signing_in)
                    CircularProgressIndicator(
                        color = LocalContentColor.current,
                        strokeWidth = 2.dp,
                        modifier = Modifier
                            .size(18.dp)
                            .testTag("login_progress")
                            .semantics { contentDescription = signingIn }
                    )
                }
            }
        }
    }
}

/**
 * The remote on a text field. The field keeps the D-pad for its cursor, so
 * on the stick Down from the password reached no button: Up and Down leave
 * the field instead. Play on a Fire TV remote is the field's Enter; [onPlay]
 * makes it sign in from the last field, where it used to do nothing.
 */
private fun Modifier.remoteKeys(focusManager: FocusManager, onPlay: (() -> Unit)? = null) =
    onPreviewKeyEvent { event ->
        val isPlay = event.key == Key.MediaPlayPause || event.key == Key.MediaPlay
        // The press signs in; its release is kept from the media keys too.
        if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent isPlay && onPlay != null
        when (event.key) {
            Key.DirectionDown -> focusManager.moveFocus(FocusDirection.Down)
            Key.DirectionUp -> focusManager.moveFocus(FocusDirection.Up)
            Key.MediaPlayPause, Key.MediaPlay -> onPlay?.let { it(); true } ?: false
            else -> false
        }
    }

/** One server that answered, or the way to type an address: frame 1's rows. */
@Composable
private fun ServerRow(
    title: String,
    detail: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    ListItem(
        selected = false,
        onClick = onClick,
        modifier = modifier
            .formWidth()
            .padding(bottom = 8.dp),
        leadingContent = { Icon(icon, contentDescription = null, tint = LocalContentColor.current) },
        headlineContent = { Text(title) },
        supportingContent = { Text(detail) }
    )
}

/** Sorts dotted IPv4 URLs numerically, so .9 comes before .81. */
private fun addressOrder(address: String): Long =
    displayHost(address).substringBefore(':').split('.')
        .fold(0L) { acc, part -> acc * 256 + (part.toLongOrNull() ?: 0L) }

/** Who and where, as a fact rather than two fields to fill in: frame 3's card. */
@Composable
private fun RememberedAccount(username: String, host: String) {
    val scheme = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(10.dp)
    Row(
        modifier = Modifier
            .padding(bottom = 16.dp)
            .formWidth()
            .testTag("login_account")
            .semantics(mergeDescendants = true) {}
            .background(scheme.surface, shape)
            .border(1.dp, scheme.onSurfaceVariant, shape)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .background(scheme.surfaceVariant, RoundedCornerShape(6.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Filled.Person,
                contentDescription = null,
                tint = scheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        }
        Column {
            Text(
                username,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = scheme.onSurface,
                modifier = Modifier.testTag("login_account_name")
            )
            Text(
                displayHost(host),
                style = MaterialTheme.typography.bodyMedium,
                color = scheme.onSurfaceVariant,
                modifier = Modifier.testTag("login_account_server")
            )
        }
    }
}

/** The address as a person reads it: no scheme, no trailing slash. */
private fun displayHost(host: String): String =
    host.substringAfter("://").trimEnd('/')

@Composable
private fun LoginErrorBand(error: LoginError, addressShown: Boolean) {
    val danger = MaterialTheme.colorScheme.error
    val text = if (error is LoginError.ServerError) stringResource(error.message, error.code)
    else stringResource(error.messageWhere(addressShown))
    Row(
        modifier = Modifier
            .formWidth()
            .padding(bottom = 4.dp)
            .background(danger.copy(alpha = 0.12f), RoundedCornerShape(12.dp))
            .border(1.dp, danger, RoundedCornerShape(12.dp))
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .testTag("login_error")
            .semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(Icons.Filled.ErrorOutline, contentDescription = null, tint = danger)
        Text(text, color = danger, style = MaterialTheme.typography.bodyMedium)
    }
}

// A TV screen is 960dp wide. A field across all of it is a metre of empty box
// behind an eight-character username.
private val FormWidth = 720.dp

// widthIn first: constraints flow outside-in, so filling the width before
// capping it fills the whole screen and caps only the content inside.
private fun Modifier.formWidth(): Modifier = this
    .widthIn(max = FormWidth)
    .fillMaxWidth()

@Composable
private fun fieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = MaterialTheme.colorScheme.onSurface,
    unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
    cursorColor = MaterialTheme.colorScheme.primary,
    focusedBorderColor = MaterialTheme.colorScheme.primary,
    unfocusedBorderColor = MaterialTheme.colorScheme.onSurfaceVariant
)

/**
 * Where the address is a fact rather than a field, "check the address" has
 * nothing to point at; these say how to change it instead.
 */
@StringRes
private fun LoginError.messageWhere(addressShown: Boolean): Int = when {
    addressShown -> message
    this == LoginError.Unreachable -> R.string.login_error_unreachable_change
    this == LoginError.UnusableSession -> R.string.login_error_unusable_session_change
    else -> message
}

/** What the screen says for each [LoginError]: what to do, not what broke. */
@get:StringRes
val LoginError.message: Int
    get() = when (this) {
        LoginError.HostMissing -> R.string.login_error_host_missing
        LoginError.HostScheme -> R.string.login_error_host_scheme
        LoginError.UsernameMissing -> R.string.login_error_username_missing
        LoginError.ApiKeyMissing -> R.string.login_error_api_key_missing
        LoginError.PasswordRejected -> R.string.login_error_password_rejected
        LoginError.ApiKeyRejected -> R.string.login_error_api_key_rejected
        LoginError.Unreachable -> R.string.login_error_unreachable
        is LoginError.ServerError -> R.string.login_error_server
        LoginError.Unexpected -> R.string.login_error_unexpected
        LoginError.UnusableSession -> R.string.login_error_unusable_session
    }
