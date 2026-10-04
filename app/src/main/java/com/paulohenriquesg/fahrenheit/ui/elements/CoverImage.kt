package com.paulohenriquesg.fahrenheit.ui.elements

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsPropertyKey
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import coil.compose.AsyncImagePainter
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import coil.size.Scale
import com.paulohenriquesg.fahrenheit.api.ApiClient
import kotlin.math.abs

/**
 * An item's cover, drawn as the server's own web client draws it (#149).
 *
 * - No cover: a placeholder with the title in the middle and the author near
 *   the bottom, not an empty box. An item that says it has none is not asked
 *   for (a 404 is not cached, so it would be asked for on every drawing); one
 *   that fails to load gets the same placeholder. While a cover may still come
 *   the square is plain, so a cached one never flashes a title first.
 * - A cover of another shape, as podcasts' often are, is fitted whole inside
 *   the square over a blurred, dimmed copy of itself rather than cropped.
 *   Blur needs Android 12; older sticks draw the copy dimmed but sharp. A
 *   square cover fills the square and draws no copy.
 *
 * @param contentDescription what a screen reader says; null where the
 *   surrounding card already says it.
 * @param title the placeholder's title; [contentDescription] when null.
 * @param hasCover false when the item is known to have no cover.
 * @param url where the cover is fetched; the item's cover on the server.
 */
@Composable
fun CoverImage(
    itemId: String,
    contentDescription: String?,
    size: Dp = 200.dp,
    title: String? = null,
    author: String? = null,
    hasCover: Boolean = true,
    url: String? = ApiClient.generateFullUrl("/api/items/$itemId/cover")
) {
    Box(modifier = Modifier.size(size)) {
        if (!hasCover) {
            CoverPlaceholder(title = title ?: contentDescription.orEmpty(), author = author, size = size)
        } else {
            FetchedCover(url, contentDescription, size, title, author)
        }
    }
}

@Composable
private fun FetchedCover(url: String?, contentDescription: String?, size: Dp, title: String?, author: String?) {
    val context = LocalContext.current
    val sidePx = with(LocalDensity.current) { size.roundToPx() }
    // The token is a key: a screen open past its hour asks again with the new one.
    val token = ApiClient.getToken()
    val request = remember(url, sidePx, token) {
        ImageRequest.Builder(context)
            .data(url)
            .addHeader("Authorization", "Bearer ${token ?: ""}")
            // Decoded whole at the size drawn: the painter would otherwise wait
            // for a draw to learn its size, and Scale.FIT keeps a wide cover
            // whole for the fitted layer.
            .size(sidePx)
            .scale(Scale.FIT)
            .crossfade(true)
            .build()
    }
    // One painter drawn twice: two AsyncImages would fetch the cover twice.
    val painter = rememberAsyncImagePainter(request)

    // Kept for the login screen's backdrop: what is drawn here is on the device.
    val loaded = painter.state is AsyncImagePainter.State.Success
    LaunchedEffect(loaded, url) {
        if (loaded && url != null) RecentCovers.note(context, url)
    }

    when (painter.state) {
        is AsyncImagePainter.State.Error ->
            CoverPlaceholder(title = title ?: contentDescription.orEmpty(), author = author, size = size)
        is AsyncImagePainter.State.Success -> if (!painter.intrinsicSize.isNearlySquare()) {
            // Behind a cover of another shape: the same image, cropped to the
            // square and dimmed, so it sits in a band without bare edges.
            Image(
                painter = painter,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(size)
                    .testTag(CoverTags.BACKDROP)
                    .blur(16.dp)
                    .drawWithContent {
                        drawContent()
                        drawRect(Color.Black.copy(alpha = BACKDROP_DIM))
                    }
            )
        }
        else -> Box(Modifier.size(size).background(MaterialTheme.colorScheme.surfaceVariant))
    }
    Image(
        painter = painter,
        contentDescription = contentDescription,
        contentScale = ContentScale.Fit,
        modifier = Modifier
            .size(size)
            .then(if (painter.state is AsyncImagePainter.State.Success) Modifier.testTag(CoverTags.LOADED) else Modifier)
    )
}

/** As the web client judges it: within 15% of square needs no backdrop. */
private fun Size.isNearlySquare(): Boolean =
    width <= 0f || height <= 0f || abs(width / height - 1f) <= SQUARE_TOLERANCE

/** The title in the middle and the author near the bottom, as on the web client. */
@Composable
private fun CoverPlaceholder(title: String, author: String?, size: Dp) {
    Box(
        modifier = Modifier
            .size(size)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            // Decoration: the cover's image already says the title, and the
            // screen around it the author, so neither is read twice. Tests
            // read the lines from CoverPlaceholderLines instead.
            .clearAndSetSemantics {
                testTag = CoverTags.PLACEHOLDER
                this[CoverPlaceholderLines] = listOfNotNull(title, author)
            }
            .padding(size / 12)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth()
                .clearAndSetSemantics {}
        )
        if (author != null) {
            Text(
                text = author,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .clearAndSetSemantics {}
            )
        }
    }
}

/** How far the copy behind a fitted cover is darkened. */
private const val BACKDROP_DIM = 0.45f

private const val SQUARE_TOLERANCE = 0.15f

/** What a cover placeholder shows, for tests: its title, then its author. */
val CoverPlaceholderLines = SemanticsPropertyKey<List<String>>("CoverPlaceholderLines")

object CoverTags {
    const val PLACEHOLDER = "cover-placeholder"
    const val GROUP_COVER = "group-cover"
    const val BACKDROP = "cover-backdrop"
    const val LOADED = "cover-loaded"
}
