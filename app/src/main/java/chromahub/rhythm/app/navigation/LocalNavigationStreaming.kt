package chromahub.rhythm.app.navigation

import com.qlzhj.rhythm.feature.local.presentation.navigation.Screen
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel
import chromahub.rhythm.app.R
import com.qlzhj.rhythm.core.data.shared.data.model.AppSettings
import com.qlzhj.rhythm.core.media.util.rememberOffMain
import androidx.navigation.NavHostController
import com.qlzhj.rhythm.core.ui.shared.presentation.components.bottomsheets.SongInfoBottomSheet
import com.qlzhj.rhythm.feature.local.presentation.screens.PlaylistDetailScreen
import com.qlzhj.rhythm.feature.local.presentation.screens.ArtistDetailScreen
import com.qlzhj.rhythm.feature.local.presentation.screens.AlbumDetailScreen
import com.qlzhj.rhythm.core.ui.shared.presentation.screens.settings.*
import com.qlzhj.rhythm.feature.settings.shared.presentation.screens.settings.*
import com.qlzhj.rhythm.feature.streaming.presentation.screens.StreamingServiceSetupScreen
import com.qlzhj.rhythm.feature.streaming.presentation.screens.GoSettingsScreen
import com.qlzhj.rhythm.core.model.toLibraryAlbum
import com.qlzhj.rhythm.feature.streaming.presentation.viewmodel.StreamingMusicViewModel
import androidx.core.net.toUri
import com.qlzhj.rhythm.core.model.StreamingArtist
import com.qlzhj.rhythm.core.model.StreamingAlbum
import com.qlzhj.rhythm.core.model.StreamingPlaybackUris
import com.qlzhj.rhythm.core.model.StreamingPlaylist
import com.qlzhj.rhythm.core.model.StreamingSong
import com.qlzhj.rhythm.core.model.Album
import com.qlzhj.rhythm.core.model.Artist
import com.qlzhj.rhythm.core.model.Playlist
import com.qlzhj.rhythm.core.model.Song
import com.qlzhj.rhythm.feature.streaming.presentation.navigation.StreamingRoutes
import com.qlzhj.rhythm.core.common.ArtistSeparator
import com.qlzhj.rhythm.feature.local.presentation.viewmodel.MusicViewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.compose.ui.platform.LocalContext
import com.qlzhj.rhythm.core.model.toLibraryAlbum

// Streaming detail route bodies + route constants and streaming->local mappers,
// extracted verbatim from LocalNavigationContent (P4-phase P4-3).

