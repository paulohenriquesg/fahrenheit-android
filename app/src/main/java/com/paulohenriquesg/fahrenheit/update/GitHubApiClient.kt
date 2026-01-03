package com.paulohenriquesg.fahrenheit.update

import com.google.gson.GsonBuilder
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object GitHubApiClient {
    private const val BASE_URL = "https://api.github.com/"
    private const val GITHUB_OWNER = "paulohenriquesg"
    private const val GITHUB_REPO = "fahrenheit-android"

    private var apiService: GitHubApiService? = null

    fun getApiService(): GitHubApiService {
        if (apiService == null) {
            val okHttpClient = OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(10, TimeUnit.SECONDS)
                .build()

            val gson = GsonBuilder()
                .create()

            val retrofit = Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(okHttpClient)
                .addConverterFactory(GsonConverterFactory.create(gson))
                .build()

            apiService = retrofit.create(GitHubApiService::class.java)
        }

        return apiService!!
    }

    fun getOwner() = GITHUB_OWNER
    fun getRepo() = GITHUB_REPO
}
