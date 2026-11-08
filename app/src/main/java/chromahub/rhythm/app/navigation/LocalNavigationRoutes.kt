package chromahub.rhythm.app.navigation

import chromahub.rhythm.app.features.local.presentation.navigation.Screen
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import chromahub.rhythm.app.R
import chromahub.rhythm.app.shared.data.model.AppSettings
import chromahub.rhythm.app.util.rememberOffMain
import androidx.navigation.NavHostController
import chromahub.rhythm.app.shared.presentation.components.bottomsheets.AddToPlaylistBottomSheet
import chromahub.rhythm.app.shared.presentation.components.bottomsheets.SongInfoBottomSheet
import chromahub.rhythm.app.features.local.presentation.components.dialogs.CreatePlaylistDialog
import chromahub.rhythm.app.features.local.presentation.screens.LibraryScreen
import chromahub.rhythm.app.features.local.presentation.screens.HomeScreen
import chromahub.rhythm.app.features.local.presentation.screens.PlaylistDetailScreen
import chromahub.rhythm.app.features.local.presentation.screens.ArtistDetailScreen
import chromahub.rhythm.app.features.local.presentation.screens.AlbumDetailScreen
import chromahub.rhythm.app.shared.presentation.screens.settings.*
import chromahub.rhythm.app.core.model.toLibraryAlbum
import chromahub.rhythm.app.core.model.toLibraryArtist
import chromahub.rhythm.app.core.model.toLibraryPlaylist
import chromahub.rhythm.app.core.model.toLibrarySong
import chromahub.rhythm.app.shared.presentation.components.common.StreamingServiceOptions
import chromahub.rhythm.app.features.streaming.presentation.viewmodel.StreamingMusicViewModel
import chromahub.rhythm.app.core.model.StreamingArtist
import chromahub.rhythm.app.core.model.StreamingSong
import chromahub.rhythm.app.core.model.Album
import chromahub.rhythm.app.core.model.Artist
import chromahub.rhythm.app.core.model.Playlist
import chromahub.rhythm.app.core.model.Song
import chromahub.rhythm.app.features.streaming.presentation.navigation.StreamingRoutes
import chromahub.rhythm.app.core.model.findAlbumForSong
import chromahub.rhythm.app.core.common.ArtistSeparator
import chromahub.rhythm.app.features.local.presentation.viewmodel.MusicViewModel
import chromahub.rhythm.app.features.local.presentation.viewmodel.MusicViewModel.SortOrder
import kotlinx.coroutines.launch
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDp
import androidx.compose.animation.core.spring
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.compose.ui.graphics.Color
import chromahub.rhythm.app.features.local.presentation.screens.LibraryTab
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalContext
import chromahub.rhythm.app.core.model.toLibraryAlbum
import chromahub.rhythm.app.core.model.toLibraryArtist
import chromahub.rhythm.app.core.model.toLibraryPlaylist
import chromahub.rhythm.app.core.model.toLibrarySong

