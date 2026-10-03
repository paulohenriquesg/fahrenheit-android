package com.paulohenriquesg.fahrenheit.ui.elements

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.tv.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import com.paulohenriquesg.fahrenheit.api.Series
import com.paulohenriquesg.fahrenheit.api.Shelf
import com.paulohenriquesg.fahrenheit.ui.StableKeys

@Composable
fun SeriesShelfRow(shelf: Shelf, series: List<Series>, seeAllTotal: Int? = null, onSeeAll: () -> Unit = {}, onItemClick: (Series) -> Unit) {
    Column {
        ShelfHeading(shelf.label)
        Spacer(modifier = Modifier.height(8.dp))
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(horizontal = 16.dp)
        ) {
            val keys = StableKeys.of(series) { s -> s.id }
            items(series.size, key = { keys[it] }) { index ->
                SeriesCard(series = series[index], onClick = onItemClick)
            }
            if (seeAllTotal != null) {
                // Last, after everything the server sent (#123).
                item(key = "see-all") {
                    SeeAllCard(seeAllTotal, onSeeAll, Modifier.padding(8.dp).width(200.dp).height(300.dp))
                }
            }
        }
    }
}
