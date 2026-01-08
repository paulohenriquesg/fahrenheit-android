package com.paulohenriquesg.fahrenheit.library

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.LibraryBooks
import androidx.compose.material.icons.filled.Podcasts
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.tv.material3.*
import com.paulohenriquesg.fahrenheit.api.ApiClient
import com.paulohenriquesg.fahrenheit.api.Library
import com.paulohenriquesg.fahrenheit.api.LibrariesResponse
import com.paulohenriquesg.fahrenheit.storage.SharedPreferencesHandler
import com.paulohenriquesg.fahrenheit.ui.theme.FahrenheitTheme
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class LibrarySelectionActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            FahrenheitTheme {
                Surface(
                    color = MaterialTheme.colorScheme.background,
                    contentColor = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.fillMaxSize(),
                    shape = RectangleShape
                ) {
                    LibrarySelectionScreen()
                }
            }
        }
    }

    companion object {
        fun createIntent(context: Context): Intent {
            return Intent(context, LibrarySelectionActivity::class.java)
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun LibrarySelectionScreen() {
    val context = LocalContext.current
    var libraries by remember { mutableStateOf<List<Library>>(emptyList()) }
    var currentLibraryId by remember {
        mutableStateOf(SharedPreferencesHandler(context).getSelectedLibraryId())
    }

    // Fetch libraries on startup
    LaunchedEffect(Unit) {
        val apiClient = ApiClient.getApiService()
        apiClient?.getLibraries()?.enqueue(object : Callback<LibrariesResponse> {
            override fun onResponse(
                call: Call<LibrariesResponse>,
                response: Response<LibrariesResponse>
            ) {
                if (response.isSuccessful) {
                    libraries = response.body()?.libraries?.sortedBy { it.displayOrder } ?: emptyList()
                }
            }

            override fun onFailure(call: Call<LibrariesResponse>, t: Throwable) {
                // Handle error
            }
        })
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp)
    ) {
        // Title
        Text(
            text = "Select Library",
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Library Grid
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 250.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(libraries) { library ->
                LibrarySelectionCard(
                    library = library,
                    isSelected = library.id == currentLibraryId,
                    onClick = {
                        // Save selected library
                        library.id?.let { id ->
                            SharedPreferencesHandler(context).saveSelectedLibraryId(id)
                            currentLibraryId = id
                            // Close activity and return to main
                            (context as? ComponentActivity)?.finish()
                        }
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun LibrarySelectionCard(
    library: Library,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .size(width = 250.dp, height = 200.dp),
        colors = CardDefaults.colors(
            containerColor = if (isSelected)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.surface,
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
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = getIconForMediaType(library.mediaType),
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = if (isSelected)
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = library.name ?: "Unknown",
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Center,
                color = if (isSelected)
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.onSurface
            )

            if (isSelected) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "✓ Currently Selected",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

/**
 * Get appropriate icon for library media type
 */
fun getIconForMediaType(mediaType: String?): ImageVector {
    return when (mediaType) {
        "book" -> Icons.AutoMirrored.Filled.LibraryBooks
        "podcast" -> Icons.Filled.Podcasts
        else -> Icons.AutoMirrored.Filled.LibraryBooks
    }
}
