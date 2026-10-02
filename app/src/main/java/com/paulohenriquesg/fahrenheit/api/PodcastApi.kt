package com.paulohenriquesg.fahrenheit.api

import com.google.gson.annotations.SerializedName
import retrofit2.http.GET
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
}

/** Only what the app reads from GET /api/me. */
data class Me(@SerializedName("type") val type: String?)

data class CheckNewResponse(@SerializedName("episodes") val episodes: List<FeedEpisode>?)

/** An episode as the feed describes it, before the server has downloaded it. */
data class FeedEpisode(@SerializedName("title") val title: String?)
