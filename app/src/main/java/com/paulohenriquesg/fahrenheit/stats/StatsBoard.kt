package com.paulohenriquesg.fahrenheit.stats

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.paulohenriquesg.fahrenheit.R

private val Gap = 16.dp
private val PanelShape = RoundedCornerShape(10.dp)

/**
 * The listening stats, as boxes: the headline figures in a row, then the week
 * and the most listened books side by side, then the latest sessions.
 *
 * It scrolls. The screen it replaces did not, and its fourth card fell off the
 * bottom of a 1080p TV where nothing could reach it.
 */
@Composable
fun StatsBoard(summary: StatsSummary, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(Gap)
    ) {
        Text(
            text = stringResource(R.string.listening_statistics),
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground
        )

        Row(horizontalArrangement = Arrangement.spacedBy(Gap)) {
            StatTile(
                label = stringResource(R.string.stats_total_listened),
                value = shortDuration(summary.totalListened),
                modifier = Modifier.weight(1f)
            )
            StatTile(
                label = stringResource(R.string.stats_today),
                value = shortDuration(summary.today),
                modifier = Modifier.weight(1f)
            )
            StatTile(
                label = stringResource(R.string.stats_books_touched),
                value = summary.booksTouched.toString(),
                modifier = Modifier.weight(1f)
            )
            StatTile(
                label = stringResource(R.string.stats_days_with_activity),
                value = summary.activeDays.toString(),
                modifier = Modifier.weight(1f)
            )
            StatTile(
                label = stringResource(R.string.stats_average_per_day),
                value = shortDuration(summary.averagePerDay),
                modifier = Modifier.weight(1f)
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(Gap)) {
            Panel(
                title = stringResource(R.string.stats_by_weekday),
                modifier = Modifier
                    .weight(1.15f)
                    .heightIn(min = 160.dp)
            ) {
                WeekBars(summary.weekdays)
            }
            Panel(
                title = stringResource(R.string.stats_most_listened),
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 160.dp)
            ) {
                TopBooks(summary.topBooks)
            }
        }

        Panel(title = stringResource(R.string.stats_recent_sessions)) {
            Sessions(summary.recentSessions)
        }
    }
}

@Composable
private fun StatTile(label: String, value: String, modifier: Modifier = Modifier) {
    var focused by remember { mutableStateOf(false) }
    Column(
        modifier = modifier
            .clip(PanelShape)
            .background(MaterialTheme.colorScheme.surface)
            .border(
                width = if (focused) 3.dp else 1.dp,
                color = if (focused) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.surfaceVariant
                },
                shape = PanelShape
            )
            .onFocusChanged { focused = it.isFocused }
            .focusable()
            .padding(14.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun Panel(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(PanelShape)
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.surfaceVariant, PanelShape)
            .padding(14.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(12.dp))
        content()
    }
}

@Composable
private fun WeekBars(weekdays: List<WeekdayShare>) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(110.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        weekdays.forEach { day ->
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        // A day with nothing on it still gets a sliver, so the
                        // week reads as seven days rather than four.
                        .weight(day.share.coerceAtLeast(0.02f))
                        .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                        .background(
                            if (day.share >= 1f) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.secondary
                            }
                        )
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = day.label,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun TopBooks(books: List<BookTime>) {
    if (books.isEmpty()) {
        Text(
            text = stringResource(R.string.stats_nothing_listened_yet),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        return
    }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        books.forEach { book ->
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = book.title,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = shortDuration(book.seconds),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(book.share.coerceIn(0f, 1f))
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(3.dp))
                            .background(MaterialTheme.colorScheme.secondary)
                    )
                }
            }
        }
    }
}

@Composable
private fun Sessions(sessions: List<SessionRow>) {
    if (sessions.isEmpty()) {
        Text(
            text = stringResource(R.string.stats_nothing_listened_yet),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        return
    }
    Row(horizontalArrangement = Arrangement.spacedBy(Gap)) {
        sessions.forEach { session ->
            Row(
                modifier = Modifier
                    .weight(1f)
                    .clip(PanelShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                    .padding(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color.Black.copy(alpha = 0.35f))
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = session.title,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = shortDuration(session.seconds),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
            }
        }
    }
}
