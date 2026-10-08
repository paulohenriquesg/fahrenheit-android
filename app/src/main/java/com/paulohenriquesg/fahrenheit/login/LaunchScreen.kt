package com.paulohenriquesg.fahrenheit.login

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.paulohenriquesg.fahrenheit.R
import com.paulohenriquesg.fahrenheit.ui.Space
import kotlinx.coroutines.delay

/** Past this, a check is slow enough to say what it is waiting for. */
private const val SLOW_CHECK_MS = 3_000L

/**
 * Opening the app with a stored session (#191): the launch screen while
 * [check] runs, then Home through [onReady], or [form] once [onSignIn] has
 * been told why. With no [check], that is no stored session, the form shows
 * straight away, as it always has.
 *
 * On Ready the launch screen stays up while Home starts, so the form never
 * flashes past on the way.
 */
@Composable
fun LaunchGate(
    check: (suspend () -> SessionCheck.Result)?,
    onReady: () -> Unit,
    onSignIn: (LoginError?) -> Unit,
    form: @Composable () -> Unit
) {
    var checking by remember { mutableStateOf(check != null) }
    val ready by rememberUpdatedState(onReady)
    val signIn by rememberUpdatedState(onSignIn)
    if (check != null) {
        LaunchedEffect(Unit) {
            when (val result = check()) {
                SessionCheck.Result.Ready -> ready()
                is SessionCheck.Result.SignIn -> {
                    signIn(result.error)
                    checking = false
                }
            }
        }
    }
    if (checking) LaunchScreen() else form()
}

/**
 * The splash's logo, then the name and what is happening. Nothing from the
 * stored session is drawn here: the server address is not for the room.
 * The status changes once, on a timer; nothing on it animates.
 */
@Composable
fun LaunchScreen() {
    var slow by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(SLOW_CHECK_MS)
        slow = true
    }
    Box(Modifier.fillMaxSize().testTag("launch_screen")) {
        LoginBackdrop(emptyList())
        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = Space.screenH),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                painter = painterResource(R.mipmap.ic_launcher),
                contentDescription = null,
                modifier = Modifier.size(96.dp)
            )
            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.ExtraBold),
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(top = Space.gap).testTag("launch_brand")
            )
            Text(
                text = stringResource(if (slow) R.string.launch_connecting else R.string.launch_signing_in),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp).testTag("launch_status")
            )
        }
    }
}
