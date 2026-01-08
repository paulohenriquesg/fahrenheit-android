package com.paulohenriquesg.fahrenheit.api

import com.google.gson.GsonBuilder
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class ShelfDeserializerTest {
    private lateinit var gson: com.google.gson.Gson

    @Before
    fun setup() {
        gson = GsonBuilder()
            .registerTypeAdapter(Shelf::class.java, ShelfDeserializer())
            .create()
    }

    @Test
    fun `deserialize book shelf with entities`() {
        val json = """
            {
                "id": "continue-listening",
                "label": "Continue Listening",
                "labelStringKey": "LabelContinueListening",
                "type": "book",
                "total": 5,
                "entities": [
                    {
                        "id": "book-1",
                        "ino": "ino-1",
                        "libraryId": "lib-1",
                        "folderId": "folder-1",
                        "path": "/audiobooks/book1",
                        "relPath": "book1",
                        "isFile": false,
                        "mtimeMs": 1234567890000,
                        "ctimeMs": 1234567890000,
                        "birthtimeMs": 1234567890000,
                        "addedAt": 1234567890000,
                        "updatedAt": 1234567890000,
                        "isMissing": false,
                        "isInvalid": false,
                        "mediaType": "book",
                        "media": {
                            "metadata": {
                                "title": "Test Book",
                                "genres": [],
                                "publishedYear": "2024",
                                "publisher": "Test",
                                "description": "Test"
                            },
                            "coverPath": "/covers/test.jpg",
                            "tags": [],
                            "numTracks": 1,
                            "numAudioFiles": 1,
                            "numChapters": 0,
                            "duration": 3600.0,
                            "size": 1000000
                        },
                        "numFiles": 1,
                        "size": 1000000
                    }
                ]
            }
        """.trimIndent()

        val shelf = gson.fromJson(json, Shelf::class.java)

        assertEquals("continue-listening", shelf.id)
        assertEquals("Continue Listening", shelf.label)
        assertEquals("LabelContinueListening", shelf.labelStringKey)
        assertEquals("book", shelf.type)
        assertEquals(5, shelf.total)
        assertNotNull(shelf.bookEntities)
        assertEquals(1, shelf.bookEntities?.size)
        assertEquals("book-1", shelf.bookEntities?.first()?.id)
        assertNull(shelf.authorEntities)
        assertNull(shelf.seriesEntities)
    }

    @Test
    fun `deserialize podcast shelf with entities`() {
        val json = """
            {
                "id": "podcasts",
                "label": "Podcasts",
                "labelStringKey": "LabelPodcasts",
                "type": "podcast",
                "total": 3,
                "entities": [
                    {
                        "id": "podcast-1",
                        "ino": "ino-1",
                        "libraryId": "lib-1",
                        "folderId": "folder-1",
                        "path": "/podcasts/show1",
                        "relPath": "show1",
                        "isFile": false,
                        "mtimeMs": 1234567890000,
                        "ctimeMs": 1234567890000,
                        "birthtimeMs": 1234567890000,
                        "addedAt": 1234567890000,
                        "updatedAt": 1234567890000,
                        "isMissing": false,
                        "isInvalid": false,
                        "mediaType": "podcast",
                        "media": {
                            "metadata": {
                                "title": "Test Podcast",
                                "genres": [],
                                "publishedYear": "2024",
                                "publisher": "Test",
                                "description": "Test"
                            },
                            "coverPath": "/covers/podcast.jpg",
                            "tags": [],
                            "numTracks": 1,
                            "numAudioFiles": 1,
                            "numChapters": 0,
                            "duration": 1800.0,
                            "size": 500000
                        },
                        "numFiles": 1,
                        "size": 500000
                    }
                ]
            }
        """.trimIndent()

        val shelf = gson.fromJson(json, Shelf::class.java)

        assertEquals("podcasts", shelf.id)
        assertEquals("Podcasts", shelf.label)
        assertEquals("podcast", shelf.type)
        assertEquals(3, shelf.total)
        assertNotNull(shelf.bookEntities)
        assertEquals(1, shelf.bookEntities?.size)
        assertEquals("podcast-1", shelf.bookEntities?.first()?.id)
    }

    @Test
    fun `deserialize episode shelf with entities`() {
        val json = """
            {
                "id": "continue-listening",
                "label": "Continue Listening",
                "labelStringKey": "LabelContinueListening",
                "type": "episode",
                "total": 2,
                "entities": [
                    {
                        "id": "episode-1",
                        "ino": "ino-1",
                        "libraryId": "lib-1",
                        "folderId": "folder-1",
                        "path": "/podcasts/show1",
                        "relPath": "show1",
                        "isFile": false,
                        "mtimeMs": 1234567890000,
                        "ctimeMs": 1234567890000,
                        "birthtimeMs": 1234567890000,
                        "addedAt": 1234567890000,
                        "updatedAt": 1234567890000,
                        "isMissing": false,
                        "isInvalid": false,
                        "mediaType": "podcast",
                        "media": {
                            "metadata": {
                                "title": "Test Episode",
                                "genres": [],
                                "publishedYear": "2024",
                                "publisher": "Test",
                                "description": "Test"
                            },
                            "coverPath": "/covers/episode.jpg",
                            "tags": [],
                            "numTracks": 1,
                            "numAudioFiles": 1,
                            "numChapters": 0,
                            "duration": 1800.0,
                            "size": 500000
                        },
                        "numFiles": 1,
                        "size": 500000
                    }
                ]
            }
        """.trimIndent()

        val shelf = gson.fromJson(json, Shelf::class.java)

        assertEquals("continue-listening", shelf.id)
        assertEquals("Continue Listening", shelf.label)
        assertEquals("episode", shelf.type)
        assertEquals(2, shelf.total)
        assertNotNull(shelf.bookEntities)
        assertEquals(1, shelf.bookEntities?.size)
        assertEquals("episode-1", shelf.bookEntities?.first()?.id)
    }

    @Test
    fun `deserialize authors shelf with entities`() {
        val json = """
            {
                "id": "authors",
                "label": "Authors",
                "labelStringKey": "LabelAuthors",
                "type": "authors",
                "total": 10,
                "entities": [
                    {
                        "id": "author-1",
                        "name": "John Doe",
                        "numBooks": 5
                    },
                    {
                        "id": "author-2",
                        "name": "Jane Smith",
                        "numBooks": 3
                    }
                ]
            }
        """.trimIndent()

        val shelf = gson.fromJson(json, Shelf::class.java)

        assertEquals("authors", shelf.id)
        assertEquals("Authors", shelf.label)
        assertEquals("authors", shelf.type)
        assertEquals(10, shelf.total)
        assertNull(shelf.bookEntities)
        assertNotNull(shelf.authorEntities)
        assertEquals(2, shelf.authorEntities?.size)
        assertEquals("John Doe", shelf.authorEntities?.first()?.name)
        assertEquals("Jane Smith", shelf.authorEntities?.get(1)?.name)
        assertNull(shelf.seriesEntities)
    }

    @Test
    fun `deserialize series shelf with entities`() {
        val json = """
            {
                "id": "series",
                "label": "Series",
                "labelStringKey": "LabelSeries",
                "type": "series",
                "total": 7,
                "entities": [
                    {
                        "id": "series-1",
                        "name": "Harry Potter",
                        "nameIgnorePrefix": "Harry Potter"
                    },
                    {
                        "id": "series-2",
                        "name": "The Lord of the Rings",
                        "nameIgnorePrefix": "Lord of the Rings"
                    }
                ]
            }
        """.trimIndent()

        val shelf = gson.fromJson(json, Shelf::class.java)

        assertEquals("series", shelf.id)
        assertEquals("Series", shelf.label)
        assertEquals("series", shelf.type)
        assertEquals(7, shelf.total)
        assertNull(shelf.bookEntities)
        assertNull(shelf.authorEntities)
        assertNotNull(shelf.seriesEntities)
        assertEquals(2, shelf.seriesEntities?.size)
        assertEquals("Harry Potter", shelf.seriesEntities?.first()?.name)
        assertEquals("The Lord of the Rings", shelf.seriesEntities?.get(1)?.name)
    }

    @Test
    fun `deserialize shelf with null entities`() {
        val json = """
            {
                "id": "empty-shelf",
                "label": "Empty Shelf",
                "labelStringKey": "LabelEmpty",
                "type": "book",
                "total": 0,
                "entities": null
            }
        """.trimIndent()

        val shelf = gson.fromJson(json, Shelf::class.java)

        assertEquals("empty-shelf", shelf.id)
        assertEquals("Empty Shelf", shelf.label)
        assertEquals("book", shelf.type)
        assertEquals(0, shelf.total)
        assertNull(shelf.bookEntities)
        assertNull(shelf.authorEntities)
        assertNull(shelf.seriesEntities)
    }

    @Test
    fun `deserialize shelf with missing entities field`() {
        val json = """
            {
                "id": "no-entities",
                "label": "No Entities",
                "labelStringKey": "LabelNoEntities",
                "type": "book",
                "total": 0
            }
        """.trimIndent()

        val shelf = gson.fromJson(json, Shelf::class.java)

        assertEquals("no-entities", shelf.id)
        assertEquals("No Entities", shelf.label)
        assertEquals("book", shelf.type)
        assertNull(shelf.bookEntities)
    }

    @Test
    fun `deserialize shelf with empty entities array`() {
        val json = """
            {
                "id": "empty-array",
                "label": "Empty Array",
                "labelStringKey": "LabelEmptyArray",
                "type": "book",
                "total": 0,
                "entities": []
            }
        """.trimIndent()

        val shelf = gson.fromJson(json, Shelf::class.java)

        assertEquals("empty-array", shelf.id)
        assertEquals("book", shelf.type)
        assertNotNull(shelf.bookEntities)
        assertEquals(0, shelf.bookEntities?.size)
    }

    @Test
    fun `deserialize shelf with unknown type`() {
        val json = """
            {
                "id": "unknown",
                "label": "Unknown Type",
                "labelStringKey": "LabelUnknown",
                "type": "custom-type",
                "total": 0,
                "entities": []
            }
        """.trimIndent()

        val shelf = gson.fromJson(json, Shelf::class.java)

        assertEquals("unknown", shelf.id)
        assertEquals("Unknown Type", shelf.label)
        assertEquals("custom-type", shelf.type)
        assertNull(shelf.bookEntities)
        assertNull(shelf.authorEntities)
        assertNull(shelf.seriesEntities)
    }

    @Test
    fun `deserialize multiple shelves of different types`() {
        val bookJson = """{"id":"books","label":"Books","labelStringKey":"LabelBooks","type":"book","total":5,"entities":[]}"""
        val authorJson = """{"id":"authors","label":"Authors","labelStringKey":"LabelAuthors","type":"authors","total":3,"entities":[]}"""
        val seriesJson = """{"id":"series","label":"Series","labelStringKey":"LabelSeries","type":"series","total":2,"entities":[]}"""

        val bookShelf = gson.fromJson(bookJson, Shelf::class.java)
        val authorShelf = gson.fromJson(authorJson, Shelf::class.java)
        val seriesShelf = gson.fromJson(seriesJson, Shelf::class.java)

        assertEquals("book", bookShelf.type)
        assertNotNull(bookShelf.bookEntities)
        assertNull(bookShelf.authorEntities)
        assertNull(bookShelf.seriesEntities)

        assertEquals("authors", authorShelf.type)
        assertNull(authorShelf.bookEntities)
        assertNotNull(authorShelf.authorEntities)
        assertNull(authorShelf.seriesEntities)

        assertEquals("series", seriesShelf.type)
        assertNull(seriesShelf.bookEntities)
        assertNull(seriesShelf.authorEntities)
        assertNotNull(seriesShelf.seriesEntities)
    }

    @Test
    fun `entities property returns bookEntities for book type`() {
        val json = """
            {
                "id": "test",
                "label": "Test",
                "labelStringKey": "LabelTest",
                "type": "book",
                "total": 1,
                "entities": [
                    {
                        "id": "book-1",
                        "ino": "ino-1",
                        "libraryId": "lib-1",
                        "folderId": "folder-1",
                        "path": "/path",
                        "relPath": "path",
                        "isFile": false,
                        "mtimeMs": 1234567890000,
                        "ctimeMs": 1234567890000,
                        "birthtimeMs": 1234567890000,
                        "addedAt": 1234567890000,
                        "updatedAt": 1234567890000,
                        "isMissing": false,
                        "isInvalid": false,
                        "mediaType": "book",
                        "media": {
                            "metadata": {"title":"Test","genres":[],"publishedYear":"2024","publisher":"Test","description":"Test"},
                            "coverPath": "/cover.jpg",
                            "tags": [],
                            "numTracks": 1,
                            "numAudioFiles": 1,
                            "numChapters": 0,
                            "duration": 3600.0,
                            "size": 1000000
                        },
                        "numFiles": 1,
                        "size": 1000000
                    }
                ]
            }
        """.trimIndent()

        val shelf = gson.fromJson(json, Shelf::class.java)

        assertEquals(shelf.bookEntities, shelf.entities)
        assertEquals(1, shelf.entities?.size)
    }

    @Test
    fun `total field handles null value`() {
        val json = """
            {
                "id": "test",
                "label": "Test",
                "labelStringKey": "LabelTest",
                "type": "book",
                "total": null,
                "entities": []
            }
        """.trimIndent()

        val shelf = gson.fromJson(json, Shelf::class.java)

        assertNull(shelf.total)
    }
}
