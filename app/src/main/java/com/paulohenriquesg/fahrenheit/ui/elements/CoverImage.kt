package com.paulohenriquesg.fahrenheit.ui.elements

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.paulohenriquesg.fahrenheit.api.ApiClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun CoverImage(itemId: String, contentDescription: String, size: Dp = 200.dp) {
    val context = LocalContext.current
    var coverImage by remember(itemId) { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(itemId) {
        withContext(Dispatchers.IO) {
            val apiClient = ApiClient.getApiService()
            if (apiClient != null) {
                val response = apiClient.getItemCover(itemId).execute()
                if (response.isSuccessful) {
                    response.body()?.let { responseBody ->
                        val inputStream = responseBody.byteStream()
                        coverImage = BitmapFactory.decodeStream(inputStream)
                    }
                } else if (response.code() == 404) {
                    val defaultResponse = apiClient.getDefaultItemCover().execute()
                    if (defaultResponse.isSuccessful) {
                        defaultResponse.body()?.let { responseBody ->
                            val inputStream = responseBody.byteStream()
                            coverImage = BitmapFactory.decodeStream(inputStream)
                        }
                    } else {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(context, "Failed to load default cover image", Toast.LENGTH_SHORT).show()
                        }
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Failed to load cover image", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    coverImage?.let {
        Image(
            bitmap = it.asImageBitmap(),
            contentDescription = contentDescription,
            modifier = Modifier
                .size(size)
        )
    } ?: Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(size)
            .background(Color.Gray)
    )
}