package com.paulohenriquesg.fahrenheit.ui.elements

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.paulohenriquesg.fahrenheit.api.ApiClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun AuthorImage(
    authorId: String,
    imagePath: String?,
    contentDescription: String,
    modifier: Modifier = Modifier
) {
    var bitmap by remember { mutableStateOf<ImageBitmap?>(null) }
    val context = LocalContext.current

    LaunchedEffect(authorId) {
        android.util.Log.d("AuthorImage", "Loading image for author $authorId")
        try {
            // Use the API endpoint /api/authors/{id}/image
            val imageUrl = ApiClient.generateFullUrl("/api/authors/$authorId/image")
            android.util.Log.d("AuthorImage", "Generated imageUrl: $imageUrl")

            if (imageUrl != null) {
                withContext(Dispatchers.IO) {
                    val token = ApiClient.getToken()
                    if (token != null) {
                        val response = okhttp3.OkHttpClient().newCall(
                            okhttp3.Request.Builder()
                                .url(imageUrl)
                                .header("Authorization", "Bearer $token")
                                .build()
                        ).execute()

                        android.util.Log.d("AuthorImage", "Response code: ${response.code}, body size: ${response.body?.contentLength()}")

                        if (response.isSuccessful) {
                            response.body?.byteStream()?.use { inputStream ->
                                BitmapFactory.decodeStream(inputStream)?.let {
                                    android.util.Log.d("AuthorImage", "Successfully decoded bitmap for $authorId")
                                    bitmap = it.asImageBitmap()
                                } ?: android.util.Log.e("AuthorImage", "Failed to decode bitmap for $authorId")
                            }
                        } else {
                            android.util.Log.e("AuthorImage", "Failed to load image: ${response.code} - ${response.message}")
                        }
                    } else {
                        android.util.Log.e("AuthorImage", "Token is null")
                    }
                }
            } else {
                android.util.Log.e("AuthorImage", "Generated imageUrl is null")
            }
        } catch (e: Exception) {
            android.util.Log.e("AuthorImage", "Error loading image for $authorId", e)
            e.printStackTrace()
        }
    }

    bitmap?.let {
        Image(
            bitmap = it,
            contentDescription = contentDescription,
            modifier = modifier
                .fillMaxWidth()
                .height(200.dp),
            contentScale = ContentScale.Crop
        )
    } ?: run {
        // Show placeholder for authors without images
        PlaceholderImage(
            modifier = modifier
                .fillMaxWidth()
                .height(200.dp)
        )
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
