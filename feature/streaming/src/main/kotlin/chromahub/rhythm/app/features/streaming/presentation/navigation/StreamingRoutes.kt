package chromahub.rhythm.app.features.streaming.presentation.navigation

import android.net.Uri

/**
 * Route constants for streaming destinations hosted in :app's local graph
 * (the composition root registers them; :feature:streaming exports the
 * names — SettingsRoutes precedent). Extracted from :app's
 * LocalNavigationStreaming.kt in P4-phase P4-4.
 */
object StreamingRoutes {
    const val ArtistDetail = "streaming_artist/{artistId}?artistName={artistName}"
    const val AlbumDetail = "streaming_album/{albumId}?albumName={albumName}"
    const val PlaylistDetail = "streaming_playlist/{playlistId}"
    const val ServiceSetup = "streaming_service_setup/{serviceId}"
    const val GoSettings = "streaming_go_settings"

    fun artist(artistId: String, artistName: String): String =
        "streaming_artist/${Uri.encode(artistId)}?artistName=${Uri.encode(artistName)}"
    fun album(albumId: String, albumName: String): String =
        "streaming_album/${Uri.encode(albumId)}?albumName=${Uri.encode(albumName)}"
    fun playlist(playlistId: String): String =
        "streaming_playlist/${Uri.encode(playlistId)}"
    fun serviceSetup(serviceId: String): String =
        "streaming_service_setup/${Uri.encode(serviceId)}"
}
