package com.paulohenriquesg.fahrenheit.author

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.tv.material3.MaterialTheme
import com.paulohenriquesg.fahrenheit.ui.theme.FahrenheitTheme

class AuthorDetailActivity : ComponentActivity() {
    companion object {
        private const val EXTRA_AUTHOR_ID = "author_id"

        fun createIntent(context: Context, authorId: String): Intent {
            return Intent(context, AuthorDetailActivity::class.java).apply {
                putExtra(EXTRA_AUTHOR_ID, authorId)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val authorId = intent.getStringExtra(EXTRA_AUTHOR_ID)
        if (authorId == null) {
            finish()
            return
        }

        setContent {
            FahrenheitTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AuthorDetailScreen(authorId = authorId)
                }
            }
        }
    }
}
