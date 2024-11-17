package com.paulohenriquesg.fahrenheit

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import com.paulohenriquesg.fahrenheit.api.ApiClient
import com.paulohenriquesg.fahrenheit.api.LibrariesResponse
import com.paulohenriquesg.fahrenheit.api.Library
import com.paulohenriquesg.fahrenheit.api.LibraryItem
import com.paulohenriquesg.fahrenheit.api.LibraryItemsResponse
import com.paulohenriquesg.fahrenheit.ui.theme.FahrenheitTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import androidx.compose.material3.Button
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid


class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalTvMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Retrieve username from SharedPreferences
        val sharedPreferences = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        val username = sharedPreferences.getString("username", "User")

        setContent {
            FahrenheitTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    shape = RectangleShape
                ) {
                    MainScreen(username ?: "User", ::fetchLibraryItems)
                }
            }
        }
    }

    private fun fetchLibraryItems(libraryId: String, updateItems: (List<LibraryItem>) -> Unit) {
        val apiClient = ApiClient.getApiService()
        if (apiClient != null) {
            apiClient.getLibraryItems(libraryId).enqueue(object : Callback<LibraryItemsResponse> {
                override fun onResponse(call: Call<LibraryItemsResponse>, response: Response<LibraryItemsResponse>) {
                    if (response.isSuccessful) {
                        val libraryItems = response.body()?.results
                        // Update the UI with the fetched library items
                        updateUIWithLibraryItems(libraryItems, updateItems)
                    } else {
                        Toast.makeText(this@MainActivity, "Failed to load library items", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<LibraryItemsResponse>, t: Throwable) {
                    Toast.makeText(this@MainActivity, "Network error: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            })
        }
    }

    private fun updateUIWithLibraryItems(items: List<LibraryItem>?, updateItems: (List<LibraryItem>) -> Unit) {
        items?.let {
            updateItems(it)
        }
    }
}

@Composable
fun MainScreen(username: String, fetchLibraryItems: (String, (List<LibraryItem>) -> Unit) -> Unit) {
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var libraries by remember { mutableStateOf(listOf<Library>()) }
    var libraryItems by remember { mutableStateOf(listOf<LibraryItem>()) }
    val listState = rememberLazyListState()
    var isRowLayout by remember { mutableStateOf(true) }

    val apiClient = ApiClient.getApiService()
    if (apiClient == null) {
        // Show error and redirect to login
        LaunchedEffect(Unit) {
            Toast.makeText(context, "Host is not configured. Please log in.", Toast.LENGTH_LONG)
                .show()
            val intent = Intent(context, LoginActivity::class.java)
            context.startActivity(intent)
            (context as ComponentActivity).finish()
        }
        return
    }

    // Fetch libraries from the API
    LaunchedEffect(Unit) {
        apiClient.getLibraries().enqueue(object : Callback<LibrariesResponse> {
            override fun onResponse(
                call: Call<LibrariesResponse>,
                response: Response<LibrariesResponse>
            ) {
                if (response.isSuccessful) {
                    libraries = response.body()?.libraries ?: emptyList()

                    if (libraries.isNotEmpty()) {
                        fetchLibraryItems(libraries[0].id) { items ->
                            libraryItems = items
                        }
                    }
                }
            }

            override fun onFailure(call: Call<LibrariesResponse>, t: Throwable) {
                // Handle error
            }
        })
    }

    // Handle back button press
    BackHandler(enabled = drawerState.isOpen) {
        scope.launch { drawerState.close() }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            Column(
                modifier = Modifier
                    .width(300.dp) // Set a fixed width for the drawer
                    .background(Color.White) // Set the background color to white
                    .fillMaxSize()
            ) {
                Spacer(modifier = Modifier.height(56.dp)) // Add a gap at the top

                Row(
                    modifier = Modifier
                        .padding(16.dp)
                        .clickable {
                            scope.launch { drawerState.close() }
                        },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.Home, contentDescription = "Home")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Home")
                }
                Row(
                    modifier = Modifier
                        .padding(16.dp)
                        .clickable {
                            scope.launch { drawerState.close() }
                        },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.Person, contentDescription = "Profile")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Profile")
                }
                Row(
                    modifier = Modifier
                        .padding(16.dp)
                        .clickable {
                            scope.launch { drawerState.close() }
                        },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.Settings, contentDescription = "Settings")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Settings")
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp)) // Add a separator before Libraries

                Row(
                    modifier = Modifier
                        .padding(16.dp)
                        .clickable { /* Handle Libraries click */ },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.Settings, contentDescription = "Libraries")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Libraries")
                }

                // Display library items fetched from the API
                libraries.forEach { library ->
                    Row(
                        modifier = Modifier
                            .padding(16.dp)
                            .clickable {
                                fetchLibraryItems(library.id) { items ->
                                    libraryItems = items
                                }
                                scope.launch { drawerState.close() }
                            },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(library.name)
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp)) // Add a separator after Libraries

                Spacer(modifier = Modifier.weight(1f)) // Push the Logout item to the bottom

                Row(
                    modifier = Modifier
                        .padding(16.dp)
                        .clickable {
                            // Handle Logout click
                            val prefs =
                                context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
                            prefs
                                .edit()
                                .clear()
                                .apply()
                            val intent = Intent(context, LoginActivity::class.java)
                            context.startActivity(intent)
                            (context as ComponentActivity).finish()
                        },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = "Logout")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Logout")
                }
            }
        },
        content = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .onKeyEvent { keyEvent ->
                        if (keyEvent.key.keyCode == Key.DirectionLeft.keyCode && !listState.isScrollInProgress) {
                            scope.launch { drawerState.open() }
                            true
                        } else {
                            false
                        }
                    }
            ) {
                IconButton(
                    onClick = { if (!listState.isScrollInProgress) scope.launch { drawerState.open() } },
                    modifier = Modifier.align(Alignment.TopStart)
                ) {
                    Icon(Icons.Filled.Menu, contentDescription = "Open Menu")
                }
                Button(
                    onClick = { isRowLayout = !isRowLayout },
                    modifier = Modifier.align(Alignment.TopCenter)
                ) {
                    Text(text = if (isRowLayout) "Switch to Fluid Layout" else "Switch to Row Layout")
                }
                Greeting(
                    name = username,
                    modifier = Modifier.align(Alignment.TopEnd)
                )
                Column(
                    modifier = Modifier.align(Alignment.Center)
                ) {
                    if (isRowLayout) {
                        LibraryItemsRow(libraryItems, listState)
                    } else {
                        LibraryItemsFluid(libraryItems)
                    }
                }
            }
        }
    )
}

