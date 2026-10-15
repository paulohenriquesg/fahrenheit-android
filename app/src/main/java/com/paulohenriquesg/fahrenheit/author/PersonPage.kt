@file:OptIn(ExperimentalComposeUiApi::class) // focusProperties.exit

package com.paulohenriquesg.fahrenheit.author

import androidx.activity.compose.BackHandler
import androidx.annotation.PluralsRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Border
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.LocalContentColor
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import coil.compose.AsyncImage
import com.paulohenriquesg.fahrenheit.R
import com.paulohenriquesg.fahrenheit.api.ApiClient
import com.paulohenriquesg.fahrenheit.api.AuthorDetailResponse
import com.paulohenriquesg.fahrenheit.api.LibraryItem
import com.paulohenriquesg.fahrenheit.player.PlayerPreview
import com.paulohenriquesg.fahrenheit.ui.Border as FocusBorder
import com.paulohenriquesg.fahrenheit.ui.StableKeys
import com.paulohenriquesg.fahrenheit.ui.components.BookColumn
import com.paulohenriquesg.fahrenheit.ui.components.DescriptionBox
import com.paulohenriquesg.fahrenheit.ui.elements.CoverImage
import com.paulohenriquesg.fahrenheit.ui.elements.PlaceholderShape
import com.paulohenriquesg.fahrenheit.ui.elements.authorPhotoRequest
import com.paulohenriquesg.fahrenheit.ui.elements.authorPhotoUrlOf
import com.paulohenriquesg.fahrenheit.ui.rememberInitialFocus
import com.paulohenriquesg.fahrenheit.ui.requestFocusWhenAttached
import com.paulohenriquesg.fahrenheit.utils.RichText
import java.text.NumberFormat

/**
 * What an author's or a narrator's page shows once loaded (#230). A narrator
 * is only a name on books to the server: no photo, no bio.
 */
data class PersonPage(
    val name: String,
    val narrator: Boolean,
    val items: List<LibraryItem>,
    val shelves: List<PersonShelf>,
    val photoUrl: String? = null,
    val bio: String? = null
) {
    companion object {
        fun author(author: AuthorDetailResponse) = PersonPage(
            name = author.name,
            narrator = false,
            items = author.libraryItems.orEmpty(),
            shelves = PersonShelves.of(author),
            photoUrl = author.imagePath?.let { authorPhotoUrlOf(author.id) },
            bio = author.description?.takeIf { it.isNotBlank() }
        )

        fun narrator(name: String, items: List<LibraryItem>) =
            PersonPage(name = name, narrator = true, items = items, shelves = PersonShelves.of(items))
    }
}

/** Up to two letters for a person with no photo: the first and last names' ("A Reader" is "AR"). */
internal fun initialsOf(name: String): String {
    val words = name.split(' ').filter { it.firstOrNull()?.isLetterOrDigit() == true }
    return listOfNotNull(words.firstOrNull(), words.drop(1).lastOrNull())
        .joinToString("") { it.substring(0, it.offsetByCodePoints(0, 1)).uppercase() }
}

object PersonTags {
    const val BOOK = "person_book"
    const val BOOK_TITLE = "person_book_title"
    const val CONTINUE = "person_continue"
    const val BIO_BOX = "person_bio_box"
    const val FULL_BIO = "person_full_bio"
}

object AuthorLoadingTags {
    const val FRAME = "author_loading"
}

/** The page's margin, as the book page keeps it (`DetailBody`). */
private val PAGE_PADDING = PaddingValues(horizontal = 24.dp, vertical = 16.dp)
private val COVER = 104.dp
private val ROW_GAP = 14.dp
private const val BIO_LINES = 3

/**
 * The page as far as it can be drawn: the loading frame (#225), why it
 * failed, or the person. [resume] is the book Continue plays, if any.
 */
@Composable
fun PersonStage(
    preview: PlayerPreview?,
    narrator: Boolean,
    loading: Boolean,
    error: String?,
    page: PersonPage?,
    resume: LibraryItem? = null,
    onBook: (LibraryItem) -> Unit = {},
    onContinue: (LibraryItem) -> Unit = {}
) {
    Box(Modifier.fillMaxSize()) {
        when {
            loading -> PersonLoading(preview, narrator)
            error != null -> Text(
                text = error,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.align(Alignment.Center).padding(16.dp)
            )
            page != null -> PersonContent(page, resume, onBook, onContinue)
        }
    }
}

/**
 * The book page's layout (#202, #225; docs/mocks/author-page.html): the
 * photo, name and counts on the left with Continue; on the right, scrolling,
 * the bio's box and the books one row per series. Focus lands on Continue,
 * or else on the first book.
 */
