package chromahub.rhythm.app.network

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Service interface for LRCLib lyrics API
 * LRCLib API Documentation: https://lrclib.net/docs
 */
interface LRCLibApiService {
    /**
     * Search lyrics records by query or track/artist details.
     * This endpoint returns a JSON array of matching records.
     */
    @GET("api/search")
    suspend fun searchLyrics(
        @Query("q") query: String? = null,
        @Query("track_name") trackName: String? = null,
        @Query("artist_name") artistName: String? = null,
        @Query("album_name") albumName: String? = null,
        @Query("duration") duration: Int? = null
    ): List<LrcLibLyrics>
    
    /**
     * Get lyrics by ID for precise lookups
     */
    @GET("api/get/{id}")
    suspend fun getLyricsById(@Path("id") id: Int): LrcLibLyrics?
}

/**
 * Data class mapping of LRCLib search result
 */
@Serializable
data class LrcLibLyrics(
    @SerialName("id") val id: Int = 0,
    @SerialName("name") val name: String? = null, // Legacy field
    @SerialName("trackName") val trackName: String? = null,
    @SerialName("artistName") val artistName: String? = null,
    @SerialName("albumName") val albumName: String? = null,
    @SerialName("duration") val duration: Double? = null,
    @SerialName("instrumental") val instrumental: Boolean? = false,
    @SerialName("plainLyrics") val plainLyrics: String? = null,
    @SerialName("syncedLyrics") val syncedLyrics: String? = null) {
    /**
     * Helper method to check if lyrics are available
     */
    fun hasLyrics(): Boolean = !plainLyrics.isNullOrBlank() || !syncedLyrics.isNullOrBlank()
    
    /**
     * Helper method to check if synced lyrics are available
     */
    fun hasSyncedLyrics(): Boolean = !syncedLyrics.isNullOrBlank()
    
    /**
     * Helper method to get the best available lyrics (prioritizing synced)
     */
    fun getBestLyrics(): String? = syncedLyrics?.takeIf { it.isNotBlank() } ?: plainLyrics?.takeIf { it.isNotBlank() }
    
    /**
     * Get synced lyrics or null if not available
     */
    fun getSyncedLyricsOrNull(): String? = if (syncedLyrics.isNullOrBlank()) null else syncedLyrics
    
    /**
     * Get plain lyrics or null if not available
     */
    fun getPlainLyricsOrNull(): String? = if (plainLyrics.isNullOrBlank()) null else plainLyrics
}
