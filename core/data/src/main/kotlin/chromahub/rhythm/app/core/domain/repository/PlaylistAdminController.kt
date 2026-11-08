package chromahub.rhythm.app.core.domain.repository

import android.net.Uri
import com.qlzhj.rhythm.core.model.Playlist
import com.qlzhj.rhythm.core.model.Song
import chromahub.rhythm.app.util.PlaylistImportExportUtils
import kotlinx.coroutines.flow.StateFlow

/**
 * Playlist management seam implemented by MusicViewModel (:feature:local)
 * and consumed by the playlist settings screens across module boundaries
 * (B1 PR-8, docs/b1-musicviewmodel-seams-design.md §4.2). Members mirror the
 * ViewModel's declarations verbatim; callers pass arguments explicitly since
 * interface types do not expose the ViewModel's default values.
 */
interface PlaylistAdminController {
    val playlists: StateFlow<List<Playlist>>
    fun createPlaylist(
        name: String,
        songs: List<Song> = emptyList(),
        onCreated: ((Playlist) -> Unit)? = null,
        showSnackbar: ((String) -> Unit)? = null
    )
    fun deletePlaylist(playlistId: String)
    fun importPlaylist(
        uri: Uri,
        onResult: (Result<String>) -> Unit,
        onRestartRequired: (() -> Unit)? = null
    )
    fun exportAllPlaylists(
        format: PlaylistImportExportUtils.PlaylistExportFormat,
        includeDefaultPlaylists: Boolean = false,
        userSelectedDirectoryUri: Uri? = null,
        onResult: (Result<String>) -> Unit
    )
    fun ensurePlaylistsSaved()
    fun setDefaultPlaylistsEnabled(enabled: Boolean)
    fun setShowLikedInPlaylists(enabled: Boolean)
    fun setSmartPlaylistMostPlayed(enabled: Boolean)
    fun setSmartPlaylistRecentlyPlayed(enabled: Boolean)
    fun setSmartPlaylistRecentlyAdded(enabled: Boolean)
    fun setSmartPlaylistOnRepeat(enabled: Boolean)
    fun setSmartPlaylistForgottenFavorites(enabled: Boolean)
}
