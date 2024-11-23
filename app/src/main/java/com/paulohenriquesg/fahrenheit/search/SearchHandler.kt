package com.paulohenriquesg.fahrenheit.search

import android.content.Context
import android.widget.Toast
import com.paulohenriquesg.fahrenheit.api.ApiClient
import com.paulohenriquesg.fahrenheit.api.LibraryItem
import com.paulohenriquesg.fahrenheit.api.SearchLibraryItemsResponse
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class SearchHandler(private val context: Context) {
    fun performLibrarySearch(libraryId: String, query: String, mediaType: String, updateItems: (List<LibraryItem>) -> Unit) {
        val apiClient = ApiClient.getApiService()
        if (apiClient != null) {
        apiClient.searchLibraryItems(libraryId, query).enqueue(object : Callback<SearchLibraryItemsResponse> {
            override fun onResponse(call: Call<SearchLibraryItemsResponse>, response: Response<SearchLibraryItemsResponse>) {
                    if (response.isSuccessful) {
                        val response = response.body()
                        if (response != null) {
                            if (mediaType == "book") {
                                val books = response.book
                                if (books != null) {
                                    val libraryItems = books.mapNotNull { it.libraryItem }
                                    updateUIWithLibraryItems(libraryItems, updateItems)
                                }
                            } else if (mediaType == "podcast") {
                                val podcasts = response.podcast
                                if (podcasts != null) {
                                    val libraryItems = podcasts.mapNotNull { it.libraryItem }
                                    updateUIWithLibraryItems(libraryItems, updateItems)
                                }
                            }
                        }
                        // Update the UI with the fetched library items
//                        updateUIWithLibraryItems(libraryItems, updateItems)
                    } else {
                        Toast.makeText(context, "Failed to load library items", Toast.LENGTH_SHORT).show()
                    }
                }

            override fun onFailure(call: Call<SearchLibraryItemsResponse>, t: Throwable) {
                    Toast.makeText(context, "Network error: ${t.message}", Toast.LENGTH_SHORT).show()
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