@Composable
private fun PersonContent(
    page: PersonPage,
    resume: LibraryItem?,
    onBook: (LibraryItem) -> Unit,
    onContinue: (LibraryItem) -> Unit
) {
    var fullBio by remember { mutableStateOf(false) }
    var bioOpened by remember { mutableStateOf(false) }
    val bioBox = remember { FocusRequester() }
    val firstBook = page.shelves.firstOrNull()?.books?.firstOrNull()
    val landing = rememberInitialFocus(enabled = resume != null || firstBook != null, page.name, resume?.id)
    LaunchedEffect(fullBio) {
        // Back from the full bio, onto the box it opened from.
        if (!fullBio && bioOpened) bioBox.requestFocusWhenAttached()
    }
    Box(Modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxSize().padding(PAGE_PADDING),
            horizontalArrangement = Arrangement.spacedBy(BookColumn.gap)
        ) {
            Column(Modifier.width(BookColumn.width), verticalArrangement = Arrangement.spacedBy(BookColumn.spacing)) {
                PersonPhoto(page.name, page.photoUrl)
                Name(page.name)
                Text(
                    countLine(page),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                resume?.let { book ->
                    Spacer(Modifier.height(6.dp))
                    ContinueButton(
                        stringResource(R.string.person_continue, book.media.metadata.title),
                        onClick = { onContinue(book) },
                        modifier = Modifier.focusRequester(landing).testTag(PersonTags.CONTINUE)
                    )
                }
            }
            LazyColumn(Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                page.bio?.let { bio ->
                    item(key = "about") {
                        Column {
                            Heading(stringResource(R.string.person_about))
                            BioBox(bio, Modifier.focusRequester(bioBox)) { fullBio = true; bioOpened = true }
                        }
                    }
                }
                val keys = StableKeys.of(page.shelves) { it.series ?: OTHER_BOOKS_KEY }
                itemsIndexed(page.shelves, key = { index, _ -> keys[index] }) { index, shelf ->
                    Column {
                        Heading(
                            shelf.series?.let { count(R.plurals.person_shelf_series, shelf.books.size, it) }
                                ?: count(R.plurals.person_shelf_other, shelf.books.size)
                        )
                        BookRow(
                            shelf,
                            byAuthor = page.narrator,
                            onBook = onBook,
                            // On the first book when there is no Continue.
                            landHere = if (index == 0 && resume == null) Modifier.focusRequester(landing) else Modifier
                        )
                    }
                }
            }
        }
        if (fullBio) page.bio?.let { FullBio(page.name, it) { fullBio = false } }
    }
}

private const val OTHER_BOOKS_KEY = "\u0000other"

@Composable
private fun Name(name: String) {
    Text(
        name,
        style = MaterialTheme.typography.headlineSmall,
        color = MaterialTheme.colorScheme.onSurface,
        maxLines = 3,
        overflow = TextOverflow.Ellipsis
    )
}

/** "7 books · 2 series", or a narrator's "Reads 12 books". */
@Composable
private fun countLine(page: PersonPage): String {
    val books = page.items.size
    if (page.narrator) return count(R.plurals.person_reads, books)
    val series = page.shelves.count { it.series != null }
    val line = count(R.plurals.person_books, books)
    return if (series == 0) line else "$line · ${count(R.plurals.person_series, series)}"
}

/** A count grouped as the device writes numbers, as `SwitchLibraryView` does (#115). */
@Composable
internal fun count(@PluralsRes id: Int, n: Int, vararg before: Any): String {
    val locale = LocalConfiguration.current.locales[0]
    val formatted = remember(locale, n) { NumberFormat.getIntegerInstance(locale).format(n) }
    return pluralStringResource(id, n, *before, formatted)
}

/** A section's label, as the book page's series row has it. */
@Composable
private fun Heading(text: String) {
    Text(
        text.uppercase(),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.padding(bottom = 8.dp)
    )
}

/**
 * A round photo, or the initials where there is none or it fails to load;
 * [name] null draws a grey circle, for a frame that knows nothing yet.
 */
