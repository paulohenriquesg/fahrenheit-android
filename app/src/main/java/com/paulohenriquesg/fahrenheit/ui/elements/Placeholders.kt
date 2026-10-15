package com.paulohenriquesg.fahrenheit.ui.elements

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import com.paulohenriquesg.fahrenheit.ui.Radius

/*
 * What a list draws while it loads (#224, docs/mocks/list-placeholders.html):
 * grey shapes the size of the cards on their way, so the screen has its layout
 * from the first frame and the cards replace them in place. They are still -
 * no shimmer - and nothing here can take focus, so the rail keeps it until
 * real cards arrive.
 */

object PlaceholderTags {
    const val SHAPE = "placeholder"
    const val ROUND = "placeholder-round"
    const val HEADING = "placeholder-heading"
}

/** One grey shape, in the covers' placeholder tone, so it reads as "a cover is coming". */
@Composable
fun PlaceholderShape(modifier: Modifier = Modifier, round: Boolean = false) {
    Box(
        modifier = modifier
            .clip(if (round) CircleShape else Radius.inner)
            .background(CoverPlaceholderTone.of(MaterialTheme.colorScheme))
            .clearAndSetSemantics { testTag = if (round) PlaceholderTags.ROUND else PlaceholderTags.SHAPE }
    )
}

/** Home: two shelves, each a heading over a row of covers. */
@Composable
fun ShelvesPlaceholder() {
    Column(modifier = Modifier.fillMaxSize().clipToBounds()) {
        repeat(SHELVES) {
            // A line of the real heading's text, unseen, so the bar takes its
            // height and the covers below do not move when the shelves arrive.
            Box(
                modifier = Modifier.clearAndSetSemantics { testTag = PlaceholderTags.HEADING },
                contentAlignment = Alignment.CenterStart
            ) {
                Box(Modifier.alpha(0f)) { ShelfHeading(" ") }
                PlaceholderShape(Modifier.width(160.dp).height(16.dp))
            }
            Spacer(modifier = Modifier.height(8.dp))
            CoverRowPlaceholder()
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

/** A row of covers; by default as `ShelfRow` lays them out. */
@Composable
fun CoverRowPlaceholder(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(horizontal = 16.dp),
    spacing: Dp = 8.dp
) {
    LazyRow(
        modifier = modifier,
        userScrollEnabled = false,
        horizontalArrangement = Arrangement.spacedBy(spacing),
        contentPadding = contentPadding
    ) {
        items(ROW_CARDS, key = { "placeholder-$it" }) { CoverCardPlaceholder() }
    }
}

/** Which card a browse grid is about to show. */
enum class GridPlaceholderLook(val minSize: Dp, val spacing: Dp) {
    /** `LibraryItemCard`, as Library's grid lays it out. */
    Covers(minSize = 200.dp, spacing = 0.dp),

    /** `BookGroupCard`: Series and Collections. */
    Groups(minSize = 200.dp, spacing = 16.dp),

    /** `AuthorCard`, round photo and all. */
    People(minSize = 180.dp, spacing = 16.dp)
}

/** A browse grid's first rows, in the grid's own columns. */
@Composable
fun GridPlaceholder(look: GridPlaceholderLook, modifier: Modifier = Modifier) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = look.minSize),
        userScrollEnabled = false,
        horizontalArrangement = Arrangement.spacedBy(look.spacing),
        verticalArrangement = Arrangement.spacedBy(look.spacing),
        modifier = modifier.fillMaxSize()
    ) {
        items(GRID_CARDS, key = { "placeholder-$it" }) {
            when (look) {
                GridPlaceholderLook.Covers -> CoverCardPlaceholder()
                GridPlaceholderLook.Groups -> GroupCardPlaceholder()
                GridPlaceholderLook.People -> PersonCardPlaceholder()
            }
        }
    }
}

/** Latest Episodes: a day's heading over rows of episodes, as `EpisodeCard` draws them. */
@Composable
fun RowsPlaceholder() {
    Column(
        modifier = Modifier.fillMaxSize().clipToBounds(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Grey too: until the episodes arrive nobody knows which days there are.
        PlaceholderShape(Modifier.padding(vertical = 8.dp).width(80.dp).height(14.dp))
        repeat(ROWS) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .background(MaterialTheme.colorScheme.surface, CardShape)
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                PlaceholderShape(Modifier.size(88.dp))
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    PlaceholderShape(Modifier.fillMaxWidth(0.6f).height(20.dp))
                    PlaceholderShape(Modifier.fillMaxWidth(0.4f).height(14.dp))
                    PlaceholderShape(Modifier.fillMaxWidth(0.8f).height(12.dp))
                }
            }
        }
    }
}

/** A card's body: its size and fill, with the grey shapes inside. */
@Composable
private fun CardPlaceholder(
    width: Dp,
    height: Dp,
    padding: Dp,
    horizontalAlignment: Alignment.Horizontal = Alignment.Start,
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier
            .width(width)
            .height(height)
            .background(MaterialTheme.colorScheme.surface, CardShape)
            .padding(padding),
        horizontalAlignment = horizontalAlignment,
        verticalArrangement = verticalArrangement,
        content = content
    )
}

@Composable
private fun CoverCardPlaceholder() {
    Box(modifier = Modifier.padding(8.dp)) {
        CardPlaceholder(width = 200.dp, height = 300.dp, padding = 16.dp) {
            PlaceholderShape(Modifier.fillMaxWidth().aspectRatio(1f))
            Spacer(modifier = Modifier.height(12.dp))
            PlaceholderShape(Modifier.fillMaxWidth(0.85f).height(20.dp))
            Spacer(modifier = Modifier.height(8.dp))
            PlaceholderShape(Modifier.fillMaxWidth(0.55f).height(14.dp))
        }
    }
}

@Composable
private fun GroupCardPlaceholder() {
    CardPlaceholder(
        width = 200.dp, height = 260.dp, padding = 12.dp,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        PlaceholderShape(Modifier.fillMaxWidth().height(150.dp))
        Spacer(modifier = Modifier.height(8.dp))
        PlaceholderShape(Modifier.fillMaxWidth(0.8f).height(18.dp))
        Spacer(modifier = Modifier.height(8.dp))
        PlaceholderShape(Modifier.fillMaxWidth(0.4f).height(12.dp))
    }
}

@Composable
private fun PersonCardPlaceholder() {
    CardPlaceholder(
        width = 180.dp, height = 240.dp, padding = 16.dp,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        PlaceholderShape(Modifier.size(120.dp), round = true)
        Spacer(modifier = Modifier.height(12.dp))
        PlaceholderShape(Modifier.fillMaxWidth(0.8f).height(18.dp))
        Spacer(modifier = Modifier.height(12.dp))
        PlaceholderShape(Modifier.fillMaxWidth(0.4f).height(12.dp))
    }
}

/** The TV `Card`'s own corners, so a card arriving does not change shape. */
private val CardShape = RoundedCornerShape(8.dp)

private const val SHELVES = 2
private const val ROW_CARDS = 6
private const val GRID_CARDS = 12
private const val ROWS = 5
