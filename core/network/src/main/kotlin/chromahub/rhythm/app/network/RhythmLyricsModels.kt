package chromahub.rhythm.app.network

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName


/**
 * Rhythm lyrics song search result
 */
@Serializable
data class RhythmLyricsSearchResult(
    @SerialName("id") val id: String = "",
    @SerialName("songName") val songName: String? = null,
    @SerialName("artistName") val artistName: String? = null,
    @SerialName("albumName") val albumName: String? = null,
    @SerialName("artwork") val artwork: String? = null,
    @SerialName("releaseDate") val releaseDate: String? = null,
    @SerialName("duration") val duration: Long? = null,
    @SerialName("isrc") val isrc: String? = null,
    @SerialName("url") val url: String? = null,
    @SerialName("contentRating") val contentRating: String? = null,
    @SerialName("albumId") val albumId: String? = null)

/**
 * Rhythm lyrics response containing word-by-word synchronized lyrics
 */
@Serializable
data class RhythmLyricsResponse(
    @SerialName("info") val info: String? = null,
    @SerialName("type") val type: String? = null,
    @SerialName("content") val content: List<RhythmLyricsLine>? = null,
    @SerialName("ttml_content") val ttmlContent: String? = null,
    @SerialName("source") val source: String? = null,
    @SerialName("track") val track: RhythmLyricsTrackInfo? = null)

/**
 * Represents a line of lyrics with word-level synchronization
 */
@Serializable
data class RhythmLyricsLine(
    @SerialName("text") val text: List<RhythmLyricsWord>? = null,
    @SerialName("background") val background: Boolean? = null,
    @SerialName("backgroundText") val backgroundText: List<String>? = null,
    @SerialName("oppositeTurn") val oppositeTurn: Boolean? = null,
    @SerialName("timestamp") val timestamp: Long? = null,
    @SerialName("endtime") val endtime: Long? = null,
    @SerialName("endIsImplicit") val endIsImplicit: Boolean? = null
)

/**
 * Represents a single word or syllable with precise timing
 */
@Serializable
data class RhythmLyricsWord(
    @SerialName("text") val text: String = "",
    @SerialName("part") val part: Boolean? = null,
    @SerialName("timestamp") val timestamp: Long = 0L,
    @SerialName("endtime") val endtime: Long // Word end timestamp in milliseconds
)

/**
 * Track information from Rhythm lyrics source
 */
@Serializable
data class RhythmLyricsTrackInfo(
    @SerialName("albumName") val albumName: String? = null,
    @SerialName("artistName") val artistName: String? = null,
    @SerialName("name") val name: String? = null,
    @SerialName("releaseDate") val releaseDate: String? = null,
    @SerialName("hasLyrics") val hasLyrics: Boolean? = null,
    @SerialName("hasTimeSyncedLyrics") val hasTimeSyncedLyrics: Boolean? = null)
