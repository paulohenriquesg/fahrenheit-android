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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import com.paulohenriquesg.fahrenheit.R
import com.paulohenriquesg.fahrenheit.storage.SharedPreferencesHandler

@Composable
fun LoginScreen(
    handleLogin: (String, String, String, MutableState<Boolean>) -> Unit,
    handleApiKeyLogin: (String, String, MutableState<Boolean>) -> Unit,
    error: LoginError? = null
) {
    val context = LocalContext.current
    val sharedPreferencesHandler = SharedPreferencesHandler(context)
    val userPreferences = sharedPreferencesHandler.getUserPreferences()

    var host by remember { mutableStateOf(userPreferences.host) }
    var username by remember { mutableStateOf(userPreferences.username) }
    var password by remember { mutableStateOf("") }
    var apiKey by remember { mutableStateOf("") }
    // Password is the default so the focus walk (host -> username -> password ->
    // Login) is unchanged; the API-key option sits below Login. An account with
    // no password signs in here with the field left empty (#2); the API key
    // stays for OpenID-only servers, where local sign-in is refused outright.
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

    // After a sign-out the server and username are still known (#63), so a
    // return visit shows them as a fact and asks only for the password (#100).
    val remembered = remember {
        userPreferences.host.isNotBlank() && userPreferences.username.isNotBlank()
    }
    var differentServer by remember { mutableStateOf(false) }
    val returning = remembered && !differentServer

    // Beside the field that caused it - the band above the keyboard is the only
    // part of the screen visible while typing. If that field is not on screen
    // (a password error, then a switch to the API key), the first one that is.
    val shownFields = when {
        returning && useApiKey -> listOf(LoginField.ApiKey)
        returning -> listOf(LoginField.Password)
        useApiKey -> listOf(LoginField.Host, LoginField.ApiKey)
        else -> listOf(LoginField.Host, LoginField.Username, LoginField.Password)
    }
    val errorField = error?.field?.takeIf { it in shownFields } ?: shownFields.first()
    // One way in, whatever pressed it: the keyboard's Done, the remote's Play
    // or the button. Ignored while a request is out, so a second press does
    // not send a second one.
    val submit: () -> Unit = {
        if (!isLoading.value) {
            if (useApiKey) handleApiKeyLogin(host, apiKey, isLoading)
            else handleLogin(host, username, password, isLoading)
        }
    }
    @Composable
    fun ErrorAbove(field: LoginField) {
        if (error != null && errorField == field) LoginErrorBand(error)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .testTag("login_screen")
            // The Fire TV keyboard owns the bottom 45% while typing; a centred
            // form puts its own buttons under it.
            .padding(start = if (returning) 60.dp else 16.dp, end = 16.dp, top = 32.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.Top,
        // Frame 3 of the login mock reads from the left, like a page.
        horizontalAlignment = if (returning) Alignment.Start else Alignment.CenterHorizontally
    ) {
        Text(
            text = if (returning) stringResource(R.string.login_welcome_back)
            else context.getString(R.string.app_name),
            style = if (returning) {
                MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.ExtraBold)
            } else {
                MaterialTheme.typography.headlineMedium
            },
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier
                .padding(bottom = 16.dp)
                .testTag("login_title")
        )

        if (returning) {
            RememberedAccount(username, host)
        } else {
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
                    .remoteKeys(focusManager)
                    .onFocusChanged {
                        isHostFocused = it.isFocused
                    },
                keyboardOptions = KeyboardOptions.Default.copy(
                    imeAction = ImeAction.Next
                ),
                keyboardActions = KeyboardActions(
                    onNext = { focusManager.moveFocus(FocusDirection.Down) }
                ),
                colors = fieldColors()
            )
            Spacer(modifier = Modifier.height(8.dp))
        }
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
                    .focusProperties { down = submitFocus }
                    .remoteKeys(focusManager, onPlay = submit)
                    .onFocusChanged { isApiKeyFocused = it.isFocused },
                keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Done),
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
                Spacer(modifier = Modifier.height(8.dp))
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
                    .focusProperties { down = submitFocus }
                    .remoteKeys(focusManager, onPlay = submit)
                    .onFocusChanged {
                        isPasswordFocused = it.isFocused
                    },
                keyboardOptions = KeyboardOptions.Default.copy(
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = { submit() }
                ),
                colors = fieldColors()
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        // The buttons stay while signing in: the spinner used to replace them,
        // taking focus along, and on the stick that looked like nothing at all.
        val loading = isLoading.value
        if (returning) {
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                LoginButton(
                    text = stringResource(R.string.login_sign_in),
                    onClick = submit,
                    primary = true,
                    loading = loading,
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
                    onClick = { differentServer = true },
                    modifier = Modifier.testTag("login_different_server")
                )
            }
        } else {
            LoginButton(
                text = stringResource(R.string.login),
                onClick = submit,
                primary = true,
                loading = loading,
                fillWidth = true,
                modifier = Modifier
                    .formWidth()
                    .focusRequester(submitFocus)
                    .testTag("login_submit_button")
            )
            Spacer(modifier = Modifier.height(8.dp))
            LoginButton(
                text = stringResource(if (useApiKey) R.string.login_use_username_instead else R.string.login_use_api_key_instead),
                onClick = { useApiKey = !useApiKey },
                fillWidth = true,
                modifier = Modifier
                    .formWidth()
                    .testTag("login_mode_toggle")
            )
        }
    }
}

/**
 * Frame 3's buttons: the one that signs in filled with the primary colour,
 * the rest outlined.
 *
 * @param loading shows progress in place of the label; the button keeps its
 *   place and its focus.
 * @param fillWidth centres the label across a button given a width; in a row
 *   it would take the whole row from its neighbours.
 */
@Composable
private fun LoginButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    primary: Boolean = false,
    loading: Boolean = false,
    fillWidth: Boolean = false
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
            modifier = if (fillWidth) Modifier.fillMaxWidth() else Modifier,
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
private fun LoginErrorBand(error: LoginError) {
    val danger = MaterialTheme.colorScheme.error
    val text = if (error is LoginError.ServerError) stringResource(error.message, error.code)
    else stringResource(error.message)
    Row(
        modifier = Modifier
            .formWidth()
            .padding(bottom = 8.dp)
            .background(danger.copy(alpha = 0.12f), RoundedCornerShape(12.dp))
            .border(1.dp, danger, RoundedCornerShape(12.dp))
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .testTag("login_error")
            .semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(Icons.Filled.ErrorOutline, contentDescription = null, tint = danger)
        Text(text, color = danger, style = MaterialTheme.typography.bodyLarge)
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
