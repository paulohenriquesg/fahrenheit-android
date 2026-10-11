package com.paulohenriquesg.fahrenheit.ui.elements

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.tv.material3.MaterialTheme

/**
 * The mark of something heard to the end: a tick reads as "done". One drawing
 * for the podcast page's rows (#181) and Home's episode cards (#192).
 */
@Composable
fun FinishedTick(modifier: Modifier = Modifier) {
    Icon(
        imageVector = Icons.Filled.CheckCircle,
        contentDescription = "Finished",
        tint = MaterialTheme.colorScheme.primary,
        modifier = modifier
    )
}
