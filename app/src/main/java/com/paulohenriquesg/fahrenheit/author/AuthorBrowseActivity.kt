package com.paulohenriquesg.fahrenheit.author

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.*
import com.paulohenriquesg.fahrenheit.api.ApiClient
import com.paulohenriquesg.fahrenheit.api.Author
import com.paulohenriquesg.fahrenheit.api.AuthorsResponse
import com.paulohenriquesg.fahrenheit.ui.elements.AuthorImage
import com.paulohenriquesg.fahrenheit.ui.theme.FahrenheitTheme
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class AuthorBrowseActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val libraryId = intent.getStringExtra(EXTRA_LIBRARY_ID) ?: run {
            finish()
            return
        }

        setContent {
            FahrenheitTheme {
                Surface(
                    color = MaterialTheme.colorScheme.background,
                    contentColor = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.fillMaxSize(),
                    shape = RectangleShape
                ) {
                    AuthorBrowseScreen(libraryId = libraryId)
                }
            }
        }
    }

    companion object {
        private const val EXTRA_LIBRARY_ID = "library_id"

        fun createIntent(context: Context, libraryId: String): Intent {
            return Intent(context, AuthorBrowseActivity::class.java).apply {
                putExtra(EXTRA_LIBRARY_ID, libraryId)
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun AuthorBrowseScreen(libraryId: String) {
    val context = LocalContext.current
    var authors by remember { mutableStateOf<List<Author>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    // Fetch authors on startup
    LaunchedEffect(libraryId) {
        val apiClient = ApiClient.getApiService()
        apiClient?.getLibraryAuthors(libraryId)?.enqueue(object : Callback<AuthorsResponse> {
            override fun onResponse(
                call: Call<AuthorsResponse>,
                response: Response<AuthorsResponse>
            ) {
                if (response.isSuccessful) {
                    authors = response.body()?.authors?.sortedBy { it.name } ?: emptyList()
                }
                isLoading = false
            }

            override fun onFailure(call: Call<AuthorsResponse>, t: Throwable) {
                isLoading = false
            }
        })
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 48.dp, vertical = 32.dp)
    ) {
        // Title
        Text(
            text = "Authors",
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(24.dp))

        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Loading authors...",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else if (authors.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No authors found",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            // Authors Grid
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 180.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(authors) { author ->
                    AuthorCard(
                        author = author,
                        onClick = {
                            val intent = Intent(context, AuthorDetailActivity::class.java).apply {
                                putExtra("authorId", author.id)
                            }
                            context.startActivity(intent)
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun AuthorCard(
    author: Author,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .width(180.dp)
            .height(240.dp),
        colors = CardDefaults.colors(
            containerColor = MaterialTheme.colorScheme.surface,
            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        border = CardDefaults.border(
            focusedBorder = Border(
                border = BorderStroke(3.dp, MaterialTheme.colorScheme.primary)
            )
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Author image (circular)
            AuthorImage(
                authorId = author.id,
                imagePath = author.imagePath,
                contentDescription = author.name,
                modifier = Modifier.clip(CircleShape),
                size = 120.dp
            )

            // Author name
            Text(
                text = author.name,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth()
            )

            // Book count
            author.numBooks?.let { count ->
                Text(
                    text = "$count ${if (count == 1) "book" else "books"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
