package com.paulohenriquesg.fahrenheit.search

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.tv.material3.MaterialTheme
import com.paulohenriquesg.fahrenheit.ui.theme.FahrenheitTheme

class SearchActivity : ComponentActivity() {
    var libraryId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        libraryId = intent.getStringExtra(EXTRA_LIBRARY)

        setContent {
            FahrenheitTheme {
                Surface(
                    color = MaterialTheme.colorScheme.background,
                    contentColor = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.fillMaxSize(),
                    shape = RectangleShape
                ) {
                    SearchScreen()
                }
            }
        }
    }

    companion object {
        private const val EXTRA_LIBRARY = "libraryId"

        fun createIntent(context: Context, libraryId: String): Intent {
            return Intent(context, SearchActivity::class.java).apply {
                putExtra(EXTRA_LIBRARY, libraryId)
            }
        }
    }
}