internal fun StreamingSong.toLocalSong(): Song? {
    // 服务端签发的播放地址会过期，不能固化进 Song.uri：可重新解析的歌曲统一走 streaming://，
    // 播放时由 RhythmPlayerEngine 的数据源现问一个新地址。
    val playbackUri = StreamingPlaybackUris.of(id)
        ?: streamingUrl?.takeIf { it.isNotBlank() }?.toUri()
        ?: previewUrl?.takeIf { it.isNotBlank() }?.toUri()
        ?: ("streaming://track/$id").toUri()

    return Song(
        id = id,
        title = title,
        artist = artist,
        album = album,
        albumId = albumId.orEmpty(),
        duration = duration,
        uri = playbackUri,
        artworkUri = artworkUri?.takeIf { it.isNotBlank() }?.let(Uri::parse),
        albumArtist = albumArtist,
        trackNumber = trackNumber ?: 0,
        year = year ?: 0,
        genre = genre,
        bitrate = bitrate,
        sampleRate = sampleRate,
        channels = channels,
        codec = codec
    )
}
internal fun StreamingSong.toDisplaySong(): Song {
    // 服务端签发的播放地址会过期，不能固化进 Song.uri：可重新解析的歌曲统一走 streaming://，
    // 播放时由 RhythmPlayerEngine 的数据源现问一个新地址。
    val playbackUri = StreamingPlaybackUris.of(id)
        ?: streamingUrl?.takeIf { it.isNotBlank() }?.toUri()
        ?: previewUrl?.takeIf { it.isNotBlank() }?.toUri()
        ?: ("streaming://track/$id").toUri()

    return Song(
        id = id,
        title = title,
        artist = artist,
        album = album,
        albumId = albumId.orEmpty(),
        duration = duration,
        uri = playbackUri,
        artworkUri = artworkUri?.takeIf { it.isNotBlank() }?.let(Uri::parse),
        albumArtist = albumArtist,
        trackNumber = trackNumber ?: 0,
        year = year ?: 0,
        genre = genre,
        bitrate = bitrate,
        sampleRate = sampleRate,
        channels = channels,
        codec = codec
    )
}
internal fun StreamingArtist.toDisplayArtist(
    fallbackName: String,
    songs: List<Song>,
    albums: List<Album>
): Artist {
    return Artist(
        id = id,
        name = name.ifBlank { fallbackName.ifBlank { "Artist" } },
        artworkUri = artworkUri?.takeIf { it.isNotBlank() }?.let(Uri::parse),
        albums = albums,
        songs = songs,
        numberOfAlbums = if (albumCount > 0) albumCount else albums.size,
        numberOfTracks = if (songCount > 0) songCount else songs.size
    )
}
internal fun StreamingPlaylist.toDisplayPlaylist(displaySongs: List<Song>): Playlist {
    return Playlist(
        id = id,
        name = name,
        songs = displaySongs,
        artworkUri = artworkUri?.takeIf { it.isNotBlank() }?.let(Uri::parse)
    )
}
@Composable
internal fun LocalStreamingAlbumDetailRoute(
    backStackEntry: androidx.navigation.NavBackStackEntry,
    appSettings: com.qlzhj.rhythm.core.data.shared.data.model.AppSettings,
    currentSong: com.qlzhj.rhythm.core.model.Song?,
    isPlaying: Boolean,
    isShuffleEnabled: Boolean,
    navController: NavHostController,
    onPlayerClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    streamingMusicViewModel: StreamingMusicViewModel,
    viewModel: MusicViewModel,
    selectedStreamingSongForPlaylist: androidx.compose.runtime.MutableState<StreamingSong?>,
    showStreamingAddToPlaylist: androidx.compose.runtime.MutableState<Boolean>
) {
    val appMode by appSettings.appMode.collectAsState()
    val isStreamingMode = appMode == "STREAMING"
    val streamingLikedSongs by streamingMusicViewModel.likedSongs.collectAsState()
    val streamingSavedAlbums by streamingMusicViewModel.savedAlbums.collectAsState()
    val streamingNewReleases by streamingMusicViewModel.newReleases.collectAsState()
    val streamingRecommendations by streamingMusicViewModel.recommendations.collectAsState()
    val streamingDownloadedSongs by streamingMusicViewModel.downloadedSongs.collectAsState()
    val streamingDownloadedAlbums by streamingMusicViewModel.downloadedAlbums.collectAsState()
    val streamingAllSongs by streamingMusicViewModel.allSongs.collectAsState()
    val streamingDownloadingSongIds by streamingMusicViewModel.downloadingSongIds.collectAsState()
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
    val streamingSongById by rememberOffMain(emptyMap<String, StreamingSong>(), streamingAllSongs, streamingRecommendations, streamingLikedSongs, streamingDownloadedSongs) {
        (streamingAllSongs + streamingRecommendations + streamingLikedSongs + streamingDownloadedSongs)
            .distinctBy { it.id }
            .associateBy { it.id }
    }
    val respectAlbumOnPlay by appSettings.respectAlbumOnPlay.collectAsState()

    val albumId = backStackEntry.arguments?.getString("albumId")?.let(Uri::decode).orEmpty()
    val albumName = backStackEntry.arguments?.getString("albumName")?.let(Uri::decode).orEmpty()

    var isAlbumLoading by remember(albumId) { mutableStateOf(true) }
    var albumSongs by remember(albumId) { mutableStateOf<List<StreamingSong>>(emptyList()) }
    var streamingAlbum by remember(albumId) { mutableStateOf<StreamingAlbum?>(null) }

    val searchResults by streamingMusicViewModel.searchResults.collectAsState()
    val currentService by streamingMusicViewModel.currentService.collectAsState()

    val matchedAlbum = remember(albumId, searchResults, currentService, streamingDownloadedAlbums, streamingSavedAlbums, streamingNewReleases) {
        (streamingDownloadedAlbums + streamingSavedAlbums + streamingNewReleases + searchResults.albums)
            .distinctBy { it.id }
            .firstOrNull { it.id == albumId }
            ?: StreamingAlbum(
                id = albumId,
                title = albumName,
                artist = "",
                artworkUri = null,
                songCount = 0,
                year = 0,
                sourceType = currentService
            )
    }

    LaunchedEffect(albumId) {
        if (albumId.isBlank()) {
            isAlbumLoading = false
            return@LaunchedEffect
        }

        // Immediate offline/memory resolution: if matchedAlbum already has tracks or downloaded songs match
        if (matchedAlbum.tracks.isNotEmpty()) {
            streamingAlbum = matchedAlbum
            albumSongs = matchedAlbum.tracks
            isAlbumLoading = false
        } else {
            val localSongs = streamingMusicViewModel.getAlbumSongs(matchedAlbum)
            if (localSongs.isNotEmpty()) {
                streamingAlbum = matchedAlbum
                albumSongs = localSongs
                isAlbumLoading = false
            } else {
                isAlbumLoading = true
            }
        }

        try {
            val albumDetail = streamingMusicViewModel.repository.getAlbumById(albumId)
            if (albumDetail != null && albumDetail is StreamingAlbum) {
                streamingAlbum = albumDetail
            }

            val targetAlbum = (albumDetail as? StreamingAlbum) ?: streamingAlbum ?: matchedAlbum
            var songs = streamingMusicViewModel.getAlbumSongs(targetAlbum)

            if (songs.isEmpty() && albumName.isNotBlank()) {
                val queueSongs = streamingMusicViewModel.queue.value
                val queueMatches = queueSongs.filter {
                    it.album.equals(albumName, ignoreCase = true)
                }
                if (queueMatches.isNotEmpty()) {
                    songs = queueMatches
                    if (streamingAlbum == null) {
                        val first = queueMatches.first()
                        streamingAlbum = StreamingAlbum(
                            id = albumId,
                            title = albumName,
                            artist = first.albumArtist ?: first.artist,
                            artworkUri = first.artworkUri,
                            songCount = queueMatches.size,
                            year = first.year,
                            sourceType = currentService
                        )
                    }
                }
            }

            if (songs.isNotEmpty()) {
                albumSongs = songs
            }
        } catch (e: Exception) {
            Log.w("AlbumDetail", "Failed to load/refresh album $albumId", e)
        } finally {
            isAlbumLoading = false
        }
    }

    val localAlbumSongs = remember(albumSongs) {
        albumSongs.map { it.toDisplaySong() }
    }

    val localAlbum = remember(matchedAlbum, streamingAlbum, localAlbumSongs) {
        val active = streamingAlbum ?: matchedAlbum
        val albumWithArt = if (active.artworkUri.isNullOrBlank()) {
            val songArt = localAlbumSongs.firstOrNull { it.artworkUri != null }?.artworkUri?.toString()
            if (!songArt.isNullOrBlank()) active.copy(artworkUri = songArt) else active
        } else {
            active
        }
        albumWithArt.toLibraryAlbum(localAlbumSongs)
    }

    val albumSongsById = remember(albumSongs) { albumSongs.associateBy { it.id } }
    var showSongInfoSheet by remember { mutableStateOf(false) }
    var selectedSongForInfo by remember { mutableStateOf<com.qlzhj.rhythm.core.model.Song?>(null) }

    AlbumDetailScreen(
        albumId = albumId,
        albumName = localAlbum.title,
        onBack = {
            val popped = navController.popBackStack()
            if (!popped) {
                // Entry already popped
            }
        },
        onSongClick = { localSong ->
            val queue = if (respectAlbumOnPlay && albumSongs.isNotEmpty()) albumSongs else {
                albumSongsById[localSong.id]?.let { listOf(it) }.orEmpty()
            }
            if (queue.isNotEmpty()) {
                val index = queue.indexOfFirst { it.id == localSong.id }.coerceAtLeast(0)
                val keepShuffle = appSettings.keepShuffleOnSelection.value && isShuffleEnabled
                streamingMusicViewModel.playQueue(
                    queue = queue,
                    startIndex = index,
                    shuffle = keepShuffle,
                    pinStartIndex = keepShuffle
                )
            }
        },
        onPlayAll = { songs ->
            if (albumSongs.isNotEmpty()) {
                streamingMusicViewModel.playQueue(
                    queue = albumSongs,
                    startIndex = 0,
                    shuffle = false
                )
            }
        },
        onShufflePlay = {
            if (albumSongs.isNotEmpty()) {
                streamingMusicViewModel.playQueue(
                    queue = albumSongs,
                    startIndex = 0,
                    shuffle = true
                )
            }
        },
        onAddToQueue = { localSong ->
            albumSongsById[localSong.id]?.let { streamingSong ->
                streamingMusicViewModel.addSongToQueue(streamingSong, viewModel)
            }
        },
        onAddSongToPlaylist = { localSong ->
            albumSongsById[localSong.id]?.let { onStreamingAddSongToPlaylist(it) }
        },
        onGoToArtist = { song ->
            val separatorEnabled = appSettings.artistSeparatorEnabled.value
            val delimiters = appSettings.artistSeparatorDelimiters.value.ifBlank { AppSettings.DEFAULT_ARTIST_SEPARATOR_DELIMITERS }
            val candidates = ArtistSeparator.splitArtistNames(
                song.artist,
                delimiters = delimiters,
                enabled = separatorEnabled
            )
            val artistName = candidates.firstOrNull()?.trim().orEmpty().ifBlank { song.artist.trim() }
            if (artistName.isNotBlank()) {
                val streamingSong = albumSongsById[song.id]
                val artistId = streamingSong?.artist?.trim() ?: artistName
                navController.navigate(StreamingRoutes.artist(artistId, artistName)) {
                    launchSingleTop = true
                }
            }
        },
        onPlayerClick = {
            navController.navigate(Screen.Player.route) {
                launchSingleTop = true
            }
        },
        onPlayNext = { localSong ->
            albumSongsById[localSong.id]?.let { streamingSong ->
                streamingMusicViewModel.playNext(streamingSong, viewModel)
            }
        },
        onToggleFavorite = { localSong ->
            albumSongsById[localSong.id]?.let { streamingSong ->
                val isLiked = streamingLikedSongIds.contains(streamingSong.id)
                if (isLiked) streamingMusicViewModel.unlikeSong(streamingSong)
                else streamingMusicViewModel.likeSong(streamingSong)
            }
        },
        isStreamingMode = true,
        favoriteSongs = streamingLikedSongIds,
        onShowSongInfo = { song ->
            selectedSongForInfo = song
            showSongInfoSheet = true
        },
        currentSong = currentSong,
        isPlaying = isPlaying,
        albumOverride = localAlbum,
        songsOverride = localAlbumSongs,
        isContentLoadingOverride = isAlbumLoading,
        viewModel = viewModel
    )

    if (showSongInfoSheet && selectedSongForInfo != null) {
        val currentSelectedSong = selectedSongForInfo!!
        SongInfoBottomSheet(
            song = currentSelectedSong,
            onDismiss = {
                showSongInfoSheet = false
                selectedSongForInfo = null
            },
            appSettings = appSettings,
            isStreamingMode = true,
            isDownloaded = streamingDownloadedSongIds.contains(currentSelectedSong.id),
            isDownloading = streamingDownloadingSongIds.contains(currentSelectedSong.id),
            onToggleDownload = {
                if (streamingDownloadedSongIds.contains(currentSelectedSong.id)) {
                    streamingMusicViewModel.removeDownload(currentSelectedSong.id)
                } else {
                    val orig = streamingSongById[currentSelectedSong.id]
                    if (orig != null) {
                        streamingMusicViewModel.downloadSong(orig)
                    } else {
                        streamingMusicViewModel.downloadSongById(currentSelectedSong.id)
                    }
                }
            }
        )
    }
}

