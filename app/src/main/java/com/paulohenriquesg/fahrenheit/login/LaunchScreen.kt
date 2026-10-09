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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.paulohenriquesg.fahrenheit.R
import com.paulohenriquesg.fahrenheit.ui.Space
import com.paulohenriquesg.fahrenheit.ui.rememberInitialFocus
import kotlinx.coroutines.delay

/** Past this, a check is slow enough to say what it is waiting for. */
private const val SLOW_CHECK_MS = 3_000L

/** The waits before asking again on its own, after each check with no answer. */
private val RETRY_AFTER_MS = longArrayOf(2_000L, 4_000L, 8_000L)

/**
 * Opening the app with a stored session (#191): the launch screen while
 * [check] runs, then Home through [onReady]. Only [SessionCheck.Result.Rejected]
 * brings the sign-in [form]. With no answer the launch screen stays, says so
 * and offers Try again, and asks again on its own after 2, 4 and 8 seconds.
 * A press of Try again asks at once and starts those waits over. The stored
 * session is not touched here. With no [check], meaning no stored session,
 * the form shows straight away, as it always has.
 *
 * On Ready the launch screen stays up while Home starts, so the form never
 * flashes past on the way.
 */
@Composable
fun LaunchGate(
    check: (suspend () -> SessionCheck.Result)?,
    onReady: () -> Unit,
    form: @Composable () -> Unit
) {
    var rejected by remember { mutableStateOf(check == null) }
    var unreachable by remember { mutableStateOf(false) }
    // Each press of Try again is a new round: it cancels the wait in progress.
    var round by remember { mutableIntStateOf(0) }
    val ready by rememberUpdatedState(onReady)
    if (check != null) {
        LaunchedEffect(round) {
            // One check, then one more after each wait: a bounded run, so a
            // server that stays away leaves the screen idle, waiting for a press.
            repeat(RETRY_AFTER_MS.size + 1) { attempt ->
                when (check()) {
                    SessionCheck.Result.Ready -> return@LaunchedEffect ready()
                    SessionCheck.Result.Rejected -> {
                        rejected = true
                        return@LaunchedEffect
                    }
                    SessionCheck.Result.Unreachable -> unreachable = true
                }
                RETRY_AFTER_MS.getOrNull(attempt)?.let { delay(it) }
            }
        }
    }
    if (rejected) form() else LaunchScreen(unreachable = unreachable, onTryAgain = { round++ })
}

/**
 * The splash's logo, then the name and what is happening. Nothing from the
 * stored session is drawn here: the server address is not for the room.
 * The status changes on timers; nothing on it animates.
 *
 * @param unreachable no answer from the server: says so, with Try again
 *   focused, since nothing responds to a remote until something holds focus.
 */
@Composable
fun LaunchScreen(unreachable: Boolean = false, onTryAgain: () -> Unit = {}) {
    var slow by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(SLOW_CHECK_MS)
        slow = true
    }
    val tryAgainFocus = rememberInitialFocus(enabled = unreachable)
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
                text = stringResource(
                    when {
                        unreachable -> R.string.launch_unreachable
                        slow -> R.string.launch_connecting
                        else -> R.string.launch_signing_in
                    }
                ),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp).testTag("launch_status")
            )
            if (unreachable) {
                LoginButton(
                    text = stringResource(R.string.launch_try_again),
                    onClick = onTryAgain,
                    primary = true,
                    modifier = Modifier
                        .padding(top = Space.gap)
                        .focusRequester(tryAgainFocus)
                        .testTag("launch_try_again")
                )
            }
        }
    }
}
