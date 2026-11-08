package chromahub.rhythm.app.core.domain.repository

import com.qlzhj.rhythm.core.model.Song
import kotlinx.coroutines.flow.StateFlow

/**
 * Library data and refresh seam implemented by MusicViewModel
 * (:feature:local) and consumed by the media scan and backup settings
 * screens (B1 PR-8, docs/b1-musicviewmodel-seams-design.md §4.2). Callers
 * pass [refreshLibrary]'s argument explicitly — the ViewModel's default is
 * not visible through the interface.
 */
interface LibraryAdminController {
    val songs: StateFlow<List<Song>>
    val filteredSongs: StateFlow<List<Song>>
    val isLibraryRefreshing: StateFlow<Boolean>

    /** Default carried here from the ViewModel's former declaration (Kotlin
     *  forbids defaults on overrides); all call sites inherit it unchanged. */
    fun refreshLibrary(showMediaScanLoader: Boolean = true)
}
