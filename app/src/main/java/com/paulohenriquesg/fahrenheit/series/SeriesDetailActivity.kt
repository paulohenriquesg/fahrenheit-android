package com.paulohenriquesg.fahrenheit.series

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
import com.paulohenriquesg.fahrenheit.api.Series
import com.paulohenriquesg.fahrenheit.ui.theme.FahrenheitTheme

class SeriesDetailActivity : ComponentActivity() {
    companion object {
        private const val EXTRA_SERIES_JSON = "series_json"

        fun createIntent(context: Context, series: Series): Intent {
            val gson = Gson()
            return Intent(context, SeriesDetailActivity::class.java).apply {
                putExtra(EXTRA_SERIES_JSON, gson.toJson(series))
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val seriesJson = intent.getStringExtra(EXTRA_SERIES_JSON)
        if (seriesJson == null) {
            finish()
            return
        }

        val gson = Gson()
        val series = gson.fromJson(seriesJson, Series::class.java)

        setContent {
            FahrenheitTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    SeriesDetailContent(series = series, onBookClick = { book ->
                        val intent = com.paulohenriquesg.fahrenheit.detail.DetailActivity.createIntent(this, book.id)
                        startActivity(intent)
                    })
                }
            }
        }
    }
}
