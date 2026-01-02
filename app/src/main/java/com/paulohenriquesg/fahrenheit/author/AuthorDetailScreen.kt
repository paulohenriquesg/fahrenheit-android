package com.paulohenriquesg.fahrenheit.author

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import com.paulohenriquesg.fahrenheit.ui.components.DetailHeader
import com.paulohenriquesg.fahrenheit.ui.components.ItemsGrid
import com.paulohenriquesg.fahrenheit.ui.elements.AuthorImage
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
            else -> {
                authorDetail?.let { author ->
                    AuthorDetailContent(
                        author = author,
                        onBookClick = { book ->
                            val intent = DetailActivity.createIntent(context, book.id)
                            context.startActivity(intent)
                        }
                    )
                }
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
            text = "Books",
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
