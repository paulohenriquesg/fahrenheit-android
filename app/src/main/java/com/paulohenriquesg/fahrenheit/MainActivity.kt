package com.paulohenriquesg.fahrenheit

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
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
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.tv.material3.ExperimentalTvMaterial3Api
import com.paulohenriquesg.fahrenheit.api.ApiClient
import com.paulohenriquesg.fahrenheit.api.LibrariesResponse
import com.paulohenriquesg.fahrenheit.api.Library
import com.paulohenriquesg.fahrenheit.ui.theme.FahrenheitTheme
import kotlinx.coroutines.launch
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

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
                    MainScreen(username ?: "User")
                }
            }
        }
    }
}

@Composable
fun MainScreen(username: String) {
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var libraries by remember { mutableStateOf(listOf<Library>()) }

    // Retrieve base URL and token from SharedPreferences
    val sharedPreferences = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
    val host = sharedPreferences.getString("host", null)
    val token = sharedPreferences.getString("token", null)

    if (host == null) {
        // Show error and redirect to login
        LaunchedEffect(Unit) {
            Toast.makeText(context, "Host is not configured. Please log in.", Toast.LENGTH_LONG).show()
            val intent = Intent(context, LoginActivity::class.java)
            context.startActivity(intent)
            (context as ComponentActivity).finish()
        }
        return
    }

//    val baseUrl = "$host"

    // Fetch libraries from the API
    LaunchedEffect(Unit) {
        val apiService = ApiClient.create(host, token)
        apiService.getLibraries().enqueue(object : Callback<LibrariesResponse> {
            override fun onResponse(call: Call<LibrariesResponse>, response: Response<LibrariesResponse>) {
                if (response.isSuccessful) {
                    libraries = response.body()?.libraries ?: emptyList()
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
                    .background(Color.White) // Set the background color to white
                    .fillMaxSize()
            ) {
                Spacer(modifier = Modifier.height(56.dp)) // Add a gap at the top

                Row(
                    modifier = Modifier
                        .padding(16.dp)
                        .clickable { /* Handle Home click */ },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.Home, contentDescription = "Home")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Home")
                }
                Row(
                    modifier = Modifier
                        .padding(16.dp)
                        .clickable { /* Handle Profile click */ },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.Person, contentDescription = "Profile")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Profile")
                }
                Row(
                    modifier = Modifier
                        .padding(16.dp)
                        .clickable { /* Handle Settings click */ },
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
                            .clickable { /* Handle library item click */ },
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
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .onKeyEvent { keyEvent ->
                        if (keyEvent.key.keyCode == Key.DirectionLeft.keyCode) {
                            scope.launch { drawerState.open() }
                            true
                        } else {
                            false
                        }
                    }
            ) {
                IconButton(onClick = { scope.launch { drawerState.open() } }) {
                    Icon(Icons.Filled.Menu, contentDescription = "Open Menu")
                }
                Greeting(username)
            }
        }
    )
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
        MainScreen("Android")
    }
}