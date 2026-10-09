package com.paulohenriquesg.fahrenheit.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.paulohenriquesg.fahrenheit.api.MediaProgressResponse
import com.paulohenriquesg.fahrenheit.podcast.EpisodeProgress
import com.paulohenriquesg.fahrenheit.progress.ProgressStore
import com.paulohenriquesg.fahrenheit.ui.elements.CoverProgress
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/**
 * What Home shows, as one value (#208).
 *
 * @property covers how far into each book or episode on a shelf (#104).
 * @property episodes what has been heard, by episode id, for Latest Episodes (#78).
 */
data class HomeUiState(
    val covers: CoverProgress = CoverProgress.None,
    val episodes: Map<String, EpisodeProgress> = emptyMap()
)

/**
 * Home's state, out of the composables (#208). So far its progress, read from
 * the shared [ProgressStore] as it changes (#207): what the player and the
 * marks wrote shows on Home with no read of its own. The rest moves as Home
 * is next touched.
 */
class HomeViewModel(store: ProgressStore) : ViewModel() {
    val uiState: StateFlow<HomeUiState> = store.entries
        .map { all -> stateOf(all.values.toList()) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, stateOf(store.entries.value.values.toList()))

    private fun stateOf(progress: List<MediaProgressResponse>) =
        HomeUiState(covers = CoverProgress.index(progress), episodes = EpisodeProgress.byEpisode(progress))
}
