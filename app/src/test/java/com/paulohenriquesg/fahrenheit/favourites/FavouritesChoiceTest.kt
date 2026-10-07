package com.paulohenriquesg.fahrenheit.favourites

import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** The Favourites playlist is chosen per library, on this device (#180). */
@RunWith(RobolectricTestRunner::class)
class FavouritesChoiceTest {

    private fun choice() = FavouritesChoice(ApplicationProvider.getApplicationContext())

    @Test
    fun `nothing is chosen at first`() {
        assertNull(choice().playlistFor("lib_1"))
    }

    @Test
    fun `each library keeps its own choice`() {
        choice().choose("lib_1", "pl_1")
        choice().choose("lib_2", "pl_9")

        assertEquals("pl_1", choice().playlistFor("lib_1"))
        assertEquals("pl_9", choice().playlistFor("lib_2"))
    }

    @Test
    fun `choosing None forgets the library's playlist`() {
        choice().choose("lib_1", "pl_1")
        choice().choose("lib_1", null)

        assertNull(choice().playlistFor("lib_1"))
    }
}
