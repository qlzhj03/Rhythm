package chromahub.rhythm.app.infrastructure.service

import com.qlzhj.rhythm.core.model.Song
import kotlinx.coroutines.flow.StateFlow

/**
 * Read-only playback UI state, implemented by MusicViewModel (:feature:local)
 * and consumed across module boundaries by the settings scaffold (B1 PR-8,
 * docs/b1-musicviewmodel-seams-design.md §4.2).
 */
interface PlaybackStateSource {
    val currentSong: StateFlow<Song?>
    val isPlaying: StateFlow<Boolean>
}
