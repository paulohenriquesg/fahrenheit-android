package com.paulohenriquesg.fahrenheit.search

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import com.paulohenriquesg.fahrenheit.api.ApiClient
import com.paulohenriquesg.fahrenheit.api.Library
import com.paulohenriquesg.fahrenheit.api.LibraryItem
import com.paulohenriquesg.fahrenheit.ui.navigation.LibraryItemsRow
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

@Composable
fun SearchScreen(searchHandler: SearchHandler) {
    val context = LocalContext.current
    val libraryId = (context as? SearchActivity)?.libraryId
    var query by remember { mutableStateOf(TextFieldValue("")) }
    var searchResults by remember { mutableStateOf(listOf<LibraryItem>()) }
    var library by remember { mutableStateOf<Library?>(null) }
    val keyboardController = LocalSoftwareKeyboardController.current
    val listState = remember { LazyListState() }

    LaunchedEffect(libraryId) {
        if (libraryId != null) {
            loadLibrary(context, libraryId) { response ->
                library = response
            }
        }
    }

    LaunchedEffect(query.text) {
        libraryId?.let {
            searchHandler.performLibrarySearch(it, query.text, library?.mediaType ?: "books") { results ->
                searchResults = results
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        TextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            placeholder = { Text("Search...") },
            keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Next),
            keyboardActions = KeyboardActions(
                onNext = {
                    keyboardController?.hide()
                    // Handle the search action here if needed
                }
            )
        )
//        Button(
//            onClick = {
//                libraryId?.let {
//                    searchHandler.performLibrarySearch(it) { results ->
//                        searchResults = results
//                    }
//                }
//            },
//            modifier = Modifier.align(Alignment.End)
//        ) {
//            Text("Search")
//        }

        library?.let {
            Text(text = it.name ?: "No name", style = MaterialTheme.typography.titleLarge)
            // Add more UI elements to display library details
        } ?: Text(text = "Loading...")

        Spacer(modifier = Modifier.height(16.dp))
        LibraryItemsRow(libraryItems = searchResults, listState = listState)
    }
}

private fun loadLibrary(
    context: Context,
    libraryId: String,
    callback: (Library?) -> Unit
) {
    val apiService = ApiClient.getApiService()
    apiService?.getLibrary(libraryId)?.enqueue(object : Callback<Library> {
        override fun onResponse(call: Call<Library>, response: Response<Library>) {
            if (response.isSuccessful) {
                callback(response.body())
            } else {
                Toast.makeText(context, "Failed to load library", Toast.LENGTH_SHORT).show()
    }
}

        override fun onFailure(call: Call<Library>, t: Throwable) {
            Toast.makeText(context, "Network error: ${t.message}", Toast.LENGTH_SHORT).show()
        }
    })
}