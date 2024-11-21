package com.paulohenriquesg.fahrenheit.main

import android.content.Context
import android.widget.Toast
import com.paulohenriquesg.fahrenheit.api.ApiClient
import com.paulohenriquesg.fahrenheit.api.LibraryItem
import com.paulohenriquesg.fahrenheit.api.LibraryItemsResponse
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class MainHandler(private val context: Context) {

    fun fetchLibraryItems(libraryId: String, updateItems: (List<LibraryItem>) -> Unit) {
        val apiClient = ApiClient.getApiService()
        if (apiClient != null) {
            apiClient.getLibraryItems(libraryId).enqueue(object : Callback<LibraryItemsResponse> {
                override fun onResponse(call: Call<LibraryItemsResponse>, response: Response<LibraryItemsResponse>) {
                    if (response.isSuccessful) {
                        val libraryItems = response.body()?.results
                        // Update the UI with the fetched library items
                        updateUIWithLibraryItems(libraryItems, updateItems)
                    } else {
                        Toast.makeText(context, "Failed to load library items", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<LibraryItemsResponse>, t: Throwable) {
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