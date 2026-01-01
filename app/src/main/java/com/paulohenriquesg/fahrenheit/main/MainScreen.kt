package com.paulohenriquesg.fahrenheit.main

import android.content.Intent
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.LibraryBooks
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Podcasts
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalNavigationDrawer
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import com.paulohenriquesg.fahrenheit.api.ApiClient
import com.paulohenriquesg.fahrenheit.api.LibrariesResponse
import com.paulohenriquesg.fahrenheit.api.Library
import com.paulohenriquesg.fahrenheit.api.LibraryItem
import com.paulohenriquesg.fahrenheit.login.LoginActivity
import com.paulohenriquesg.fahrenheit.search.SearchActivity
import com.paulohenriquesg.fahrenheit.settings.SettingsActivity
import com.paulohenriquesg.fahrenheit.storage.SharedPreferencesHandler
import com.paulohenriquesg.fahrenheit.ui.navigation.LibraryItemsFluid
import com.paulohenriquesg.fahrenheit.ui.navigation.LibraryItemsRow
import kotlinx.coroutines.launch
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

@Composable
fun MainScreen(fetchLibraryItems: (String, (List<LibraryItem>) -> Unit) -> Unit) {
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val sharedPreferencesHandler = remember { SharedPreferencesHandler(context) }
    val userPreferences = sharedPreferencesHandler.getUserPreferences()
    val username = userPreferences.username
    var libraries by remember { mutableStateOf(listOf<Library>()) }
    var libraryItems by remember { mutableStateOf(listOf<LibraryItem>()) }
    var currentLibrary by remember { mutableStateOf<Library?>(null) }
    val listState = rememberLazyListState()
    var isRowLayout by remember { mutableStateOf(true) }
    var selectedItem by remember { mutableStateOf<String?>(null) }

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
                    libraries =
                        response.body()?.libraries?.sortedBy { it.displayOrder } ?: emptyList()

                    if (libraries.isNotEmpty()) {
                        currentLibrary = libraries[0]
                        libraries[0].id?.let {
                            fetchLibraryItems(it) { items ->
                                libraryItems = items
                            }
                        }
                    }
                }
            }

            override fun onFailure(call: Call<LibrariesResponse>, t: Throwable) {
                // Handle error
            }
        })
    }

    // Function to scroll to the first item
    fun scrollToFirstItem() {
        scope.launch {
            listState.scrollToItem(0)
        }
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
                    .background(MaterialTheme.colorScheme.surface)
                    .fillMaxSize()
            ) {
                Spacer(modifier = Modifier.height(56.dp)) // Add a gap at the top

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .clickable {
                            selectedItem = "Home"
                            scope.launch { drawerState.close() }
                        },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.Home, contentDescription = "Home")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Home",
                        color = MaterialTheme.colorScheme.onSurface
                    ) // Use surface color for text
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .clickable {
                            selectedItem = "Profile"
                            scope.launch { drawerState.close() }
                        },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.Person, contentDescription = "Profile")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Profile",
                        color = MaterialTheme.colorScheme.onSurface
                    ) // Use surface color for text
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .clickable {
                            selectedItem = "Settings"
                            val intent = SettingsActivity.createIntent(context)
                            context.startActivity(intent)
                            scope.launch { drawerState.close() }
                        },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.Settings, contentDescription = "Settings")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Settings",
                        color = MaterialTheme.colorScheme.onSurface
                    ) // Use surface color for text
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp)) // Add a separator before Libraries

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .clickable {
                            selectedItem = "Libraries"
                            selectedItem = null // Unselect the item
                        },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.AutoMirrored.Filled.LibraryBooks, contentDescription = "Libraries")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Libraries",
                        color = MaterialTheme.colorScheme.onSurface
                    ) // Use surface color for text
                }

                // Display library items fetched from the API
                libraries.forEach { library ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                            .clickable {
                                selectedItem = library.name
                                currentLibrary = library
                                library.id?.let {
                                    fetchLibraryItems(it) { items ->
                                        libraryItems = items
                                        scrollToFirstItem() // Scroll to the first item when switching libraries
                                    }
                                }
                                scope.launch {
                                    drawerState.close()
                                    selectedItem = null // Unselect the item
                                }
                            },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            getIconForMediaType(library.mediaType),
                            contentDescription = library.name
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            library.name!!,
                            color = MaterialTheme.colorScheme.onSurface
                        ) // Use surface color for text
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp)) // Add a separator after Libraries

                Spacer(modifier = Modifier.weight(1f)) // Push the Logout item to the bottom

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .clickable {
                            // Handle Logout click
                            selectedItem = "Logout"
                            sharedPreferencesHandler.clearPreferences()
                            val intent = Intent(context, LoginActivity::class.java)
                            context.startActivity(intent)
                            (context as ComponentActivity).finish()
                            selectedItem = null // Unselect the item
                        },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = "Logout")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Logout",
                        color = MaterialTheme.colorScheme.surface
                    ) // Use surface color for text
                }
            }
        },
        content = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
//                    .onKeyEvent { keyEvent ->
//                        if (keyEvent.key.keyCode == Key.DirectionLeft.keyCode && !listState.isScrollInProgress && !listState.canScrollBackward) {
//                            scope.launch { drawerState.open() }
//                            true
//                        } else {
//                            false
//                        }
//                    }
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
                Row(
                    modifier = Modifier.align(Alignment.TopEnd),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            val intent = Intent(context, SearchActivity::class.java).apply {
                                putExtra("libraryId", currentLibrary?.id)
                            }
                            context.startActivity(intent)
                        },
                    ) {
                        Icon(Icons.Filled.Search, contentDescription = "Search")
                    }
                    Greeting(
                        name = username,
                        modifier = Modifier.padding(16.dp)
                    )
                }
                Column(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 30.dp) // Add padding to avoid overlap with the menu icon
                ) {
                    val itemCount = libraryItems.size
                    val itemLabel =
                        if (libraries.find { it.name == currentLibrary?.name }?.mediaType == "book") "books" else "podcasts"
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(16.dp)
                    ) {
                        currentLibrary?.name?.let {
                            Text(
                                text = it,
                                style = MaterialTheme.typography.titleLarge
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "($itemCount $itemLabel)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
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
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(text = "Hello, $name!", modifier = modifier)
}

fun getIconForMediaType(mediaType: String?): ImageVector {
    return when (mediaType) {
        "book" -> Icons.Filled.Book
        "podcast" -> Icons.Filled.Podcasts
        else -> Icons.Filled.Book // default icon
    }
}