@Composable
internal fun LocalStreamingArtistDetailRoute(
    backStackEntry: androidx.navigation.NavBackStackEntry,
    appSettings: com.qlzhj.rhythm.core.data.shared.data.model.AppSettings,
    currentSong: com.qlzhj.rhythm.core.model.Song?,
    isPlaying: Boolean,
    isShuffleEnabled: Boolean,
    navController: NavHostController,
    onPlayerClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    streamingMusicViewModel: StreamingMusicViewModel,
    viewModel: MusicViewModel,
    selectedStreamingSongForPlaylist: androidx.compose.runtime.MutableState<StreamingSong?>,
    showStreamingAddToPlaylist: androidx.compose.runtime.MutableState<Boolean>
) {
    val context = LocalContext.current
    val appMode by appSettings.appMode.collectAsState()
    val isStreamingMode = appMode == "STREAMING"
    val streamingLikedSongs by streamingMusicViewModel.likedSongs.collectAsState()
    val streamingRecommendations by streamingMusicViewModel.recommendations.collectAsState()
    val streamingDownloadedSongs by streamingMusicViewModel.downloadedSongs.collectAsState()
    val streamingDownloadedArtists by streamingMusicViewModel.downloadedArtists.collectAsState()
    val streamingAllSongs by streamingMusicViewModel.allSongs.collectAsState()
    val streamingDownloadingSongIds by streamingMusicViewModel.downloadingSongIds.collectAsState()
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
    val streamingSongById by rememberOffMain(emptyMap<String, StreamingSong>(), streamingAllSongs, streamingRecommendations, streamingLikedSongs, streamingDownloadedSongs) {
        (streamingAllSongs + streamingRecommendations + streamingLikedSongs + streamingDownloadedSongs)
            .distinctBy { it.id }
            .associateBy { it.id }
    }
    val respectAlbumOnPlay by appSettings.respectAlbumOnPlay.collectAsState()

    val artistId = backStackEntry.arguments?.getString("artistId")?.let(Uri::decode).orEmpty()
    val routeArtistName = backStackEntry.arguments?.getString("artistName")?.let(Uri::decode).orEmpty()

    val followedArtists by streamingMusicViewModel.followedArtists.collectAsState()
    val searchResults by streamingMusicViewModel.searchResults.collectAsState()
    val currentService by streamingMusicViewModel.currentService.collectAsState()

    val resolvedArtistName = remember(artistId, routeArtistName) {
        routeArtistName.ifBlank { artistId }
    }

    val selectedArtist = remember(
        artistId,
        resolvedArtistName,
        followedArtists,
        searchResults,
        currentService,
        streamingDownloadedArtists
    ) {
        (streamingDownloadedArtists + followedArtists + searchResults.artists)
            .distinctBy { it.id }
            .firstOrNull { it.id == artistId || it.name.equals(resolvedArtistName, ignoreCase = true) }
            ?: StreamingArtist(
                id = artistId,
                name = resolvedArtistName.ifBlank {
                    context.getString(R.string.artists_title)
                },
                artworkUri = null,
                songCount = 0,
                albumCount = 0,
                sourceType = currentService
            )
    }

    var artistSongs by remember(artistId) {
        mutableStateOf(selectedArtist.getTopTracks())
    }
    var artistAlbums by remember(artistId) { mutableStateOf<List<StreamingAlbum>>(emptyList()) }
    var isArtistLoading by remember(artistId) { mutableStateOf(true) }
    var fetchedArtist by remember(artistId) { mutableStateOf<StreamingArtist?>(null) }

    LaunchedEffect(artistId, selectedArtist.name) {
        if (artistId.isBlank()) {
            artistSongs = emptyList()
            isArtistLoading = false
            return@LaunchedEffect
        }

        // Immediate resolution from downloads / memory
        val separatorEnabled = appSettings.artistSeparatorEnabled.value
        val separatorDelimiters = appSettings.artistSeparatorDelimiters.value.ifBlank { AppSettings.DEFAULT_ARTIST_SEPARATOR_DELIMITERS }

        val downloadedTracks = streamingMusicViewModel.downloadedSongs.value.filter { song ->
            song.artist.equals(selectedArtist.name, ignoreCase = true) ||
            song.albumArtist?.equals(selectedArtist.name, ignoreCase = true) == true ||
            ArtistSeparator.splitArtistNames(song.artist, delimiters = separatorDelimiters, enabled = separatorEnabled).any { it.equals(selectedArtist.name, ignoreCase = true) } ||
            (song.albumArtist != null && ArtistSeparator.splitArtistNames(song.albumArtist, delimiters = separatorDelimiters, enabled = separatorEnabled).any { it.equals(selectedArtist.name, ignoreCase = true) })
        }
        val downloadedAlbumsForArtist = streamingMusicViewModel.downloadedAlbums.value.filter { album ->
            album.artist.equals(selectedArtist.name, ignoreCase = true) ||
            ArtistSeparator.splitArtistNames(album.artist, delimiters = separatorDelimiters, enabled = separatorEnabled).any { it.equals(selectedArtist.name, ignoreCase = true) }
        }

        if (downloadedTracks.isNotEmpty() || downloadedAlbumsForArtist.isNotEmpty() || selectedArtist.getTopTracks().isNotEmpty()) {
            artistSongs = downloadedTracks.ifEmpty { selectedArtist.getTopTracks() }
            artistAlbums = downloadedAlbumsForArtist
            isArtistLoading = false
        } else {
            isArtistLoading = true
        }

        try {
            if (selectedArtist.artworkUri == null) {
                fetchedArtist = streamingMusicViewModel.getArtistInfo(
                    artistId = artistId,
                    artistNameHint = selectedArtist.name
                )
            }
            val songs = streamingMusicViewModel.getArtistTopSongs(
                artistId = artistId,
                artistNameHint = selectedArtist.name,
                limit = 80
            )
            if (songs.isNotEmpty()) {
                artistSongs = songs
            }
            val albums = streamingMusicViewModel.getArtistAlbums(
                artistId = artistId,
                artistNameHint = selectedArtist.name
            )
            if (albums.isNotEmpty()) {
                artistAlbums = albums
            }
        } catch (e: Exception) {
            Log.w("ArtistDetail", "Failed to fetch remote artist details", e)
        } finally {
            isArtistLoading = false
        }
    }

    val localArtistSongs = remember(artistSongs) {
        artistSongs.map { it.toDisplaySong() }
    }
    val localArtistAlbums = remember(artistAlbums) {
        artistAlbums.map { it.toLibraryAlbum(emptyList()) }
    }
    val effectiveArtist = remember(selectedArtist, fetchedArtist, localArtistSongs, localArtistAlbums) {
        val base = when {
            selectedArtist.artworkUri != null -> selectedArtist
            fetchedArtist?.artworkUri != null -> fetchedArtist!!
            else -> selectedArtist
        }
        if (base.artworkUri.isNullOrBlank()) {
            val fallbackArt = localArtistSongs.firstOrNull { it.artworkUri != null }?.artworkUri?.toString()
                ?: localArtistAlbums.firstOrNull { it.artworkUri != null }?.artworkUri?.toString()
            if (!fallbackArt.isNullOrBlank()) base.copy(artworkUri = fallbackArt) else base
        } else {
            base
        }
    }
    val localArtist = remember(
        effectiveArtist,
        resolvedArtistName,
        localArtistSongs,
        localArtistAlbums
    ) {
        effectiveArtist.toDisplayArtist(
            fallbackName = resolvedArtistName,
            songs = localArtistSongs,
            albums = localArtistAlbums
        )
    }
    val artistSongsById = remember(artistSongs) { artistSongs.associateBy { it.id } }
    var showSongInfoSheet by remember { mutableStateOf(false) }
    var selectedSongForInfo by remember { mutableStateOf<com.qlzhj.rhythm.core.model.Song?>(null) }

    ArtistDetailScreen(
        viewModel = viewModel,
        artistName = localArtist.name,
        onBack = {
            val popped = navController.popBackStack()
            if (!popped) {
                // Entry already popped
            }
        },
        onSongClick = { localSong ->
            val queue = if (respectAlbumOnPlay && artistSongs.isNotEmpty()) artistSongs else {
                artistSongsById[localSong.id]?.let { listOf(it) }.orEmpty()
            }
            if (queue.isNotEmpty()) {
                val index = queue.indexOfFirst { it.id == localSong.id }.coerceAtLeast(0)
                val keepShuffle = appSettings.keepShuffleOnSelection.value && isShuffleEnabled
                streamingMusicViewModel.playQueue(
                    queue = queue,
                    startIndex = index,
                    shuffle = keepShuffle,
                    pinStartIndex = keepShuffle
                )
            }
        },
        onAlbumClick = { album ->
            navController.navigate(StreamingRoutes.album(album.id, album.title)) {
                launchSingleTop = true
            }
        },
        onPlayAll = { songs ->
            if (artistSongs.isNotEmpty()) {
                streamingMusicViewModel.playQueue(
                    queue = artistSongs,
                    startIndex = 0,
                    shuffle = false
                )
            } else {
                val fallbackQueue = songs.mapNotNull { artistSongsById[it.id] }
                if (fallbackQueue.isNotEmpty()) {
                    streamingMusicViewModel.playQueue(
                        queue = fallbackQueue,
                        startIndex = 0,
                        shuffle = false
                    )
                }
            }
        },
        onShufflePlay = {
            if (artistSongs.isNotEmpty()) {
                streamingMusicViewModel.playQueue(
                    queue = artistSongs,
                    startIndex = 0,
                    shuffle = true
                )
            }
        },
        onAddToQueue = { localSong ->
            artistSongsById[localSong.id]?.let { streamingSong ->
                streamingMusicViewModel.addSongToQueue(streamingSong, viewModel)
            }
        },
        onAddSongToPlaylist = { localSong ->
            artistSongsById[localSong.id]?.let { onStreamingAddSongToPlaylist(it) }
        },
        onPlayerClick = {
            navController.navigate(Screen.Player.route) {
                launchSingleTop = true
            }
        },
        onPlayNext = { localSong ->
            artistSongsById[localSong.id]?.let { streamingSong ->
                streamingMusicViewModel.playNext(streamingSong, viewModel)
            }
        },
        onToggleFavorite = { localSong ->
            artistSongsById[localSong.id]?.let { streamingSong ->
                val isLiked = streamingLikedSongIds.contains(streamingSong.id)
                if (isLiked) streamingMusicViewModel.unlikeSong(streamingSong)
                else streamingMusicViewModel.likeSong(streamingSong)
            }
        },
        favoriteSongs = streamingLikedSongIds,
        onShowSongInfo = { song ->
            selectedSongForInfo = song
            showSongInfoSheet = true
        },
        showPlayNextAction = false,
        currentSong = currentSong,
        isPlaying = isPlaying,
        artistOverride = localArtist,
        songsOverride = localArtistSongs,
        albumsOverride = localArtistAlbums,
        isContentLoadingOverride = isArtistLoading
    )

    if (showSongInfoSheet && selectedSongForInfo != null) {
        val currentSelectedSong = selectedSongForInfo!!
        SongInfoBottomSheet(
            song = currentSelectedSong,
            onDismiss = {
                showSongInfoSheet = false
                selectedSongForInfo = null
            },
            appSettings = appSettings,
            isStreamingMode = true,
            isDownloaded = streamingDownloadedSongIds.contains(currentSelectedSong.id),
            isDownloading = streamingDownloadingSongIds.contains(currentSelectedSong.id),
            onToggleDownload = {
                if (streamingDownloadedSongIds.contains(currentSelectedSong.id)) {
                    streamingMusicViewModel.removeDownload(currentSelectedSong.id)
                } else {
                    val orig = streamingSongById[currentSelectedSong.id]
                    if (orig != null) {
                        streamingMusicViewModel.downloadSong(orig)
                    } else {
                        streamingMusicViewModel.downloadSongById(currentSelectedSong.id)
                    }
                }
            }
        )
    }
}

