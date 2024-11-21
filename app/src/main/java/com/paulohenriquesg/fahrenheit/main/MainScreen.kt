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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.paulohenriquesg.fahrenheit.api.ApiClient
import com.paulohenriquesg.fahrenheit.api.LibrariesResponse
import com.paulohenriquesg.fahrenheit.api.Library
import com.paulohenriquesg.fahrenheit.api.LibraryItem
import com.paulohenriquesg.fahrenheit.login.LoginActivity
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
    var currentLibraryName by remember { mutableStateOf("") }
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
                        currentLibraryName = libraries[0].name
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
                            val intent = SettingsActivity.createIntent(context)
                            context.startActivity(intent)
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
                                currentLibraryName = library.name
                                fetchLibraryItems(library.id) { items ->
                                    libraryItems = items
                                    scrollToFirstItem() // Scroll to the first item when switching libraries
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
                            sharedPreferencesHandler.clearPreferences()
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
                    Text(
                        text = currentLibraryName,
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(16.dp)
                    )
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