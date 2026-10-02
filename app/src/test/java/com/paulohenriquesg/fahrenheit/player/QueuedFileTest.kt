package com.paulohenriquesg.fahrenheit.player

import androidx.media3.common.MediaItem
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * What a queued file knows about the book it belongs to. It rides inside the
 * MediaItem, because that is all that crosses from the screen to the service.
 */
@RunWith(AndroidJUnit4::class)
class QueuedFileTest {

    private val partTwo = QueuedFile(itemId = "b1", episodeId = null, startOffset = 3600.0, bookTotal = 5400.0)

    @Test
    fun `a position in a later file is that far past the file's start in the book`() =
        assertEquals(3900.0, partTwo.bookTime(300.0), 0.0)

    @Test
    fun `the facts survive the trip through a MediaItem`() {
        val item = MediaItem.Builder()
            .setRequestMetadata(MediaItem.RequestMetadata.Builder().setExtras(partTwo.toBundle()).build())
            .build()

        assertEquals(partTwo, QueuedFile.of(item))
    }

    @Test
    fun `an episode's id survives too`() {
        val episode = QueuedFile(itemId = "p1", episodeId = "e1", startOffset = 0.0, bookTotal = 1800.0)
        val item = MediaItem.Builder()
            .setRequestMetadata(MediaItem.RequestMetadata.Builder().setExtras(episode.toBundle()).build())
            .build()

        assertEquals(episode, QueuedFile.of(item))
    }

    @Test
    fun `an item that is not one of ours has no facts`() {
        assertNull(QueuedFile.of(MediaItem.fromUri("https://abs.test/x")))
        assertNull(QueuedFile.of(null))
    }

    @Test
    fun `the same book is the same book`() = assertTrue(partTwo.isFor("b1", null))

    @Test
    fun `another episode of the same podcast is not the one playing`() =
        assertFalse(QueuedFile("p1", "e1", 0.0, 1800.0).isFor("p1", "e2"))

    @Test
    fun `a book is not an episode that shares its item id`() =
        assertFalse(partTwo.isFor("b1", "e1"))
}