@Composable
internal fun LocalStreamingPlaylistDetailRoute(
    backStackEntry: androidx.navigation.NavBackStackEntry,
    appSettings: com.qlzhj.rhythm.core.data.shared.data.model.AppSettings,
    currentSong: com.qlzhj.rhythm.core.model.Song?,
    isPlaying: Boolean,
    isShuffleEnabled: Boolean,
    navController: NavHostController,
    onPlayPause: () -> Unit,
    onPlayerClick: () -> Unit,
    onSkipNext: () -> Unit,
    onToggleFavorite: () -> Unit,
    songs: List<com.qlzhj.rhythm.core.model.Song>,
    streamingMusicViewModel: StreamingMusicViewModel,
    viewModel: MusicViewModel,
    selectedStreamingSongForPlaylist: androidx.compose.runtime.MutableState<StreamingSong?>,
    showStreamingAddToPlaylist: androidx.compose.runtime.MutableState<Boolean>
) {
    val context = LocalContext.current
    val streamingLikedSongs by streamingMusicViewModel.likedSongs.collectAsState()
    val streamingSavedAlbums by streamingMusicViewModel.savedAlbums.collectAsState()
    val streamingNewReleases by streamingMusicViewModel.newReleases.collectAsState()
    val streamingDownloadedAlbums by streamingMusicViewModel.downloadedAlbums.collectAsState()
    val streamingAlbumCatalog = remember(streamingSavedAlbums, streamingNewReleases, streamingDownloadedAlbums) {
        (streamingSavedAlbums + streamingNewReleases + streamingDownloadedAlbums).distinctBy { it.id }
    }
    val onStreamingAddSongToPlaylist: (StreamingSong) -> Unit = { song ->
        selectedStreamingSongForPlaylist.value = song
        showStreamingAddToPlaylist.value = true
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
    val respectAlbumOnPlay by appSettings.respectAlbumOnPlay.collectAsState()

    val playlistId = backStackEntry.arguments?.getString("playlistId")?.let(Uri::decode).orEmpty()

    val savedPlaylists by streamingMusicViewModel.savedPlaylists.collectAsState()
    val featuredPlaylists by streamingMusicViewModel.featuredPlaylists.collectAsState()
    val searchResults by streamingMusicViewModel.searchResults.collectAsState()

    val selectedPlaylist = remember(
        playlistId,
        savedPlaylists,
        featuredPlaylists,
        searchResults
    ) {
        (savedPlaylists + featuredPlaylists + searchResults.playlists)
            .distinctBy { it.id }
            .firstOrNull { it.id == playlistId }
    }

    val playlistTracks = remember(selectedPlaylist) {
        selectedPlaylist?.getTracks().orEmpty()
    }
    val playlistTracksById = remember(playlistTracks) {
        playlistTracks.associateBy { it.id }
    }
    val localPlaylistSongs = remember(playlistTracks) {
        playlistTracks.map { it.toDisplaySong() }
    }
    val localPlaylist = remember(selectedPlaylist, localPlaylistSongs, playlistId) {
        selectedPlaylist?.toDisplayPlaylist(localPlaylistSongs)
            ?: Playlist(
                id = playlistId,
                name = context.getString(R.string.library_tab_playlists),
                songs = localPlaylistSongs
            )
    }
    val scope = rememberCoroutineScope()

    PlaylistDetailScreen(
        musicViewModel = viewModel,
        playlist = localPlaylist,
        isStreamingPlaylist = true,
        currentSong = currentSong,
        isPlaying = isPlaying,
        onPlayPause = { viewModel.togglePlayPause() },
        onPlayerClick = {
            navController.navigate(Screen.Player.route) {
                launchSingleTop = true
            }
        },
        onPlayAll = {
            if (playlistTracks.isNotEmpty()) {
                streamingMusicViewModel.playQueue(
                    queue = playlistTracks,
                    startIndex = 0,
                    shuffle = false
                )
            }
        },
        onShufflePlay = {
            if (playlistTracks.isNotEmpty()) {
                streamingMusicViewModel.playQueue(
                    queue = playlistTracks,
                    startIndex = 0,
                    shuffle = true
                )
            }
        },
        onSongClick = { localSong ->
            val keepShuffle = appSettings.keepShuffleOnSelection.value && isShuffleEnabled
            if (respectAlbumOnPlay) {
                val index = playlistTracks.indexOfFirst { it.id == localSong.id }
                if (index >= 0) {
                    streamingMusicViewModel.playQueue(
                        queue = playlistTracks,
                        startIndex = index,
                        shuffle = keepShuffle,
                        pinStartIndex = keepShuffle
                    )
                }
            } else {
                playlistTracks.firstOrNull { it.id == localSong.id }?.let { single ->
                    streamingMusicViewModel.playQueue(
                        queue = listOf(single),
                        startIndex = 0,
                        shuffle = keepShuffle,
                        pinStartIndex = keepShuffle
                    )
                }
            }
        },
        onPlaySongFromPlaylist = { localSong, localQueue ->
            val queue = localQueue
                .mapNotNull { playlistTracksById[it.id] }
                .ifEmpty { playlistTracks }
            if (queue.isNotEmpty()) {
                val index = queue.indexOfFirst { it.id == localSong.id }.coerceAtLeast(0)
                val keepShuffle = appSettings.keepShuffleOnSelection.value && isShuffleEnabled
                streamingMusicViewModel.playQueue(
                    queue = queue,
                    startIndex = index,
                    shuffle = keepShuffle,
                    pinStartIndex = keepShuffle
                )
            }
        },
        onBack = {
            val popped = navController.popBackStack()
            if (!popped) {
                // Entry already popped
            }
        },
        onRemoveSong = { localSong, _ ->
            playlistTracksById[localSong.id]?.let { streamingSong ->
                streamingMusicViewModel.removeSongFromPlaylist(playlistId, streamingSong.id)
            }
        },
        onRenamePlaylist = { newName ->
            selectedPlaylist?.let { playlist ->
                streamingMusicViewModel.renamePlaylist(playlist, newName)
            }
        },
        onDeletePlaylist = {
            selectedPlaylist?.let { playlist ->
                streamingMusicViewModel.deletePlaylist(playlist) { success ->
                    if (success) {
                        val popped = navController.popBackStack()
                        if (!popped) {
                            // Entry already popped
                        }
                    }
                }
            }
        },
        onSkipNext = { streamingMusicViewModel.skipToNext() },
        onSearchClick = { navigateToTopLevel(Screen.Search.route) },
        onPlayNext = { localSong ->
            playlistTracksById[localSong.id]?.let { streamingSong ->
                streamingMusicViewModel.playNext(streamingSong, viewModel)
            }
        },
        onAddToQueue = { localSong ->
            playlistTracksById[localSong.id]?.let { streamingSong ->
                streamingMusicViewModel.addSongToQueue(streamingSong, viewModel)
            }
        },
        onToggleFavorite = { localSong ->
            playlistTracksById[localSong.id]?.let { streamingSong ->
                val isLiked = streamingLikedSongs.any { it.id == streamingSong.id }
                if (isLiked) streamingMusicViewModel.unlikeSong(streamingSong)
                else streamingMusicViewModel.likeSong(streamingSong)
            }
        },
        onAddToPlaylist = { localSong ->
            playlistTracksById[localSong.id]?.let { onStreamingAddSongToPlaylist(it) }
        },
        onGoToAlbum = { song ->
            val baseAlbumId = song.albumId.takeIf { it.isNotBlank() }
            val albumArtist = song.albumArtist?.takeIf { it.isNotBlank() } ?: song.artist
            val matchingAlbum = streamingAlbumCatalog.firstOrNull { album ->
                val albumMatchesByTrack = album.tracks.any { it.id == song.id }
                val albumMatchesById = baseAlbumId?.let { album.id == it } == true
                val albumMatchesByMetadata = album.title.equals(song.album, ignoreCase = true) &&
                    album.artist.equals(albumArtist, ignoreCase = true)
                albumMatchesByTrack || albumMatchesById || albumMatchesByMetadata
            } ?: streamingAlbumCatalog.firstOrNull { album ->
                album.title.equals(song.album, ignoreCase = true)
            }

            val resolvedAlbumId = matchingAlbum?.id ?: baseAlbumId ?: "streaming-playlist:${song.id}:${song.album.lowercase()}"
            val resolvedAlbumTitle = matchingAlbum?.title ?: song.album

            navController.navigate(
                StreamingRoutes.album(resolvedAlbumId, resolvedAlbumTitle)
            ) {
                launchSingleTop = true
            }
        },
        onGoToArtist = { song ->
            val separatorEnabled = appSettings.artistSeparatorEnabled.value
            val delimiters = appSettings.artistSeparatorDelimiters.value.ifBlank { AppSettings.DEFAULT_ARTIST_SEPARATOR_DELIMITERS }
            val candidates = ArtistSeparator.splitArtistNames(
                song.artist,
                delimiters = delimiters,
                enabled = separatorEnabled
            )
            val artistName = candidates.firstOrNull()?.trim().orEmpty().ifBlank { song.artist.trim() }
            if (artistName.isNotBlank()) {
                val streamingSong = playlistTracksById[song.id]
                val artistId = streamingSong?.artist?.trim()?.let { name ->
                    name
                } ?: artistName
                navController.navigate(
                    StreamingRoutes.artist(artistId, artistName)
                ) {
                    launchSingleTop = true
                }
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

@Composable
internal fun LocalStreamingServiceSetupRoute(
    backStackEntry: androidx.navigation.NavBackStackEntry,
    navController: NavHostController,
    streamingMusicViewModel: StreamingMusicViewModel,
    viewModel: MusicViewModel
) {
    val serviceId = backStackEntry.arguments?.getString("serviceId")?.let(Uri::decode).orEmpty()
    StreamingServiceSetupScreen(
        serviceId = serviceId,
        viewModel = streamingMusicViewModel,
        onBackClick = {
            val popped = navController.popBackStack()
            if (!popped) {
                // Entry already popped
            }
        },
        onLoginSuccess = {
            // 扫码登录成功后不要把人留在配置页，返回进入前的页面（首页/设置）。
            val popped = navController.popBackStack()
            if (!popped) {
                navController.navigate(Screen.Home.route) {
                    launchSingleTop = true
                }
            }
        }
    )
}

@Composable
internal fun LocalStreamingGoSettingsRoute(
    navController: NavHostController,
    streamingMusicViewModel: StreamingMusicViewModel,
    viewModel: MusicViewModel
) {
    GoSettingsScreen(
        onBackClick = {
            val popped = navController.popBackStack()
            if (!popped) {
                // Entry already popped
            }
        },
        onConfigureCurrentProvider = { serviceId ->
            navController.navigate(StreamingRoutes.serviceSetup(serviceId)) {
                launchSingleTop = true
            }
        },
        viewModel = streamingMusicViewModel
    )
}

