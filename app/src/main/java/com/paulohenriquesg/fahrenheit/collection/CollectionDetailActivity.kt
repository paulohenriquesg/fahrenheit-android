package com.paulohenriquesg.fahrenheit.collection

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.tv.material3.MaterialTheme
import com.google.gson.Gson
import com.paulohenriquesg.fahrenheit.api.Collection
import com.paulohenriquesg.fahrenheit.ui.theme.FahrenheitTheme

class CollectionDetailActivity : ComponentActivity() {
    companion object {
        private const val EXTRA_COLLECTION_JSON = "collection_json"

        fun createIntent(context: Context, collection: Collection): Intent {
            val gson = Gson()
            return Intent(context, CollectionDetailActivity::class.java).apply {
                putExtra(EXTRA_COLLECTION_JSON, gson.toJson(collection))
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val collectionJson = intent.getStringExtra(EXTRA_COLLECTION_JSON)
        if (collectionJson == null) {
            finish()
            return
        }

        val gson = Gson()
        val collection = gson.fromJson(collectionJson, Collection::class.java)

        setContent {
            FahrenheitTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    CollectionDetailContent(collection = collection, onBookClick = { book ->
                        val intent = com.paulohenriquesg.fahrenheit.detail.DetailActivity.createIntent(this, book.id)
                        startActivity(intent)
                    })
                }
            }
        }
    }
}
