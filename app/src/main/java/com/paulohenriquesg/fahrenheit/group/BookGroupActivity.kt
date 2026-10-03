package com.paulohenriquesg.fahrenheit.group

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.res.stringResource
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.SurfaceDefaults
import com.google.gson.Gson
import com.paulohenriquesg.fahrenheit.R
import com.paulohenriquesg.fahrenheit.api.Collection
import com.paulohenriquesg.fahrenheit.api.Series
import com.paulohenriquesg.fahrenheit.detail.DetailActivity
import com.paulohenriquesg.fahrenheit.ui.components.BookGroup
import com.paulohenriquesg.fahrenheit.ui.components.BookGroupDetailContent
import com.paulohenriquesg.fahrenheit.ui.theme.FahrenheitTheme

/**
 * A series or a collection: its books under its name (#73).
 *
 * These were two Activities, the same file with the nouns swapped, so every
 * change had to be made twice. What still differs between them is the line
 * an empty one shows, carried by [Kind].
 */
class BookGroupActivity : ComponentActivity() {

    enum class Kind(@StringRes val emptyMessage: Int) {
        SERIES(R.string.series_no_books),
        COLLECTION(R.string.collection_no_books)
    }

    companion object {
        private const val EXTRA_GROUP_JSON = "group_json"
        private const val EXTRA_KIND = "kind"

        fun forSeries(context: Context, series: Series): Intent =
            intent(context, Kind.SERIES, BookGroup.of(series))

        fun forCollection(context: Context, collection: Collection): Intent =
            intent(context, Kind.COLLECTION, BookGroup.of(collection))

        private fun intent(context: Context, kind: Kind, group: BookGroup): Intent =
            Intent(context, BookGroupActivity::class.java).apply {
                putExtra(EXTRA_GROUP_JSON, Gson().toJson(group))
                putExtra(EXTRA_KIND, kind.name)
            }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val groupJson = intent.getStringExtra(EXTRA_GROUP_JSON)
        val kind = intent.getStringExtra(EXTRA_KIND)?.let { name -> Kind.entries.find { it.name == name } }
        if (groupJson == null || kind == null) {
            finish()
            return
        }
        val group = Gson().fromJson(groupJson, BookGroup::class.java)

        setContent {
            FahrenheitTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    colors = SurfaceDefaults.colors(containerColor = MaterialTheme.colorScheme.background),
                    shape = RectangleShape,
                ) {
                    BookGroupDetailContent(
                        group = group,
                        emptyMessage = stringResource(kind.emptyMessage),
                        onBookClick = { book -> startActivity(DetailActivity.createIntent(this@BookGroupActivity, book.id)) }
                    )
                }
            }
        }
    }
}