@Composable
fun LibraryItemsRow(libraryItems: List<LibraryItem>, listState: LazyListState) {
    val context = LocalContext.current

    LazyRow(
        state = listState,
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        items(libraryItems) { item ->
            LibraryItemCard(item) { clickedItem ->
                val intent = DetailActivity.createIntent(context, clickedItem.id)
                context.startActivity(intent)
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LibraryItemsFluid(libraryItems: List<LibraryItem>) {
    val context = LocalContext.current

    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 150.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        items(libraryItems.size) { index ->
            val item = libraryItems[index]

            LibraryItemCard(
                item = item,
                onClick = { clickedItem ->
                    val intent = DetailActivity.createIntent(context, clickedItem.id)
                    context.startActivity(intent)
                }
            )
        }
    }
}

@Composable
fun LibraryItemCard(item: LibraryItem, onClick: (LibraryItem) -> Unit) {
    val context = LocalContext.current
    var coverImage by remember { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(item.id) {
        withContext(Dispatchers.IO) {
            val apiClient = ApiClient.getApiService()
            if (apiClient != null) {
                val response = apiClient.getItemCover(item.id).execute()
                if (response.isSuccessful) {
                    response.body()?.let { responseBody ->
                        val inputStream = responseBody.byteStream()
                        coverImage = BitmapFactory.decodeStream(inputStream)
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Failed to load cover image", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .padding(8.dp)
            .width(200.dp)
            .height(300.dp)
            .clickable { onClick(item) }
    ) {
        Card(
            elevation = CardDefaults.cardElevation(4.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                coverImage?.let {
                    Image(
                        bitmap = it.asImageBitmap(),
                        contentDescription = item.media.metadata.title,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                    )
                } ?: Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .background(Color.Gray)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = item.media.metadata.title,
                    style = MaterialTheme.typography.titleLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        if (item.mediaType == "podcast" && item.numEpisodesIncomplete != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .background(Color.Red, shape = RoundedCornerShape(50))
                    .padding(8.dp)
            ) {
                Text(
                    text = if (item.numEpisodesIncomplete > 99) "99+" else item.numEpisodesIncomplete.toString(),
                    color = Color.White,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier.padding(16.dp)
    )
}

@Preview(showBackground = true)
@Composable
fun MainScreenPreview() {
    FahrenheitTheme {
        MainScreen("Android", { _, _ -> })
    }
}