package chromahub.rhythm.app.network

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

import retrofit2.http.GET
import retrofit2.http.Query

interface ITunesSearchApiService {
    @GET("search")
    suspend fun searchSongs(
        @Query("term") term: String,
        @Query("media") media: String = "music",
        @Query("entity") entity: String = "song",
        @Query("limit") limit: Int = 10,
        @Query("explicit") explicit: String = "Yes"
    ): ITunesSearchResponse
}

@Serializable
data class ITunesSearchResponse(
    @SerialName("resultCount") val resultCount: Int = 0,
    @SerialName("results") val results: List<ITunesTrackResult> = emptyList())

@Serializable
data class ITunesTrackResult(
    @SerialName("trackId") val trackId: Long = 0L,
    @SerialName("trackName") val trackName: String? = null,
    @SerialName("artistName") val artistName: String? = null,
    @SerialName("collectionName") val collectionName: String? = null,
    @SerialName("trackTimeMillis") val trackTimeMillis: Long? = null,
    @SerialName("artworkUrl100") val artworkUrl100: String? = null)
