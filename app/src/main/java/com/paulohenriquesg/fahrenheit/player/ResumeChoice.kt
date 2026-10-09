package com.paulohenriquesg.fahrenheit.player

import android.content.res.Resources
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Border
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonBorder
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.LocalContentColor
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.OutlinedButton
import androidx.tv.material3.OutlinedButtonDefaults
import androidx.tv.material3.Text
import com.paulohenriquesg.fahrenheit.R
import com.paulohenriquesg.fahrenheit.ui.elements.CoverImage
import com.paulohenriquesg.fahrenheit.ui.requestFocusWhenAttached
import java.util.Locale
import kotlin.math.roundToInt

/** What the card shows of the item (#158). [length] is the whole book's or episode's, in seconds. */
data class ResumeItem(
    val itemId: String,
    val title: String,
    val length: Double,
    val chapters: List<ChapterSpan>,
    val episode: Boolean
)

/** The card's words (#158). */
data class ResumeWording(
    val headline: String,
    val heard: String,
    val hereMark: String,
    val thereMark: String,
    val continueLabel: String,
    val continueDetail: String,
    val stayLabel: String,
    val stayDetail: String
)

/**
 * "You listened further on iPhone · 10 minutes ago · 1 h 20 min there,
 * 1 h 05 min here", and the buttons' words (#158, after #90).
 *
 * A book's places are its chapters, when both have a name and they differ;
 * otherwise, and for an episode, times. The device's name goes in bare: Audiobookshelf
 * names an Android device "manufacturer model", so "your" would not always fit.
 *
 * @param now the time [ResumeOffer.listenedAt] is measured against, in ms.
 */
fun resumeWording(resources: Resources, offer: ResumeOffer, item: ResumeItem, now: Long): ResumeWording {
    val further = offer.there > offer.here
    val device = offer.device
    val headline = when {
        device != null -> resources.getString(if (further) R.string.resume_further_on else R.string.resume_back_on, device)
        item.episode -> resources.getString(if (further) R.string.resume_episode_further_elsewhere else R.string.resume_episode_back_elsewhere)
        else -> resources.getString(if (further) R.string.resume_book_further_elsewhere else R.string.resume_book_back_elsewhere)
    }
    val (hereTime, thereTime) = positionLabels(offer.here, offer.there)
    val chapters = if (item.episode) null else chapterNames(item.chapters, offer)
    val herePlace = chapters?.first ?: hereTime
    val therePlace = chapters?.second ?: thereTime
    // With a chapter on the button, its time goes in the detail.
    val withTime = { time: String, detail: String ->
        if (chapters == null) detail else resources.getString(R.string.resume_detail, time, detail)
    }
    return ResumeWording(
        headline = headline,
        heard = resources.getString(R.string.resume_heard, heardAgo(resources, now - offer.listenedAt), thereTime, hereTime),
        hereMark = resources.getString(R.string.resume_mark_here, herePlace),
        thereMark = resources.getString(R.string.resume_mark_there, device ?: resources.getString(R.string.resume_elsewhere), therePlace),
        continueLabel = resources.getString(R.string.resume_continue_from, therePlace),
        continueDetail = withTime(
            thereTime,
            device?.let { resources.getString(R.string.resume_left_off_there, it) } ?: resources.getString(R.string.resume_newer_position)
        ),
        stayLabel = resources.getString(R.string.resume_stay_at, herePlace),
        stayDetail = withTime(hereTime, resources.getString(R.string.resume_left_off_here))
    )
}

/** Here's and there's chapter names; null when either has none or they are one chapter. */
private fun chapterNames(spans: List<ChapterSpan>, offer: ResumeOffer): Pair<String, String>? {
    val here = ChapterClock.at(spans, offer.here)?.title?.takeIf { it.isNotBlank() } ?: return null
    val there = ChapterClock.at(spans, offer.there)?.title?.takeIf { it.isNotBlank() } ?: return null
    return if (here == there) null else here to there
}

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

/**
 * Both places, apart: to the minute, or to the second when the minutes match
 * (more than 30 s apart, yet in one minute).
 */
private fun positionLabels(here: Double, there: Double): Pair<String, String> {
    val minutes = positionLabel(here) to positionLabel(there)
    if (minutes.first != minutes.second) return minutes
    return withSeconds(here) to withSeconds(there)
}

private fun withSeconds(seconds: Double): String =
    String.format(Locale.ROOT, "%s %02d s", positionLabel(seconds), seconds.coerceAtLeast(0.0).toInt() % 60)

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

/** A place on the card's timeline: its share of the item, 0 to 1. */
private fun share(position: Double, length: Double): Float =
    if (length <= 0) 0f else (position / length).coerceIn(0.0, 1.0).toFloat()

private val MARK = 18.dp

/** As tall as the transport's Play, as the card is the player's own. */
private val BUTTON_HEIGHT = 60.dp

private val ANSWER_SHAPE = RoundedCornerShape(16.dp)

/**
 * The library's outline for an outlined button, strokes and colours as they
 * are, drawn on the answer's own corners: the default keeps its pill (#209).
 */
@Composable
private fun answerBorder(): ButtonBorder {
    val color = MaterialTheme.colorScheme.onSurfaceVariant
    val disabled = Border(BorderStroke(1.5.dp, color.copy(alpha = 0.2f)), shape = ANSWER_SHAPE)
    return OutlinedButtonDefaults.border(
        border = Border(BorderStroke(1.5.dp, color.copy(alpha = 0.4f)), shape = ANSWER_SHAPE),
        focusedBorder = Border(BorderStroke(1.65.dp, color), shape = ANSWER_SHAPE),
        pressedBorder = Border(BorderStroke(1.5.dp, color), shape = ANSWER_SHAPE),
        disabledBorder = disabled,
        focusedDisabledBorder = disabled
    )
}

