package com.paulohenriquesg.fahrenheit.api

import com.google.gson.annotations.SerializedName

data class RecentEpisodesResponse(
    @SerializedName("episodes") val episodes: List<RecentPodcastEpisode>
)

// Episodes come back flat, not wrapped: their `episode` field is the episode
// number as a string, and `podcast` is the podcast media object rather than a
// library item. Modelling either as an object made the whole response fail to
// parse, so the Latest Episodes screen was always empty.
data class RecentPodcastEpisode(
    @SerializedName("id") val id: String,
    @SerializedName("libraryItemId") val libraryItemId: String,
    @SerializedName("title") val title: String?,
    @SerializedName("description") val description: String?,
    @SerializedName("podcast") val podcast: RecentEpisodePodcast?
)

data class RecentEpisodePodcast(
    @SerializedName("metadata") val metadata: RecentEpisodePodcastMetadata?
)

data class RecentEpisodePodcastMetadata(
    @SerializedName("title") val title: String?
)
