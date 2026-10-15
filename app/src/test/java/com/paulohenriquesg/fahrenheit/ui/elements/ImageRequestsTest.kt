package com.paulohenriquesg.fahrenheit.ui.elements

import androidx.test.core.app.ApplicationProvider
import coil.memory.MemoryCache
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * A page opened from a card draws the cover or photo at another size than the
 * card did, which Coil's memory cache will not serve. Until its own size is
 * decoded, the card's copy - same URL, so the same cache key - stands in (#225).
 */
@RunWith(RobolectricTestRunner::class)
class ImageRequestsTest {

    private val context = ApplicationProvider.getApplicationContext<android.content.Context>()

    @Test
    fun `a cover asks for whatever copy of it is in memory while its size loads`() {
        val url = "https://abs.test/api/items/b1/cover"

        assertEquals(MemoryCache.Key(url), coverRequest(context, url, sidePx = 360, token = "t").placeholderMemoryCacheKey)
    }

    @Test
    fun `an author's photo does too`() {
        val url = "https://abs.test/api/authors/a1/image"

        assertEquals(MemoryCache.Key(url), authorPhotoRequest(context, url, token = "t").placeholderMemoryCacheKey)
    }
}
