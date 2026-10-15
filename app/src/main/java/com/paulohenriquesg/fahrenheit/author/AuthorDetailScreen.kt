package com.paulohenriquesg.fahrenheit.author

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.paulohenriquesg.fahrenheit.api.ApiClient
import com.paulohenriquesg.fahrenheit.api.BrowseRepository
import com.paulohenriquesg.fahrenheit.api.LibraryRepository
import com.paulohenriquesg.fahrenheit.detail.DetailActivity
import com.paulohenriquesg.fahrenheit.player.PlayerPreview
import com.paulohenriquesg.fahrenheit.progress.ProgressStore

/** Who a page is about: an author by id, or a narrator, whom the server knows only by name in a library. */
sealed interface Person {
    data class Author(val id: String) : Person
    data class Narrator(val libraryId: String, val name: String) : Person
}

/**
 * An author's or a narrator's page (#230). [preview] is the name and photo the
 * opening card showed, drawn in place while the rest loads (#225).
 */
@Composable
fun PersonScreen(person: Person, preview: PlayerPreview? = null) {
    val context = LocalContext.current
    var page by remember { mutableStateOf<PersonPage?>(null) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(person) {
        val loaded = when (person) {
            is Person.Author -> ApiClient.getBrowseApi()?.let { api ->
                BrowseRepository(api).author(person.id).map { PersonPage.author(it) }
            }
            is Person.Narrator -> ApiClient.getLibraryApi()?.let { api ->
                LibraryRepository(api).narratorBooks(person.libraryId, person.name).map { PersonPage.narrator(person.name, it) }
            }
        }
        when {
            loaded == null -> error = "API client not initialized"
            else -> loaded.onSuccess { page = it }.onFailure { error = "Failed to load: ${it.message}" }
        }
        loading = false
    }

    // Progress is the shared store's (#207): back from the player, Continue follows.
    val progress by ProgressStore.process.entries.collectAsStateWithLifecycle()
    val resume = remember(page, progress) { page?.let { PersonShelves.continueOf(it.items, progress) } }

    PersonStage(
        preview = preview,
        narrator = person is Person.Narrator,
        loading = loading,
        error = error,
        page = page,
        resume = resume,
        onBook = { context.startActivity(DetailActivity.intentFor(context, it)) },
        onContinue = { context.startActivity(DetailActivity.playBookIntent(context, it.id, PlayerPreview.of(it))) }
    )
}
