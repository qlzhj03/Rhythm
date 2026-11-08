package chromahub.rhythm.app.core.domain.repository

import com.qlzhj.rhythm.core.model.StreamingAlbum
import com.qlzhj.rhythm.core.model.StreamingSong
import kotlinx.coroutines.flow.StateFlow

/**
 * Narrow seam for streaming library actions consumed by :feature:local's
 * HomeScreen (D-phase D3): download badge/toggles, pull-to-refresh, the
 * streaming home body's state loading and on-demand album tracks, without a
 * feature-to-feature dependency. Members mirror the screen's verified
 * consumption set. Implemented by StreamingMusicViewModel; :app passes the
 * root instance.
 */
interface StreamingLibraryController {
    fun downloadSong(song: StreamingSong)

    fun downloadSongById(songId: String)

    fun removeDownload(songId: String)

    fun refreshHome()

    fun loadLibrary(forceSync: Boolean)

    fun loadHomeContent()

    fun clearError()

    suspend fun getAlbumSongs(album: StreamingAlbum): List<StreamingSong>

    val allSongs: StateFlow<List<StreamingSong>>

    val recommendations: StateFlow<List<StreamingSong>>

    val downloadedSongs: StateFlow<List<StreamingSong>>

    val downloadingSongIds: StateFlow<Set<String>>
}
