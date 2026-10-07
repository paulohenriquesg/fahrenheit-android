package com.paulohenriquesg.fahrenheit.player

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import com.paulohenriquesg.fahrenheit.R
import com.paulohenriquesg.fahrenheit.api.LibraryItemMetadata

/** One of About's facts: what it is, and its value as written. */
data class AboutFact(val kind: Kind, val value: String) {
    /** [Progress] only on the book screen (#134): how far in, or finished. [Show], [Season] and [Episode] an episode's (#178). */
    enum class Kind { ReadBy, Publisher, Published, Length, Genres, Progress, Show, Season, Episode }
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

    /**
     * An episode's (#178): its show, when it came out in [EpisodeDate]'s words,
     * how long it is, and its season and number when the feed gives them.
     */
    fun episode(show: String?, published: String?, length: Double?, season: String?, episode: String?): List<AboutFact> = listOfNotNull(
        show?.takeIf { it.isNotBlank() }?.let { AboutFact(AboutFact.Kind.Show, it) },
        published?.takeIf { it.isNotBlank() }?.let { AboutFact(AboutFact.Kind.Published, it) },
        length?.takeIf { it > 0 }?.let { AboutFact(AboutFact.Kind.Length, PlaybackPosition.spoken(it)) },
        season?.takeIf { it.isNotBlank() }?.let { AboutFact(AboutFact.Kind.Season, it.trim()) },
        episode?.takeIf { it.isNotBlank() }?.let { AboutFact(AboutFact.Kind.Episode, it.trim()) }
    )
}

@Composable
fun AboutChip(panels: PlayerPanels) = ActionChip(
    text = stringResource(R.string.about),
    onClick = { panels.open(PlayerPanel.About) },
    modifier = Modifier.focusRequester(panels.opener(PlayerPanel.About))
)

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
        AboutFact.Kind.Show -> R.string.fact_show
        AboutFact.Kind.Season -> R.string.fact_season
        AboutFact.Kind.Episode -> R.string.fact_episode
    }
)
