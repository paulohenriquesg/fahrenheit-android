package com.paulohenriquesg.fahrenheit.ui.elements

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.tv.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import com.paulohenriquesg.fahrenheit.api.Author
import com.paulohenriquesg.fahrenheit.api.Shelf
import com.paulohenriquesg.fahrenheit.ui.StableKeys

@Composable
fun AuthorShelfRow(shelf: Shelf, authors: List<Author>, onItemClick: (Author) -> Unit) {
    Column {
        ShelfHeading(shelf.label)
        Spacer(modifier = Modifier.height(8.dp))
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(horizontal = 16.dp)
        ) {
            val keys = StableKeys.of(authors) { a -> a.id }
            items(authors.size, key = { keys[it] }) { index ->
                val author = authors[index]
                AuthorCard(author = author, onClick = { onItemClick(author) })
            }
        }
    }
}
