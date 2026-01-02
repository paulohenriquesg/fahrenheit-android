package com.paulohenriquesg.fahrenheit.ui.elements

import androidx.compose.foundation.layout.Box
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

@Composable
fun AuthorImage(
    authorId: String,
    imagePath: String?,
    contentDescription: String,
    modifier: Modifier = Modifier,
    size: androidx.compose.ui.unit.Dp = 200.dp
) {
    val context = LocalContext.current
    val imageUrl = ApiClient.generateFullUrl("/api/authors/$authorId/image")

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
            contentDescription = "Author placeholder",
            modifier = Modifier.fillMaxSize(),
            tint = androidx.tv.material3.MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
        )
    }
}
