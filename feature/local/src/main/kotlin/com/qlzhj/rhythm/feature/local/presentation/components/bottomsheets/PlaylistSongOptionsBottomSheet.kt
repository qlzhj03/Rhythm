package com.qlzhj.rhythm.feature.local.presentation.components.bottomsheets

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.ui.hapticfeedback.HapticFeedback
import com.qlzhj.rhythm.core.model.Song
import com.qlzhj.rhythm.core.ui.shared.presentation.components.bottomsheets.SongOverflowBottomSheet

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistSongOptionsBottomSheet(
    song: Song,
    onDismiss: () -> Unit,
    onRemoveFromPlaylist: () -> Unit,
    onPlayNext: () -> Unit,
    onAddToQueue: () -> Unit,
    onAddToPlaylist: () -> Unit,
    onShowSongInfo: () -> Unit,
    onGoToAlbum: () -> Unit,
    onGoToArtist: () -> Unit,
    onShare: () -> Unit,
    onDeleteSong: () -> Unit,
    showRemoveFromPlaylist: Boolean = true,
    showAddToPlaylist: Boolean = true,
    showGoToAlbum: Boolean = true,
    isStreamingMode: Boolean = false,
    haptics: HapticFeedback? = null,
    onPlay: (() -> Unit)? = null
) {
    SongOverflowBottomSheet(
        song = song,
        onDismiss = onDismiss,
        onPlay = onPlay ?: {},
        onPlayNext = onPlayNext,
        onAddToQueue = onAddToQueue,
        onAddToPlaylist = if (showAddToPlaylist) onAddToPlaylist else null,
        onGoToAlbum = if (showGoToAlbum) onGoToAlbum else null,
        onGoToArtist = onGoToArtist,
        onShowSongInfo = onShowSongInfo,
        onRemoveFromPlaylist = if (showRemoveFromPlaylist) onRemoveFromPlaylist else null,
        onDeleteSong = onDeleteSong,
        onShare = onShare,
        isStreaming = isStreamingMode
    )
}
