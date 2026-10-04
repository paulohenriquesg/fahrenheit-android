package com.paulohenriquesg.fahrenheit.ui.elements

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import androidx.compose.foundation.Image
import coil.compose.rememberAsyncImagePainter
import coil.compose.AsyncImagePainter
import coil.request.ImageRequest
import coil.size.Scale
import androidx.compose.ui.platform.LocalDensity
import com.paulohenriquesg.fahrenheit.api.ApiClient

/**
 * An item's cover, drawn as the server's own web client draws it (#149).
 *
 * - No cover (the server answers 404) or one that fails to load: a
 *   placeholder with the title in the middle and the author near the bottom,
 *   not an empty box.
 * - A cover of another shape, as podcasts' often are, is fitted whole inside
 *   the square over a blurred, dimmed copy of itself rather than cropped.
 *   Blur needs Android 12; older sticks draw the copy dimmed but sharp.
 *
 * @param title the placeholder's title; [contentDescription] when null.
 * @param url where the cover is fetched; the item's cover on the server.
 */
@Composable
fun CoverImage(
    itemId: String,
    contentDescription: String,
    size: Dp = 200.dp,
    title: String? = null,
    author: String? = null,
    url: String? = ApiClient.generateFullUrl("/api/items/$itemId/cover")
) {
    val context = LocalContext.current
    val sidePx = with(LocalDensity.current) { size.roundToPx() }
    val request = remember(url, sidePx) {
        ImageRequest.Builder(context)
            .data(url)
            .addHeader("Authorization", "Bearer ${ApiClient.getToken() ?: ""}")
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
    val loaded = painter.state is AsyncImagePainter.State.Success

    Box(modifier = Modifier.size(size)) {
        if (!loaded) {
            CoverPlaceholder(title = title ?: contentDescription, author = author, size = size)
        }
        // Behind the cover: the same image, cropped to the square and dimmed,
        // so a wide or tall cover sits in a band without bare edges.
        Image(
            painter = painter,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(size)
                .blur(16.dp)
                .drawWithContent {
                    drawContent()
                    if (loaded) drawRect(Color.Black.copy(alpha = BACKDROP_DIM))
                }
        )
        Image(
            painter = painter,
            contentDescription = contentDescription,
            contentScale = ContentScale.Fit,
            modifier = Modifier.size(size)
        )
    }
}

/** The title in the middle and the author near the bottom, as on the web client. */
@Composable
private fun CoverPlaceholder(title: String, author: String?, size: Dp) {
    Box(
        modifier = Modifier
            .size(size)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .testTag(CoverTags.PLACEHOLDER)
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
            )
        }
    }
}

/** How far the copy behind a fitted cover is darkened. */
private const val BACKDROP_DIM = 0.45f

object CoverTags {
    const val PLACEHOLDER = "cover-placeholder"
    const val GROUP_COVER = "group-cover"
}
