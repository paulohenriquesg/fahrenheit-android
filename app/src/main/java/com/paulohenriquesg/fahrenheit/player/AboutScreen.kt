package com.paulohenriquesg.fahrenheit.player

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import com.paulohenriquesg.fahrenheit.R
import com.paulohenriquesg.fahrenheit.ui.components.BookOverview
import com.paulohenriquesg.fahrenheit.ui.rememberInitialFocus

/**
 * A book's About from the player (#134): the one book layout, in place of the
 * player while it is open and over the same cover wash; Back returns to the
 * player. Drawn over the player rather than instead of it, so the player keeps
 * its state and focus goes back to the About chip, as for every panel.
 *
 * Focus lands on the description, or on Mark finished when there is none;
 * it cannot wander to the player hidden underneath.
 */
@OptIn(ExperimentalComposeUiApi::class) // focusProperties.exit
@Composable
fun AboutScreen(
    nowPlaying: NowPlaying,
    wash: Color?,
    series: SeriesBooks?,
    finished: Boolean?,
    marking: Boolean = false,
    onPlayInstead: (SeriesBook) -> Unit,
    onMarkFinished: (Boolean) -> Unit,
    onClose: () -> Unit
) {
    BackHandler(onBack = onClose)
    val markFocus = rememberInitialFocus(enabled = nowPlaying.description.isNullOrBlank() && finished != null)
    Box(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .coverWash(wash)
            .focusProperties { exit = { FocusRequester.Cancel } }
            .focusGroup()
            .padding(horizontal = 60.dp, vertical = 40.dp)
    ) {
        BookOverview(
            itemId = nowPlaying.itemId,
            title = nowPlaying.title,
            byline = nowPlaying.byline,
            description = nowPlaying.description,
            facts = nowPlaying.facts,
            series = series,
            seriesName = nowPlaying.series?.name,
            onSeriesBook = onPlayInstead,
            askBeforeSwitching = true,
            landOnDescription = true
        ) {
            finished?.let { done ->
                ActionChip(
                    text = stringResource(if (done) R.string.mark_unfinished else R.string.mark_finished),
                    onClick = { onMarkFinished(!done) },
                    modifier = Modifier.focusRequester(markFocus),
                    icon = Icons.Filled.Check,
                    // One mark at a time: the service first waits for the closing report.
                    enabled = !marking
                )
            }
        }
    }
}
