package com.paulohenriquesg.fahrenheit.main

import android.os.Looper
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.paulohenriquesg.fahrenheit.TestFixtures
import com.paulohenriquesg.fahrenheit.api.ProgressMark
import com.paulohenriquesg.fahrenheit.podcast.EpisodeProgress
import com.paulohenriquesg.fahrenheit.progress.ProgressStore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf

/** Home's progress comes from the shared store, as it changes (#207, #208). */
@RunWith(AndroidJUnit4::class)
class HomeViewModelTest {

    private val store = ProgressStore(now = { 0L })
    private val model = HomeViewModel(store)
    private val book = TestFixtures.createMockLibraryItemEntry(id = "book")

    private fun settle() = shadowOf(Looper.getMainLooper()).idle()

    @Test
    fun `nothing started, no cover has a bar`() {
        assertNull(model.uiState.value.covers.of(book))
    }

    @Test
    fun `the player's report reaches a cover on Home`() {
        store.played("book", null, position = 600.0, duration = 3480.0)
        settle()

        assertEquals(2880.0, model.uiState.value.covers.of(book)!!.secondsLeft, 0.5)
    }

    @Test
    fun `an episode marked finished is heard on Home`() {
        store.marked("pod", "e1", ProgressMark(isFinished = true))
        settle()

        assertEquals(EpisodeProgress.Heard, model.uiState.value.episodes["e1"])
    }

    @Test
    fun `a finished book loses its bar`() {
        store.played("book", null, position = 600.0, duration = 3480.0)
        store.marked("book", null, ProgressMark(isFinished = true))
        settle()

        assertNull(model.uiState.value.covers.of(book))
    }
}
