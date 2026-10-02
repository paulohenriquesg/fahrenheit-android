package com.paulohenriquesg.fahrenheit.ui.components

import com.paulohenriquesg.fahrenheit.api.Collection
import com.paulohenriquesg.fahrenheit.api.Series
import org.junit.Assert.assertEquals
import org.junit.Test

class BookGroupTest {

    @Test
    fun `a series is a group of books`() {
        val group = BookGroup.of(
            Series(id = "s1", name = "Foundation", description = "Asimov's", books = null)
        )

        assertEquals("Foundation", group.name)
        assertEquals("Asimov's", group.description)
    }

    @Test
    fun `so is a collection`() {
        val group = BookGroup.of(
            Collection(
                id = "c1",
                libraryId = "lib",
                name = "Favourites",
                description = "The good ones",
                books = null,
                createdAt = 0L,
                lastUpdate = 0L
            )
        )

        assertEquals("Favourites", group.name)
        assertEquals("The good ones", group.description)
    }
}
