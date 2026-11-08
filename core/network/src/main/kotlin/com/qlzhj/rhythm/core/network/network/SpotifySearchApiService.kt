package com.qlzhj.rhythm.core.network.network

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query

/**
 * Service interface for Spotify Web API
 * Used to search for tracks and get Spotify track IDs
 */
interface SpotifySearchApiService {
    @GET("search")
    suspend fun searchTracks(
        @Query("q") query: String,
        @Query("type") type: String = "track",
        @Query("limit") limit: Int = 10,
        @Query("market") market: String = "US",
        @Header("Authorization") authorization: String
    ): Response<SpotifySearchResponse>
}

// ---------- DTOs ----------

@Serializable
data class SpotifySearchResponse(
    val tracks: SpotifyTracksResponse? = null)

@Serializable
data class SpotifyTracksResponse(
    val items: List<SpotifyTrack> = emptyList(),
    val total: Int = 0,
    val limit: Int = 0,
    val offset: Int = 0
)

@Serializable
data class SpotifyTrack(
    val id: String = "",
    val name: String = "",
    val artists: List<SpotifyArtist> = emptyList(),
    val album: SpotifyAlbum? = null,
    @SerialName("duration_ms") val durationMs: Long = 0,
    val popularity: Int = 0,
    @SerialName("external_urls") val externalUrls: SpotifyExternalUrls? = null)

@Serializable
data class SpotifyArtist(
    val id: String = "",
    val name: String = "",
    @SerialName("external_urls") val externalUrls: SpotifyExternalUrls? = null)

@Serializable
data class SpotifyAlbum(
    val id: String = "",
    val name: String = "",
    val images: List<SpotifyImage> = emptyList(),
    @SerialName("release_date") val releaseDate: String? = null)

@Serializable
data class SpotifyImage(
    val url: String = "",
    val width: Int? = null,
    val height: Int? = null)

@Serializable
data class SpotifyExternalUrls(
    val spotify: String? = null)