// Route bodies for the local graph, extracted verbatim from
// LocalNavigationContent (P4-phase P4-3). Pure cut: bodies reference the
// enclosing composable's parameters and re-declared derived locals only.
@OptIn(androidx.compose.animation.ExperimentalAnimationApi::class)
@Composable
fun androidx.compose.animation.AnimatedVisibilityScope.SlideUpCornerWrapper(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val cornerRadius by transition.animateDp(
        label = "slideUpCornerRadius",
        transitionSpec = { androidx.compose.animation.core.spring(stiffness = androidx.compose.animation.core.Spring.StiffnessMedium) }
    ) { state ->
        if (state == androidx.compose.animation.EnterExitState.Visible) 0.dp else 28.dp
    }
    
    androidx.compose.material3.Surface(
        modifier = modifier
            .fillMaxSize()
            .clip(androidx.compose.foundation.shape.RoundedCornerShape(topStart = cornerRadius, topEnd = cornerRadius)),
        color = androidx.compose.ui.graphics.Color.Transparent
    ) {
        content()
    }
}
@Composable
internal fun LocalHomeRoute(
    albums: List<chromahub.rhythm.app.core.model.Album>,
    allSongs: List<chromahub.rhythm.app.core.model.Song>,
    appSettings: chromahub.rhythm.app.shared.data.model.AppSettings,
    artists: List<chromahub.rhythm.app.core.model.Artist>,
    currentSong: chromahub.rhythm.app.core.model.Song?,
    isPlaying: Boolean,
    navController: NavHostController,
    onPlayArtist: (chromahub.rhythm.app.core.model.Artist) -> Unit,
    onPlayPause: () -> Unit,
    onPlaySong: (chromahub.rhythm.app.core.model.Song) -> Unit,
    onPlayerClick: () -> Unit,
    onSkipNext: () -> Unit,
    playlists: List<chromahub.rhythm.app.core.model.Playlist>,
    recentlyPlayed: List<chromahub.rhythm.app.core.model.Song>,
    songs: List<chromahub.rhythm.app.core.model.Song>,
    streamingMusicViewModel: StreamingMusicViewModel,
    viewModel: MusicViewModel
) {
    val context = LocalContext.current
    val appMode by appSettings.appMode.collectAsState()
    val isStreamingMode = appMode == "STREAMING"
    val streamingSessions by streamingMusicViewModel.serviceSessions.collectAsState()
    val streamingLikedSongs by streamingMusicViewModel.likedSongs.collectAsState()
    val streamingSavedPlaylists by streamingMusicViewModel.savedPlaylists.collectAsState()
    val streamingSavedAlbums by streamingMusicViewModel.savedAlbums.collectAsState()
    val streamingNewReleases by streamingMusicViewModel.newReleases.collectAsState()
    val streamingRecommendations by streamingMusicViewModel.recommendations.collectAsState()
    val streamingDownloadedSongs by streamingMusicViewModel.downloadedSongs.collectAsState()
    val streamingDownloadedAlbums by streamingMusicViewModel.downloadedAlbums.collectAsState()
    val streamingDownloadedArtists by streamingMusicViewModel.downloadedArtists.collectAsState()
    val streamingAllSongs by streamingMusicViewModel.allSongs.collectAsState()
    val streamingFollowedArtists by streamingMusicViewModel.followedArtists.collectAsState()
    val streamingIsLoading by streamingMusicViewModel.isLoading.collectAsState()
    val streamingError by streamingMusicViewModel.error.collectAsState()
    val selectedStreamingService by appSettings.streamingService.collectAsState()
    val streamingServiceId = remember(selectedStreamingService) {
        StreamingServiceOptions.defaults.firstOrNull { it.id == selectedStreamingService }?.id
            ?: selectedStreamingService
    }
    val streamingServiceName = remember(streamingServiceId) {
        StreamingServiceOptions.defaults.firstOrNull { it.id == streamingServiceId }
            ?.let { context.getString(it.nameRes) }
            ?: streamingServiceId
    }
    val streamingIsAuthenticated by streamingMusicViewModel.isAuthenticated.collectAsState()
    val streamingIsOnline by streamingMusicViewModel.isOnline.collectAsState()
    val streamingSyncProgress by streamingMusicViewModel.syncProgress.collectAsState()
    val streamingHasLoadedHomeContent by streamingMusicViewModel.hasLoadedHomeContent.collectAsState()
    val streamingHasLoadedLibrary by streamingMusicViewModel.hasLoadedLibrary.collectAsState()
    val offlineMode by appSettings.offlineMode.collectAsState()
    val isEffectivelyOffline = !streamingIsOnline || !streamingIsAuthenticated || offlineMode
    val streamingServiceConnected = remember(streamingSessions, streamingServiceId, isEffectivelyOffline) {
        streamingSessions[streamingServiceId]?.isConnected == true && !isEffectivelyOffline
    }
    val streamingMappedSongs by rememberOffMain(emptyList<chromahub.rhythm.app.core.model.Song>(), streamingServiceConnected, streamingAllSongs, streamingRecommendations, streamingLikedSongs, streamingDownloadedSongs) {
        val base = if (!streamingServiceConnected) {
            streamingDownloadedSongs
        } else if (streamingAllSongs.isNotEmpty()) {
            (streamingAllSongs + streamingDownloadedSongs).distinctBy { it.id }
        } else {
            (streamingRecommendations + streamingLikedSongs + streamingDownloadedSongs).distinctBy { it.id }
        }
        base.map { it.toLibrarySong() }
    }
    val streamingMappedAlbums = remember(streamingServiceConnected, streamingSavedAlbums, streamingNewReleases, streamingDownloadedAlbums) {
        val base = if (!streamingServiceConnected) {
            streamingDownloadedAlbums
        } else {
            (streamingSavedAlbums + streamingNewReleases + streamingDownloadedAlbums).distinctBy { it.id }
        }
        base.map { it.toLibraryAlbum(emptyList()) }
    }
    val streamingMappedArtists by rememberOffMain(emptyList<chromahub.rhythm.app.core.model.Artist>(), streamingServiceConnected, streamingFollowedArtists, streamingAllSongs, streamingDownloadedArtists, streamingDownloadedSongs) {
        val baseArtists = if (!streamingServiceConnected) {
            streamingDownloadedArtists
        } else if (streamingFollowedArtists.isNotEmpty() || streamingDownloadedArtists.isNotEmpty()) {
            (streamingFollowedArtists + streamingDownloadedArtists).distinctBy { it.id }
        } else {
            val allSongsPool = (streamingAllSongs + streamingDownloadedSongs).distinctBy { it.id }
            val artistNames = allSongsPool.mapNotNull { it.artist.takeIf { a -> a.isNotBlank() } }.distinct()
            artistNames.map { name ->
                val fallbackArt = allSongsPool.firstOrNull { it.artist.equals(name, ignoreCase = true) }?.artworkUri
                StreamingArtist(
                    id = "derived::$name",
                    name = name,
                    artworkUri = fallbackArt,
                    songCount = allSongsPool.count { it.artist.equals(name, ignoreCase = true) },
                    albumCount = allSongsPool.filter { it.artist.equals(name, ignoreCase = true) }.map { it.album }.distinct().size,
                    sourceType = streamingMusicViewModel.currentService.value
                )
            }
        }
        baseArtists.map {
            it.toLibraryArtist(
                librarySongs = emptyList(),
                libraryAlbums = emptyList(),
                separatorEnabled = false,
                separatorDelimiters = ""
            )
        }
    }
    val streamingMappedPlaylists = remember(streamingServiceConnected, streamingSavedPlaylists) {
        if (!streamingServiceConnected) emptyList() else streamingSavedPlaylists.map { it.toLibraryPlaylist(context, placeholderTrackTitle = { i -> context.getString(chromahub.rhythm.app.R.string.streaming_placeholder_track_format, i) }) }
    }
    val navigateToTopLevel: (String) -> Unit = { route ->
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    HomeScreen(
            musicViewModel = viewModel,
            songs = songs,
            albums = albums,
            artists = artists,
            recentlyPlayed = recentlyPlayed,
            currentSong = currentSong,
            isPlaying = isPlaying,
            onSongClick = onPlaySong,
            onAlbumClick = { album ->
                navController.navigate(Screen.AlbumDetail.createRoute(album.id, album.title))
            },
            onArtistClick = onPlayArtist,
            onPlayPause = onPlayPause,
            onPlayerClick = {
                navController.navigate(Screen.Player.route)
            },
            onViewAllSongs = {
                // Navigate to songs screen
                navController.navigate(Screen.Library.createRoute(LibraryTab.SONGS)) {
                    popUpTo(navController.graph.findStartDestination().id) {
                        saveState = true
                    }
                    launchSingleTop = true
                    restoreState = true
                }
            },
            onViewAllAlbums = {
                navController.navigate(Screen.Library.createRoute(LibraryTab.ALBUMS)) {
                    popUpTo(navController.graph.findStartDestination().id) {
                        saveState = true
                    }
                    launchSingleTop = true
                    restoreState = true
                }
            },
            onViewAllArtists = {
                navController.navigate(Screen.Library.createRoute(LibraryTab.ARTISTS)) {
                    popUpTo(navController.graph.findStartDestination().id) {
                        saveState = true
                    }
                    launchSingleTop = true
                    restoreState = true
                }
            },
            onSkipNext = onSkipNext,
            onSearchClick = {
                navigateToTopLevel(Screen.Search.route)
            },
            onSettingsClick = {
                // Navigate to the settings screen
                navigateToTopLevel(Screen.Settings.route)
            },
            onNavigateToLibrary = {
                // Navigate to library with playlists tab selected
                navController.navigate(Screen.Library.createRoute(LibraryTab.PLAYLISTS)) {
                    popUpTo(navController.graph.findStartDestination().id) {
                        saveState = true
                    }
                    launchSingleTop = true
                    restoreState = true
                }
            },
            onNavigateToPlaylist = { playlistId ->
                // Navigate to the specified playlist
                // For "favorites", we'll use the ID "1" which is the favorites playlist
                val id = if (playlistId == "favorites") "1" else playlistId
                navController.navigate(Screen.PlaylistDetail.createRoute(id))
            },
            onAddToQueue = { song ->
                viewModel.addSongToQueue(song)
            },
            onAddSongToPlaylist = { song, playlistId ->
                viewModel.addSongToPlaylist(song, playlistId) { message ->
                    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                }
            },
            onNavigateToStats = {
                navigateToTopLevel(Screen.RhythmStats.route)
            },
            onNavigateToRhythmGuard = {
                navController.navigate(Screen.TunerRhythmGuard.route)
            },
            onNavigateToArtist = { artist ->
                navController.navigate(Screen.ArtistDetail.createRoute(artist.name))
            },
            isStreamingMode = isStreamingMode,
            streamingLibrary = streamingMusicViewModel,
            streamingHome = chromahub.rhythm.app.features.local.presentation.screens.StreamingHomeState(
                isOnline = streamingIsOnline,
                syncProgress = streamingSyncProgress,
                serviceSessions = streamingSessions,
                isAuthenticated = streamingIsAuthenticated,
                hasLoadedHomeContent = streamingHasLoadedHomeContent,
                hasLoadedLibrary = streamingHasLoadedLibrary,
                allSongs = streamingAllSongs,
                recommendations = streamingRecommendations,
                newReleases = streamingNewReleases,
                savedAlbums = streamingSavedAlbums,
                followedArtists = streamingFollowedArtists,
                savedPlaylists = streamingSavedPlaylists,
                downloadedSongs = streamingDownloadedSongs,
                downloadedAlbums = streamingDownloadedAlbums,
                downloadedArtists = streamingDownloadedArtists,
            ),
            streamingSongs = streamingMappedSongs,
            streamingAlbums = streamingMappedAlbums,
            streamingArtists = streamingMappedArtists,
            streamingPlaylists = streamingMappedPlaylists,
            streamingRecentlyPlayed = recentlyPlayed,
            streamingServiceName = streamingServiceName,
            streamingServiceConnected = streamingServiceConnected,
            streamingIsLoading = streamingIsLoading,
            streamingError = streamingError,
            onConfigureService = { serviceId ->
                val target = StreamingServiceOptions.defaults
                    .firstOrNull { it.id.equals(serviceId, ignoreCase = true) }
                    ?.id
                    ?: streamingServiceId
                navController.navigate(StreamingRoutes.serviceSetup(target)) {
                    launchSingleTop = true
                }
            },
            onSwitchToLocalMode = {
                appSettings.setAppMode("LOCAL")
                viewModel.restartApp()
            },
            onStreamingNavigateToArtist = { artist ->
                navController.navigate(StreamingRoutes.artist(artist.id, artist.name)) {
                    launchSingleTop = true
                }
            },
            onStreamingNavigateToAlbum = { album ->
                navController.navigate(StreamingRoutes.album(album.id, album.title)) {
                    launchSingleTop = true
                }
            },
            onStreamingNavigateToPlaylist = { playlist ->
                navController.navigate(StreamingRoutes.playlist(playlist.id)) {
                    launchSingleTop = true
                }
            },
            onStreamingPlayQueue = { queue, startIndex, shuffle ->
                streamingMusicViewModel.playQueue(queue, startIndex, shuffle)
            },
            onStreamingShuffleQueue = { queue ->
                streamingMusicViewModel.playQueue(queue, 0, true)
            }
        )
}

@Composable
internal fun LocalLibraryRoute(
    backStackEntry: androidx.navigation.NavBackStackEntry,
    albums: List<chromahub.rhythm.app.core.model.Album>,
    appSettings: chromahub.rhythm.app.shared.data.model.AppSettings,
    artists: List<chromahub.rhythm.app.core.model.Artist>,
    currentSong: chromahub.rhythm.app.core.model.Song?,
    isPlaying: Boolean,
    isShuffleEnabled: Boolean,
    navController: NavHostController,
    onPlayAlbum: (chromahub.rhythm.app.core.model.Album) -> Unit,
    onPlayAlbumShuffled: (chromahub.rhythm.app.core.model.Album) -> Unit,
    onPlayPause: () -> Unit,
    onPlaySong: (chromahub.rhythm.app.core.model.Song) -> Unit,
    onPlayerClick: () -> Unit,
    onSkipNext: () -> Unit,
    playlists: List<chromahub.rhythm.app.core.model.Playlist>,
    songs: List<chromahub.rhythm.app.core.model.Song>,
    sortOrder: MusicViewModel.SortOrder,
    streamingMusicViewModel: StreamingMusicViewModel,
    viewModel: MusicViewModel,
    selectedStreamingSongForPlaylist: androidx.compose.runtime.MutableState<StreamingSong?>,
    showStreamingAddToPlaylist: androidx.compose.runtime.MutableState<Boolean>
) {
    val context = LocalContext.current
    val appMode by appSettings.appMode.collectAsState()
    val isStreamingMode = appMode == "STREAMING"
    val streamingSessions by streamingMusicViewModel.serviceSessions.collectAsState()
    val streamingLikedSongs by streamingMusicViewModel.likedSongs.collectAsState()
    val streamingSavedPlaylists by streamingMusicViewModel.savedPlaylists.collectAsState()
    val streamingSavedAlbums by streamingMusicViewModel.savedAlbums.collectAsState()
    val streamingNewReleases by streamingMusicViewModel.newReleases.collectAsState()
    val streamingRecommendations by streamingMusicViewModel.recommendations.collectAsState()
    val streamingDownloadedSongs by streamingMusicViewModel.downloadedSongs.collectAsState()
    val streamingDownloadedAlbums by streamingMusicViewModel.downloadedAlbums.collectAsState()
    val streamingDownloadedArtists by streamingMusicViewModel.downloadedArtists.collectAsState()
    val streamingAllSongs by streamingMusicViewModel.allSongs.collectAsState()
    val streamingDownloadingSongIds by streamingMusicViewModel.downloadingSongIds.collectAsState()
    val streamingFollowedArtists by streamingMusicViewModel.followedArtists.collectAsState()
    val streamingLikedSongIds = remember(streamingLikedSongs) {
        streamingLikedSongs.map { it.id }.toSet()
    }
    val streamingDownloadedSongIds = remember(streamingDownloadedSongs) {
        streamingDownloadedSongs.map { it.id }.toSet()
    }
    val onStreamingAddSongToPlaylist: (StreamingSong) -> Unit = { song ->
        selectedStreamingSongForPlaylist.value = song
        showStreamingAddToPlaylist.value = true
    }
    val streamingIsLoading by streamingMusicViewModel.isLoading.collectAsState()
    val streamingError by streamingMusicViewModel.error.collectAsState()
    val selectedStreamingService by appSettings.streamingService.collectAsState()
    val streamingServiceId = remember(selectedStreamingService) {
        StreamingServiceOptions.defaults.firstOrNull { it.id == selectedStreamingService }?.id
            ?: selectedStreamingService
    }
    val streamingServiceName = remember(streamingServiceId) {
        StreamingServiceOptions.defaults.firstOrNull { it.id == streamingServiceId }
            ?.let { context.getString(it.nameRes) }
            ?: streamingServiceId
    }
    val streamingIsAuthenticated by streamingMusicViewModel.isAuthenticated.collectAsState()
    val streamingIsOnline by streamingMusicViewModel.isOnline.collectAsState()
    val offlineMode by appSettings.offlineMode.collectAsState()
    val isEffectivelyOffline = !streamingIsOnline || !streamingIsAuthenticated || offlineMode
    val streamingServiceConnected = remember(streamingSessions, streamingServiceId, isEffectivelyOffline) {
        streamingSessions[streamingServiceId]?.isConnected == true && !isEffectivelyOffline
    }
    val streamingSongById by rememberOffMain(emptyMap<String, StreamingSong>(), streamingAllSongs, streamingRecommendations, streamingLikedSongs, streamingDownloadedSongs) {
        (streamingAllSongs + streamingRecommendations + streamingLikedSongs + streamingDownloadedSongs)
            .distinctBy { it.id }
            .associateBy { it.id }
    }
    val playStreamingMappedQueue: (List<chromahub.rhythm.app.core.model.Song>, Int, Boolean) -> Unit =
        { mappedSongs, startIndex, shuffle ->
            val originals = mappedSongs.mapNotNull { streamingSongById[it.id] }
            if (originals.isNotEmpty()) {
                streamingMusicViewModel.playQueue(originals, startIndex, shuffle, pinStartIndex = shuffle)
            }
        }
    val streamingMappedSongs by rememberOffMain(emptyList<chromahub.rhythm.app.core.model.Song>(), streamingServiceConnected, streamingAllSongs, streamingRecommendations, streamingLikedSongs, streamingDownloadedSongs) {
        val base = if (!streamingServiceConnected) {
            streamingDownloadedSongs
        } else if (streamingAllSongs.isNotEmpty()) {
            (streamingAllSongs + streamingDownloadedSongs).distinctBy { it.id }
        } else {
            (streamingRecommendations + streamingLikedSongs + streamingDownloadedSongs).distinctBy { it.id }
        }
        base.map { it.toLibrarySong() }
    }
    val streamingMappedAlbums = remember(streamingServiceConnected, streamingSavedAlbums, streamingNewReleases, streamingDownloadedAlbums) {
        val base = if (!streamingServiceConnected) {
            streamingDownloadedAlbums
        } else {
            (streamingSavedAlbums + streamingNewReleases + streamingDownloadedAlbums).distinctBy { it.id }
        }
        base.map { it.toLibraryAlbum(emptyList()) }
    }
    val streamingMappedArtists by rememberOffMain(emptyList<chromahub.rhythm.app.core.model.Artist>(), streamingServiceConnected, streamingFollowedArtists, streamingAllSongs, streamingDownloadedArtists, streamingDownloadedSongs) {
        val baseArtists = if (!streamingServiceConnected) {
            streamingDownloadedArtists
        } else if (streamingFollowedArtists.isNotEmpty() || streamingDownloadedArtists.isNotEmpty()) {
            (streamingFollowedArtists + streamingDownloadedArtists).distinctBy { it.id }
        } else {
            val allSongsPool = (streamingAllSongs + streamingDownloadedSongs).distinctBy { it.id }
            val artistNames = allSongsPool.mapNotNull { it.artist.takeIf { a -> a.isNotBlank() } }.distinct()
            artistNames.map { name ->
                val fallbackArt = allSongsPool.firstOrNull { it.artist.equals(name, ignoreCase = true) }?.artworkUri
                StreamingArtist(
                    id = "derived::$name",
                    name = name,
                    artworkUri = fallbackArt,
                    songCount = allSongsPool.count { it.artist.equals(name, ignoreCase = true) },
                    albumCount = allSongsPool.filter { it.artist.equals(name, ignoreCase = true) }.map { it.album }.distinct().size,
                    sourceType = streamingMusicViewModel.currentService.value
                )
            }
        }
        baseArtists.map {
            it.toLibraryArtist(
                librarySongs = emptyList(),
                libraryAlbums = emptyList(),
                separatorEnabled = false,
                separatorDelimiters = ""
            )
        }
    }
    val streamingMappedPlaylists = remember(streamingServiceConnected, streamingSavedPlaylists) {
        if (!streamingServiceConnected) emptyList() else streamingSavedPlaylists.map { it.toLibraryPlaylist(context, placeholderTrackTitle = { i -> context.getString(chromahub.rhythm.app.R.string.streaming_placeholder_track_format, i) }) }
    }
    val respectAlbumOnPlay by appSettings.respectAlbumOnPlay.collectAsState()

    val tabArg = backStackEntry.arguments?.getString("tab") ?: "songs"
    val initialTab = when (tabArg) {
        "playlists" -> LibraryTab.PLAYLISTS
        "albums" -> LibraryTab.ALBUMS
        "artists" -> LibraryTab.ARTISTS
        "explorer" -> LibraryTab.EXPLORER
        else -> LibraryTab.SONGS
    }

    LibraryScreen(
        songs = if (isStreamingMode) streamingMappedSongs else songs,
        albums = if (isStreamingMode) streamingMappedAlbums else albums,
        playlists = if (isStreamingMode) streamingMappedPlaylists else playlists,
        artists = if (isStreamingMode) streamingMappedArtists else artists,
        currentSong = currentSong,
        isPlaying = isPlaying,
        onSongClick = { song ->
            if (isStreamingMode) {
                val keepShuffle = appSettings.keepShuffleOnSelection.value && isShuffleEnabled
                if (respectAlbumOnPlay) {
                    val index = streamingMappedSongs.indexOfFirst { it.id == song.id }.coerceAtLeast(0)
                    playStreamingMappedQueue(streamingMappedSongs, index, keepShuffle)
                } else {
                    playStreamingMappedQueue(listOf(song), 0, keepShuffle)
                }
            } else {
                onPlaySong(song)
            }
        },
        onPlayPause = onPlayPause,
        onPlayerClick = {
            navController.navigate(Screen.Player.route)
        },
        onPlaylistClick = { playlist ->
            if (isStreamingMode) {
                navController.navigate(StreamingRoutes.playlist(playlist.id)) {
                    launchSingleTop = true
                }
            } else {
                navController.navigate(Screen.PlaylistDetail.createRoute(playlist.id))
            }
        },
        onAddPlaylist = {
            // This is now handled internally with the dialog
        },
        onAlbumClick = { album ->
            if (isStreamingMode) {
                navController.navigate(StreamingRoutes.album(album.id, album.title)) {
                    launchSingleTop = true
                }
            } else {
                onPlayAlbum(album)
            }
        },
        onArtistClick = { artist ->
            if (isStreamingMode) {
                navController.navigate(StreamingRoutes.artist(artist.id, artist.name)) {
                    launchSingleTop = true
                }
            } else {
                navController.navigate(Screen.ArtistDetail.createRoute(artist.name))
            }
        },
        onAlbumShufflePlay = { album ->
            if (isStreamingMode) {
                playStreamingMappedQueue(album.songs, 0, true)
            } else {
                onPlayAlbumShuffled(album)
            }
        },
        onPlayQueue = { queue ->
            if (isStreamingMode) {
                playStreamingMappedQueue(queue, 0, false)
            } else {
                viewModel.playSongs(queue)
            }
        },
        onPlayQueueFromIndex = { queue, startIndex ->
            val keepShuffle = appSettings.keepShuffleOnSelection.value && isShuffleEnabled
            if (isStreamingMode) {
                playStreamingMappedQueue(queue, startIndex, keepShuffle)
            } else {
                viewModel.playQueue(
                    songs = queue,
                    enableShuffle = keepShuffle,
                    startIndex = startIndex,
                    pinStartIndex = keepShuffle
                )
            }
        },
        onShuffleQueue = { queue ->
            if (isStreamingMode) {
                playStreamingMappedQueue(queue, 0, true)
            } else {
                viewModel.playShuffled(queue)
            }
        },
        onAlbumBottomSheetClick = { album ->
            if (isStreamingMode) {
                navController.navigate(StreamingRoutes.album(album.id, album.title)) {
                    launchSingleTop = true
                }
            } else {
                navController.navigate(Screen.AlbumDetail.createRoute(album.id, album.title))
            }
        },
        onSort = {
            // Implement sort functionality
            viewModel.sortLibrary()
        },
        onAddSongToPlaylist = { song, playlistId ->
            if (isStreamingMode) {
                streamingSongById[song.id]?.let { onStreamingAddSongToPlaylist(it) }
            } else {
                viewModel.addSongToPlaylist(song, playlistId) { message ->
                    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                }
            }
        },
        onCreatePlaylist = { name ->
            if (isStreamingMode) {
                streamingMusicViewModel.createPlaylist(name)
            } else {
                viewModel.createPlaylist(name)
            }
        },
        onRefreshClick = {
            if (isStreamingMode) {
                streamingMusicViewModel.loadLibrary(false)
            } else {
                viewModel.refreshLibrary(showMediaScanLoader = false)
            }
        }, // Added onRefreshClick
        sortOrder = sortOrder,
        onSkipNext = onSkipNext,
        onAddToQueue = { song ->
            // Add song to queue
            viewModel.addSongToQueue(song)
        },
        initialTab = initialTab,
        musicViewModel = viewModel, // Pass musicViewModel
        onExportAllPlaylists = if (isStreamingMode) null else { format, includeDefault, userDirectoryUri, resultCallback ->
            // Export all playlists with optional user-selected directory
            viewModel.exportAllPlaylists(
                format,
                includeDefault,
                userDirectoryUri
            ) { result ->
                result.fold(
                    onSuccess = { message ->
                        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                    },
                    onFailure = { error ->
                        Toast.makeText(context, context.getString(R.string.backup_export_failed, error.message), Toast.LENGTH_SHORT).show()
                    }
                )
                resultCallback(result)
            }
        },
        onImportPlaylist = if (isStreamingMode) null else { uri, resultCallback, onRestartRequired ->
            // Import playlist from URI with restart functionality
            viewModel.importPlaylist(uri, { result ->
                result.fold(
                    onSuccess = { message ->
                        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                    },
                    onFailure = { error ->
                        Toast.makeText(context, context.getString(R.string.backup_import_failed, error.message), Toast.LENGTH_SHORT).show()
                    }
                )
                resultCallback(result)
            }, onRestartRequired = {
                // Trigger restart dialog or function
                onRestartRequired?.invoke()
            })
        },
        onRestartApp = {
            viewModel.restartApp()
        },
            onNavigateToArtist = { artist ->
                if (isStreamingMode) {
                    navController.navigate(StreamingRoutes.artist(artist.id, artist.name)) {
                        launchSingleTop = true
                    }
                } else {
                    navController.navigate(Screen.ArtistDetail.createRoute(artist.name))
                }
            },
        isStreamingMode = isStreamingMode,
        streamingServiceName = streamingServiceName,
        streamingServiceConnected = streamingServiceConnected,
        streamingIsLoading = streamingIsLoading,
        streamingError = streamingError,
        onConfigureService = { serviceId ->
            val target = StreamingServiceOptions.defaults
                .firstOrNull { it.id.equals(serviceId, ignoreCase = true) }
                ?.id
                ?: streamingServiceId
            navController.navigate(StreamingRoutes.serviceSetup(target)) {
                launchSingleTop = true
            }
        },
        onStreamingPlayNext = { song ->
            streamingSongById[song.id]?.let { original ->
                streamingMusicViewModel.playNext(original, viewModel)
            }
        },
        onStreamingAddToQueue = { song ->
            streamingSongById[song.id]?.let { original ->
                streamingMusicViewModel.addSongToQueue(original, viewModel)
            }
        },
        onStreamingToggleFavorite = if (isStreamingMode) ({ song ->
            streamingSongById[song.id]?.let { original ->
                val isLiked = streamingLikedSongIds.contains(original.id)
                if (isLiked) streamingMusicViewModel.unlikeSong(original)
                else streamingMusicViewModel.likeSong(original)
            }
        }) else null,
        onStreamingSetFavorite = if (isStreamingMode) ({ song, shouldLike ->
            streamingSongById[song.id]?.let { original ->
                if (shouldLike) streamingMusicViewModel.likeSong(original)
                else streamingMusicViewModel.unlikeSong(original)
            }
        }) else null,
        streamingFavoriteSongIds = streamingLikedSongIds,
        streamingDownloadedSongIds = streamingDownloadedSongIds,
        streamingDownloadingSongIds = streamingDownloadingSongIds,
        onStreamingToggleDownload = if (isStreamingMode) ({ song ->
            if (streamingDownloadedSongIds.contains(song.id)) {
                streamingMusicViewModel.removeDownload(song.id)
            } else {
                val orig = streamingSongById[song.id]
                if (orig != null) {
                    streamingMusicViewModel.downloadSong(orig)
                } else {
                    streamingMusicViewModel.downloadSongById(song.id)
                }
            }
        }) else null
    )
}

@Composable
internal fun LocalPlaylistDetailRoute(
    backStackEntry: androidx.navigation.NavBackStackEntry,
    appSettings: chromahub.rhythm.app.shared.data.model.AppSettings,
    artists: List<chromahub.rhythm.app.core.model.Artist>,
    currentSong: chromahub.rhythm.app.core.model.Song?,
    isPlaying: Boolean,
    navController: NavHostController,
    onPlayPause: () -> Unit,
    onPlayPlaylist: (chromahub.rhythm.app.core.model.Playlist) -> Unit,
    onPlayPlaylistShuffled: (chromahub.rhythm.app.core.model.Playlist) -> Unit,
    onPlaySong: (chromahub.rhythm.app.core.model.Song) -> Unit,
    onPlayerClick: () -> Unit,
    onSkipNext: () -> Unit,
    playlists: List<chromahub.rhythm.app.core.model.Playlist>,
    songs: List<chromahub.rhythm.app.core.model.Song>,
    startDestination: String,
    viewModel: MusicViewModel
) {
    val context = LocalContext.current
    val navigateBackOrToLanding: () -> Unit = {
        val previousRoute = navController.previousBackStackEntry?.destination?.route
        if (previousRoute == Screen.Player.route) {
            navController.popBackStack(Screen.Player.route, inclusive = true)
        } else {
            val popped = navController.popBackStack()
            if (!popped) {
                navController.navigate(startDestination) {
                    popUpTo(navController.graph.id) {
                        inclusive = true
                    }
                    launchSingleTop = true
                    restoreState = true
                }
            }
        }
    }

    val playlistId = backStackEntry.arguments?.getString("playlistId") ?: ""
    val playlist = playlists.find { it.id == playlistId }
    val favoriteSongs by viewModel.favoriteSongs.collectAsState()

    // Album/Artist data for bottom sheets
    val allAlbums by viewModel.albums.collectAsState()
    val playlistHaptics = LocalHapticFeedback.current

    // Write permission launcher for Android 11+ metadata editing
    val writePermissionLauncher = rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.StartIntentSenderForResult()
    ) { result: androidx.activity.result.ActivityResult ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            if (viewModel.pendingBatchWriteRequest.value != null) {
                viewModel.completeBatchMetadataWriteAfterPermission(
                    onSuccess = {
                        android.widget.Toast.makeText(context, R.string.localnavigation_metadata_saved_successfully, android.widget.Toast.LENGTH_SHORT).show()
                    },
                    onError = { errorMessage ->
                        android.widget.Toast.makeText(context, errorMessage, android.widget.Toast.LENGTH_LONG).show()
                    }
                )
            } else {
                viewModel.completeMetadataWriteAfterPermission(
                    onSuccess = {
                        android.widget.Toast.makeText(context, R.string.localnavigation_metadata_saved_successfully, android.widget.Toast.LENGTH_SHORT).show()
                    },
                    onError = { errorMessage ->
                        android.widget.Toast.makeText(context, errorMessage, android.widget.Toast.LENGTH_LONG).show()
                    }
                )
            }
        } else {
            if (viewModel.pendingBatchWriteRequest.value != null) {
                viewModel.cancelPendingBatchMetadataWrite()
            } else {
                viewModel.cancelPendingMetadataWrite()
            }
            android.widget.Toast.makeText(context, R.string.localnavigation_permission_denied_changes_saved, android.widget.Toast.LENGTH_LONG).show()
        }
    }

    if (playlist != null) {
        PlaylistDetailScreen(
            musicViewModel = viewModel,
            playlist = playlist,
            currentSong = currentSong,
            isPlaying = isPlaying,
            onPlayPause = onPlayPause,
            onPlayerClick = {
                navController.navigate(Screen.Player.route)
            },
            onPlayAll = {
                onPlayPlaylist(playlist)
            },
            onShufflePlay = {
                // Play shuffled playlist songs using the proper shuffled playlist playback
                onPlayPlaylistShuffled(playlist)
            },
            onSongClick = onPlaySong,
            onPlaySongFromPlaylist = { song, playlistSongs ->
                viewModel.playSongFromContext(song, playlistSongs, playlist.name)
            },
            onBack = {
                navigateBackOrToLanding()
            },
            onRemoveSong = { song, message ->
                viewModel.removeSongFromPlaylist(
                    song,
                    playlistId
                ) { snackbarMessage ->
                    Toast.makeText(context, snackbarMessage, Toast.LENGTH_SHORT).show()
                }
            },
            onRenamePlaylist = { newName ->
                viewModel.renamePlaylist(playlistId, newName)
            },
            onDeletePlaylist = {
                viewModel.deletePlaylist(playlistId)
                navigateBackOrToLanding()
            },
            onAddSongsToPlaylist = {},
            onSkipNext = onSkipNext,
            onReorderSongs = { fromIndex, toIndex ->
                viewModel.reorderPlaylistSongs(playlistId, fromIndex, toIndex)
            },
            onUpdatePlaylistSongs = { newSongList ->
                viewModel.updatePlaylistSongs(playlistId, newSongList)
            },
            onPlayNext = { song ->
                viewModel.playNext(song)
            },
            onAddToQueue = { song ->
                viewModel.addSongToQueue(song)
            },
            onGoToAlbum = { song ->
                val album = allAlbums.findAlbumForSong(song)
                val albumId = album?.id ?: song.albumId.ifBlank { "unknown_${song.album}" }
                val albumTitle = album?.title ?: song.album
                if (albumTitle.isNotBlank() || albumId.isNotBlank()) {
                    navController.navigate(Screen.AlbumDetail.createRoute(albumId, albumTitle))
                }
            },
            onGoToArtist = { song ->
                val separatorEnabled = appSettings.artistSeparatorEnabled.value
                val delimiters = appSettings.artistSeparatorDelimiters.value.ifBlank { chromahub.rhythm.app.shared.data.model.AppSettings.DEFAULT_ARTIST_SEPARATOR_DELIMITERS }
                val candidates = ArtistSeparator.splitArtistNames(
                    song.artist,
                    delimiters = delimiters,
                    enabled = separatorEnabled
                )
                val artistRouteName = candidates.firstOrNull { candidate ->
                    artists.any { it.name.equals(candidate, ignoreCase = true) }
                } ?: candidates.firstOrNull()?.trim().orEmpty().ifBlank {
                    song.artist.trim()
                }

                if (artistRouteName.isNotBlank()) {
                    navController.navigate(Screen.ArtistDetail.createRoute(artistRouteName))
                }
            },
            onShare = { song ->
                try {
                    val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                        type = "audio/*"
                        putExtra(android.content.Intent.EXTRA_STREAM, song.uri)
                        addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    context.startActivity(android.content.Intent.createChooser(shareIntent, "Share ${song.title}"))
                } catch (e: Exception) {
                    android.widget.Toast.makeText(context, R.string.materialplayerscreen_unable_to_share_file, android.widget.Toast.LENGTH_SHORT).show()
                }
            }
        )
    }
}

