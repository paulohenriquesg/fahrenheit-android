package com.paulohenriquesg.fahrenheit.series

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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.*
import com.paulohenriquesg.fahrenheit.api.ApiClient
import com.paulohenriquesg.fahrenheit.api.Series
import com.paulohenriquesg.fahrenheit.api.SeriesResponse
import com.paulohenriquesg.fahrenheit.ui.theme.FahrenheitTheme
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class SeriesBrowseActivity : ComponentActivity() {
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
                    SeriesBrowseScreen(libraryId = libraryId)
                }
            }
        }
    }

    companion object {
        private const val EXTRA_LIBRARY_ID = "library_id"

        fun createIntent(context: Context, libraryId: String): Intent {
            return Intent(context, SeriesBrowseActivity::class.java).apply {
                putExtra(EXTRA_LIBRARY_ID, libraryId)
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun SeriesBrowseScreen(libraryId: String) {
    val context = LocalContext.current
    var seriesList by remember { mutableStateOf<List<Series>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(libraryId) {
        val apiClient = ApiClient.getApiService()
        apiClient?.getLibrarySeries(libraryId)?.enqueue(object : Callback<SeriesResponse> {
            override fun onResponse(
                call: Call<SeriesResponse>,
                response: Response<SeriesResponse>
            ) {
                if (response.isSuccessful) {
                    val body = response.body()
                    seriesList = body?.results?.sortedBy { it.name } ?: emptyList()
                    android.util.Log.d("SeriesBrowse", "Loaded ${seriesList.size} series")
                } else {
                    android.util.Log.e("SeriesBrowse", "Error: ${response.code()} - ${response.message()}")
                    android.util.Log.e("SeriesBrowse", "Error body: ${response.errorBody()?.string()}")
                }
                isLoading = false
            }

            override fun onFailure(call: Call<SeriesResponse>, t: Throwable) {
                android.util.Log.e("SeriesBrowse", "Failure: ${t.message}", t)
                isLoading = false
            }
        })
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 48.dp, vertical = 32.dp)
    ) {
        Text(
            text = "Series",
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
                    text = "Loading series...",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else if (seriesList.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No series found",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 200.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(seriesList) { series ->
                    SeriesCard(
                        series = series,
                        onClick = {
                            val intent = SeriesDetailActivity.createIntent(context, series)
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
fun SeriesCard(
    series: Series,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .width(200.dp)
            .height(160.dp),
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
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = series.name,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth()
            )

            series.books?.size?.let { count ->
                Spacer(modifier = Modifier.height(8.dp))
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
