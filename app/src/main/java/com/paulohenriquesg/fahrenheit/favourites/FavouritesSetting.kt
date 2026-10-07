package com.paulohenriquesg.fahrenheit.favourites

import android.widget.Toast
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.paulohenriquesg.fahrenheit.R
import com.paulohenriquesg.fahrenheit.api.Playlist
import com.paulohenriquesg.fahrenheit.player.PanelOption
import com.paulohenriquesg.fahrenheit.player.SidePanel
import com.paulohenriquesg.fahrenheit.ui.requestFocusWhenAttached
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale

const val FAVOURITES_ROW_TAG = "settings_favourites"
const val FAVOURITES_NONE_TAG = "favourites_none"
const val FAVOURITES_CREATE_TAG = "favourites_create"
const val FAVOURITES_SEARCH_TAG = "favourites_search"
fun favouritesOptionTag(playlistId: String) = "favourites_$playlistId"

/** Above this many playlists the panel gets a search field: fewer scroll quicker than a remote types. */
private const val SEARCH_ABOVE = 12

/**
 * The Favourites row in Settings for the library in use (#180), and what its
 * panel needs: the library's playlists, and a way to keep a pick.
 */
class FavouritesSetting(
    val libraryName: String,
    /** The chosen playlist; null for None. */
    val chosen: Playlist?,
    val load: suspend () -> Result<List<Playlist>>,
    val choose: suspend (FavouritesPick) -> Result<Unit>
)

/**
 * The side panel the row opens (frame 0): None, Create "Favourites" unless one
 * is already called that, then the playlists by name with their counts, under
 * a search field when there are many. Focus opens on the current choice;
 * Center picks and closes. A pick that fails says so and leaves the panel open.
 */
@Composable
fun FavouritesPanel(setting: FavouritesSetting, onClose: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var playlists by remember(setting) { mutableStateOf<List<Playlist>?>(null) }
    var failed by remember(setting) { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    var picking by remember { mutableStateOf(false) }
    LaunchedEffect(setting) {
        setting.load().onSuccess { playlists = it }.onFailure { failed = true }
    }
    val failedText = stringResource(R.string.favourites_change_failed)
    fun pick(choice: FavouritesPick) {
        if (picking) return
        picking = true
        scope.launch {
            setting.choose(choice)
                .onSuccess { onClose() }
                .onFailure { Toast.makeText(context, failedText, Toast.LENGTH_LONG).show() }
            picking = false
        }
    }

    SidePanel(stringResource(R.string.favourites_panel), onClose) {
        val all = playlists
        val count = all?.size ?: 0
        Text(
            text = pluralStringResource(
                R.plurals.favourites_playlists, count, setting.libraryName,
                NumberFormat.getIntegerInstance(Locale.getDefault()).format(count)
            ),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 10.dp)
        )
        when {
            failed -> Notice(stringResource(R.string.favourites_load_failed))
            all == null -> Notice(stringResource(R.string.favourites_loading))
            else -> Choices(all, setting.chosen?.id, query, { query = it }, ::pick)
        }
    }
}

@Composable
private fun Notice(text: String) {
    Text(text, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun Choices(
    playlists: List<Playlist>,
    chosenId: String?,
    query: String,
    onQuery: (String) -> Unit,
    onPick: (FavouritesPick) -> Unit
) {
    val searching = playlists.size > SEARCH_ABOVE
    val shown = playlists.filter { query.isBlank() || it.name.contains(query.trim(), ignoreCase = true) }
    val create = Favourites.offersCreate(playlists)
    val landing = remember { FocusRequester() }
    val focusAt = chosenId?.takeIf { id -> playlists.any { it.id == id } }
    val state = rememberLazyListState()
    // Lines before the playlists: the field, None, Create.
    val before = listOf(searching, true, create).count { it }
    LaunchedEffect(Unit) {
        val at = focusAt?.let { id -> before + shown.indexOfFirst { it.id == id } } ?: if (searching) 1 else 0
        // Only when it is out of view: the search field heads the list and should stay in sight.
        if (state.layoutInfo.visibleItemsInfo.none { it.index == at }) state.scrollToItem(at)
        landing.requestFocusWhenAttached()
    }
    LazyColumn(state = state, modifier = Modifier.fillMaxWidth()) {
        if (searching) {
            item(key = "search") {
                OutlinedTextField(
                    value = query,
                    onValueChange = onQuery,
                    singleLine = true,
                    placeholder = { androidx.compose.material3.Text(stringResource(R.string.favourites_search)) },
                    keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Search),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp).testTag(FAVOURITES_SEARCH_TAG)
                )
            }
        }
        item(key = "none") {
            PanelOption(
                label = stringResource(R.string.favourites_none),
                detail = stringResource(R.string.favourites_none_detail),
                selected = chosenId == null,
                onClick = { onPick(FavouritesPick.None) },
                modifier = Modifier.testTag(FAVOURITES_NONE_TAG).then(if (focusAt == null) Modifier.focusRequester(landing) else Modifier)
            )
        }
        if (create) {
            item(key = "create") {
                PanelOption(
                    label = stringResource(R.string.favourites_create, Favourites.NAME),
                    detail = stringResource(R.string.favourites_create_detail),
                    selected = false,
                    onClick = { onPick(FavouritesPick.Create) },
                    modifier = Modifier.testTag(FAVOURITES_CREATE_TAG)
                )
            }
        }
        items(shown, key = { it.id }) { playlist ->
            val size = playlist.items.orEmpty().size
            PanelOption(
                label = playlist.name,
                detail = pluralStringResource(
                    R.plurals.favourites_items, size, NumberFormat.getIntegerInstance(Locale.getDefault()).format(size)
                ),
                selected = playlist.id == chosenId,
                onClick = { onPick(FavouritesPick.Existing(playlist)) },
                modifier = Modifier.testTag(favouritesOptionTag(playlist.id))
                    .then(if (playlist.id == focusAt) Modifier.focusRequester(landing) else Modifier)
            )
        }
    }
}
