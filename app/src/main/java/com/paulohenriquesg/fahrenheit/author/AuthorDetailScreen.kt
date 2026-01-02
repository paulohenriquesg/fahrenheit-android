package com.paulohenriquesg.fahrenheit.author

import android.widget.Toast
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
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
import com.paulohenriquesg.fahrenheit.api.AuthorDetailResponse
import com.paulohenriquesg.fahrenheit.detail.DetailActivity
import com.paulohenriquesg.fahrenheit.ui.elements.AuthorImage
import com.paulohenriquesg.fahrenheit.ui.elements.LibraryItemCard
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

@Composable
fun AuthorDetailScreen(authorId: String) {
    val context = LocalContext.current
    var authorDetail by remember { mutableStateOf<AuthorDetailResponse?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(authorId) {
        val apiClient = ApiClient.getApiService()
        if (apiClient == null) {
            errorMessage = "API client not initialized"
            isLoading = false
            return@LaunchedEffect
        }

        apiClient.getAuthor(authorId).enqueue(object : Callback<AuthorDetailResponse> {
            override fun onResponse(
                call: Call<AuthorDetailResponse>,
                response: Response<AuthorDetailResponse>
            ) {
                isLoading = false
                if (response.isSuccessful) {
                    authorDetail = response.body()
                } else {
                    errorMessage = "Failed to load author: ${response.code()}"
                }
            }

            override fun onFailure(call: Call<AuthorDetailResponse>, t: Throwable) {
                isLoading = false
                errorMessage = "Error: ${t.message}"
            }
        })
    }

    Box(modifier = Modifier.fillMaxSize()) {
        when {
            isLoading -> {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )
            }
            errorMessage != null -> {
                Text(
                    text = errorMessage ?: "Unknown error",
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(16.dp)
                )
            }
            authorDetail != null -> {
                AuthorDetailContent(
                    author = authorDetail!!,
                    onBookClick = { book ->
                        val intent = DetailActivity.createIntent(context, book.id)
                        context.startActivity(intent)
                    }
                )
            }
        }
    }
}

@Composable
fun AuthorDetailContent(
    author: AuthorDetailResponse,
    onBookClick: (com.paulohenriquesg.fahrenheit.api.LibraryItem) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Author header section - similar to book/podcast detail
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp)
        ) {
            AuthorImage(
                authorId = author.id,
                imagePath = author.imagePath,
                contentDescription = author.name
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(
                modifier = Modifier.height(200.dp)
            ) {
                Text(
                    text = author.name,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )

                val booksCount = author.libraryItems?.size ?: 0
                Text(
                    text = "$booksCount ${if (booksCount == 1) "book" else "books"}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )

                author.description?.let { desc ->
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = desc,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 5,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Books grid
        author.libraryItems?.let { books ->
            if (books.isNotEmpty()) {
                Text(
                    text = "Books",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                LazyVerticalGrid(
                    columns = GridCells.Fixed(4),
                    contentPadding = PaddingValues(8.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(books) { book ->
                        LibraryItemCard(
                            item = book,
                            onClick = { onBookClick(book) }
                        )
                    }
                }
            } else {
                Text(
                    text = "No books found for this author",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }
        }
    }
}
