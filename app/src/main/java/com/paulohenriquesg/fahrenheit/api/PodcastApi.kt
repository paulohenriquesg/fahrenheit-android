package com.paulohenriquesg.fahrenheit.api

import com.google.gson.JsonObject
import com.google.gson.annotations.SerializedName
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Asking the server about a podcast's feed (#76).
 *
 * The server owns the feed parser, the download queue and the de-duplication;
 * the app never reads a feed itself, because it can only play what the server
 * has downloaded.
 */
interface PodcastApi {
    /** The signed-in user, asked fresh: an account can be promoted or demoted. */
    @GET("api/me")
    suspend fun me(): Me

    /**
     * Checks the feed and queues what is new for download. Admin or root only;
     * 403 for anyone else, 400 for a podcast with no feed.
     */
    @GET("api/podcasts/{id}/checknew")
    suspend fun checkNew(
        @Path("id") podcastId: String,
        @Query("limit") limit: Int
    ): CheckNewResponse

    /**
     * Reads a feed and returns its episodes without downloading any. Admin or
     * root only. The episodes stay raw JSON: [downloadEpisodes] takes them
     * back in exactly this shape.
     */
    @POST("api/podcasts/feed")
    suspend fun feed(@Body request: FeedRequest): FeedResponse

    /**
     * The download queue for a library: which episode is downloading now and
     * which wait, in order. No percentage - the server does not track one.
     */
    @GET("api/libraries/{id}/episode-downloads")
    suspend fun downloadQueue(@Path("id") libraryId: String): DownloadQueue

    /** Queues the given feed episodes for download. Admin or root only. */
    @POST("api/podcasts/{id}/download-episodes")
    suspend fun downloadEpisodes(
        @Path("id") podcastId: String,
        @Body episodes: List<JsonObject>
    )
}

/** The server's one download queue, for a library: what is downloading, and what waits. */
data class DownloadQueue(
    @SerializedName("currentDownload") val currentDownload: QueuedDownload?,
    @SerializedName("queue") val queue: List<QueuedDownload>?
)

data class QueuedDownload(
    @SerializedName("libraryItemId") val libraryItemId: String?,
    @SerializedName("guid") val guid: String?,
    @SerializedName("url") val url: String?
)

data class FeedRequest(@SerializedName("rssFeed") val rssFeed: String)

data class FeedResponse(@SerializedName("podcast") val podcast: FeedPodcast?)

data class FeedPodcast(@SerializedName("episodes") val episodes: List<JsonObject>?)

/** Only what the app reads from GET /api/me. */
data class Me(@SerializedName("type") val type: String?)

data class CheckNewResponse(@SerializedName("episodes") val episodes: List<FeedEpisode>?)

/** An episode as the feed describes it, before the server has downloaded it. */
data class FeedEpisode(@SerializedName("title") val title: String?)
