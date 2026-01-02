package com.paulohenriquesg.fahrenheit.ui.elements

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.paulohenriquesg.fahrenheit.api.ApiClient

@Composable
fun CoverImage(itemId: String, contentDescription: String, size: Dp = 200.dp) {
    val context = LocalContext.current
    val imageUrl = ApiClient.generateFullUrl("/api/items/$itemId/cover")

    Box(modifier = Modifier.size(size)) {
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(imageUrl)
                .addHeader("Authorization", "Bearer ${ApiClient.getToken() ?: ""}")
                .crossfade(true)
                .build(),
            contentDescription = contentDescription,
            contentScale = ContentScale.Crop,
            modifier = Modifier.size(size),
            placeholder = null,
            error = null
        )
    }
}