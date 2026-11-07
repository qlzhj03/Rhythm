package chromahub.rhythm.app.features.local.presentation.screens

import chromahub.rhythm.app.core.model.StreamingAlbum
import chromahub.rhythm.app.core.model.StreamingArtist
import chromahub.rhythm.app.core.model.StreamingPlaylist
import chromahub.rhythm.app.core.model.StreamingServiceSession
import chromahub.rhythm.app.core.model.StreamingSong
import chromahub.rhythm.app.core.model.StreamingSyncProgress

/**
 * Immutable snapshot of the streaming (Go-mode) home state, collected in :app
 * from StreamingMusicViewModel (D-phase D3, Option T): HomeScreen's streaming
 * body renders from this instead of touching the streaming view model
 * directly, keeping :feature:local free of a compile dependency on
 * :feature:streaming. Members are all :core:model types.
 */
data class StreamingHomeState(
    val isOnline: Boolean = true,
    val syncProgress: StreamingSyncProgress = StreamingSyncProgress(),
    val serviceSessions: Map<String, StreamingServiceSession> = emptyMap(),
    val isAuthenticated: Boolean = false,
    val hasLoadedHomeContent: Boolean = false,
    val hasLoadedLibrary: Boolean = false,
    val allSongs: List<StreamingSong> = emptyList(),
    val recommendations: List<StreamingSong> = emptyList(),
    val newReleases: List<StreamingAlbum> = emptyList(),
    val savedAlbums: List<StreamingAlbum> = emptyList(),
    val followedArtists: List<StreamingArtist> = emptyList(),
    val savedPlaylists: List<StreamingPlaylist> = emptyList(),
    val downloadedSongs: List<StreamingSong> = emptyList(),
    val downloadedAlbums: List<StreamingAlbum> = emptyList(),
    val downloadedArtists: List<StreamingArtist> = emptyList(),
)