/**
 * The question, as a card over the dimmed player (#158;
 * docs/mocks/resume-question.html). Focus starts on the server's position:
 * the listener most likely moved on elsewhere on purpose.
 */
@OptIn(ExperimentalComposeUiApi::class) // focusProperties.exit
@Composable
fun ResumeChoice(offer: ResumeOffer, item: ResumeItem, now: Long, onContinue: () -> Unit, onStay: () -> Unit) {
    val resources = LocalContext.current.resources
    val words = resumeWording(resources, offer, item, now)
    // Back is the safe answer: stay where this player is.
    BackHandler(onBack = onStay)
    val continueFocus = remember { FocusRequester() }
    LaunchedEffect(offer) { continueFocus.requestFocusWhenAttached() }
    // The player and its wash stay behind, dimmed.
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.6f))
            .testTag("resume_choice"),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .width(620.dp)
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.92f), RoundedCornerShape(24.dp))
                // Holds the D-pad until answered: the transport sits behind it.
                .focusProperties { exit = { FocusRequester.Cancel } }
                .focusGroup()
                .padding(horizontal = 36.dp, vertical = 32.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CoverImage(itemId = item.itemId, contentDescription = item.title, size = 72.dp)
                Column(modifier = Modifier.padding(start = 20.dp)) {
                    Text(
                        text = words.headline,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.testTag("resume_headline")
                    )
                    Text(
                        text = words.heard,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp).testTag("resume_heard")
                    )
                }
            }
            Timeline(
                here = share(offer.here, item.length),
                there = share(offer.there, item.length),
                hereMark = words.hereMark,
                thereMark = words.thereMark,
                modifier = Modifier.padding(vertical = 8.dp)
            )
            // A focused answer grows by a tenth, about 13 dp here: room for it (#209).
            Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                Button(
                    onClick = onContinue,
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = BUTTON_HEIGHT)
                        .focusRequester(continueFocus)
                        .testTag("resume_continue"),
                    shape = ButtonDefaults.shape(ANSWER_SHAPE)
                ) {
                    Answer(words.continueLabel, words.continueDetail)
                }
                OutlinedButton(
                    onClick = onStay,
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = BUTTON_HEIGHT)
                        .testTag("resume_stay"),
                    shape = OutlinedButtonDefaults.shape(ANSWER_SHAPE),
                    border = answerBorder()
                ) {
                    Answer(words.stayLabel, words.stayDetail)
                }
            }
        }
    }
}

@Composable
private fun Answer(label: String, detail: String) {
    Column {
        Text(label, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        // The button's content colour, a little quieter.
        Text(
            text = detail,
            style = MaterialTheme.typography.bodyMedium,
            color = LocalContentColor.current.copy(alpha = 0.75f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 2.dp)
        )
    }
}

/**
 * The item as a line, with "there" labelled above and "here" below, each
 * mark centred on its share of the item.
 */
@Composable
private fun Timeline(here: Float, there: Float, hereMark: String, thereMark: String, modifier: Modifier = Modifier) {
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    val primary = MaterialTheme.colorScheme.primary
    Column(modifier.fillMaxWidth()) {
        Label(thereMark, there, primary, bold = true, tag = "resume_there_label")
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .height(MARK)
        ) {
            val width = maxWidth
            Box(
                Modifier
                    .align(Alignment.CenterStart)
                    .fillMaxWidth()
                    .height(5.dp)
                    .background(muted.copy(alpha = 0.3f), CircleShape)
                    .testTag("resume_line")
            ) {
                // Heard so far, up to the further of the two.
                Box(
                    Modifier
                        .fillMaxHeight()
                        .width(width * maxOf(here, there))
                        .background(muted.copy(alpha = 0.6f), CircleShape)
                )
            }
            Mark(here, width, muted, "resume_mark_here")
            Mark(there, width, primary, "resume_mark_there")
        }
        Label(hereMark, here, muted, bold = false, tag = "resume_here_label")
    }
}

@Composable
private fun Mark(share: Float, width: Dp, color: Color, tag: String) {
    Box(
        Modifier
            .offset(x = width * share - MARK / 2)
            .size(MARK)
            .background(MaterialTheme.colorScheme.surface, CircleShape)
            .padding(3.dp)
            .background(color, CircleShape)
            .testTag(tag)
    )
}

/** A mark's label, centred on it where there is room, kept inside the line otherwise. */
@Composable
private fun Label(text: String, share: Float, color: Color, bold: Boolean, tag: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyLarge,
        fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal,
        color = color,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier
            .fillMaxWidth()
            .wrapAt(share)
            .padding(vertical = 4.dp)
            .testTag(tag)
    )
}

/** Lays its content out at its own width, centred on [share] of the space and clamped to it. */
private fun Modifier.wrapAt(share: Float): Modifier = layout { measurable, constraints ->
    val placeable = measurable.measure(constraints.copy(minWidth = 0))
    val space = constraints.maxWidth
    val x = (space * share - placeable.width / 2f).roundToInt().coerceIn(0, (space - placeable.width).coerceAtLeast(0))
    layout(space, placeable.height) { placeable.place(x, 0) }
}
