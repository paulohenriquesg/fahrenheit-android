package com.paulohenriquesg.fahrenheit.podcast

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.tv.material3.Button
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.paulohenriquesg.fahrenheit.R
import com.paulohenriquesg.fahrenheit.ui.Space

const val FEED_CHECK_TAG = "check_new_episodes"

sealed interface FeedCheckState {
    data object Idle : FeedCheckState
    data object Checking : FeedCheckState
    /** The check returned; [count] episodes were queued, possibly none. */
    data class Found(val count: Int) : FeedCheckState
    data object Failed : FeedCheckState
}

/**
 * The admin's "Check for new episodes" button and what it last found (#76).
 *
 * Says how much it will fetch before it fetches it: every episode costs disk
 * on the server.
 */
@Composable
fun FeedCheckRow(state: FeedCheckState, onCheck: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        // Never disabled: a disabled button gives up focus, and on a TV the
        // focus would jump somewhere the viewer did not put it. A press during
        // a check is ignored instead.
        Button(
            onClick = { if (state != FeedCheckState.Checking) onCheck() },
            modifier = Modifier.testTag(FEED_CHECK_TAG)
        ) {
            Text(text = stringResource(R.string.check_new_episodes))
        }
        Spacer(modifier = Modifier.width(Space.gap))
        Text(
            text = when (state) {
                FeedCheckState.Idle -> stringResource(R.string.check_new_episodes_limit, FeedCheck.LIMIT)
                FeedCheckState.Checking -> stringResource(R.string.check_new_episodes_running)
                FeedCheckState.Failed -> stringResource(R.string.check_new_episodes_failed)
                is FeedCheckState.Found ->
                    if (state.count == 0) {
                        stringResource(R.string.check_new_episodes_none)
                    } else {
                        pluralStringResource(R.plurals.check_new_episodes_found, state.count, state.count)
                    }
            },
            style = MaterialTheme.typography.bodyLarge,
            color = if (state == FeedCheckState.Failed) {
                MaterialTheme.colorScheme.error
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            }
        )
    }
}
