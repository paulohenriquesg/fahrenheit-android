package com.paulohenriquesg.fahrenheit.player

import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Media3 strips an item's playable URI on its way from a controller to the
 * session, and by default refuses items without one. Ours carry it in request
 * metadata, which survives, and are rebuilt from it.
 */
@RunWith(AndroidJUnit4::class)
class PlayableItemsTest {

    private fun asSentByAController(uri: String?) = MediaItem.Builder()
        .setMediaId("b1//0")
        .setRequestMetadata(
            MediaItem.RequestMetadata.Builder()
                .setMediaUri(uri?.let(Uri::parse))
                .setExtras(QueuedFile("b1", null, 0.0, 60.0).toBundle())
                .build()
        )
        .build()

    @Test
    fun `an item is playable again from its request metadata, keeping its facts`() {
        val resolved = PlayableItems.resolve(listOf(asSentByAController("https://abs.test/part1")))!!.single()

        assertEquals("https://abs.test/part1", resolved.localConfiguration?.uri.toString())
        assertEquals(QueuedFile("b1", null, 0.0, 60.0), QueuedFile.of(resolved))
    }

    @Test
    fun `an item that is already playable is left alone`() {
        val item = MediaItem.fromUri("https://abs.test/x")

        assertEquals(item, PlayableItems.resolve(listOf(item))!!.single())
    }

    @Test
    fun `an item with no URI anywhere refuses the whole queue`() =
        assertNull(PlayableItems.resolve(listOf(asSentByAController("https://abs.test/a"), asSentByAController(null))))
}
