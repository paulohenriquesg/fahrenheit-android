package com.paulohenriquesg.fahrenheit.stats

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import com.paulohenriquesg.fahrenheit.api.ApiClient
import com.paulohenriquesg.fahrenheit.api.ListeningStatsResponse
import com.paulohenriquesg.fahrenheit.ui.components.BrowseTopBar
import com.paulohenriquesg.fahrenheit.ui.theme.FahrenheitTheme
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import kotlin.math.roundToInt

class StatsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            FahrenheitTheme {
                Surface(
                    color = MaterialTheme.colorScheme.background,
                    contentColor = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.fillMaxSize(),
                    shape = RectangleShape
                ) {
                    StatsScreen()
                }
            }
        }
    }

    companion object {
        fun createIntent(context: Context): Intent {
            return Intent(context, StatsActivity::class.java)
        }
    }
}

@Composable
fun StatsScreen() {
    val context = LocalContext.current
    var stats by remember { mutableStateOf<ListeningStatsResponse?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        val apiClient = ApiClient.getApiService()
        apiClient?.getListeningStats()?.enqueue(object : Callback<ListeningStatsResponse> {
            override fun onResponse(
                call: Call<ListeningStatsResponse>,
                response: Response<ListeningStatsResponse>
            ) {
                if (response.isSuccessful) {
                    stats = response.body()
                }
                isLoading = false
            }

            override fun onFailure(call: Call<ListeningStatsResponse>, t: Throwable) {
                isLoading = false
            }
        })
    }

    Column(modifier = Modifier.fillMaxSize()) {
        BrowseTopBar(
            title = "Listening Statistics",
            onBackClick = { (context as? ComponentActivity)?.finish() }
        )

        Spacer(modifier = Modifier.height(8.dp))

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 48.dp, vertical = 16.dp)
        ) {
            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Loading stats...",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else if (stats == null) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No statistics available",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                Column(
                    verticalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    // Total listening time
                    StatCard(
                        title = "Total Listening Time",
                        value = formatTime(stats!!.totalTime)
                    )

                    // Number of items
                    stats!!.items.size.let { itemCount ->
                        StatCard(
                            title = "Items Listened To",
                            value = "$itemCount ${if (itemCount == 1) "item" else "items"}"
                        )
                    }

                    // Number of days tracked
                    stats!!.days.size.let { dayCount ->
                        StatCard(
                            title = "Days with Activity",
                            value = "$dayCount ${if (dayCount == 1) "day" else "days"}"
                        )
                    }

                    // Average per day
                    if (stats!!.days.isNotEmpty()) {
                        val avgPerDay = stats!!.totalTime / stats!!.days.size
                        StatCard(
                            title = "Average per Day",
                            value = formatTime(avgPerDay)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun StatCard(title: String, value: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.displaySmall,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

fun formatTime(seconds: Long): String {
    val hours = (seconds / 3600.0).roundToInt()
    val minutes = ((seconds % 3600) / 60.0).roundToInt()

    return when {
        hours > 0 && minutes > 0 -> "$hours ${if (hours == 1) "hour" else "hours"}, $minutes ${if (minutes == 1) "minute" else "minutes"}"
        hours > 0 -> "$hours ${if (hours == 1) "hour" else "hours"}"
        minutes > 0 -> "$minutes ${if (minutes == 1) "minute" else "minutes"}"
        else -> "$seconds ${if (seconds == 1L) "second" else "seconds"}"
    }
}
