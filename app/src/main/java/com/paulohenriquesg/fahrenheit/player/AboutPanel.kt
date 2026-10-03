package com.paulohenriquesg.fahrenheit.player

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Border
import androidx.tv.material3.Button
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.OutlinedButton
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import com.paulohenriquesg.fahrenheit.R
import com.paulohenriquesg.fahrenheit.api.LibraryItemMetadata
import com.paulohenriquesg.fahrenheit.detail.FullDescription
import com.paulohenriquesg.fahrenheit.ui.components.SeriesShelf
import com.paulohenriquesg.fahrenheit.ui.StableKeys
import com.paulohenriquesg.fahrenheit.ui.elements.CoverImage
import com.paulohenriquesg.fahrenheit.ui.rememberInitialFocus
import com.paulohenriquesg.fahrenheit.ui.requestFocusWhenAttached

/** One of About's facts: what it is, and its value as written. */
data class AboutFact(val kind: Kind, val value: String) {
    /** [Progress] only on the book screen (#134): how far in, or finished. */
    enum class Kind { ReadBy, Publisher, Published, Length, Genres, Progress }
}

/** About's facts, one line each, in the mock's order; a missing one is left out (#107). */
object AboutFacts {
    fun book(metadata: LibraryItemMetadata, length: Double?): List<AboutFact> = listOfNotNull(
        (metadata.narrators?.filter { it.isNotBlank() }?.takeIf { it.isNotEmpty() }?.joinToString(", ")
            ?: metadata.narratorName?.takeIf { it.isNotBlank() })
            ?.let { AboutFact(AboutFact.Kind.ReadBy, it) },
        metadata.publisher?.takeIf { it.isNotBlank() }?.let { AboutFact(AboutFact.Kind.Publisher, it) },
        metadata.publishedYear?.takeIf { it.isNotBlank() }?.let { AboutFact(AboutFact.Kind.Published, it) },
        length?.takeIf { it > 0 }?.let { AboutFact(AboutFact.Kind.Length, PlaybackPosition.spoken(it)) },
        metadata.genres?.filter { it.isNotBlank() }?.takeIf { it.isNotEmpty() }
            ?.let { AboutFact(AboutFact.Kind.Genres, it.joinToString(", ")) }
    )

    /** An episode's: when it came out, in [EpisodeDate]'s words, and how long it is. */
    fun episode(published: String?, length: Double?): List<AboutFact> = listOfNotNull(
        published?.takeIf { it.isNotBlank() }?.let { AboutFact(AboutFact.Kind.Published, it) },
        length?.takeIf { it > 0 }?.let { AboutFact(AboutFact.Kind.Length, PlaybackPosition.spoken(it)) }
    )
}

@Composable
fun AboutChip(panels: PlayerPanels) = ActionChip(
    text = stringResource(R.string.about),
    onClick = { panels.open(PlayerPanel.About) },
    modifier = Modifier.focusRequester(panels.opener(PlayerPanel.About))
)

/**
 * About (frame "A, with About open"): what you use to decide, on demand.
 * The description first - focus lands on it, and Down scrolls a long one -
 * then the series with this book marked, the facts, and Mark finished last,
 * off the main screen where a stray press would cost a listener their place.
 *
 * @param series null for a book in no series, or one not (yet) known.
 * @param finished null for an episode: it has no Mark finished.
 */
@Composable
fun AboutPanel(
    description: String?,
    facts: List<AboutFact>,
    series: SeriesBooks?,
    finished: Boolean?,
    marking: Boolean = false,
    onPlayInstead: (SeriesBook) -> Unit,
    onMarkFinished: (Boolean) -> Unit,
    onClose: () -> Unit
) {
    SidePanel(stringResource(R.string.about), onClose, width = 430.dp) {
        val landing = rememberInitialFocus(enabled = true)
        val text = description?.takeIf { it.isNotBlank() }
        val row = series?.takeIf { it.total > 1 }
        // Focus must land inside the panel whatever it holds, or it stays on
        // the chip behind the scrim: the description, else this book, else
        // Mark finished, else the facts themselves.
        val landOn = when {
            text != null -> Landing.Description
            row != null -> Landing.Series
            finished != null -> Landing.Mark
            else -> Landing.Facts
        }
        fun Modifier.landing(on: Landing) = if (landOn == on) focusRequester(landing) else this
        // Long facts and a long description together outgrow the screen.
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            text?.let {
                val scroll = rememberScrollState()
                Box(Modifier.heightIn(max = 170.dp).verticalScroll(scroll)) {
                    FullDescription(it, scroll, modifier = Modifier.landing(Landing.Description), alwaysFocusable = true)
                }
            }
            row?.let {
                SeriesShelf(
                    series = it,
                    label = stringResource(R.string.about_series),
                    askBeforeSwitching = true,
                    onChoose = onPlayInstead,
                    landHere = Modifier.landing(Landing.Series)
                )
            }
            Column(Modifier.landing(Landing.Facts).then(if (landOn == Landing.Facts) Modifier.focusable() else Modifier)) {
                facts.forEach { FactLine(it) }
            }
            finished?.let { done ->
                OutlinedButton(
                    onClick = { if (!marking) onMarkFinished(!done) },
                    // One mark at a time: a second press would send a second request.
                    enabled = !marking,
                    modifier = Modifier.padding(top = 14.dp).landing(Landing.Mark)
                ) {
                    Text(stringResource(if (done) R.string.mark_unfinished else R.string.mark_finished))
                }
            }
        }
    }
}

private enum class Landing { Description, Series, Mark, Facts }

/** "Read by A Reader": the label quiet and bold, the value plain. */
@Composable
private fun FactLine(fact: AboutFact) {
    val label = factLabel(fact.kind)
    Text(
        buildAnnotatedString {
            withStyle(SpanStyle(fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)) { append(label) }
            append(" ")
            append(fact.value)
        },
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

/** What a fact is called: "Read by", "Length". */
@Composable
fun factLabel(kind: AboutFact.Kind): String = stringResource(
    when (kind) {
        AboutFact.Kind.ReadBy -> R.string.fact_read_by
        AboutFact.Kind.Publisher -> R.string.fact_publisher
        AboutFact.Kind.Published -> R.string.fact_published
        AboutFact.Kind.Length -> R.string.fact_length
        AboutFact.Kind.Genres -> R.string.fact_genres
        AboutFact.Kind.Progress -> R.string.fact_progress
    }
)
