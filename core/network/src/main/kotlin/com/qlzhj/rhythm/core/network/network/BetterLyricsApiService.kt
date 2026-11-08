package com.qlzhj.rhythm.core.network.network

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Service interface for Better Lyrics API.
 * Better Lyrics API serves synchronized lyrics in TTML format.
 */
interface BetterLyricsApiService {
    /**
     * Get lyrics for a song and artist.
     */
    @GET("getLyrics")
    suspend fun getLyrics(
        @Query("a") artist: String,
        @Query("s") song: String
    ): BetterLyricsResponse
}

/**
 * Response model for Better Lyrics getLyrics endpoint.
 */
@Serializable
data class BetterLyricsResponse(
    @SerialName("ttml") val ttml: String? = null)
