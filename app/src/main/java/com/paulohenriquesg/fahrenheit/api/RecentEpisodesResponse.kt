package com.paulohenriquesg.fahrenheit.api

import com.google.gson.annotations.SerializedName

data class RecentEpisodesResponse(
    @SerializedName("episodes") val episodes: List<RecentPodcastEpisode>
)

data class RecentPodcastEpisode(
    @SerializedName("libraryItemId") val libraryItemId: String,
    @SerializedName("episodeId") val episodeId: String,
    @SerializedName("episode") val episode: Episode,
    @SerializedName("podcast") val podcast: LibraryItem
)