@Composable
internal fun LocalArtistDetailRoute(
    backStackEntry: androidx.navigation.NavBackStackEntry,
    appSettings: chromahub.rhythm.app.shared.data.model.AppSettings,
    currentSong: chromahub.rhythm.app.core.model.Song?,
    isPlaying: Boolean,
    navController: NavHostController,
    onPlaySong: (chromahub.rhythm.app.core.model.Song) -> Unit,
    onPlayerClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    playlists: List<chromahub.rhythm.app.core.model.Playlist>,
    songs: List<chromahub.rhythm.app.core.model.Song>,
    startDestination: String,
    viewModel: MusicViewModel
) {
    val context = LocalContext.current
    val navigateBackOrToLanding: () -> Unit = {
        val previousRoute = navController.previousBackStackEntry?.destination?.route
        if (previousRoute == Screen.Player.route) {
            navController.popBackStack(Screen.Player.route, inclusive = true)
        } else {
            val popped = navController.popBackStack()
            if (!popped) {
                navController.navigate(startDestination) {
                    popUpTo(navController.graph.id) {
                        inclusive = true
                    }
                    launchSingleTop = true
                    restoreState = true
                }
            }
        }
    }
    val respectAlbumOnPlay by appSettings.respectAlbumOnPlay.collectAsState()

    val artistName = backStackEntry.arguments?.getString("artistName")?.let { Uri.decode(it) } ?: ""
    val favoriteSongs by viewModel.favoriteSongs.collectAsState()
    var pendingMetadataEditCompleteCallback by remember { mutableStateOf<((Boolean) -> Unit)?>(null) }

    // Write permission launcher for Android 11+ metadata editing
    val writePermissionLauncher = rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.StartIntentSenderForResult()
    ) { result: ActivityResult ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            if (viewModel.pendingBatchWriteRequest.value != null) {
                viewModel.completeBatchMetadataWriteAfterPermission(
                    onSuccess = {
                        android.widget.Toast.makeText(context, R.string.localnavigation_metadata_saved_successfully, android.widget.Toast.LENGTH_SHORT).show()
                    },
                    onError = { errorMessage ->
                        android.widget.Toast.makeText(context, errorMessage, android.widget.Toast.LENGTH_LONG).show()
                    }
                )
            } else {
                viewModel.completeMetadataWriteAfterPermission(
                    onSuccess = {
                        android.widget.Toast.makeText(context, R.string.localnavigation_metadata_saved_successfully, android.widget.Toast.LENGTH_SHORT).show()
                        pendingMetadataEditCompleteCallback?.invoke(true)
                        pendingMetadataEditCompleteCallback = null
                    },
                    onError = { errorMessage ->
                        android.widget.Toast.makeText(context, errorMessage, android.widget.Toast.LENGTH_LONG).show()
                        pendingMetadataEditCompleteCallback?.invoke(false)
                        pendingMetadataEditCompleteCallback = null
                    }
                )
            }
        } else {
            if (viewModel.pendingBatchWriteRequest.value != null) {
                viewModel.cancelPendingBatchMetadataWrite()
            } else {
                viewModel.cancelPendingMetadataWrite()
                pendingMetadataEditCompleteCallback?.invoke(false)
                pendingMetadataEditCompleteCallback = null
            }
            android.widget.Toast.makeText(context, R.string.localnavigation_permission_denied_changes_saved, android.widget.Toast.LENGTH_LONG).show()
        }
    }

    // State for bottom sheets
    var showAddToPlaylistSheet by remember { mutableStateOf(false) }
    var selectedSongForPlaylist by remember { mutableStateOf<chromahub.rhythm.app.core.model.Song?>(null) }
    var showCreatePlaylistDialog by remember { mutableStateOf(false) }
    var songForCreatePlaylistDialog by remember { mutableStateOf<chromahub.rhythm.app.core.model.Song?>(null) }
    var showSongInfoSheet by remember { mutableStateOf(false) }
    var selectedSongForInfo by remember { mutableStateOf<chromahub.rhythm.app.core.model.Song?>(null) }

    ArtistDetailScreen(
        viewModel = viewModel,
        artistName = artistName,
        onBack = {
            navigateBackOrToLanding()
        },
        onSongClick = onPlaySong,
        onSongClickInContext = { song, contextSongs ->
            if (respectAlbumOnPlay) {
                viewModel.playSongFromContext(song, contextSongs, artistName)
            } else {
                viewModel.playSong(song)
            }
        },
        onAlbumClick = { album ->
            navController.navigate(Screen.AlbumDetail.createRoute(album.id, album.title))
        },
        onPlayAll = { songs ->
            if (songs.isNotEmpty()) {
                viewModel.playSongs(songs)
            }
        },
        onShufflePlay = { songs ->
            if (songs.isNotEmpty()) {
                viewModel.playShuffled(songs)
            }
        },
        onAddToQueue = { song ->
            viewModel.addSongToQueue(song)
        },
        onAddToQueueAll = { songs ->
            viewModel.addSongsToQueue(songs)
        },
        onAddSongToPlaylist = { song ->
            selectedSongForPlaylist = song
            showAddToPlaylistSheet = true
        },
        onPlayerClick = {
            navController.navigate(Screen.Player.route)
        },
        onPlayNext = { song ->
            viewModel.playNext(song)
        },
        onToggleFavorite = { song ->
            viewModel.toggleFavorite(song)
        },
        favoriteSongs = favoriteSongs,
        onShowSongInfo = { song ->
            selectedSongForInfo = song
            showSongInfoSheet = true
        },
        currentSong = currentSong,
        isPlaying = isPlaying
    )

    // Add to playlist bottom sheet
    if (showAddToPlaylistSheet && selectedSongForPlaylist != null) {
        AddToPlaylistBottomSheet(
            song = selectedSongForPlaylist!!,
            playlists = playlists,
            onDismissRequest = { 
                showAddToPlaylistSheet = false
                selectedSongForPlaylist = null
            },
            onAddToPlaylist = { playlist ->
                viewModel.addSongToPlaylist(selectedSongForPlaylist!!, playlist.id) { message ->
                    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                }
                showAddToPlaylistSheet = false
                selectedSongForPlaylist = null
            },
            onCreateNewPlaylist = {
                songForCreatePlaylistDialog = selectedSongForPlaylist
                showCreatePlaylistDialog = true
                showAddToPlaylistSheet = false
            }
        )
    }

    if (showCreatePlaylistDialog) {
        CreatePlaylistDialog(
            onDismiss = {
                showCreatePlaylistDialog = false
                songForCreatePlaylistDialog = null
            },
            onConfirm = { name ->
                viewModel.createPlaylist(name)
                showCreatePlaylistDialog = false
                songForCreatePlaylistDialog = null
            },
            song = songForCreatePlaylistDialog,
            onConfirmWithSong = { name ->
                if (songForCreatePlaylistDialog != null) {
                    viewModel.createPlaylist(name, listOf(songForCreatePlaylistDialog!!)) { message ->
                        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                    }
                } else {
                    viewModel.createPlaylist(name)
                }
                showCreatePlaylistDialog = false
                songForCreatePlaylistDialog = null
            }
        )
    }

    // Song info bottom sheet
    if (showSongInfoSheet && selectedSongForInfo != null) {
        SongInfoBottomSheet(
            song = selectedSongForInfo!!,
            onDismiss = { 
                showSongInfoSheet = false
                selectedSongForInfo = null
            },
            appSettings = appSettings,
            onEditSong = { title, artist, album, genre, year, trackNumber, artworkUri, removeArtwork, albumArtist, composer, discNumber, onComplete ->
                pendingMetadataEditCompleteCallback = onComplete
                viewModel.saveMetadataChanges(
                    song = selectedSongForInfo!!,
                    title = title,
                    artist = artist,
                    album = album,
                    genre = genre,
                    year = year,
                    trackNumber = trackNumber,
                    artworkUri = artworkUri,
                    removeArtwork = removeArtwork,
                    albumArtist = albumArtist,
                    composer = composer,
                    discNumber = discNumber,
                    onSuccess = { fileWriteSucceeded ->
                        if (fileWriteSucceeded) {
                            android.widget.Toast.makeText(context, R.string.localnavigation_metadata_saved_successfully_to, android.widget.Toast.LENGTH_SHORT).show()
                        }
                        pendingMetadataEditCompleteCallback?.invoke(true)
                        pendingMetadataEditCompleteCallback = null
                    },
                    onError = { errorMessage ->
                        android.widget.Toast.makeText(context, errorMessage, android.widget.Toast.LENGTH_LONG).show()
                        pendingMetadataEditCompleteCallback?.invoke(false)
                        pendingMetadataEditCompleteCallback = null
                    },
                    onPermissionRequired = { pendingRequest ->
                        try {
                            val intentSenderRequest = androidx.activity.result.IntentSenderRequest.Builder(
                                pendingRequest.intentSender
                            ).build()
                            writePermissionLauncher.launch(intentSenderRequest)
                        } catch (e: Exception) {
                            android.widget.Toast.makeText(context, R.string.localnavigation_permission_required_to_modify, android.widget.Toast.LENGTH_SHORT).show()
                            pendingMetadataEditCompleteCallback?.invoke(false)
                            pendingMetadataEditCompleteCallback = null
                        }
                    }
                )
            }
        )
    }

}

