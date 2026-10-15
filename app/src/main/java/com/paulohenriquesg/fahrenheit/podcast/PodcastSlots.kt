package com.paulohenriquesg.fahrenheit.podcast

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.paulohenriquesg.fahrenheit.auth.SessionRole
import com.paulohenriquesg.fahrenheit.ui.elements.PlaceholderShape

/**
 * Which of an admin's buttons the podcast page keeps a place for before it
 * knows the item (#225): its loading frame draws them as grey pills, and the
 * page keeps an unseen one where the item turns out not to need it (a show
 * with no feed), so the cover and name above never move. From the role kept
 * with the session; with none, a listener's page.
 */
data class PodcastSlots(val feedCheck: Boolean, val downloads: Boolean) {
    companion object {
        const val TAG = "podcast_reserved_slot"
        val NONE = PodcastSlots(feedCheck = false, downloads = false)

        /** Mirrors `FeedCheck.mayCheck`'s type rule and `DownloadSettings.mayChange`. */
        fun of(role: SessionRole?): PodcastSlots = if (role == null) NONE else PodcastSlots(
            feedCheck = role.type == "admin" || role.type == "root",
            downloads = role.mayUpdate
        )
    }
}

/** The kept place for the feed check, grey in the loading frame, unseen on the page. */
@Composable
internal fun FeedCheckSlot(grey: Boolean) = ReservedSlot(grey) {
    FeedCheckButton(FeedCheckState.Idle, onCheck = {}, enabled = false)
}

/** The kept place for Automatic downloads; its summary is one line, as the real one's. */
@Composable
internal fun DownloadsSlot(grey: Boolean) = ReservedSlot(grey) {
    DownloadsButton(summary = " ", onClick = {}, enabled = false)
}

/**
 * The button itself, unseen and disabled - so it is exactly its size and
 * cannot take focus - with a grey pill over it in the loading frame.
 */
@Composable
private fun ReservedSlot(grey: Boolean, button: @Composable () -> Unit) {
    Box(Modifier.clearAndSetSemantics { testTag = PodcastSlots.TAG }) {
        Box(Modifier.alpha(0f)) { button() }
        if (grey) PlaceholderShape(Modifier.matchParentSize())
    }
}

/**
 * The facts' place: three lines, as many as the page shows, so the column above
 * them is the same height whether they have arrived or not.
 */
@Composable
internal fun FactsSlot() {
    Column(
        Modifier.alpha(0f).clearAndSetSemantics {},
        verticalArrangement = Arrangement.spacedBy(FACT_SPACING)
    ) {
        repeat(FACT_LINES) { Text(" ", style = MaterialTheme.typography.bodyMedium, maxLines = 1) }
    }
}

internal val FACT_SPACING = 4.dp
private const val FACT_LINES = 3
