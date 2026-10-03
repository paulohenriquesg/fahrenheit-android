package com.paulohenriquesg.fahrenheit.player

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.palette.graphics.Palette
import coil.imageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.paulohenriquesg.fahrenheit.api.ApiClient
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** The wash for a cover bitmap, or null when it has no usable colour (see [CoverWash]). */
suspend fun coverWashOf(bitmap: Bitmap?): Color? {
    bitmap ?: return null
    val palette = withContext(Dispatchers.Default) { Palette.from(bitmap).generate() }
    return CoverWash.pick(
        listOf(palette.darkVibrantSwatch, palette.vibrantSwatch, palette.darkMutedSwatch, palette.mutedSwatch, palette.dominantSwatch)
            .map { it?.rgb }
    )
}

/**
 * The item's cover, small and in software memory so its pixels can be read,
 * with the same address and credentials as [com.paulohenriquesg.fahrenheit.ui.elements.CoverImage].
 * Null on any failure: the player then simply has no wash.
 */
suspend fun coverBitmap(context: Context, itemId: String): Bitmap? {
    val url = ApiClient.generateFullUrl("/api/items/$itemId/cover") ?: return null
    val request = ImageRequest.Builder(context)
        .data(url)
        .addHeader("Authorization", "Bearer ${ApiClient.getToken() ?: ""}")
        .allowHardware(false)
        .size(128)
        .build()
    return try {
        ((context.imageLoader.execute(request) as? SuccessResult)?.drawable as? BitmapDrawable)?.bitmap
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        null
    }
}

/**
 * The wash for an item's cover: null until it is known, and when there is none.
 *
 * @param load where the bitmap comes from; the server by default.
 */
@Composable
fun rememberCoverWash(itemId: String, load: (suspend (String) -> Bitmap?)? = null): Color? {
    val context = LocalContext.current
    var wash by remember(itemId) { mutableStateOf<Color?>(null) }
    LaunchedEffect(itemId) {
        wash = coverWashOf(load?.invoke(itemId) ?: coverBitmap(context, itemId))
    }
    return wash
}
