package com.paulohenriquesg.fahrenheit.player

import android.content.res.Resources
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Button
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.OutlinedButton
import androidx.tv.material3.Text
import com.paulohenriquesg.fahrenheit.R
import java.util.Locale

/**
 * "You're at 1 h 05 min here. Continue from 1 h 20 min, listened to 10
 * minutes ago on another device?" (#90)
 *
 * @param now the time [ResumeOffer.listenedAt] is measured against, in ms.
 */
fun resumeQuestion(resources: Resources, offer: ResumeOffer, now: Long): String =
    resources.getString(
        R.string.resume_question,
        positionLabel(offer.here),
        positionLabel(offer.there),
        heardAgo(resources, now - offer.listenedAt)
    )

/**
 * A place in a book as the question says it: "1 h 05 min", or "45 min" under
 * an hour. Locale.ROOT, as the units are English.
 */
internal fun positionLabel(seconds: Double): String {
    val wholeMinutes = (seconds.coerceAtLeast(0.0) / 60).toInt()
    val hours = wholeMinutes / 60
    val minutes = wholeMinutes % 60
    return if (hours > 0) String.format(Locale.ROOT, "%d h %02d min", hours, minutes)
    else String.format(Locale.ROOT, "%d min", minutes)
}

private fun heardAgo(resources: Resources, millis: Long): String {
    val minutes = (millis / 60_000).toInt()
    val hours = minutes / 60
    val days = hours / 24
    return when {
        minutes < 1 -> resources.getString(R.string.resume_just_now)
        hours < 1 -> resources.getQuantityString(R.plurals.resume_minutes_ago, minutes, minutes)
        days < 1 -> resources.getQuantityString(R.plurals.resume_hours_ago, hours, hours)
        else -> resources.getQuantityString(R.plurals.resume_days_ago, days, days)
    }
}

/**
 * The question, over the player. Focus starts on the server's position: the
 * listener most likely moved on elsewhere on purpose.
 */
@Composable
fun ResumeChoice(offer: ResumeOffer, now: Long, onContinue: () -> Unit, onStay: () -> Unit) {
    val resources = LocalContext.current.resources
    val continueFocus = remember { FocusRequester() }
    LaunchedEffect(offer) { continueFocus.requestFocus() }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.6f))
            .testTag("resume_choice"),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 640.dp)
                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(16.dp))
                .padding(28.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Text(
                text = resumeQuestion(resources, offer, now),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.testTag("resume_question")
            )
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Button(
                    onClick = onContinue,
                    modifier = Modifier
                        .focusRequester(continueFocus)
                        .testTag("resume_continue")
                ) {
                    Text(stringResource(R.string.resume_continue_from, positionLabel(offer.there)))
                }
                OutlinedButton(onClick = onStay, modifier = Modifier.testTag("resume_stay")) {
                    Text(stringResource(R.string.resume_stay_at, positionLabel(offer.here)))
                }
            }
        }
    }
}