@Composable
internal fun LocalAlbumDetailRoute(
    backStackEntry: androidx.navigation.NavBackStackEntry,
    appSettings: chromahub.rhythm.app.shared.data.model.AppSettings,
    artists: List<chromahub.rhythm.app.core.model.Artist>,
    currentSong: chromahub.rhythm.app.core.model.Song?,
    isPlaying: Boolean,
    navController: NavHostController,
    onPlaySong: (chromahub.rhythm.app.core.model.Song) -> Unit,
    onPlayerClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    playlists: List<chromahub.rhythm.app.core.model.Playlist>,
    songs: List<chromahub.rhythm.app.core.model.Song>,
    startDestination: String,
    viewModel: MusicViewModel
) {
    val context = LocalContext.current
    val navigateBackOrToLanding: () -> Unit = {
        val previousRoute = navController.previousBackStackEntry?.destination?.route
        if (previousRoute == Screen.Player.route) {
            navController.popBackStack(Screen.Player.route, inclusive = true)
        } else {
            val popped = navController.popBackStack()
            if (!popped) {
                navController.navigate(startDestination) {
                    popUpTo(navController.graph.id) {
                        inclusive = true
                    }
                    launchSingleTop = true
                    restoreState = true
                }
            }
        }
    }
    val respectAlbumOnPlay by appSettings.respectAlbumOnPlay.collectAsState()

    val albumId = backStackEntry.arguments?.getString("albumId")?.let { Uri.decode(it) } ?: ""
    val albumName = backStackEntry.arguments?.getString("albumName")?.let { Uri.decode(it) } ?: ""
    val favoriteSongs by viewModel.favoriteSongs.collectAsState()

    // Write permission launcher for Android 11+ metadata editing
    val writePermissionLauncher = rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.StartIntentSenderForResult()
    ) { result: androidx.activity.result.ActivityResult ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            if (viewModel.pendingBatchWriteRequest.value != null) {
                viewModel.completeBatchMetadataWriteAfterPermission(
                    onSuccess = {
                        android.widget.Toast.makeText(context, R.string.localnavigation_metadata_saved_successfully, android.widget.Toast.LENGTH_SHORT).show()
                    },
                    onError = { errorMessage ->
                        android.widget.Toast.makeText(context, errorMessage, android.widget.Toast.LENGTH_LONG).show()
                    }
                )
            }
        } else {
            viewModel.cancelPendingBatchMetadataWrite()
            android.widget.Toast.makeText(context, R.string.localnavigation_permission_denied_changes_saved, android.widget.Toast.LENGTH_LONG).show()
        }
    }

    var showAddToPlaylistSheet by remember { mutableStateOf(false) }
    var selectedSongForPlaylist by remember { mutableStateOf<chromahub.rhythm.app.core.model.Song?>(null) }
    var showCreatePlaylistDialog by remember { mutableStateOf(false) }
    var songForCreatePlaylistDialog by remember { mutableStateOf<chromahub.rhythm.app.core.model.Song?>(null) }
    var showSongInfoSheet by remember { mutableStateOf(false) }
    var selectedSongForInfo by remember { mutableStateOf<chromahub.rhythm.app.core.model.Song?>(null) }

    AlbumDetailScreen(
        albumId = albumId,
        albumName = albumName,
        onBack = {
            navigateBackOrToLanding()
        },
        onSongClick = onPlaySong,
        onSongClickInContext = { song, contextSongs ->
            if (respectAlbumOnPlay) {
                viewModel.playSongFromContext(song, contextSongs, albumName)
            } else {
                viewModel.playSong(song)
            }
        },
        onPlayAll = { songs ->
            if (songs.isNotEmpty()) {
                viewModel.playSongs(songs)
            }
        },
        onShufflePlay = { songs ->
            if (songs.isNotEmpty()) {
                viewModel.playShuffled(songs)
            }
        },
        onAddToQueue = { song ->
            viewModel.addSongToQueue(song)
        },
        onAddSongToPlaylist = { song ->
            selectedSongForPlaylist = song
            showAddToPlaylistSheet = true
        },
        onPlayerClick = {
            navController.navigate(Screen.Player.route)
        },
        onPlayNext = { song ->
            viewModel.playNext(song)
        },
        onToggleFavorite = { song ->
            viewModel.toggleFavorite(song)
        },
        favoriteSongs = favoriteSongs,
        onShowSongInfo = { song ->
            selectedSongForInfo = song
            showSongInfoSheet = true
        },
        onAddToBlacklist = { song ->
            appSettings.addToBlacklist(song.id)
        },
        currentSong = currentSong,
        isPlaying = isPlaying,
        onEditAlbum = { title, artist, artworkUri, removeArtwork, onProgress, onComplete ->
            val allAlbumsList = viewModel.albums.value
            val activeAlbum = allAlbumsList.find { it.id == albumId }
            if (activeAlbum != null) {
                viewModel.batchEditMetadata(
                    songs = activeAlbum.songs,
                    artist = artist,
                    album = title,
                    genre = null,
                    year = null,
                    artworkUri = artworkUri,
                    removeArtwork = removeArtwork,
                    onProgress = onProgress,
                    onComplete = { successCount, failCount ->
                        onComplete(successCount, failCount)
                    },
                    onPermissionRequired = { pendingRequest ->
                        try {
                            val intentSenderRequest = androidx.activity.result.IntentSenderRequest.Builder(
                                pendingRequest.intentSender
                            ).build()
                            writePermissionLauncher.launch(intentSenderRequest)
                        } catch (e: Exception) {
                            android.widget.Toast.makeText(
                                context,
                                "Failed to request permission: ${e.message}",
                                android.widget.Toast.LENGTH_LONG
                            ).show()
                            viewModel.cancelPendingBatchMetadataWrite()
                        }
                    }
                )
            }
        },
        onGoToArtist = { song ->
            val separatorEnabled = appSettings.artistSeparatorEnabled.value
            val delimiters = appSettings.artistSeparatorDelimiters.value.ifBlank { chromahub.rhythm.app.shared.data.model.AppSettings.DEFAULT_ARTIST_SEPARATOR_DELIMITERS }
            val candidates = chromahub.rhythm.app.core.common.ArtistSeparator.splitArtistNames(
                song.artist,
                delimiters = delimiters,
                enabled = separatorEnabled
            )
            val artistRouteName = candidates.firstOrNull { candidate ->
                artists.any { it.name.equals(candidate, ignoreCase = true) }
            } ?: candidates.firstOrNull()?.trim().orEmpty().ifBlank {
                song.artist.trim()
            }
            if (artistRouteName.isNotBlank()) {
                navController.navigate(Screen.ArtistDetail.createRoute(artistRouteName))
            }
        },
        onShare = { song ->
            try {
                val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                    type = "audio/*"
                    putExtra(android.content.Intent.EXTRA_STREAM, song.uri)
                    addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(android.content.Intent.createChooser(shareIntent, "Share ${song.title}"))
            } catch (e: Exception) {
                android.widget.Toast.makeText(context, R.string.materialplayerscreen_unable_to_share_file, android.widget.Toast.LENGTH_SHORT).show()
            }
        },
        viewModel = viewModel
    )

    // Bottom sheets & Dialogs helpers
    if (showAddToPlaylistSheet && selectedSongForPlaylist != null) {
        AddToPlaylistBottomSheet(
            song = selectedSongForPlaylist!!,
            playlists = playlists,
            onDismissRequest = { 
                showAddToPlaylistSheet = false
                selectedSongForPlaylist = null
            },
            onAddToPlaylist = { playlist ->
                viewModel.addSongToPlaylist(selectedSongForPlaylist!!, playlist.id) { message ->
                    android.widget.Toast.makeText(context, message, android.widget.Toast.LENGTH_SHORT).show()
                }
                showAddToPlaylistSheet = false
                selectedSongForPlaylist = null
            },
            onCreateNewPlaylist = {
                songForCreatePlaylistDialog = selectedSongForPlaylist
                showCreatePlaylistDialog = true
                showAddToPlaylistSheet = false
            }
        )
    }

    if (showCreatePlaylistDialog) {
        CreatePlaylistDialog(
            onDismiss = {
                showCreatePlaylistDialog = false
                songForCreatePlaylistDialog = null
            },
            onConfirm = { name ->
                viewModel.createPlaylist(name)
                showCreatePlaylistDialog = false
                songForCreatePlaylistDialog = null
            },
            song = songForCreatePlaylistDialog,
            onConfirmWithSong = { name ->
                if (songForCreatePlaylistDialog != null) {
                    viewModel.createPlaylist(name, listOf(songForCreatePlaylistDialog!!)) { message ->
                        android.widget.Toast.makeText(context, message, android.widget.Toast.LENGTH_SHORT).show()
                    }
                } else {
                    viewModel.createPlaylist(name)
                }
                showCreatePlaylistDialog = false
                songForCreatePlaylistDialog = null
            }
        )
    }

    if (showSongInfoSheet && selectedSongForInfo != null) {
        SongInfoBottomSheet(
            song = selectedSongForInfo!!,
            onDismiss = { 
                showSongInfoSheet = false
                selectedSongForInfo = null
            },
            appSettings = appSettings,
            onEditSong = { title, artist, album, genre, year, trackNumber, artworkUri, removeArtwork, albumArtist, composer, discNumber, onComplete ->
                viewModel.saveMetadataChanges(
                    song = selectedSongForInfo!!,
                    title = title,
                    artist = artist,
                    album = album,
                    genre = genre,
                    year = year,
                    trackNumber = trackNumber,
                    artworkUri = artworkUri,
                    removeArtwork = removeArtwork,
                    albumArtist = albumArtist,
                    composer = composer,
                    discNumber = discNumber,
                    onSuccess = { fileWriteSucceeded ->
                        if (fileWriteSucceeded) {
                            android.widget.Toast.makeText(context, R.string.localnavigation_metadata_saved_successfully_to, android.widget.Toast.LENGTH_SHORT).show()
                        }
                        onComplete(true)
                    },
                    onError = { errorMessage ->
                        android.widget.Toast.makeText(context, errorMessage, android.widget.Toast.LENGTH_LONG).show()
                        onComplete(false)
                    },
                    onPermissionRequired = { pendingRequest ->
                        try {
                            val intentSenderRequest = androidx.activity.result.IntentSenderRequest.Builder(
                                pendingRequest.intentSender
                            ).build()
                            writePermissionLauncher.launch(intentSenderRequest)
                        } catch (e: Exception) {
                            android.widget.Toast.makeText(context, R.string.localnavigation_permission_required_to_modify, android.widget.Toast.LENGTH_SHORT).show()
                            onComplete(false)
                        }
                    }
                )
            }
        )
    }
}

