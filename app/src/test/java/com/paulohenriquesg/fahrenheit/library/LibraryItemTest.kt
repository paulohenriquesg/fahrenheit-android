package com.paulohenriquesg.fahrenheit.library

import com.paulohenriquesg.fahrenheit.TestFixtures
import org.junit.Assert.*
import org.junit.Test

class LibraryItemTest {

    @Test
    fun `library item has valid metadata`() {
        val item = TestFixtures.createMockLibraryItem(
            title = "Test Audiobook",
            duration = 7200.0
        )

        assertEquals("Test Audiobook", item.media.metadata.title)
        assertEquals("Test Author", item.media.metadata.authorName)
        assertEquals(7200.0, item.media.duration ?: 0.0, 0.01)
    }

    @Test
    fun `library item has audio tracks`() {
        val item = TestFixtures.createMockLibraryItem()

        assertTrue((item.media.tracks?.size ?: 0) > 0)
        assertTrue(!item.media.tracks.isNullOrEmpty())
    }

    @Test
    fun `audio track content URL is properly formatted`() {
        val track = TestFixtures.createMockAudioTrack(
            contentUrl = "/api/items/test-item-123/file/test-file-456"
        )

        assertTrue(track.contentUrl.startsWith("/api/items/"))
        assertTrue(track.contentUrl.contains("/file/"))
    }

    @Test
    fun `library item duration matches track duration`() {
        val trackDuration = 3600.0
        val item = TestFixtures.createMockLibraryItem(duration = trackDuration)

        assertEquals(trackDuration, item.media.duration ?: 0.0, 0.01)
        assertEquals(trackDuration, item.media.tracks?.first()?.duration ?: 0.0, 0.01)
    }

    @Test
    fun `shelf contains valid library items`() {
        val shelf = TestFixtures.createMockShelf(
            label = "Continue Listening",
            type = "book"
        )

        assertEquals("Continue Listening", shelf.label)
        assertTrue(!shelf.entities.isNullOrEmpty())
        assertEquals("book", shelf.type)
    }

    @Test
    fun `shelf entity has required metadata`() {
        val shelf = TestFixtures.createMockShelf()
        val entity = shelf.entities?.first()

        assertNotNull(entity)
        assertNotNull(entity?.id)
        assertNotNull(entity?.media)
        assertNotNull(entity?.media?.metadata)
        assertNotNull(entity?.media?.metadata?.title)
    }

    @Test
    fun `library item metadata includes author information`() {
        val metadata = TestFixtures.createMockLibraryItemMetadata(
            title = "Test Book",
            authorName = "John Doe"
        )

        assertEquals("Test Book", metadata.title)
        assertEquals("John Doe", metadata.authorName)
        assertTrue(!metadata.authors.isNullOrEmpty())
        assertEquals("John Doe", metadata.authors?.first()?.name)
    }

    @Test
    fun `library item has valid file paths`() {
        val item = TestFixtures.createMockLibraryItem()

        assertNotNull(item.path)
        assertNotNull(item.relPath)
        assertTrue(item.path.startsWith("/"))
    }

    @Test
    fun `audio file metadata is complete`() {
        val track = TestFixtures.createMockAudioTrack()

        assertNotNull(track.metadata)
        assertNotNull(track.metadata.filename)
        assertTrue(track.metadata.filename.endsWith(".mp3"))
        assertEquals(".mp3", track.metadata.ext)
    }

    @Test
    fun `library item timestamps are valid`() {
        val item = TestFixtures.createMockLibraryItem()

        assertTrue(item.addedAt > 0)
        assertTrue(item.updatedAt > 0)
        assertTrue(item.mtimeMs > 0)
    }

    @Test
    fun `media type is correctly identified`() {
        val item = TestFixtures.createMockLibraryItem()

        assertEquals("book", item.mediaType)
    }

    @Test
    fun `library item is not missing or invalid`() {
        val item = TestFixtures.createMockLibraryItem()

        assertFalse(item.isMissing)
        assertFalse(item.isInvalid)
    }

    @Test
    fun `cover path is available for display`() {
        val item = TestFixtures.createMockLibraryItem()

        assertNotNull(item.media.coverPath)
        assertTrue(item.media.coverPath!!.isNotEmpty())
    }

    @Test
    fun `audio track has valid MIME type`() {
        val track = TestFixtures.createMockAudioTrack()

        assertEquals("audio/mpeg", track.mimeType)
    }

    @Test
    fun `library item size is tracked`() {
        val item = TestFixtures.createMockLibraryItem()

        assertTrue(item.size > 0)
        assertTrue(item.media.size > 0)
    }
}
