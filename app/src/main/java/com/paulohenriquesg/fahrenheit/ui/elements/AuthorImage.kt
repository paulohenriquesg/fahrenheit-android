package com.paulohenriquesg.fahrenheit.ui.elements

import androidx.compose.foundation.layout.Box
import com.paulohenriquesg.fahrenheit.R
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.paulohenriquesg.fahrenheit.api.ApiClient

/** Where an author's photo is fetched: one URL, Coil's memory-cache key, as [coverUrlOf] for covers. */
fun authorPhotoUrlOf(authorId: String): String? = ApiClient.generateFullUrl("/api/authors/$authorId/image")

@Composable
fun AuthorImage(
    authorId: String,
    imagePath: String?,
    contentDescription: String,
    modifier: Modifier = Modifier,
    size: androidx.compose.ui.unit.Dp = 200.dp
) {
    val context = LocalContext.current
    val imageUrl = authorPhotoUrlOf(authorId)

    Box(
        modifier = modifier
            .width(size)
            .height(size)
    ) {
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(imageUrl)
                .addHeader("Authorization", "Bearer ${ApiClient.getToken() ?: ""}")
                .crossfade(true)
                .build(),
            contentDescription = contentDescription,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .width(size)
                .height(size),
            placeholder = null,
            error = null
        )

        // This will show if image fails to load (Coil shows nothing by default on error)
        // The Box background could be customized here if needed
    }
}

@Composable
fun PlaceholderImage(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.Person,
            contentDescription = stringResource(R.string.author_placeholder),
            modifier = Modifier.fillMaxSize(),
            tint = androidx.tv.material3.MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
        )
    }
}
