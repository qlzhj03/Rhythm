package com.qlzhj.rhythm.core.network.network

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Service interface for Deezer API
 * Deezer provides a free public API for searching artists and getting their images
 * API Documentation: https://developers.deezer.com/api
 */
interface DeezerApiService {
    @GET("search/artist")
    suspend fun searchArtists(
        @Query("q") query: String,
        @Query("limit") limit: Int = 25 // Increased limit to get more results
    ): DeezerSearchResponse

    @GET("search/album")
    suspend fun searchAlbums(
        @Query("q") query: String,
        @Query("limit") limit: Int = 25 // Increased limit to get more results
    ): DeezerAlbumSearchResponse

    @GET("search")
    suspend fun searchTracks(
        @Query("q") query: String,
        @Query("limit") limit: Int = 25
    ): DeezerTrackSearchResponse
}

// ---------- DTOs ----------

@Serializable
data class DeezerSearchResponse(
    val data: List<DeezerArtist> = emptyList(),
    val total: Int = 0,
    val next: String? = null
)

@Serializable
data class DeezerAlbumSearchResponse(
    val data: List<DeezerAlbum> = emptyList(),
    val total: Int = 0,
    val next: String? = null
)

@Serializable
data class DeezerArtist(
    val id: Long = 0L,
    val name: String = "",
    val link: String = "",
    val share: String? = null,
    val picture: String? = null,
    @SerialName("picture_small") val pictureSmall: String? = null,
    @SerialName("picture_medium") val pictureMedium: String? = null,
    @SerialName("picture_big") val pictureBig: String? = null,
    @SerialName("picture_xl") val pictureXl: String? = null,
    @SerialName("nb_album") val nbAlbum: Int = 0,
    @SerialName("nb_fan") val nbFan: Int = 0,
    val radio: Boolean = false,
    val tracklist: String? = null,
    val type: String = "artist"
)

@Serializable
data class DeezerAlbum(
    val id: Long = 0L,
    val title: String = "",
    val link: String? = null,
    val cover: String? = null,
    @SerialName("cover_small") val coverSmall: String? = null,
    @SerialName("cover_medium") val coverMedium: String? = null,
    @SerialName("cover_big") val coverBig: String? = null,
    @SerialName("cover_xl") val coverXl: String? = null,
    @SerialName("nb_tracks") val nbTracks: Int = 0,
    @SerialName("release_date") val releaseDate: String? = null,
    val artist: DeezerAlbumArtist? = null,
    val type: String = "album"
)

@Serializable
data class DeezerAlbumArtist(
    val id: Long = 0L,
    val name: String = "",
    val link: String? = null,
    val picture: String? = null,
    @SerialName("picture_small") val pictureSmall: String? = null,
    @SerialName("picture_medium") val pictureMedium: String? = null,
    @SerialName("picture_big") val pictureBig: String? = null,
    @SerialName("picture_xl") val pictureXl: String? = null
)

@Serializable
data class DeezerTrackSearchResponse(
    val data: List<DeezerTrack> = emptyList()
)

@Serializable
data class DeezerTrack(
    val id: Long = 0L,
    val title: String = "",
    val artist: DeezerTrackArtist? = null,
    val album: DeezerTrackAlbum? = null)

@Serializable
data class DeezerTrackArtist(
    val id: Long = 0L,
    val name: String = "")

@Serializable
data class DeezerTrackAlbum(
    val id: Long = 0L,
    val title: String = "")
