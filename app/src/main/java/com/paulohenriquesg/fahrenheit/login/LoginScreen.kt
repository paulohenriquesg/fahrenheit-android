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
            .padding(start = 16.dp, end = 16.dp, top = 32.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = if (returning) stringResource(R.string.login_welcome_back)
            else context.getString(R.string.app_name),
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier
                .padding(bottom = 16.dp)
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
                    .onFocusChanged { isApiKeyFocused = it.isFocused },
                keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(
                    onDone = { handleApiKeyLogin(host, apiKey, isLoading) }
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
                    .onFocusChanged {
                        isPasswordFocused = it.isFocused
                    },
                keyboardOptions = KeyboardOptions.Default.copy(
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = { handleLogin(host, username, password, isLoading) }
                ),
                colors = fieldColors()
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        if (isLoading.value) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        } else if (returning) {
            Row(
                modifier = Modifier.formWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Button(
                    onClick = {
                        if (useApiKey) handleApiKeyLogin(host, apiKey, isLoading)
                        else handleLogin(host, username, password, isLoading)
                    },
                    modifier = Modifier.testTag("login_submit_button")
                ) {
                    Text(stringResource(R.string.login_sign_in))
                }
                Button(
                    onClick = { useApiKey = !useApiKey },
                    modifier = Modifier.testTag("login_mode_toggle")
                ) {
                    Text(
                        stringResource(
                            if (useApiKey) R.string.login_use_password else R.string.login_use_api_key
                        )
                    )
                }
                Button(
                    onClick = { differentServer = true },
                    modifier = Modifier.testTag("login_different_server")
                ) {
                    Text(stringResource(R.string.login_different_server))
                }
            }
        } else {
            Button(
                onClick = {
                    if (useApiKey) handleApiKeyLogin(host, apiKey, isLoading)
                    else handleLogin(host, username, password, isLoading)
                },
                modifier = Modifier
                    .formWidth()
                    .testTag("login_submit_button")
            ) {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text(stringResource(R.string.login))
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = { useApiKey = !useApiKey },
                modifier = Modifier
                    .formWidth()
                    .testTag("login_mode_toggle")
            ) {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text(
                        if (useApiKey) "Use username and password instead"
                        else "Sign in with an API key instead"
                    )
                }
            }
        }
    }
}

/** Who and where, as a fact rather than two fields to fill in. */
@Composable
private fun RememberedAccount(username: String, host: String) {
    Row(
        modifier = Modifier
            .padding(bottom = 16.dp)
            .testTag("login_account")
            .semantics(mergeDescendants = true) {},
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            Icons.Filled.Person,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            stringResource(R.string.login_account, username, displayHost(host)),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
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
