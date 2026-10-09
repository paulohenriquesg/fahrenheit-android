package com.paulohenriquesg.fahrenheit.podcast

import org.junit.Assert.assertEquals
import org.junit.Test

/** Which tab the show screen is on, and what its Favourites tab keeps (#180). */
class EpisodeTabChoiceTest {

    @Test
    fun `it opens on All`() = assertEquals(EpisodeTab.All, EpisodeTabChoice().tab)

    @Test
    fun `choosing Favourites keeps what is in the playlist then`() {
        val choice = EpisodeTabChoice()

        choice.choose(EpisodeTab.Favourites, inFavourites = setOf("a", "b"))

        assertEquals(EpisodeTab.Favourites, choice.tab)
        assertEquals(setOf("a", "b"), choice.kept)
    }

    @Test
    fun `choosing it again forgets the ones taken out since`() {
        val choice = EpisodeTabChoice()
        choice.choose(EpisodeTab.Favourites, inFavourites = setOf("a", "b"))

        choice.choose(EpisodeTab.Favourites, inFavourites = setOf("a"))

        assertEquals(setOf("a"), choice.kept)
    }

    @Test
    fun `when the choice goes to None, a screen on Favourites goes back to All and forgets`() {
        val choice = EpisodeTabChoice()
        choice.choose(EpisodeTab.Favourites, inFavourites = setOf("a"))

        choice.follow(inFavourites = null)

        assertEquals(EpisodeTab.All, choice.tab)
        assertEquals(emptySet<String>(), choice.kept)
    }

    @Test
    fun `another tab stays where it is`() {
        val choice = EpisodeTabChoice()
        choice.choose(EpisodeTab.OnServer, inFavourites = setOf("a"))

        choice.follow(inFavourites = null)

        assertEquals(EpisodeTab.OnServer, choice.tab)
    }
}
