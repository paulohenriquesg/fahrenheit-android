package com.paulohenriquesg.fahrenheit.author

import androidx.compose.foundation.layout.Box
import com.paulohenriquesg.fahrenheit.R
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.tv.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import com.paulohenriquesg.fahrenheit.api.ApiClient
import com.paulohenriquesg.fahrenheit.api.BrowseRepository
import com.paulohenriquesg.fahrenheit.api.AuthorDetailResponse
import com.paulohenriquesg.fahrenheit.api.LibraryItem
import com.paulohenriquesg.fahrenheit.player.PlayerPreview
import com.paulohenriquesg.fahrenheit.ui.elements.GridPlaceholder
import com.paulohenriquesg.fahrenheit.ui.elements.GridPlaceholderLook
import com.paulohenriquesg.fahrenheit.ui.elements.PlaceholderShape
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.ui.platform.testTag
import com.paulohenriquesg.fahrenheit.detail.DetailActivity
import com.paulohenriquesg.fahrenheit.ui.components.DetailHeader
import com.paulohenriquesg.fahrenheit.ui.components.ItemsGrid
import com.paulohenriquesg.fahrenheit.ui.elements.AuthorImage

/**
 * An author's page. [preview] is the name and photo the opening card showed,
 * drawn in place while the author loads (#225).
 */
@Composable
fun AuthorDetailScreen(authorId: String, preview: PlayerPreview? = null) {
    val context = LocalContext.current
    var authorDetail by remember { mutableStateOf<AuthorDetailResponse?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(authorId) {
        val api = ApiClient.getBrowseApi()
        if (api == null) {
            errorMessage = "API client not initialized"
            isLoading = false
            return@LaunchedEffect
        }

        BrowseRepository(api).author(authorId)
            .onSuccess { authorDetail = it }
            .onFailure { errorMessage = "Failed to load author: ${it.message}" }
        isLoading = false
    }

    AuthorStage(authorId, preview, isLoading, errorMessage, authorDetail) { book ->
        context.startActivity(DetailActivity.intentFor(context, book))
    }
}

/** The page as far as it can be drawn: the loading frame, why it failed, or the author. */
@Composable
fun AuthorStage(
    authorId: String,
    preview: PlayerPreview?,
    loading: Boolean,
    error: String?,
    author: AuthorDetailResponse?,
    onBookClick: (LibraryItem) -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        when {
            loading -> AuthorLoading(authorId, preview)
            error != null -> {
                Text(
                    text = error,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(16.dp)
                )
            }
            else -> author?.let { AuthorDetailContent(author = it, onBookClick = onBookClick) }
        }
    }
}

@Composable
fun AuthorDetailContent(
    author: AuthorDetailResponse,
    onBookClick: (LibraryItem) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        val booksCount = author.libraryItems?.size ?: 0

        DetailHeader(
            imageContent = {
                AuthorImage(
                    authorId = author.id,
                    imagePath = author.imagePath,
                    contentDescription = author.name
                )
            },
            title = author.name,
            subtitle = "$booksCount ${if (booksCount == 1) "book" else "books"}",
            description = author.description
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = stringResource(R.string.books),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        ItemsGrid(
            items = author.libraryItems,
            columns = 4,
            onItemClick = onBookClick,
            emptyMessage = "No books found for this author"
        )
    }
}

object AuthorLoadingTags {
    const val FRAME = "author_loading"
}

/**
 * [AuthorDetailContent]'s layout before the author arrives (#225; frame 3 of
 * docs/mocks/detail-loading.html): the card's photo and name in their places,
 * still grey shapes for the count, the bio and the books. Nothing here can
 * take focus.
 */
@Composable
private fun AuthorLoading(authorId: String, preview: PlayerPreview?) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .testTag(AuthorLoadingTags.FRAME)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp)
        ) {
            if (preview?.coverUrl != null) {
                AuthorImage(authorId = authorId, imagePath = null, contentDescription = preview.title, size = PHOTO_SIZE)
            } else {
                PlaceholderShape(Modifier.size(PHOTO_SIZE))
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.height(PHOTO_SIZE), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (preview != null) {
                    Text(
                        text = preview.title,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                } else {
                    PlaceholderShape(Modifier.width(240.dp).height(24.dp))
                }
                PlaceholderShape(Modifier.width(80.dp).height(14.dp))
                repeat(BIO_LINES) { PlaceholderShape(Modifier.fillMaxWidth(if (it == BIO_LINES - 1) 0.6f else 0.9f).height(16.dp)) }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.books),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        // As ItemsGrid lays the books out.
        GridPlaceholder(GridPlaceholderLook.Covers, columns = GridCells.Fixed(4), contentPadding = PaddingValues(8.dp))
    }
}

/** DetailHeader's image size. */
private val PHOTO_SIZE = 200.dp
private const val BIO_LINES = 4
