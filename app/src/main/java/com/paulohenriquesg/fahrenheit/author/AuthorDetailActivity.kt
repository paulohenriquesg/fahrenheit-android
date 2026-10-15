package com.paulohenriquesg.fahrenheit.author

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.core.content.ContextCompat
import androidx.media3.session.MediaController
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.SurfaceDefaults
import com.paulohenriquesg.fahrenheit.main.MainActivity
import com.paulohenriquesg.fahrenheit.main.MainView
import com.paulohenriquesg.fahrenheit.main.NavigationRail
import com.paulohenriquesg.fahrenheit.main.NowPlayingSlot
import com.paulohenriquesg.fahrenheit.main.queuedChapters
import com.paulohenriquesg.fahrenheit.navigation.MenuConfig
import com.paulohenriquesg.fahrenheit.player.ControllerSlot
import com.paulohenriquesg.fahrenheit.player.Playback
import com.paulohenriquesg.fahrenheit.player.PlayerActivity
import com.paulohenriquesg.fahrenheit.player.PlayerPreview
import com.paulohenriquesg.fahrenheit.ui.theme.FahrenheitTheme

/**
 * An author's or a narrator's page (#230): its own screen, so Back leaves it
 * for wherever it was opened, as it leaves the book page, with the main
 * screen's side menu on it. A section chosen there opens the main screen on it.
 */
class AuthorDetailActivity : ComponentActivity() {
    companion object {
        private const val EXTRA_AUTHOR_ID = "author_id"
        private const val EXTRA_LIBRARY_ID = "library_id"
        private const val EXTRA_NARRATOR = "narrator"

        /** [preview]: the name and photo the card shows, drawn while the author loads (#225). */
        fun createIntent(context: Context, authorId: String, preview: PlayerPreview? = null): Intent {
            return Intent(context, AuthorDetailActivity::class.java).apply {
                putExtra(EXTRA_AUTHOR_ID, authorId)
                preview?.writeTo(this)
            }
        }

        /** A narrator's page: the server knows them only as [name] on books in a library. */
        fun narratorIntent(context: Context, libraryId: String, name: String): Intent =
            Intent(context, AuthorDetailActivity::class.java).apply {
                putExtra(EXTRA_LIBRARY_ID, libraryId)
                putExtra(EXTRA_NARRATOR, name)
                // The card's name, drawn at once (#225).
                PlayerPreview(name).writeTo(this)
            }

        fun previewOf(intent: Intent): PlayerPreview? = PlayerPreview.from(intent)

        internal fun personOf(intent: Intent): Person? {
            intent.getStringExtra(EXTRA_AUTHOR_ID)?.let { return Person.Author(it) }
            val libraryId = intent.getStringExtra(EXTRA_LIBRARY_ID) ?: return null
            return intent.getStringExtra(EXTRA_NARRATOR)?.let { Person.Narrator(libraryId, it) }
        }
    }

    /** The playback service's player while this screen is visible, for the rail's Now playing (#107). */
    private var playback by mutableStateOf<MediaController?>(null)
    private val connection by lazy {
        ControllerSlot(
            connect = { Playback.connect(this) },
            release = { it.release() },
            executor = ContextCompat.getMainExecutor(this),
            onChange = { playback = it }
        )
    }

    override fun onStart() {
        super.onStart()
        connection.open()
    }

    override fun onStop() {
        // Leaves playback alone: Stop on the rail's entry ends it.
        connection.close()
        playback = null
        super.onStop()
    }

    @OptIn(ExperimentalComposeUiApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val person = personOf(intent)
        if (person == null) {
            finish()
            return
        }
        val section = if (person is Person.Narrator) MainView.NARRATORS else MainView.AUTHORS

        setContent {
            FahrenheitTheme {
                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                        .semantics { testTagsAsResourceId = true },
                    colors = SurfaceDefaults.colors(containerColor = MaterialTheme.colorScheme.background),
                    shape = RectangleShape,
                ) {
                    val rail = remember { FocusRequester() }
                    NavigationRail(
                        // Authors and narrators belong to a library of books.
                        items = MenuConfig.getMenuForLibraryType("book"),
                        secondary = MenuConfig.commonItems,
                        selectedId = section.menuItemId,
                        firstFocus = rail,
                        nowPlaying = playback?.let { player ->
                            { open ->
                                NowPlayingSlot(
                                    player = player,
                                    open = open,
                                    chaptersOf = ::queuedChapters,
                                    onOpen = { startActivity(PlayerActivity.reopenIntent(this@AuthorDetailActivity, it)) },
                                    // Stop's button goes with the entry: focus to the section (#53).
                                    onStopped = { runCatching { rail.requestFocus() } }
                                )
                            }
                        },
                        onSelect = { item ->
                            startActivity(MainActivity.sectionIntent(this@AuthorDetailActivity, item.id))
                            finish()
                        }
                    ) {
                        Box(Modifier.fillMaxSize()) { PersonScreen(person, previewOf(intent)) }
                    }
                }
            }
        }
    }
}