@Composable
internal fun PersonPhoto(name: String?, photoUrl: String?, size: Dp = BookColumn.width) {
    if (name == null) {
        PlaceholderShape(Modifier.size(size), round = true)
        return
    }
    val context = LocalContext.current
    Box(
        Modifier.size(size).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        Text(
            initialsOf(name),
            style = MaterialTheme.typography.displayMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1
        )
        photoUrl?.let { url ->
            AsyncImage(
                model = remember(url) { authorPhotoRequest(context, url, ApiClient.getToken()) },
                contentDescription = name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

/** The page's one action: filled, inverting on focus, not growing (docs/ui-style-guide.md, Buttons). */
@Composable
private fun ContinueButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    Button(
        onClick = onClick,
        modifier = modifier,
        colors = ButtonDefaults.colors(
            containerColor = scheme.primary,
            contentColor = scheme.onPrimary,
            focusedContainerColor = scheme.onPrimary,
            focusedContentColor = scheme.primary
        ),
        scale = ButtonDefaults.scale(focusedScale = 1f)
    ) {
        Icon(Icons.Filled.PlayArrow, contentDescription = null, tint = LocalContentColor.current, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(8.dp))
        Text(text, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

private val BIO_SHAPE = RoundedCornerShape(12.dp)

/** The bio, clipped to a few lines with "More ›" when cut; OK opens it in full. */
@Composable
private fun BioBox(bio: String, modifier: Modifier, onOpen: () -> Unit) {
    val text = remember(bio) { RichText.fromHtml(bio) }
    var cut by remember(bio) { mutableStateOf(false) }
    Surface(
        onClick = onOpen,
        modifier = modifier.fillMaxWidth().testTag(PersonTags.BIO_BOX),
        shape = ClickableSurfaceDefaults.shape(BIO_SHAPE),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.04f),
            focusedContainerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.04f)
        ),
        border = ClickableSurfaceDefaults.border(
            focusedBorder = Border(BorderStroke(FocusBorder.focus, MaterialTheme.colorScheme.primary), shape = BIO_SHAPE)
        ),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1f)
    ) {
        Column(Modifier.padding(12.dp)) {
            Text(
                text,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = BIO_LINES,
                overflow = TextOverflow.Ellipsis,
                onTextLayout = { cut = it.hasVisualOverflow }
            )
            if (cut) {
                Text(
                    stringResource(R.string.person_more),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.align(Alignment.End)
                )
            }
        }
    }
}

/** The whole bio, over the page, in the book page's description box; Back closes it. */
@Composable
private fun FullBio(name: String, bio: String, onClose: () -> Unit) {
    BackHandler(onBack = onClose)
    val focus = rememberInitialFocus(enabled = true)
    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .focusProperties { exit = { FocusRequester.Cancel } }
            .focusGroup()
            .padding(PAGE_PADDING)
            .testTag(PersonTags.FULL_BIO),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            name,
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        DescriptionBox(bio, focus, Modifier.fillMaxWidth().weight(1f))
    }
}

/** A shelf's books, each told apart by its tag, number and length (or author). */
@Composable
private fun BookRow(shelf: PersonShelf, byAuthor: Boolean, onBook: (LibraryItem) -> Unit, landHere: Modifier) {
    val keys = remember(shelf) { StableKeys.of(shelf.books) { it.item.id } }
    LazyRow(horizontalArrangement = Arrangement.spacedBy(ROW_GAP)) {
        itemsIndexed(shelf.books, key = { index, _ -> keys[index] }) { index, book ->
            BookCard(book, byAuthor, { onBook(book.item) }, if (index == 0) landHere else Modifier)
        }
    }
}

@Composable
private fun BookCard(book: ShelfBook, byAuthor: Boolean, onClick: () -> Unit, modifier: Modifier) {
    val card = remember(book, byAuthor) { PersonCard.of(book, byAuthor) }
    val title = book.item.media.metadata.title
    Column(Modifier.width(COVER)) {
        Surface(
            onClick = onClick,
            modifier = modifier.testTag(PersonTags.BOOK),
            shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(8.dp)),
            border = ClickableSurfaceDefaults.border(
                focusedBorder = Border(BorderStroke(FocusBorder.focus, MaterialTheme.colorScheme.primary))
            ),
            scale = ClickableSurfaceDefaults.scale(focusedScale = 1f)
        ) {
            CoverImage(
                itemId = book.item.id,
                contentDescription = title,
                size = COVER,
                hasCover = book.item.media.coverPath != null
            )
        }
        Text(
            title,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 6.dp).testTag(PersonTags.BOOK_TITLE)
        )
        card.tag?.let {
            Text(
                it,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .padding(top = 2.dp)
                    .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(4.dp))
                    .padding(horizontal = 6.dp)
            )
        }
        Text(
            card.detail,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/**
 * The page before the person arrives (#225; frame 3 of docs/mocks/
 * detail-loading.html, laid out as docs/mocks/author-page.html): the card's
 * photo and name in their places - a narrator's initials - and still grey
 * shapes for the counts, the bio and the books. Nothing here can take focus.
 */
@Composable
private fun PersonLoading(preview: PlayerPreview?, narrator: Boolean) {
    Row(
        Modifier.fillMaxSize().padding(PAGE_PADDING).testTag(AuthorLoadingTags.FRAME),
        horizontalArrangement = Arrangement.spacedBy(BookColumn.gap)
    ) {
        Column(Modifier.width(BookColumn.width), verticalArrangement = Arrangement.spacedBy(BookColumn.spacing)) {
            PersonPhoto(preview?.title, preview?.coverUrl)
            preview?.let { Name(it.title) } ?: PlaceholderShape(Modifier.fillMaxWidth().height(24.dp))
            PlaceholderShape(Modifier.width(120.dp).height(16.dp))
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(20.dp)) {
            // A narrator has no bio to wait for.
            if (!narrator) PlaceholderShape(Modifier.fillMaxWidth().height(84.dp))
            repeat(2) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    PlaceholderShape(Modifier.width(160.dp).height(14.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(ROW_GAP)) {
                        repeat(5) { PlaceholderShape(Modifier.size(COVER)) }
                    }
                }
            }
        }
    }
}
