package chromahub.rhythm.app.navigation

import androidx.hilt.navigation.compose.hiltViewModel

import com.qlzhj.rhythm.core.designsystem.icons.RhythmIcons
import com.qlzhj.rhythm.core.designsystem.icons.MaterialSymbolIcon
import com.qlzhj.rhythm.core.designsystem.icons.Icon
import chromahub.rhythm.app.features.local.presentation.navigation.Screen

import android.content.Context
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import com.qlzhj.rhythm.core.designsystem.theme.MusicDimensions
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.rememberModalBottomSheetState
import com.qlzhj.rhythm.core.ui.shared.presentation.components.bottomsheets.SongPickerBottomSheet
import com.qlzhj.rhythm.core.ui.shared.presentation.components.common.CollapsibleHeaderScreen
import com.qlzhj.rhythm.core.designsystem.components.ExpressiveFilledIconButton
import com.qlzhj.rhythm.core.designsystem.components.ExpressiveShapes
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.derivedStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.qlzhj.rhythm.core.designsystem.LocalMiniPlayerPadding
import com.qlzhj.rhythm.core.designsystem.UiConstants
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.layout
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.viewmodel.compose.viewModel
import chromahub.rhythm.app.R
import com.qlzhj.rhythm.core.data.shared.data.model.AppSettings
import com.qlzhj.rhythm.core.media.util.rememberOffMain
import com.qlzhj.rhythm.core.designsystem.utils.DevicePosture
import com.qlzhj.rhythm.core.designsystem.utils.rememberDevicePosture
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.qlzhj.rhythm.core.ui.shared.presentation.components.bottomsheets.AddToPlaylistBottomSheet

import com.qlzhj.rhythm.core.ui.shared.presentation.components.bottomsheets.SongInfoBottomSheet
import com.qlzhj.rhythm.core.ui.shared.presentation.components.bottomsheets.UpdateBottomSheet
import chromahub.rhythm.app.features.local.presentation.screens.AddToPlaylistScreen
import com.qlzhj.rhythm.core.ui.features.local.presentation.components.dialogs.CreatePlaylistDialog
import com.qlzhj.rhythm.core.ui.features.local.presentation.components.dialogs.QueueActionDialog
import com.qlzhj.rhythm.core.ui.features.local.presentation.components.dialogs.QueueListActionDialog
import chromahub.rhythm.app.shared.presentation.components.player.MiniPlayer
import chromahub.rhythm.app.shared.presentation.components.player.RhythmPlayerSheet
import com.qlzhj.rhythm.core.ui.shared.presentation.components.player.SleepTimerBottomSheetNew
import chromahub.rhythm.app.features.local.presentation.screens.LibraryScreen
import chromahub.rhythm.app.features.local.presentation.screens.HomeScreen
import chromahub.rhythm.app.shared.presentation.screens.RhythmStatsScreen
import chromahub.rhythm.app.features.local.presentation.screens.EqualizerScreen
import chromahub.rhythm.app.shared.presentation.screens.player.PlayerScreen

import chromahub.rhythm.app.features.local.presentation.screens.PlaylistDetailScreen
import chromahub.rhythm.app.features.local.presentation.screens.ArtistDetailScreen
import chromahub.rhythm.app.features.local.presentation.screens.AlbumDetailScreen
import chromahub.rhythm.app.shared.presentation.screens.settings.SettingsScreenWrapper
import com.qlzhj.rhythm.core.ui.shared.presentation.screens.settings.*
import chromahub.rhythm.app.shared.presentation.screens.settings.*
import chromahub.rhythm.app.features.streaming.presentation.screens.StreamingServiceSetupScreen
import chromahub.rhythm.app.features.streaming.presentation.screens.GoSettingsScreen
import com.qlzhj.rhythm.core.model.toLibraryAlbum
import com.qlzhj.rhythm.core.model.toLibraryArtist
import com.qlzhj.rhythm.core.model.toLibraryPlaylist
import com.qlzhj.rhythm.core.model.toLibrarySong
import com.qlzhj.rhythm.core.ui.shared.presentation.components.common.StreamingServiceOptions
import chromahub.rhythm.app.features.streaming.presentation.viewmodel.StreamingMusicViewModel
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
import chromahub.rhythm.app.features.streaming.presentation.navigation.StreamingRoutes
import com.qlzhj.rhythm.core.model.PlaybackLocation
import com.qlzhj.rhythm.core.model.findAlbumForSong
import chromahub.rhythm.app.shared.presentation.components.MediaScanLoader // Add MediaScanLoader import
import com.qlzhj.rhythm.core.common.ArtistSeparator
import com.qlzhj.rhythm.core.designsystem.utils.HapticUtils
import com.qlzhj.rhythm.core.designsystem.utils.HapticType
import chromahub.rhythm.app.features.local.presentation.viewmodel.MusicViewModel
import chromahub.rhythm.app.features.local.presentation.viewmodel.MusicViewModel.SortOrder
import com.qlzhj.rhythm.core.ui.shared.presentation.viewmodel.ThemeViewModel
import com.qlzhj.rhythm.core.ui.shared.presentation.viewmodel.AppUpdaterViewModel
import com.qlzhj.rhythm.core.ui.shared.presentation.viewmodel.rememberAppUpdaterViewModel
import coil.compose.AsyncImage
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateDp
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.EaseOutQuint
import androidx.compose.animation.core.EaseInOutQuart
import androidx.compose.animation.core.EaseInBack
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.unit.sp
import chromahub.rhythm.app.features.local.presentation.screens.LibraryTab
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.OutlinedTextField
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.res.stringResource
import com.qlzhj.rhythm.core.model.toLibraryAlbum
import com.qlzhj.rhythm.core.model.toLibraryArtist
import com.qlzhj.rhythm.core.model.toLibraryPlaylist
import com.qlzhj.rhythm.core.model.toLibrarySong

// Streaming detail/service routes hosted inside the unified navigation shell.





@Composable
private fun AnimateIn(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        visible = true
    }

    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(durationMillis = 300, delayMillis = 50),
        label = "alpha"
    )

    val scale by animateFloatAsState(
        targetValue = if (visible) 1f else 0.95f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "scale"
    )

    Box(
        modifier = modifier.graphicsLayer(
            alpha = alpha,
            scaleX = scale,
            scaleY = scale
        )
    ) {
        content()
    }
}

@OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
@Composable
fun LocalNavigation(
    navController: NavHostController = rememberNavController(),
    viewModel: MusicViewModel = hiltViewModel(),
    themeViewModel: ThemeViewModel = hiltViewModel(),
    appSettings: com.qlzhj.rhythm.core.data.shared.data.model.AppSettings,
    streamingMusicViewModel: StreamingMusicViewModel = hiltViewModel()
) {
    val miniPlayerThemeId by appSettings.miniPlayerThemeId.collectAsState()
    val respectAlbumOnPlay by appSettings.respectAlbumOnPlay.collectAsState()
    // Update monitoring
    val updaterViewModel: AppUpdaterViewModel = rememberAppUpdaterViewModel()
    val updateAvailable by updaterViewModel.updateAvailable.collectAsState()
    val updatesEnabled by appSettings.updatesEnabled.collectAsState()
    val latestVersion by updaterViewModel.latestVersion.collectAsState()
    var showUpdateBottomSheet by remember { mutableStateOf(false) }
    
    // Track the version we've shown to avoid re-showing for the same version
    var lastShownVersion by remember { mutableStateOf<String?>(null) }
    
    // Monitor for updates and show bottom sheet automatically
    LaunchedEffect(updateAvailable, updatesEnabled, latestVersion) {
        if (updateAvailable && updatesEnabled) {
            val currentVersion = latestVersion?.versionName
            // Show if we haven't shown this version yet, or if the version changed
            if (currentVersion != null && currentVersion != lastShownVersion) {
                showUpdateBottomSheet = true
                lastShownVersion = currentVersion
            }
        } else {
            // Reset when update is no longer available
            lastShownVersion = null
        }
    }
    // Collect state from ViewModel
    val songs by viewModel.filteredSongs.collectAsState() // Use filtered songs to exclude blacklisted ones
    val allSongs by viewModel.songs.collectAsState() // Keep all songs for specific cases
    val albums by viewModel.filteredAlbums.collectAsState() // Use filtered albums to exclude albums with all songs blacklisted
    val artists by viewModel.filteredArtists.collectAsState() // Use filtered artists to exclude artists with all songs blacklisted
    val playlists by viewModel.playlists.collectAsState()
    val currentSong by viewModel.currentSong.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val progress by viewModel.progress.collectAsState()
    val isShuffleEnabled by viewModel.isShuffleEnabled.collectAsState()
    val repeatMode by viewModel.repeatMode.collectAsState()
    val isFavorite by viewModel.isFavorite.collectAsState()
    val sortOrder by viewModel.sortOrder.collectAsState()
    val showLyrics by viewModel.showLyrics.collectAsState()
    val showOnlineOnlyLyrics by viewModel.showOnlineOnlyLyrics.collectAsState()
    val lyrics by viewModel.currentLyrics.collectAsState()
    val isLoadingLyrics by viewModel.isLoadingLyrics.collectAsState()
    val useExperimentalPlayerUi by appSettings.useExperimentalPlayerUi.collectAsState()
    val recentlyPlayed by viewModel.recentlyPlayed.collectAsState()
    val currentDevice by viewModel.currentDevice.collectAsState()
    val isMediaScanning by viewModel.isMediaScanning.collectAsState() // Add media scanning state

    // Theme state
    val useSystemTheme by themeViewModel.useSystemTheme.collectAsState()
    val darkMode by themeViewModel.darkMode.collectAsState()
    
    // Library tab order settings - used to determine first visible tab
    val libraryTabOrder by appSettings.libraryTabOrder.collectAsState()
    val hiddenLibraryTabs by appSettings.hiddenLibraryTabs.collectAsState()
    
    // Compute the first visible tab based on user's tab order (respecting hidden tabs)
    val firstVisibleLibraryTab = remember(libraryTabOrder, hiddenLibraryTabs) {
        val firstVisibleId = libraryTabOrder.firstOrNull { !hiddenLibraryTabs.contains(it) }
        when (firstVisibleId) {
            "SONGS" -> LibraryTab.SONGS
            "PLAYLISTS" -> LibraryTab.PLAYLISTS
            "ALBUMS" -> LibraryTab.ALBUMS
            "ARTISTS" -> LibraryTab.ARTISTS
            "EXPLORER" -> LibraryTab.EXPLORER
            else -> LibraryTab.SONGS // Default fallback
        }
    }
    
    // Default landing screen
    val defaultScreen by appSettings.defaultScreen.collectAsState()
    val floatingNavigationBar by appSettings.floatingNavigationBar.collectAsState()
    val startDestination = when (defaultScreen) {
        "library" -> Screen.Library.createRoute(firstVisibleLibraryTab)
        else -> Screen.Home.route
    }

    var selectedTab by remember { mutableIntStateOf(0) }

    val haptic = LocalHapticFeedback.current
    val context = LocalContext.current
    val windowSizeClass = calculateWindowSizeClass(context as android.app.Activity)
    val postureState by rememberDevicePosture()
    val isTablet = windowSizeClass.widthSizeClass >= WindowWidthSizeClass.Medium || postureState is DevicePosture.Book || postureState is DevicePosture.Separated

    val onPlayPause = { viewModel.togglePlayPause() }
    val onSkipNext = { viewModel.skipToNext() }
    val onSkipPrevious = { viewModel.skipToPrevious() }
    val onSeek = { value: Float -> viewModel.seekTo(value) }
    val onLyricsSeek: (Long) -> Unit = { timestampMs ->
        // Use the timestamp-based seekTo method directly for lyrics and auto-play if paused
        Log.d("RhythmNavigation", "Lyrics seek: timestampMs=$timestampMs")
        viewModel.seekTo(timestampMs, autoPlayIfPaused = true)
    }
    val onPlaySong = { song: com.qlzhj.rhythm.core.model.Song -> viewModel.playSong(song) }
    val onPlayAlbum = { album: com.qlzhj.rhythm.core.model.Album -> viewModel.playAlbum(album) }
    val onPlayAlbumShuffled = { album: com.qlzhj.rhythm.core.model.Album -> viewModel.playAlbumShuffled(album) }
    val onPlayArtist = { artist: com.qlzhj.rhythm.core.model.Artist -> viewModel.playArtist(artist) }
    val onPlayPlaylist =
        { playlist: com.qlzhj.rhythm.core.model.Playlist -> viewModel.playPlaylist(playlist) }
    val onPlayPlaylistShuffled = { playlist: com.qlzhj.rhythm.core.model.Playlist -> viewModel.playPlaylistShuffled(playlist) }
    val onToggleShuffle = { viewModel.toggleShuffle() }
    val onToggleRepeat = { viewModel.toggleRepeatMode() }
    val onToggleFavorite = { viewModel.toggleFavorite() }

    // Player click navigates to full player screen
    val onPlayerClick = {
        navController.navigate(Screen.Player.route)
    }

    // Track current destination for hiding navigation bar on player screen
    var currentRoute by remember { mutableStateOf(Screen.Home.route) }

    // Update current route when destination changes
    LaunchedEffect(navController) {
        navController.currentBackStackEntryFlow.collect { backStackEntry ->
            currentRoute = backStackEntry.destination.route ?: Screen.Home.route
            val routeBase = currentRoute.substringBefore("?")
            // Update selectedTab based on current route
            when {
                currentRoute == Screen.Home.route -> selectedTab = 0
                routeBase == Screen.Library.route.substringBefore("?") -> selectedTab = 1
            }
        }
    }

    // Consume pending startup route from shared settings handoff and navigate if valid.
    LaunchedEffect(navController, appSettings) {
        val pendingRoute = appSettings.consumeInitialStreamingRoute()
        if (!pendingRoute.isNullOrBlank()) {
            val isValidLocalRoute = pendingRoute == Screen.Home.route ||
                pendingRoute == Screen.Search.route ||
                pendingRoute == Screen.Player.route ||
                pendingRoute == Screen.Settings.route ||
                pendingRoute == Screen.RhythmStats.route ||
                pendingRoute.startsWith(Screen.Library.route.substringBefore("?")) ||
                pendingRoute.startsWith("playlist/") ||
                pendingRoute.startsWith("artist/") ||
                pendingRoute.startsWith("streaming_artist/") ||
                pendingRoute.startsWith("streaming_album/") ||
                pendingRoute.startsWith("streaming_playlist/") ||
                pendingRoute.startsWith("streaming_service_setup/") ||
                pendingRoute == StreamingRoutes.GoSettings

            if (isValidLocalRoute) {
                navController.navigate(pendingRoute) {
                    launchSingleTop = true
                }
            }
        }
    }

    val isLibraryRoute = remember(currentRoute) {
        currentRoute.substringBefore("?") == Screen.Library.route.substringBefore("?")
    }

    var isMiniPlayerDismissed by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(currentSong?.id, isPlaying) {
        if (currentSong != null && isPlaying) {
            isMiniPlayerDismissed = false
        }
    }

    // Provide dynamic mini-player padding with comprehensive navigation handling
    val showMiniPlayer =
        currentSong != null &&
            !isMiniPlayerDismissed &&
            currentRoute != Screen.Player.route &&
            currentRoute != Screen.Search.route
    val showNavBar = remember(currentRoute) {
        currentRoute == Screen.Home.route ||
            isLibraryRoute ||
            currentRoute == Screen.Search.route ||
            currentRoute == Screen.Settings.route ||
            currentRoute == Screen.RhythmStats.route
    }
    val showBottomNav = remember(currentRoute, floatingNavigationBar) {
        if (floatingNavigationBar) {
            currentRoute == Screen.Home.route || isLibraryRoute
        } else {
            currentRoute == Screen.Home.route ||
                isLibraryRoute ||
                currentRoute == Screen.Settings.route
        }
    }
    
    val systemNavBarPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val miniPlayerBottomPadding by animateDpAsState(
        targetValue = if (!isTablet) {
            val miniPlayerHeight = if (miniPlayerThemeId == "EXPRESSIVE") 84.dp else 96.dp
            val bottomNavigationHeight = if (floatingNavigationBar) MusicDimensions.bottomNavigationHeight else 80.dp
            val basePadding = when {
                showBottomNav && showMiniPlayer -> {
                    if (floatingNavigationBar) {
                        bottomNavigationHeight + 16.dp + miniPlayerHeight + 16.dp
                    } else {
                        bottomNavigationHeight + miniPlayerHeight + 16.dp
                    }
                }
                showBottomNav -> {
                    if (floatingNavigationBar) bottomNavigationHeight + 16.dp else bottomNavigationHeight
                }
                showMiniPlayer -> miniPlayerHeight + 16.dp
                else -> 0.dp
            }
            basePadding + systemNavBarPadding
        } else {
            if (showMiniPlayer) {
                val miniPlayerHeight = if (miniPlayerThemeId == "EXPRESSIVE") 84.dp else 96.dp
                miniPlayerHeight + 16.dp + systemNavBarPadding
            } else {
                0.dp
            }
        },
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "local_miniplayer_bottom_padding"
    )
    val tabletContentStartPadding by animateDpAsState(
        targetValue = if (isTablet && showNavBar) {
            if (floatingNavigationBar) 96.dp else 84.dp
        } else {
            0.dp
        },
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "local_tablet_content_start_padding"
    )

    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val miniPlayerPaddingValues = PaddingValues(bottom = miniPlayerBottomPadding.coerceAtLeast(0.dp))

    CompositionLocalProvider(LocalMiniPlayerPadding provides miniPlayerPaddingValues) {
        if (isTablet) {
            val showDockedRail = showNavBar && !floatingNavigationBar
            val tabletContainerColor = MaterialTheme.colorScheme.surfaceContainer
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(if (showDockedRail) tabletContainerColor else Color.Transparent)
            ) {
                // Main content wrapped in Scaffold (without bottom nav)
                LocalNavigationContent(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(start = tabletContentStartPadding)
                        .then(
                            if (showDockedRail) {
                                Modifier.clip(RoundedCornerShape(topStart = 24.dp, bottomStart = 24.dp))
                            } else Modifier
                        ),
                    navController = navController,
                    viewModel = viewModel,
                    themeViewModel = themeViewModel,
                    appSettings = appSettings,
                    snackbarHostState = snackbarHostState,
                    coroutineScope = coroutineScope,
                    currentSong = currentSong,
                    currentRoute = currentRoute,
                    isPlaying = isPlaying,
                    progress = { progress },
                    onPlayPause = onPlayPause,
                    onPlayerClick = onPlayerClick,
                    onSkipNext = onSkipNext,
                    onSkipPrevious = onSkipPrevious,
                    onMiniPlayerDismiss = {
                        isMiniPlayerDismissed = true
                        if (isPlaying) {
                            onPlayPause()
                        }
                        viewModel.clearQueue()
                    },
                    showMiniPlayer = showMiniPlayer,
                    showBottomNav = false,
                    isTablet = true,
                    startDestination = startDestination,
                    songs = songs,
                    allSongs = allSongs,
                    albums = albums,
                    artists = artists,
                    playlists = playlists,
                    isShuffleEnabled = isShuffleEnabled,
                    repeatMode = repeatMode,
                    isFavorite = isFavorite,
                    sortOrder = sortOrder,
                    showLyrics = showLyrics,
                    showOnlineOnlyLyrics = showOnlineOnlyLyrics,
                    lyrics = lyrics,
                    isLoadingLyrics = isLoadingLyrics,
                    recentlyPlayed = recentlyPlayed,
                    currentDevice = currentDevice,
                    isMediaScanning = isMediaScanning,
                    useSystemTheme = useSystemTheme,
                    darkMode = darkMode,
                    useExperimentalPlayerUi = useExperimentalPlayerUi,
                    libraryTabOrder = libraryTabOrder,
                    hiddenLibraryTabs = hiddenLibraryTabs,
                    firstVisibleLibraryTab = firstVisibleLibraryTab,
                    onPlaySong = onPlaySong,
                    onPlayAlbum = onPlayAlbum,
                    onPlayAlbumShuffled = onPlayAlbumShuffled,
                    onPlayArtist = onPlayArtist,
                    onPlayPlaylist = onPlayPlaylist,
                    onPlayPlaylistShuffled = onPlayPlaylistShuffled,
                    onToggleShuffle = onToggleShuffle,
                    onToggleRepeat = onToggleRepeat,
                    onToggleFavorite = onToggleFavorite,
                    onSeek = onSeek,
                    onLyricsSeek = onLyricsSeek,
                    streamingMusicViewModel = streamingMusicViewModel
                )

                AnimatedVisibility(
                    visible = showNavBar,
                    enter = slideInHorizontally(
                        initialOffsetX = { -it },
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessLow
                        )
                    ) + fadeIn(
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessLow
                        )
                    ),
                    exit = slideOutHorizontally(
                        targetOffsetX = { -it },
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioNoBouncy,
                            stiffness = Spring.StiffnessLow
                        )
                    ) + fadeOut(
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioNoBouncy,
                            stiffness = Spring.StiffnessLow
                        )
                    )
                ) {
                    LocalNavigationRail(
                        currentRoute = currentRoute,
                        navController = navController,
                        firstVisibleLibraryTab = firstVisibleLibraryTab,
                        context = context,
                        haptic = haptic,
                        isFloating = floatingNavigationBar
                    )
                }
            }
        } else {
            // Main content wrapped in Scaffold for phone
            LocalNavigationContent(
                modifier = Modifier.fillMaxSize(),
                navController = navController,
                viewModel = viewModel,
                themeViewModel = themeViewModel,
                appSettings = appSettings,
                snackbarHostState = snackbarHostState,
                coroutineScope = coroutineScope,
                currentSong = currentSong,
                currentRoute = currentRoute,
                isPlaying = isPlaying,
                progress = { progress },
                onPlayPause = onPlayPause,
                onPlayerClick = onPlayerClick,
                onSkipNext = onSkipNext,
                onSkipPrevious = onSkipPrevious,
                onMiniPlayerDismiss = {
                    isMiniPlayerDismissed = true
                    if (isPlaying) {
                        onPlayPause()
                    }
                    viewModel.clearQueue()
                },
                showMiniPlayer = showMiniPlayer,
                showBottomNav = showBottomNav,
                isTablet = false,
                startDestination = startDestination,
                songs = songs,
                allSongs = allSongs,
                albums = albums,
                artists = artists,
                playlists = playlists,
                isShuffleEnabled = isShuffleEnabled,
                repeatMode = repeatMode,
                isFavorite = isFavorite,
                sortOrder = sortOrder,
                showLyrics = showLyrics,
                showOnlineOnlyLyrics = showOnlineOnlyLyrics,
                lyrics = lyrics,
                isLoadingLyrics = isLoadingLyrics,
                recentlyPlayed = recentlyPlayed,
                currentDevice = currentDevice,
                isMediaScanning = isMediaScanning,
                useSystemTheme = useSystemTheme,
                darkMode = darkMode,
                useExperimentalPlayerUi = useExperimentalPlayerUi,
                libraryTabOrder = libraryTabOrder,
                hiddenLibraryTabs = hiddenLibraryTabs,
                firstVisibleLibraryTab = firstVisibleLibraryTab,
                onPlaySong = onPlaySong,
                onPlayAlbum = onPlayAlbum,
                onPlayAlbumShuffled = onPlayAlbumShuffled,
                onPlayArtist = onPlayArtist,
                onPlayPlaylist = onPlayPlaylist,
                onPlayPlaylistShuffled = onPlayPlaylistShuffled,
                onToggleShuffle = onToggleShuffle,
                onToggleRepeat = onToggleRepeat,
                onToggleFavorite = onToggleFavorite,
                onSeek = onSeek,
                onLyricsSeek = onLyricsSeek,
                streamingMusicViewModel = streamingMusicViewModel
            )
        }
    }
    
    // Global Update Bottom Sheet - shows automatically when update is available
    val isOnUpdatesScreen = currentRoute == Screen.TunerUpdates.route
    if (showUpdateBottomSheet && !isOnUpdatesScreen) {
        UpdateBottomSheet(
            updaterViewModel = updaterViewModel,
            onDismiss = { showUpdateBottomSheet = false },
            onUpdateClick = { immediate ->
                showUpdateBottomSheet = false
                // Navigate to update settings
                navController.navigate(Screen.TunerUpdates.route) {
                    launchSingleTop = true
                }
            }
        )
    }
}

@Composable
private fun LocalNavigationContent(
    modifier: Modifier = Modifier,
    navController: NavHostController,
    viewModel: MusicViewModel,
    themeViewModel: ThemeViewModel,
    appSettings: com.qlzhj.rhythm.core.data.shared.data.model.AppSettings,
    snackbarHostState: SnackbarHostState,
    coroutineScope: kotlinx.coroutines.CoroutineScope,
    currentSong: com.qlzhj.rhythm.core.model.Song?,
    currentRoute: String,
    isPlaying: Boolean,
    progress: () -> Float,
    onPlayPause: () -> Unit,
    onPlayerClick: () -> Unit,
    onSkipNext: () -> Unit,
    onSkipPrevious: () -> Unit,
    onMiniPlayerDismiss: () -> Unit,
    showMiniPlayer: Boolean,
    showBottomNav: Boolean,
    isTablet: Boolean,
    startDestination: String,
    songs: List<com.qlzhj.rhythm.core.model.Song>,
    allSongs: List<com.qlzhj.rhythm.core.model.Song>,
    albums: List<com.qlzhj.rhythm.core.model.Album>,
    artists: List<com.qlzhj.rhythm.core.model.Artist>,
    playlists: List<com.qlzhj.rhythm.core.model.Playlist>,
    isShuffleEnabled: Boolean,
    repeatMode: Int,
    isFavorite: Boolean,
    sortOrder: MusicViewModel.SortOrder,
    showLyrics: Boolean,
    showOnlineOnlyLyrics: Boolean,
    lyrics: com.qlzhj.rhythm.core.model.LyricsData?,
    isLoadingLyrics: Boolean,
    recentlyPlayed: List<com.qlzhj.rhythm.core.model.Song>,
    currentDevice: PlaybackLocation?,
    isMediaScanning: Boolean,
    useSystemTheme: Boolean,
    darkMode: Boolean,
    useExperimentalPlayerUi: Boolean,
    libraryTabOrder: List<String>,
    hiddenLibraryTabs: Set<String>,
    firstVisibleLibraryTab: LibraryTab,
    onPlaySong: (com.qlzhj.rhythm.core.model.Song) -> Unit,
    onPlayAlbum: (com.qlzhj.rhythm.core.model.Album) -> Unit,
    onPlayAlbumShuffled: (com.qlzhj.rhythm.core.model.Album) -> Unit,
    onPlayArtist: (com.qlzhj.rhythm.core.model.Artist) -> Unit,
    onPlayPlaylist: (com.qlzhj.rhythm.core.model.Playlist) -> Unit,
    onPlayPlaylistShuffled: (com.qlzhj.rhythm.core.model.Playlist) -> Unit,
    onToggleShuffle: () -> Unit,
    onToggleRepeat: () -> Unit,
    onToggleFavorite: () -> Unit,
    onSeek: (Float) -> Unit,
    onLyricsSeek: (Long) -> Unit,
    streamingMusicViewModel: StreamingMusicViewModel
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current

    val appMode by appSettings.appMode.collectAsState()
    val isStreamingMode = appMode == "STREAMING"
    val floatingNavigationBar by appSettings.floatingNavigationBar.collectAsState()
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
    val streamingCurrentSong by streamingMusicViewModel.currentSong.collectAsState()
    val streamingLikedSongIds = remember(streamingLikedSongs) {
        streamingLikedSongs.map { it.id }.toSet()
    }
    val streamingDownloadedSongIds = remember(streamingDownloadedSongs) {
        streamingDownloadedSongs.map { it.id }.toSet()
    }
    val streamingAlbumCatalog = remember(streamingSavedAlbums, streamingNewReleases, streamingDownloadedAlbums) {
        (streamingSavedAlbums + streamingNewReleases + streamingDownloadedAlbums).distinctBy { it.id }
    }
    val playbackStatsSummary by viewModel.playbackStatsSummary.collectAsState()
    val listeningTime by viewModel.listeningTime.collectAsState()

    val showStreamingAddToPlaylistState = remember { mutableStateOf(false) }
    var showStreamingAddToPlaylist by showStreamingAddToPlaylistState
    val selectedStreamingSongForPlaylistState = remember { mutableStateOf<StreamingSong?>(null) }
    var selectedStreamingSongForPlaylist by selectedStreamingSongForPlaylistState
    val onStreamingAddSongToPlaylist: (StreamingSong) -> Unit = { song ->
        selectedStreamingSongForPlaylist = song
        showStreamingAddToPlaylist = true
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
    val streamingSyncProgress by streamingMusicViewModel.syncProgress.collectAsState()
    val streamingHasLoadedHomeContent by streamingMusicViewModel.hasLoadedHomeContent.collectAsState()
    val streamingHasLoadedLibrary by streamingMusicViewModel.hasLoadedLibrary.collectAsState()
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
    val playStreamingMappedQueue: (List<com.qlzhj.rhythm.core.model.Song>, Int, Boolean) -> Unit =
        { mappedSongs, startIndex, shuffle ->
            val originals = mappedSongs.mapNotNull { streamingSongById[it.id] }
            if (originals.isNotEmpty()) {
                streamingMusicViewModel.playQueue(originals, startIndex, shuffle, pinStartIndex = shuffle)
            }
        }
    val streamingMappedSongs by rememberOffMain(emptyList<com.qlzhj.rhythm.core.model.Song>(), streamingServiceConnected, streamingAllSongs, streamingRecommendations, streamingLikedSongs, streamingDownloadedSongs) {
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
    val streamingMappedArtists by rememberOffMain(emptyList<com.qlzhj.rhythm.core.model.Artist>(), streamingServiceConnected, streamingFollowedArtists, streamingAllSongs, streamingDownloadedArtists, streamingDownloadedSongs) {
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

    // Route streaming playback through the shared local player (streaming songs map to local songs).
    LaunchedEffect(streamingMusicViewModel, viewModel, navController) {
        streamingMusicViewModel.setPlaybackHandler { streamingQueue, startIndex ->
            val mappedQueue = streamingQueue.mapNotNull { it.toLocalSong() }
            if (mappedQueue.isEmpty()) {
                streamingMusicViewModel.reportError(
                    "Playback failed: Unable to convert streaming songs for playback. " +
                        "Try reconnecting to your service."
                )
                return@setPlaybackHandler
            }
            if (streamingQueue.size != mappedQueue.size) {
                streamingMusicViewModel.reportWarning(
                    "Some songs in queue couldn't be loaded for playback."
                )
            }
            val safeIndex = startIndex.coerceIn(0, mappedQueue.lastIndex)
            viewModel.playSongFromSearch(mappedQueue[safeIndex], mappedQueue)
            navController.navigate(Screen.Player.route) {
                launchSingleTop = true
            }
        }
        streamingMusicViewModel.setSeekHandlers(
            progressHandler = { progress -> viewModel.seekTo(progress) },
            positionHandler = { positionMs -> viewModel.seekTo(positionMs) }
        )
    }

    if (showStreamingAddToPlaylist && selectedStreamingSongForPlaylist != null) {
        val displaySong = remember(selectedStreamingSongForPlaylist) {
            selectedStreamingSongForPlaylist!!.toDisplaySong()
        }
        val displayPlaylists = remember(streamingSavedPlaylists) {
            streamingSavedPlaylists.map { it.toLibraryPlaylist(context, placeholderTrackTitle = { i -> context.getString(chromahub.rhythm.app.R.string.streaming_placeholder_track_format, i) }) }
        }
        var showCreateStreamingPlaylistDialog by remember { mutableStateOf(false) }

        if (showCreateStreamingPlaylistDialog) {
            CreatePlaylistDialog(
                onDismiss = { showCreateStreamingPlaylistDialog = false },
                onConfirm = { name ->
                    streamingMusicViewModel.createPlaylist(name)
                    showCreateStreamingPlaylistDialog = false
                },
                song = selectedStreamingSongForPlaylist?.toLocalSong(),
                onConfirmWithSong = { name ->
                    selectedStreamingSongForPlaylist?.let { streamingSong ->
                        streamingMusicViewModel.createPlaylist(name, listOf(streamingSong))
                    } ?: run {
                        streamingMusicViewModel.createPlaylist(name)
                    }
                    showCreateStreamingPlaylistDialog = false
                }
            )
        }

        AddToPlaylistBottomSheet(
            song = displaySong,
            playlists = displayPlaylists,
            onDismissRequest = {
                showStreamingAddToPlaylist = false
                selectedStreamingSongForPlaylist = null
            },
            onAddToPlaylist = { playlist ->
                selectedStreamingSongForPlaylist?.let { song ->
                    streamingMusicViewModel.addSongToPlaylist(playlist.id, song)
                }
                showStreamingAddToPlaylist = false
                selectedStreamingSongForPlaylist = null
            },
            onCreateNewPlaylist = {
                showCreateStreamingPlaylistDialog = true
            }
        )
    }

    val showAddToPlaylistSheet = remember { mutableStateOf(false) }
    val showCreatePlaylistDialog = remember { mutableStateOf(false) }
    val songForCreatePlaylistDialog = remember { mutableStateOf<com.qlzhj.rhythm.core.model.Song?>(null) }

    LaunchedEffect(viewModel.selectedSongForPlaylist.collectAsState().value) {
        if (viewModel.selectedSongForPlaylist.value != null) {
            showAddToPlaylistSheet.value = true
        }
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

    val isLibraryRoute = remember(currentRoute) {
        currentRoute.substringBefore("?") == Screen.Library.route.substringBefore("?")
    }
    val isLibraryStartDestination = remember(startDestination) {
        startDestination.substringBefore("?") == Screen.Library.route.substringBefore("?")
    }

    BackHandler(enabled = isLibraryRoute && !isLibraryStartDestination) {
        navigateToTopLevel(Screen.Home.route)
    }

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
    val navigateToLanding: () -> Unit = {
        navController.navigate(startDestination) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }
    val navigateBackOrToSettings: () -> Unit = {
        val popped = navController.popBackStack()
        if (!popped) {
            navigateToTopLevel(Screen.Settings.route)
        }
    }

    val miniPlayerThemeId by appSettings.miniPlayerThemeId.collectAsState()
    val respectAlbumOnPlay by appSettings.respectAlbumOnPlay.collectAsState()

    Scaffold(
        modifier = modifier,
        snackbarHost = {},
        bottomBar = {
            val bottomChromeVisible = showMiniPlayer || showBottomNav
            val bottomChromeAlpha by animateFloatAsState(
                targetValue = if (bottomChromeVisible) 1f else 0f,
                animationSpec = tween(durationMillis = 220),
                label = "local_bottom_chrome_alpha"
            )
            val systemNavBarPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
            val miniPlayerBottomOffset by animateDpAsState(
                targetValue = when {
                    showBottomNav -> MusicDimensions.bottomNavigationHeight + 12.dp + systemNavBarPadding
                    currentRoute == Screen.Search.route -> 88.dp + systemNavBarPadding
                    else -> 8.dp + systemNavBarPadding
                },
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessLow
                ),
                label = "local_miniplayer_bottom_offset"
            )

            val miniPlayerHeight = if (miniPlayerThemeId == "EXPRESSIVE") 84.dp else 110.dp
            val gradientHeight by animateDpAsState(
                targetValue = when {
                    showBottomNav && showMiniPlayer -> MusicDimensions.bottomNavigationHeight + 16.dp + miniPlayerHeight + 32.dp
                    showBottomNav -> MusicDimensions.bottomNavigationHeight + 32.dp
                    showMiniPlayer -> miniPlayerHeight + 32.dp
                    else -> 0.dp
                },
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessLow
                ),
                label = "local_navigation_gradient_height"
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
            ) {
                if (gradientHeight > 0.dp && !isTablet) {
                    val systemNavBarPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(gradientHeight + systemNavBarPadding)
                            .align(Alignment.BottomCenter)
                            .graphicsLayer { alpha = bottomChromeAlpha }
                            .background(
                                brush = Brush.verticalGradient(
                                    colorStops = arrayOf(
                                        0f to Color.Transparent,
                                        0.25f to MaterialTheme.colorScheme.background.copy(alpha = 0.28f),
                                        0.62f to MaterialTheme.colorScheme.background.copy(alpha = 0.76f),
                                        1f to MaterialTheme.colorScheme.background.copy(alpha = 1f)
                                    )
                                )
                            )
                    )
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                ) {

                // Global RhythmPlayerSheet
                val showPlayerSheet = currentSong != null && (showMiniPlayer || currentRoute == Screen.Player.route)
                if (showPlayerSheet) {
                    val bottomBarHeightPx = with(LocalDensity.current) { MusicDimensions.bottomNavigationHeight.roundToPx() }
                    RhythmPlayerSheet(
                        isExpanded = currentRoute == Screen.Player.route,
                        onExpand = {
                            viewModel.refreshAudioDevices()
                            navController.navigate(Screen.Player.route) {
                                launchSingleTop = true
                            }
                        },
                        onCollapse = {
                            try {
                                val hasPlayerEntry = try {
                                    navController.getBackStackEntry(Screen.Player.route)
                                    true
                                } catch (_: IllegalArgumentException) {
                                    false
                                }
                                if (hasPlayerEntry) {
                                    navController.popBackStack(Screen.Player.route, inclusive = true)
                                }
                            } catch (_: Exception) { }
                        },
                        onMiniPlayerDismiss = {
                            onMiniPlayerDismiss()
                        },
                        song = currentSong,
                        isPlaying = isPlaying,
                        progress = progress,
                        location = currentDevice,
                        queuePosition = viewModel.currentQueue.collectAsState().value.currentIndex + 1,
                        queueTotal = viewModel.currentQueue.collectAsState().value.songs.size,
                        onPlayPause = onPlayPause,
                        onSkipNext = onSkipNext,
                        onSkipPrevious = onSkipPrevious,
                        onSeek = { position ->
                            viewModel.seekTo(position)
                        },
                        onLyricsSeek = onLyricsSeek,
                        onLocationClick = {
                            viewModel.showOutputSwitcherDialog()
                        },
                        onQueueClick = {
                            // Handled internally by PlayerScreen
                        },
                        isShuffleEnabled = isShuffleEnabled,
                        repeatMode = repeatMode,
                        isFavorite = if (isStreamingMode) streamingCurrentSong?.let { streamingLikedSongIds.contains(it.id) } ?: false else isFavorite,
                        onToggleFavorite = if (isStreamingMode) {
                            {
                                val s = streamingCurrentSong
                                if (s != null) {
                                    val isLiked = streamingLikedSongIds.contains(s.id)
                                    if (isLiked) streamingMusicViewModel.unlikeSong(s)
                                    else streamingMusicViewModel.likeSong(s)
                                }
                            }
                        } else onToggleFavorite,
                        onToggleShuffle = onToggleShuffle,
                        onToggleRepeat = onToggleRepeat,
                        onAddToPlaylist = { showAddToPlaylistSheet.value = true },
                        showLyrics = showLyrics,
                        onlineOnlyLyrics = showOnlineOnlyLyrics,
                        lyrics = lyrics,
                        isLoadingLyrics = isLoadingLyrics,
                        onRetryLyrics = {
                            viewModel.retryFetchLyrics()
                        },
                        volume = viewModel.volume.collectAsState().value,
                        isMuted = viewModel.isMuted.collectAsState().value,
                        onVolumeChange = { volume ->
                            viewModel.setVolume(volume)
                        },
                        onToggleMute = {
                            viewModel.toggleMute()
                        },
                        onRefreshDevices = {
                            viewModel.startDeviceMonitoringOnDemand()
                        },
                        onStopDeviceMonitoring = {
                            viewModel.stopDeviceMonitoringOnDemand()
                        },
                        locations = viewModel.locations.collectAsState().value,
                        onLocationSelect = { location ->
                            viewModel.setCurrentDevice(location)
                        },
                        onMaxVolume = {
                            viewModel.maxVolume()
                        },
                        playlists = playlists,
                        queue = viewModel.currentQueue.collectAsState().value.songs,
                        onSongClick = { song ->
                            viewModel.playSong(song)
                        },
                        onSongClickAtIndex = { index ->
                            viewModel.playSongAtIndex(index)
                        },
                        onRemoveFromQueueAtIndex = { index ->
                            viewModel.removeFromQueueAtIndex(index)
                        },
                        onMoveQueueItem = { fromIndex, toIndex ->
                            viewModel.moveQueueItem(fromIndex, toIndex)
                        },
                        onAddSongsToQueue = {
                            viewModel.addSongsToQueue()
                        },
                        onNavigateToLibrary = { tab ->
                            navController.navigate(Screen.Library.createRoute(tab)) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        showAddToPlaylistSheet = showAddToPlaylistSheet.value,
                        onAddToPlaylistSheetDismiss = {
                            showAddToPlaylistSheet.value = false
                            viewModel.clearSelectedSongForPlaylist()
                        },
                        onAddSongToPlaylist = { song, playlistId ->
                            viewModel.addSongToPlaylist(song, playlistId) { message ->
                                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                            }
                        },
                        onCreatePlaylist = { name ->
                            viewModel.createPlaylist(name)
                        },
                        onShowCreatePlaylistDialog = { song ->
                            songForCreatePlaylistDialog.value = song
                            showCreatePlaylistDialog.value = true
                        },
                        onClearQueue = {
                            viewModel.clearQueue()
                        },
                        isMediaLoading = viewModel.isBuffering.collectAsState().value,
                        isSeeking = viewModel.isSeeking.collectAsState().value,
                        songs = viewModel.filteredSongs.collectAsState().value,
                        albums = viewModel.albums.collectAsState().value,
                        artists = viewModel.artists.collectAsState().value,
                        onPlayAlbumSongs = { songs -> viewModel.playSongs(songs) },
                        onShuffleAlbumSongs = { songs -> viewModel.playShuffled(songs) },
                        onPlayArtistSongs = { songs -> viewModel.playSongs(songs) },
                        onShuffleArtistSongs = { songs -> viewModel.playShuffled(songs) },
                        appSettings = appSettings,
                        musicViewModel = viewModel,
                        navController = navController,
                        isStreamingMode = isStreamingMode,
                        miniPlayerBottomOffset = miniPlayerBottomOffset
                    )
                }

                // Navigation bar shown only on specific routes with spring animation
                AnimatedVisibility(
                    visible = showBottomNav,
                    modifier = Modifier.align(Alignment.BottomCenter),
                    enter = slideInVertically(
                        initialOffsetY = { fullHeight -> fullHeight / 2 },
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessLow
                        )
                    ) + fadeIn(
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessLow
                        )
                    ),
                    exit = slideOutVertically(
                        targetOffsetY = { fullHeight -> fullHeight / 2 },
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioNoBouncy,
                            stiffness = Spring.StiffnessLow
                        )
                    ) + fadeOut(
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioNoBouncy,
                            stiffness = Spring.StiffnessLow
                        )
                    )
                ) {
                    if (floatingNavigationBar) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .windowInsetsPadding(WindowInsets.navigationBars)
                                .padding(top = 8.dp, bottom = 8.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp)
                                    .align(Alignment.BottomCenter),
                                verticalAlignment = Alignment.Bottom
                            ) {
                                // Expressive Navigation bar Surface with pill shape
                                Surface(
                                    color = MaterialTheme.colorScheme.surfaceContainer,
                                    shape = ExpressiveShapes.Full, // Full pill shape for expressive design
                                    tonalElevation = 3.dp,
                                    shadowElevation = 0.dp,
                                    modifier = Modifier
                                        .height(MusicDimensions.bottomNavigationHeight)
                                        .weight(1f) // Make it take up available space
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxSize(),
                                        horizontalArrangement = Arrangement.SpaceEvenly,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // Use first visible library tab based on user's tab order
                                        val libraryRoute =
                                            Screen.Library.createRoute(firstVisibleLibraryTab)
                                        val items = listOf(
                                             Triple(
                                                 Screen.Home.route, context.getString(R.string.common_home),
                                                 Pair(RhythmIcons.HomeFilled, RhythmIcons.Home)
                                             ),
                                             Triple(
                                                 libraryRoute, context.getString(R.string.common_library),
                                                 Pair(RhythmIcons.Navigation.Library, RhythmIcons.Navigation.LibraryOutlined)
                                             )
                                         )

                                         items.forEachIndexed { index, (route, title, icons) ->
                                             val isSelected = when (route) {
                                                 Screen.Home.route -> currentRoute == Screen.Home.route
                                                 libraryRoute -> currentRoute.startsWith("library")
                                                 else -> false
                                             }

                                            val (selectedIcon, unselectedIcon) = icons

                                            // Enhanced animation values with spring physics
                                            val animatedScale by animateFloatAsState(
                                                targetValue = if (isSelected) 1.05f else 1.0f,
                                                animationSpec = spring(
                                                    dampingRatio = Spring.DampingRatioMediumBouncy,
                                                    stiffness = Spring.StiffnessLow
                                                ),
                                                label = "scale_$title"
                                            )

                                            val animatedAlpha by animateFloatAsState(
                                                targetValue = if (isSelected) 1f else 0.7f,
                                                animationSpec = spring(
                                                    dampingRatio = Spring.DampingRatioNoBouncy,
                                                    stiffness = Spring.StiffnessLow
                                                ),
                                                label = "alpha_$title"
                                            )

                                            // Background pill animation with spring
                                            val pillWidth by animateDpAsState(
                                                targetValue = if (isSelected) 120.dp else 0.dp,
                                                animationSpec = spring(
                                                    dampingRatio = Spring.DampingRatioMediumBouncy,
                                                    stiffness = Spring.StiffnessLow
                                                ),
                                                label = "pillWidth_$title"
                                            )

                                            val itemWeight by animateFloatAsState(
                                                targetValue = if (isSelected) 1.8f else 1f,
                                                animationSpec = spring(
                                                    dampingRatio = Spring.DampingRatioMediumBouncy,
                                                    stiffness = Spring.StiffnessLow
                                                ),
                                                label = "weight_$title"
                                            )

                                            val iconColor by animateColorAsState(
                                                targetValue = if (isSelected)
                                                    MaterialTheme.colorScheme.onPrimaryContainer
                                                else
                                                    MaterialTheme.colorScheme.onSurfaceVariant,
                                                animationSpec = tween(300),
                                                label = "iconColor_$title"
                                            )

                                            val haptic = LocalHapticFeedback.current

                                            Box(
                                                modifier = Modifier
                                                    .weight(itemWeight)
                                                    .fillMaxHeight()
                                                    .clickable {
                                                        HapticUtils.performHapticFeedback(
                                                            context,
                                                            haptic,
                                                            HapticType.HEAVY
                                                        )
                                                        navController.navigate(route) {
                                                            popUpTo(navController.graph.findStartDestination().id) {
                                                                saveState = true
                                                            }
                                                            launchSingleTop = true
                                                            restoreState = true
                                                        }
                                                    },
                                                contentAlignment = Alignment.Center
                                            ) {
                                                // Horizontal layout for icon and text with animated pill background
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.Center,
                                                    modifier = Modifier
                                                        .graphicsLayer {
                                                            scaleX = animatedScale
                                                            scaleY = animatedScale
                                                            alpha = animatedAlpha
                                                        }
                                                        .then(
                                                            if (isSelected) Modifier
                                                                .clip(ExpressiveShapes.Full) // Expressive pill shape
                                                                .background(MaterialTheme.colorScheme.primaryContainer)
                                                                .height(48.dp)
                                                                .widthIn(min = pillWidth) // Animated width
                                                                .padding(horizontal = 18.dp)
                                                            else Modifier.padding(horizontal = 16.dp)
                                                        )
                                                ) {
                                                    // Animated icon with crossfade
                                                    androidx.compose.animation.Crossfade(
                                                        targetState = isSelected,
                                                        animationSpec = spring(
                                                            dampingRatio = Spring.DampingRatioMediumBouncy,
                                                            stiffness = Spring.StiffnessVeryLow
                                                        ),
                                                        label = "iconCrossfade_$title"
                                                    ) { selected ->
                                                        Icon(
                                                            imageVector = if (selected) selectedIcon else unselectedIcon,
                                                            contentDescription = title,
                                                            tint = iconColor,
                                                            modifier = Modifier.size(24.dp)
                                                        )
                                                    }

                                                    AnimatedVisibility(
                                                        visible = isSelected,
                                                        enter = fadeIn(
                                                            animationSpec = spring(
                                                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                                                stiffness = Spring.StiffnessMedium
                                                            )
                                                        ) + expandHorizontally(
                                                            animationSpec = spring(
                                                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                                                stiffness = Spring.StiffnessLow
                                                            )
                                                        ),
                                                        exit = fadeOut(
                                                            animationSpec = spring(
                                                                dampingRatio = Spring.DampingRatioNoBouncy,
                                                                stiffness = Spring.StiffnessLow
                                                            )
                                                        ) + shrinkHorizontally(
                                                            animationSpec = spring(
                                                                dampingRatio = Spring.DampingRatioNoBouncy,
                                                                stiffness = Spring.StiffnessLow
                                                            )
                                                        )
                                                    ) {
                                                        Row {
                                                            Spacer(modifier = Modifier.width(8.dp))
                                                            Text(
                                                                text = title,
                                                                style = MaterialTheme.typography.labelMedium,
                                                                color = iconColor,
                                                                maxLines = 1,
                                                                overflow = TextOverflow.Ellipsis
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.width(12.dp)) // Gap between nav bar and search icon

                                // Expressive Search Icon Button with bouncy animation
                                val searchInteractionSource = remember { MutableInteractionSource() }
                                val isSearchPressed by searchInteractionSource.collectIsPressedAsState()
                                val searchScale by animateFloatAsState(
                                    targetValue = if (isSearchPressed) 0.88f else 1f,
                                    animationSpec = spring(
                                        dampingRatio = Spring.DampingRatioMediumBouncy,
                                        stiffness = Spring.StiffnessMedium
                                    ),
                                    label = "search_scale"
                                )
                                
                                FilledIconButton(
                                    onClick = {
                                        HapticUtils.performHapticFeedback(
                                            context,
                                            haptic,
                                            HapticType.HEAVY
                                        )
                                        navController.navigate(Screen.Search.route) {
                                            popUpTo(navController.graph.findStartDestination().id) {
                                                saveState = true
                                            }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    },
                                    colors = IconButtonDefaults.filledIconButtonColors(
                                        containerColor = MaterialTheme.colorScheme.primary,
                                        contentColor = MaterialTheme.colorScheme.onPrimary
                                    ),
                                    shape = ExpressiveShapes.Full,
                                    interactionSource = searchInteractionSource,
                                    modifier = Modifier
                                        .size(MusicDimensions.bottomNavigationHeight) // Match height of navigation bar
                                        .graphicsLayer {
                                            scaleX = searchScale
                                            scaleY = searchScale
                                        }
                                ) {
                                    Icon(
                                        imageVector = RhythmIcons.Search,
                                        contentDescription = stringResource(R.string.cd_search),
                                        modifier = Modifier.size(25.dp)
                                    )
                                }
                            }
                        }
                    } else {
                        // Docked navigation bar
                        val libraryRoute = Screen.Library.createRoute(firstVisibleLibraryTab)
                        val dockedItems = listOf(
                            Triple(
                                Screen.Home.route,
                                context.getString(R.string.common_home),
                                Pair(RhythmIcons.HomeFilled, RhythmIcons.Home)
                            ),
                            Triple(
                                libraryRoute,
                                context.getString(R.string.common_library),
                                Pair(RhythmIcons.Navigation.Library, RhythmIcons.Navigation.LibraryOutlined)
                            ),
                            Triple(
                                Screen.Search.route,
                                stringResource(R.string.cd_search),
                                Pair(RhythmIcons.SearchFilled, RhythmIcons.Search)
                            ),
                            Triple(
                                Screen.Settings.route,
                                context.getString(R.string.settings_title),
                                Pair(RhythmIcons.SettingsFilled, RhythmIcons.Settings)
                            )
                        )

                        val navContainerColor = MaterialTheme.colorScheme.surfaceContainer
                        val cornerRadius = 24.dp

                        Box(modifier = Modifier.fillMaxWidth()) {
                            // Left inverted corner fillet
                            Canvas(
                                modifier = Modifier
                                    .size(cornerRadius)
                                    .align(Alignment.TopStart)
                                    .offset(y = -cornerRadius)
                            ) {
                                val path = Path().apply {
                                    moveTo(0f, 0f)
                                    lineTo(0f, size.height)
                                    lineTo(size.width, size.height)
                                    arcTo(
                                        rect = Rect(0f, -size.height, size.width * 2, size.height),
                                        startAngleDegrees = 90f,
                                        sweepAngleDegrees = 90f,
                                        forceMoveTo = false
                                    )
                                    close()
                                }
                                drawPath(path, color = navContainerColor)
                            }

                            // Right inverted corner fillet
                            Canvas(
                                modifier = Modifier
                                    .size(cornerRadius)
                                    .align(Alignment.TopEnd)
                                    .offset(y = -cornerRadius)
                            ) {
                                val path = Path().apply {
                                    moveTo(size.width, 0f)
                                    lineTo(size.width, size.height)
                                    lineTo(0f, size.height)
                                    arcTo(
                                        rect = Rect(-size.width, -size.height, size.width, size.height),
                                        startAngleDegrees = 90f,
                                        sweepAngleDegrees = -90f,
                                        forceMoveTo = false
                                    )
                                    close()
                                }
                                drawPath(path, color = navContainerColor)
                            }

                            NavigationBar(
                                modifier = Modifier.fillMaxWidth(),
                                containerColor = navContainerColor,
                                windowInsets = WindowInsets.navigationBars
                            ) {
                                Spacer(Modifier.width(8.dp))
                                dockedItems.forEach { (route, title, icons) ->
                                    val isSelected = when (route) {
                                        Screen.Home.route -> currentRoute == Screen.Home.route
                                        libraryRoute -> currentRoute.startsWith("library")
                                        Screen.Search.route -> currentRoute == Screen.Search.route
                                        Screen.Settings.route -> currentRoute == Screen.Settings.route
                                        else -> false
                                    }
                                    val (selectedIcon, unselectedIcon) = icons

                                    NavigationBarItem(
                                        selected = isSelected,
                                        onClick = {
                                            HapticUtils.performHapticFeedback(
                                                context,
                                                haptic,
                                                HapticType.HEAVY
                                            )
                                            navController.navigate(route) {
                                                popUpTo(navController.graph.findStartDestination().id) {
                                                    saveState = true
                                                }
                                                launchSingleTop = true
                                                restoreState = true
                                            }
                                        },
                                        icon = {
                                            Icon(
                                                imageVector = if (isSelected) selectedIcon else unselectedIcon,
                                                contentDescription = title,
                                                modifier = Modifier.size(24.dp)
                                            )
                                        },
                                        label = {
                                            Text(
                                                text = title,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    )
                                }
                                Spacer(Modifier.width(8.dp))
                            }
                        }
                    }
                }
            }
        }
    }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize()) {
            // Main content
            NavHost(
                navController = navController,
                startDestination = startDestination,
                modifier = Modifier
                    .fillMaxSize(),
                predictivePopEnterTransition = { swipeEdge ->
                    when {
                        targetState.destination.route == Screen.Player.route -> {
                            EnterTransition.None
                        }
                        targetState.destination.route?.startsWith("home") == true &&
                            initialState.destination.route?.startsWith("library") == true -> {
                            fadeIn(animationSpec = tween(300)) +
                                slideInHorizontally(
                                    initialOffsetX = { -it },
                                    animationSpec = tween(350, easing = EaseInOutQuart)
                                )
                        }
                        targetState.destination.route?.startsWith("library") == true &&
                            initialState.destination.route?.startsWith("home") == true -> {
                            fadeIn(animationSpec = tween(300)) +
                                slideInHorizontally(
                                    initialOffsetX = { it },
                                    animationSpec = tween(350, easing = EaseInOutQuart)
                                )
                        }
                        else -> {
                            fadeIn(animationSpec = tween(250))
                        }
                    }
                },
                predictivePopExitTransition = { swipeEdge ->
                    when {
                        initialState.destination.route == Screen.Player.route -> {
                            ExitTransition.None
                        }
                        initialState.destination.route?.startsWith("library") == true &&
                            targetState.destination.route?.startsWith("home") == true -> {
                            fadeOut(animationSpec = tween(300)) +
                                slideOutHorizontally(
                                    targetOffsetX = { if (swipeEdge == 0) it else -it },
                                    animationSpec = tween(350, easing = EaseInOutQuart)
                                )
                        }
                        initialState.destination.route?.startsWith("home") == true &&
                            targetState.destination.route?.startsWith("library") == true -> {
                            fadeOut(animationSpec = tween(300)) +
                                slideOutHorizontally(
                                    targetOffsetX = { if (swipeEdge == 0) it else -it },
                                    animationSpec = tween(350, easing = EaseInOutQuart)
                                )
                        }
                        else -> {
                            fadeOut(animationSpec = tween(300)) +
                                slideOutVertically(
                                    targetOffsetY = { it / 4 },
                                    animationSpec = tween(350, easing = EaseInOutQuart)
                                )
                        }
                    }
                }
            ) {
                composable(
                    route = Screen.Home.route,
                    enterTransition = {
                        when {
                            initialState.destination.route?.startsWith("library") == true -> {
                                // Horizontal slide animation when coming from Library
                                fadeIn(animationSpec = tween(300)) +
                                        slideInHorizontally(
                                            initialOffsetX = { -it },
                                            animationSpec = tween(350, easing = EaseInOutQuart)
                                        )
                            }

                            else -> {
                                // Default animation for other sources
                                fadeIn(animationSpec = tween(300))
                            }
                        }
                    },
                    exitTransition = {
                        when {
                            targetState.destination.route?.startsWith("library") == true -> {
                                // Horizontal slide animation when going to Library
                                fadeOut(animationSpec = tween(300)) +
                                        slideOutHorizontally(
                                            targetOffsetX = { -it },
                                            animationSpec = tween(350, easing = EaseInOutQuart)
                                        )
                            }

                            else -> {
                                // Default animation for other destinations
                                fadeOut(animationSpec = tween(300))
                            }
                        }
                    },
                    popEnterTransition = {
                        when {
                            initialState.destination.route?.startsWith("library") == true -> {
                                // Restore horizontal slide animation when popping back from Library
                                fadeIn(animationSpec = tween(300)) +
                                        slideInHorizontally(
                                            initialOffsetX = { -it },
                                            animationSpec = tween(350, easing = EaseInOutQuart)
                                        )
                            }

                            else -> {
                                // Simple faster fade animation when popping back from other screens
                                fadeIn(animationSpec = tween(200))
                            }
                        }
                    },
                    popExitTransition = {
                        // Simple faster fade animation when being popped from
                        fadeOut(animationSpec = tween(200))
                    }
                ) {
                    LocalHomeRoute(albums = albums, allSongs = allSongs, appSettings = appSettings, artists = artists, currentSong = currentSong, isPlaying = isPlaying, navController = navController, onPlayArtist = onPlayArtist, onPlayPause = onPlayPause, onPlaySong = onPlaySong, onPlayerClick = onPlayerClick, onSkipNext = onSkipNext, playlists = playlists, recentlyPlayed = recentlyPlayed, songs = songs, streamingMusicViewModel = streamingMusicViewModel, viewModel = viewModel)
                }

                composable(
                    Screen.Search.route,
                    enterTransition = {
                        fadeIn(animationSpec = tween(300)) +
                                slideInVertically(
                                    initialOffsetY = { it / 4 },
                                    animationSpec = tween(350, easing = EaseInOutQuart)
                                )
                    },
                    exitTransition = {
                        fadeOut(animationSpec = tween(300))
                    },
                    popExitTransition = {
                        fadeOut(animationSpec = tween(300)) +
                                slideOutVertically(
                                    targetOffsetY = { it / 4 },
                                    animationSpec = tween(350, easing = EaseInOutQuart)
                                )
                    }
                ) {
                    SlideUpCornerWrapper {
                        chromahub.rhythm.app.shared.presentation.screens.UniversalSearchScreen(
                            localViewModel = viewModel,
                            streamingSearch = streamingMusicViewModel,
                            onLocalSongClick = { song ->
                                viewModel.playSongFromSearch(song, songs)
                                navController.navigate(Screen.Player.route)
                            },
                            onLocalAlbumClick = { album ->
                                navController.navigate(Screen.AlbumDetail.createRoute(album.id, album.title))
                            },
                            onLocalArtistClick = { artist -> navController.navigate(Screen.ArtistDetail.createRoute(artist.name)) },
                            onLocalPlaylistClick = { playlist -> navController.navigate(Screen.PlaylistDetail.createRoute(playlist.id)) },
                            onStreamingSongClick = { song ->
                                streamingMusicViewModel.playSong(song)
                                navController.navigate(Screen.Player.route)
                            },
                            onStreamingAlbumClick = { streamingAlbum ->
                                navController.navigate(StreamingRoutes.album(streamingAlbum.id, streamingAlbum.title)) {
                                    launchSingleTop = true
                                }
                            },
                            onStreamingArtistClick = { artist ->
                                navController.navigate(StreamingRoutes.artist(artist.id, artist.name)) {
                                    launchSingleTop = true
                                }
                            },
                            onStreamingPlaylistClick = { playlist ->
                                navController.navigate(StreamingRoutes.playlist(playlist.id)) {
                                    launchSingleTop = true
                                }
                            },
                            onBack = { navigateToLanding() }
                        )
                    }
                }

                composable(
                    Screen.Settings.route,
                    enterTransition = {
                        fadeIn(animationSpec = tween(300)) +
                                slideInVertically(
                                    initialOffsetY = { it / 4 },
                                    animationSpec = tween(350, easing = EaseInOutQuart)
                                )
                    },
                    exitTransition = {
                        fadeOut(animationSpec = tween(300))
                    },
                    popExitTransition = {
                        fadeOut(animationSpec = tween(300)) +
                                slideOutVertically(
                                    targetOffsetY = { it / 4 },
                                    animationSpec = tween(350, easing = EaseInOutQuart)
                                )
                    }
                ) {
                    SlideUpCornerWrapper {
                        // Use the settings screen (now the default)
                        SettingsScreenWrapper(
                            onBack = {
                                navigateBackOrToLanding()
                            },
                            appSettings = appSettings,
                            navController = navController,
                            playbackState = viewModel,
                            playbackSettings = viewModel,
                            playlistAdmin = viewModel,
                            libraryAdmin = viewModel,
                            cacheManager = viewModel,
                            backupRestore = viewModel,
                            sleepTimer = viewModel
                        )
                    }
                }
                // Tuner Settings Subroutes
                composable(Screen.TunerNotifications.route) {
                    NotificationsSettingsScreen(onBackClick = navigateBackOrToSettings)
                }

                composable(Screen.TunerLabs.route) {
                    chromahub.rhythm.app.shared.presentation.screens.settings.LabsSettingsScreen(onBackClick = navigateBackOrToSettings)
                }

                composable(Screen.TunerAbout.route) {
                    chromahub.rhythm.app.shared.presentation.screens.settings.AboutScreen(
                        onBackClick = navigateBackOrToSettings,
                        onNavigateToUpdates = {
                            navController.navigate(Screen.TunerUpdates.route) {
                                launchSingleTop = true
                            }
                        },
                        onNavigateTo = { route ->
                            navController.navigate(route) {
                                launchSingleTop = true
                            }
                        }
                    )
                }

                composable(Screen.TunerNativeCapabilities.route) {
                    chromahub.rhythm.app.shared.presentation.screens.settings.NativeCapabilitiesSettingsScreen(
                        onBackClick = navigateBackOrToSettings
                    )
                }

                composable(Screen.TunerExternalCapabilities.route) {
                    chromahub.rhythm.app.shared.presentation.screens.settings.ExternalCapabilitiesSettingsScreen(
                        onBackClick = navigateBackOrToSettings,
                        onNavigateTo = { route ->
                            when (route) {
                                SettingsRoutes.DECODER_EXTENSIONS, SettingsRoutes.FALLBACK_ENGINES -> navController.navigate(Screen.TunerDecoderExtensions.route)
                                else -> navController.navigate(route)
                            }
                        }
                    )
                }

                composable(Screen.TunerDecoderExtensions.route) {
                    chromahub.rhythm.app.shared.presentation.screens.settings.DecoderExtensionsSettingsScreen(
                        onBackClick = navigateBackOrToSettings
                    )
                }

                composable(Screen.TunerUpdates.route) {
                    UpdatesSettingsScreen(onBackClick = navigateBackOrToSettings)
                }

                composable(Screen.TunerMediaScan.route) {
                    MediaScanSettingsScreen(
                        onBackClick = navigateBackOrToSettings,
                        libraryAdmin = viewModel
                    )
                }

                composable(Screen.TunerPlaylists.route) {
                    PlaylistsSettingsScreen(
                        onBackClick = navigateBackOrToSettings,
                        playlistAdmin = viewModel,
                        onNavigateTo = { route ->
                            if (route == SettingsRoutes.DEFAULT_PLAYLISTS) {
                                navController.navigate(Screen.TunerDefaultPlaylists.route)
                            }
                        }
                    )
                }

                composable(Screen.TunerDefaultPlaylists.route) {
                    DefaultPlaylistsSettingsScreen(
                        onBackClick = navigateBackOrToSettings,
                        playlistAdmin = viewModel
                    )
                }

                composable(Screen.TunerApiManagement.route) {
                    ApiManagementSettingsScreen(onBackClick = navigateBackOrToSettings)
                }

                composable(Screen.TunerCacheManagement.route) {
                    CacheManagementSettingsScreen(
                        onBackClick = navigateBackOrToSettings,
                        cacheManager = viewModel,
                        libraryAdmin = viewModel
                    )
                }

                composable(Screen.TunerBackupRestore.route) {
                    BackupRestoreSettingsScreen(
                        onBackClick = navigateBackOrToSettings,
                        libraryAdmin = viewModel,
                        backupRestore = viewModel,
                        playlistAdmin = viewModel
                    )
                }

                composable(Screen.TunerThemeCustomization.route) {
                    ThemeCustomizationSettingsScreen(onBackClick = navigateBackOrToSettings)
                }

                composable(
                    route = Screen.Equalizer.route,
                    enterTransition = {
                        fadeIn(animationSpec = tween(300)) +
                                slideInVertically(
                                    initialOffsetY = { it / 4 },
                                    animationSpec = tween(350, easing = EaseInOutQuart)
                                )
                    },
                    exitTransition = {
                        fadeOut(animationSpec = tween(300))
                    },
                    popExitTransition = {
                        fadeOut(animationSpec = tween(300)) +
                                slideOutVertically(
                                    targetOffsetY = { it / 4 },
                                    animationSpec = tween(350, easing = EaseInOutQuart)
                                )
                    }
                ) {
                    SlideUpCornerWrapper {
                        EqualizerScreen(
                            navController = navController,
                            audioEffects = viewModel.audioEffects,
                            audioDevices = viewModel.audioDevices,
                            appSettings = appSettings
                        )
                    }
                }

                composable(Screen.TunerSleepTimer.route) {
                    // Show sleep timer bottom sheet directly instead of placeholder screen
                    var showBottomSheet by remember { mutableStateOf(true) }
                    
                    if (showBottomSheet) {
                        SleepTimerBottomSheetNew(
                            onDismiss = {
                                showBottomSheet = false
                                navigateBackOrToSettings()
                            },
                            currentSong = currentSong,
                            isPlaying = isPlaying,
                            sleepTimer = viewModel,
                            appSettings = appSettings
                        )
                    } else {
                        // Navigate back if bottom sheet is dismissed without opening
                        LaunchedEffect(Unit) {
                            navigateBackOrToSettings()
                        }
                    }
                }

                composable(Screen.TunerCrashLogHistory.route) {
                    CrashLogHistorySettingsScreen(
                        onBackClick = navigateBackOrToSettings,
                        appSettings = appSettings
                    )
                }

                composable(Screen.TunerQueue.route) {
                    QueueSettingsScreen(onBackClick = navigateBackOrToSettings)
                }

                composable(Screen.TunerPlayback.route) {
                    PlaybackSettingsScreen(
                        onBackClick = navigateBackOrToSettings,
                        playbackSettings = viewModel,
                        onNavigateTo = { route ->
                            when (route) {
                                SettingsRoutes.REPLAY_GAIN -> navController.navigate(Screen.TunerReplayGain.route)
                                SettingsRoutes.CROSSFADE -> navController.navigate(Screen.TunerCrossfade.route)
                                SettingsRoutes.EXTERNAL_CAPABILITIES, SettingsRoutes.AUDIO_CAPABILITIES, SettingsRoutes.AUDIO_ENGINE -> navController.navigate(Screen.TunerExternalCapabilities.route)
                                SettingsRoutes.DECODER_EXTENSIONS, SettingsRoutes.FALLBACK_ENGINES -> navController.navigate(Screen.TunerDecoderExtensions.route)
                                SettingsRoutes.NATIVE_CAPABILITIES, SettingsRoutes.DEVICE_CAPABILITIES -> navController.navigate(Screen.TunerNativeCapabilities.route)
                            }
                        }
                    )
                }

                composable(Screen.TunerReplayGain.route) {
                    ReplayGainSettingsScreen(onBackClick = navigateBackOrToSettings)
                }

                composable(Screen.TunerCrossfade.route) {
                    CrossfadeSettingsScreen(onBackClick = navigateBackOrToSettings)
                }

                composable(Screen.TunerHomeScreen.route) {
                    HomeScreenCustomizationSettingsScreen(onBackClick = navigateBackOrToSettings)
                }

                composable(Screen.TunerExpressiveShapes.route) {
                    ExpressiveShapesSettingsScreen(onBackClick = navigateBackOrToSettings)
                }

                composable(Screen.TunerPlayerCustomization.route) {
                    PlayerCustomizationSettingsScreen(onBackClick = navigateBackOrToSettings)
                }

                composable(Screen.TunerMiniPlayerCustomization.route) {
                    MiniPlayerCustomizationSettingsScreen(onBackClick = navigateBackOrToSettings)
                }

                composable(Screen.TunerBatterySaver.route) {
                    PerformanceSettingsScreen(onBackClick = navigateBackOrToSettings)
                }

                composable(Screen.TunerLibrarySettings.route) {
                    LibrarySettingsScreen(onBackClick = navigateBackOrToSettings)
                }

                composable(Screen.TunerRhythmGuard.route) {
                    RhythmGuardSettingsScreen(onBackClick = navigateBackOrToSettings)
                }

                composable(Screen.TunerLyrics.route) {
                    LyricsSettingsScreen(onBackClick = navigateBackOrToSettings)
                }

                composable(Screen.TunerGestures.route) {
                    GesturesSettingsScreen(onBackClick = navigateBackOrToSettings)
                }

                composable(Screen.TunerWidget.route) {
                    WidgetSettingsScreen(onBackClick = navigateBackOrToSettings)
                }

                composable(Screen.TunerArtistSeparators.route) {
                    ArtistSeparatorsSettingsScreen(onBackClick = navigateBackOrToSettings)
                }

                composable(Screen.TunerGoSettings.route) {
                    chromahub.rhythm.app.features.streaming.presentation.screens.GoSettingsScreen(
                        onBackClick = navigateBackOrToSettings,
                        onConfigureCurrentProvider = { serviceId ->
                            appSettings.setInitialStreamingRoute("streaming_service_setup/$serviceId")
                            appSettings.setAppMode("STREAMING")
                            if (!navController.popBackStack()) {
                                navController.navigate("main") { launchSingleTop = true }
                            }
                        },
                        viewModel = streamingMusicViewModel
                    )
                }

                composable(
                    route = Screen.RhythmStats.route,
                    enterTransition = {
                        fadeIn(animationSpec = tween(300)) +
                                slideInVertically(
                                    initialOffsetY = { it / 4 },
                                    animationSpec = tween(350, easing = EaseInOutQuart)
                                )
                    },
                    exitTransition = {
                        fadeOut(animationSpec = tween(300))
                    },
                    popExitTransition = {
                        fadeOut(animationSpec = tween(300)) +
                                slideOutVertically(
                                    targetOffsetY = { it / 4 },
                                    animationSpec = tween(350, easing = EaseInOutQuart)
                                )
                    }
                ) {
                    SlideUpCornerWrapper {
                        RhythmStatsScreen(navController = navController, viewModel = viewModel)
                    }
                }

                composable(
                    route = StreamingRoutes.AlbumDetail,
                    arguments = listOf(
                        navArgument("albumId") { type = NavType.StringType },
                        navArgument("albumName") { type = NavType.StringType }
                    ),
                    enterTransition = {
                        fadeIn(animationSpec = tween(300)) +
                            slideInVertically(
                                initialOffsetY = { it / 4 },
                                animationSpec = tween(350, easing = EaseInOutQuart)
                            )
                    },
                    exitTransition = {
                        fadeOut(animationSpec = tween(300))
                    },
                    popExitTransition = {
                        fadeOut(animationSpec = tween(300)) +
                            slideOutVertically(
                                targetOffsetY = { it / 4 },
                                animationSpec = tween(350, easing = EaseInOutQuart)
                            )
                    }
                ) { backStackEntry ->
                    LocalStreamingAlbumDetailRoute(backStackEntry = backStackEntry, appSettings = appSettings, currentSong = currentSong, isPlaying = isPlaying, isShuffleEnabled = isShuffleEnabled, navController = navController, onPlayerClick = onPlayerClick, onToggleFavorite = onToggleFavorite, streamingMusicViewModel = streamingMusicViewModel, viewModel = viewModel, selectedStreamingSongForPlaylist = selectedStreamingSongForPlaylistState, showStreamingAddToPlaylist = showStreamingAddToPlaylistState)
                }

                composable(
                    route = StreamingRoutes.ArtistDetail,
                    arguments = listOf(
                        navArgument("artistId") { type = NavType.StringType },
                        navArgument("artistName") {
                            type = NavType.StringType
                            nullable = true
                            defaultValue = ""
                        }
                    ),
                    enterTransition = {
                        fadeIn(animationSpec = tween(300)) +
                            slideInVertically(
                                initialOffsetY = { it / 4 },
                                animationSpec = tween(350, easing = EaseInOutQuart)
                            )
                    },
                    exitTransition = {
                        fadeOut(animationSpec = tween(300))
                    },
                    popExitTransition = {
                        fadeOut(animationSpec = tween(300)) +
                            slideOutVertically(
                                targetOffsetY = { it / 4 },
                                animationSpec = tween(350, easing = EaseInOutQuart)
                            )
                    }
                ) { backStackEntry ->
                    LocalStreamingArtistDetailRoute(backStackEntry = backStackEntry, appSettings = appSettings, currentSong = currentSong, isPlaying = isPlaying, isShuffleEnabled = isShuffleEnabled, navController = navController, onPlayerClick = onPlayerClick, onToggleFavorite = onToggleFavorite, streamingMusicViewModel = streamingMusicViewModel, viewModel = viewModel, selectedStreamingSongForPlaylist = selectedStreamingSongForPlaylistState, showStreamingAddToPlaylist = showStreamingAddToPlaylistState)
                }

                composable(
                    route = StreamingRoutes.PlaylistDetail,
                    arguments = listOf(navArgument("playlistId") { type = NavType.StringType }),
                    enterTransition = {
                        fadeIn(animationSpec = tween(350)) +
                            scaleIn(
                                initialScale = 0.85f,
                                animationSpec = tween(400, easing = EaseOutQuint)
                            )
                    },
                    exitTransition = {
                        fadeOut(animationSpec = tween(350)) +
                            scaleOut(
                                targetScale = 0.85f,
                                animationSpec = tween(300, easing = EaseInOutQuart)
                            )
                    },
                    popEnterTransition = {
                        fadeIn(animationSpec = tween(350)) +
                            scaleIn(
                                initialScale = 0.85f,
                                animationSpec = tween(400, easing = EaseOutQuint)
                            )
                    },
                    popExitTransition = {
                        fadeOut(animationSpec = tween(350)) +
                            scaleOut(
                                targetScale = 0.85f,
                                animationSpec = tween(300, easing = EaseInOutQuart)
                            )
                    }
                ) { backStackEntry ->
                    LocalStreamingPlaylistDetailRoute(backStackEntry = backStackEntry, appSettings = appSettings, currentSong = currentSong, isPlaying = isPlaying, isShuffleEnabled = isShuffleEnabled, navController = navController, onPlayPause = onPlayPause, onPlayerClick = onPlayerClick, onSkipNext = onSkipNext, onToggleFavorite = onToggleFavorite, songs = songs, streamingMusicViewModel = streamingMusicViewModel, viewModel = viewModel, selectedStreamingSongForPlaylist = selectedStreamingSongForPlaylistState, showStreamingAddToPlaylist = showStreamingAddToPlaylistState)
                }

                composable(
                    route = StreamingRoutes.ServiceSetup,
                    arguments = listOf(navArgument("serviceId") { type = NavType.StringType }),
                    enterTransition = {
                        fadeIn(animationSpec = tween(300)) +
                            slideInVertically(
                                initialOffsetY = { it / 4 },
                                animationSpec = tween(350, easing = EaseInOutQuart)
                            )
                    },
                    exitTransition = {
                        fadeOut(animationSpec = tween(300))
                    },
                    popExitTransition = {
                        fadeOut(animationSpec = tween(300))
                    }
                ) { backStackEntry ->
                    LocalStreamingServiceSetupRoute(backStackEntry = backStackEntry, navController = navController, streamingMusicViewModel = streamingMusicViewModel, viewModel = viewModel)
                }

                composable(
                    route = StreamingRoutes.GoSettings,
                    enterTransition = {
                        fadeIn(animationSpec = tween(300)) +
                            slideInVertically(
                                initialOffsetY = { it / 4 },
                                animationSpec = tween(350, easing = EaseInOutQuart)
                            )
                    },
                    exitTransition = {
                        fadeOut(animationSpec = tween(300))
                    },
                    popExitTransition = {
                        fadeOut(animationSpec = tween(300))
                    }
                ) {
                    LocalStreamingGoSettingsRoute(navController = navController, streamingMusicViewModel = streamingMusicViewModel, viewModel = viewModel)
                }

                composable(
                    route = Screen.Library.route,
                    arguments = listOf(
                        navArgument("tab") {
                            type = NavType.StringType
                            defaultValue = "songs"
                        }
                    ),
                    enterTransition = {
                        when (initialState.destination.route) {
                            Screen.Home.route -> {
                                // Horizontal slide animation when coming from Home
                                fadeIn(animationSpec = tween(300)) +
                                        slideInHorizontally(
                                            initialOffsetX = { it },
                                            animationSpec = tween(350, easing = EaseInOutQuart)
                                        )
                            }

                            else -> {
                                // Default animation for other sources
                                fadeIn(animationSpec = tween(300))
                            }
                        }
                    },
                    exitTransition = {
                        when (targetState.destination.route) {
                            Screen.Home.route -> {
                                // Horizontal slide animation when going to Home
                                fadeOut(animationSpec = tween(300)) +
                                        slideOutHorizontally(
                                            targetOffsetX = { it },
                                            animationSpec = tween(350, easing = EaseInOutQuart)
                                        )
                            }

                            else -> {
                                // Default animation for other destinations
                                fadeOut(animationSpec = tween(300))
                            }
                        }
                    },
                    popEnterTransition = {
                        when (initialState.destination.route) {
                            Screen.Home.route -> {
                                // Restore horizontal slide animation when popping back from Home
                                fadeIn(animationSpec = tween(300)) +
                                        slideInHorizontally(
                                            initialOffsetX = { it },
                                            animationSpec = tween(350, easing = EaseInOutQuart)
                                        )
                            }

                            else -> {
                                // Simple faster fade animation when popping back from other screens
                                fadeIn(animationSpec = tween(200))
                            }
                        }
                    },
                    popExitTransition = {
                        when (targetState.destination.route) {
                            Screen.Home.route -> {
                                // Restore horizontal slide animation when popping back to Home
                                fadeOut(animationSpec = tween(300)) +
                                        slideOutHorizontally(
                                            targetOffsetX = { it },
                                            animationSpec = tween(350, easing = EaseInOutQuart)
                                        )
                            }

                            else -> {
                                // Simple faster fade animation when being popped from for other destinations
                                fadeOut(animationSpec = tween(200))
                            }
                        }
                    }
                ) {
                    LocalLibraryRoute(backStackEntry = it, albums = albums, appSettings = appSettings, artists = artists, currentSong = currentSong, isPlaying = isPlaying, isShuffleEnabled = isShuffleEnabled, navController = navController, onPlayAlbum = onPlayAlbum, onPlayAlbumShuffled = onPlayAlbumShuffled, onPlayPause = onPlayPause, onPlaySong = onPlaySong, onPlayerClick = onPlayerClick, onSkipNext = onSkipNext, playlists = playlists, songs = songs, sortOrder = sortOrder, streamingMusicViewModel = streamingMusicViewModel, viewModel = viewModel, selectedStreamingSongForPlaylist = selectedStreamingSongForPlaylistState, showStreamingAddToPlaylist = showStreamingAddToPlaylistState)
                }

                // Player anchor route: the player is a sheet, not a screen.
                // This empty box keeps the 14+ navigate(Screen.Player.route)
                // call sites resolving; do not remove.
                composable(
                    route = Screen.Player.route,
                    enterTransition = { EnterTransition.None },
                    exitTransition = { ExitTransition.None },
                    popEnterTransition = { EnterTransition.None },
                    popExitTransition = { ExitTransition.None }
                ) {
                    Box(modifier = Modifier.fillMaxSize())
                }

                // Add playlist detail screen
                @OptIn(ExperimentalMaterial3Api::class)
                composable(
                    route = Screen.PlaylistDetail.route,
                    arguments = listOf(
                        navArgument("playlistId") {
                            type = NavType.StringType
                        }
                    ),
                    // Enhanced transitions to match AboutScreen pattern
                    enterTransition = {
                        fadeIn(animationSpec = tween(350)) +
                                scaleIn(
                                    initialScale = 0.85f,
                                    animationSpec = tween(400, easing = EaseOutQuint)
                                )
                    },
                    exitTransition = {
                        fadeOut(animationSpec = tween(350)) +
                                scaleOut(
                                    targetScale = 0.85f,
                                    animationSpec = tween(300, easing = EaseInOutQuart)
                                )
                    },
                    popEnterTransition = {
                        fadeIn(animationSpec = tween(350)) +
                                scaleIn(
                                    initialScale = 0.85f,
                                    animationSpec = tween(400, easing = EaseOutQuint)
                                )
                    },
                    popExitTransition = {
                        fadeOut(animationSpec = tween(350)) +
                                scaleOut(
                                    targetScale = 0.85f,
                                    animationSpec = tween(300, easing = EaseInOutQuart)
                                )
                    }
                ) { backStackEntry ->
                    LocalPlaylistDetailRoute(backStackEntry = backStackEntry, appSettings = appSettings, artists = artists, currentSong = currentSong, isPlaying = isPlaying, navController = navController, onPlayPause = onPlayPause, onPlayPlaylist = onPlayPlaylist, onPlayPlaylistShuffled = onPlayPlaylistShuffled, onPlaySong = onPlaySong, onPlayerClick = onPlayerClick, onSkipNext = onSkipNext, playlists = playlists, songs = songs, startDestination = startDestination, viewModel = viewModel)
                }

                // Artist Detail Screen
                composable(
                    route = Screen.ArtistDetail.route,
                    arguments = listOf(
                        navArgument("artistName") {
                            type = NavType.StringType
                        }
                    ),
                    enterTransition = {
                        fadeIn(animationSpec = tween(300)) +
                                slideInVertically(
                                    initialOffsetY = { it / 4 },
                                    animationSpec = tween(350, easing = EaseInOutQuart)
                                )
                    },
                    exitTransition = {
                        fadeOut(animationSpec = tween(300))
                    },
                    popExitTransition = {
                        fadeOut(animationSpec = tween(300)) +
                                slideOutVertically(
                                    targetOffsetY = { it / 4 },
                                    animationSpec = tween(350, easing = EaseInOutQuart)
                                )
                    }
                ) { backStackEntry ->
                    LocalArtistDetailRoute(backStackEntry = backStackEntry, appSettings = appSettings, currentSong = currentSong, isPlaying = isPlaying, navController = navController, onPlaySong = onPlaySong, onPlayerClick = onPlayerClick, onToggleFavorite = onToggleFavorite, playlists = playlists, songs = songs, startDestination = startDestination, viewModel = viewModel)
                }

                // Album Detail Screen
                composable(
                    route = Screen.AlbumDetail.route,
                    arguments = listOf(
                        navArgument("albumId") { type = NavType.StringType },
                        navArgument("albumName") { type = NavType.StringType }
                    ),
                    enterTransition = {
                        fadeIn(animationSpec = tween(300)) +
                                slideInVertically(
                                    initialOffsetY = { it / 4 },
                                    animationSpec = tween(350, easing = EaseInOutQuart)
                                )
                    },
                    exitTransition = {
                        fadeOut(animationSpec = tween(300))
                    },
                    popExitTransition = {
                        fadeOut(animationSpec = tween(300)) +
                                slideOutVertically(
                                    targetOffsetY = { it / 4 },
                                    animationSpec = tween(350, easing = EaseInOutQuart)
                                )
                    }
                ) { backStackEntry ->
                    LocalAlbumDetailRoute(backStackEntry = backStackEntry, appSettings = appSettings, artists = artists, currentSong = currentSong, isPlaying = isPlaying, navController = navController, onPlaySong = onPlaySong, onPlayerClick = onPlayerClick, onToggleFavorite = onToggleFavorite, playlists = playlists, songs = songs, startDestination = startDestination, viewModel = viewModel)
                }

                // Add to playlist screen
                @OptIn(ExperimentalMaterial3Api::class)
                composable(
                    Screen.AddToPlaylist.route,
                    enterTransition = {
                        fadeIn(animationSpec = tween(300)) +
                                slideInVertically(
                                    initialOffsetY = { it / 4 },
                                    animationSpec = tween(350, easing = EaseInOutQuart)
                                )
                    },
                    exitTransition = {
                        fadeOut(animationSpec = tween(300))
                    },
                    popExitTransition = {
                        fadeOut(animationSpec = tween(300)) +
                                slideOutVertically(
                                    targetOffsetY = { it / 4 },
                                    animationSpec = tween(350, easing = EaseInOutQuart)
                                )
                    }
                ) {
                    val songToAdd = viewModel.selectedSongForPlaylist.collectAsState().value
                    val targetPlaylistId = viewModel.targetPlaylistId.collectAsState().value

                    var searchQuery by remember { mutableStateOf("") }

                    // If we have a target playlist ID, we're adding songs to that playlist
                    if (targetPlaylistId != null) {
                        val targetPlaylist = playlists.find { it.id == targetPlaylistId }

                        if (targetPlaylist != null) {
                            // Filter available songs
                             val availableSongs =
                                remember(songs, targetPlaylist.songs, searchQuery) {
                                    songs.filter { song ->
                                        // Filter out songs that are already in the playlist
                                        !targetPlaylist.songs.any { it.id == song.id }
                                    }
                                }

                            if (availableSongs.isEmpty() && searchQuery.isBlank()) {
                                // No songs to add and no search query
                                AlertDialog(
                                    onDismissRequest = {
                                        viewModel.clearTargetPlaylistForAddingSongs()
                                        navigateBackOrToLanding()
                                    },
                                    icon = {
                                        Icon(
                                            imageVector = RhythmIcons.Info,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(28.dp)
                                        )
                                    },
                                    title = { Text(stringResource(R.string.playlist_no_songs_available)) },
                                    text = { Text(stringResource(R.string.playlist_all_in_playlist)) },
                                    confirmButton = {
                                        Button(onClick = {
                                            viewModel.clearTargetPlaylistForAddingSongs()
                                            navigateBackOrToLanding()
                                        }) {
                                            Icon(
                                                imageVector = RhythmIcons.CheckCircle,
                                                contentDescription = null,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(stringResource(R.string.ui_ok))
                                        }
                                    },
                                    shape = RoundedCornerShape(24.dp)
                                )
                            } else {
                                AddToPlaylistScreen(
                                    targetPlaylist = targetPlaylist,
                                    availableSongs = availableSongs,
                                    searchQuery = searchQuery,
                                    onSearchQueryChange = { searchQuery = it },
                                    onBackClick = {
                                        viewModel.clearTargetPlaylistForAddingSongs()
                                        navigateBackOrToLanding()
                                    },
                                    onAddSongsToPlaylist = { songs ->
                                        if (songs.size == 1) {
                                            viewModel.addSongToPlaylist(
                                                songs[0],
                                                targetPlaylistId
                                            ) { message ->
                                                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                                            }
                                        } else {
                                            val (successCount, playlistName) = viewModel.addSongsToPlaylist(
                                                songs,
                                                targetPlaylistId
                                            )
                                            val message = when {
                                                successCount == 0 -> "No songs added - they may already be in the playlist"
                                                successCount == songs.size -> "Added $successCount songs to $playlistName"
                                                else -> "Added $successCount of ${songs.size} songs to $playlistName"
                                            }
                                            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                )
                            }
                        } else {
                            // Playlist not found, go back
                            LaunchedEffect(Unit) {
                                viewModel.clearTargetPlaylistForAddingSongs()
                                navigateBackOrToLanding()
                            }
                        }
                    }
                    // If we have a song to add (from Player or Search), we're adding it to a playlist
                    else if (songToAdd != null) {
                        // Use a simpler approach without the bottom sheet state
                        var showCreatePlaylistDialog by remember { mutableStateOf(false) }

                        if (showCreatePlaylistDialog) {
                            CreatePlaylistDialog(
                                onDismiss = {
                                    showCreatePlaylistDialog = false
                                },
                                onConfirm = { name ->
                                    viewModel.createPlaylist(name)
                                    showCreatePlaylistDialog = false
                                },
                                song = songToAdd,
                                onConfirmWithSong = { name ->
                                    viewModel.createPlaylist(name, listOf(songToAdd)) { message ->
                                        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                                    }
                                    showCreatePlaylistDialog = false
                                    viewModel.clearSelectedSongForPlaylist()
                                    navigateBackOrToLanding()
                                }
                            )
                        } else {
                            // Navigate back to the player screen and show the bottom sheet there
                            LaunchedEffect(Unit) {
                                navigateBackOrToLanding()
                            }
                        }
                    } else {
                        // No song selected and no target playlist, go back
                        LaunchedEffect(Unit) {
                            viewModel.clearSelectedSongForPlaylist()
                            viewModel.clearTargetPlaylistForAddingSongs()
                            navigateBackOrToLanding()
                        }
                    }
                }
            }
        }
    }

    // Queue action dialog - show at top level
    val queueActionRequest by viewModel.queueActionRequest.collectAsState()
    val queueListActionRequest by viewModel.queueListActionRequest.collectAsState()
    val currentQueueForDialog by viewModel.currentQueue.collectAsState()

    queueActionRequest?.let { request ->
        QueueActionDialog(
            song = request.song,
            queueSize = currentQueueForDialog.songs.size,
            onDismiss = { viewModel.dismissQueueActionDialog() },
            onClearAndPlay = {
                viewModel.handleQueueActionChoice(request.song, clearQueue = true)
            },
            onAddToQueue = {
                viewModel.handleQueueActionChoice(request.song, clearQueue = false)
            }
        )
    }

    queueListActionRequest?.let { request ->
        QueueListActionDialog(
            queueSize = currentQueueForDialog.songs.size,
            incomingCount = request.songs.size,
            sourceLabel = request.sourceLabel,
            onDismiss = { viewModel.dismissQueueListActionDialog() },
            onReplaceQueue = {
                viewModel.handleQueueListActionChoice("replace")
            },
            onPlayNext = {
                viewModel.handleQueueListActionChoice("play_next")
            },
            onAddToEnd = {
                viewModel.handleQueueListActionChoice("add_to_end")
            }
        )
    }

    // Media scan loader overlay for refresh operations
    AnimatedVisibility(
        visible = isMediaScanning,
        enter = fadeIn(
            animationSpec = tween(
                800,
                easing = androidx.compose.animation.core.EaseOutCubic
            )
        ),
        exit = fadeOut(
            animationSpec = tween(
                800,
                easing = androidx.compose.animation.core.EaseInCubic
            )
        )
    ) {
        MediaScanLoader(
            musicViewModel = viewModel,
            onScanComplete = {
                // Media scan loader will hide automatically when isMediaScanning becomes false
            }
        )
    }

    if (showCreatePlaylistDialog.value) {
        val songForDialog = songForCreatePlaylistDialog.value
        CreatePlaylistDialog(
            onDismiss = {
                showCreatePlaylistDialog.value = false
                songForCreatePlaylistDialog.value = null
            },
            onConfirm = { name ->
                viewModel.createPlaylist(name)
                showCreatePlaylistDialog.value = false
                songForCreatePlaylistDialog.value = null
            },
            song = songForDialog,
            onConfirmWithSong = { name ->
                if (songForDialog != null) {
                    viewModel.createPlaylist(name, listOf(songForDialog)) { message ->
                        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                    }
                } else {
                    viewModel.createPlaylist(name)
                }
                showCreatePlaylistDialog.value = false
                songForCreatePlaylistDialog.value = null
            }
        )
    }
}

/**
 * Navigation rail for tablets with Material 3 Expressive design - Local Navigation
 */
@Composable
private fun LocalNavigationRail(
    currentRoute: String,
    navController: NavHostController,
    firstVisibleLibraryTab: LibraryTab,
    context: android.content.Context,
    haptic: androidx.compose.ui.hapticfeedback.HapticFeedback,
    isFloating: Boolean = true
) {
    val navigateToTopLevel: (String) -> Unit = { route ->
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    val libraryRoute = Screen.Library.createRoute(firstVisibleLibraryTab)
    val coreItems = listOf(
        LocalNavRailItem(
            route = Screen.Home.route,
            title = stringResource(R.string.home),
            selectedIcon = RhythmIcons.HomeFilled,
            unselectedIcon = RhythmIcons.Home,
            onClick = {
                navigateToTopLevel(Screen.Home.route)
            }
        ),
        LocalNavRailItem(
            route = libraryRoute,
            title = stringResource(R.string.library),
            selectedIcon = RhythmIcons.Navigation.Library,
            unselectedIcon = RhythmIcons.Navigation.LibraryOutlined,
            onClick = {
                navigateToTopLevel(libraryRoute)
            }
        ),
        LocalNavRailItem(
            route = Screen.RhythmStats.route,
            title = stringResource(R.string.localnavigation_stats),
            selectedIcon = MaterialSymbolIcon("auto_graph", filled = true),
            unselectedIcon = MaterialSymbolIcon("auto_graph"),
            onClick = {
                navigateToTopLevel(Screen.RhythmStats.route)
            }
        ),
        LocalNavRailItem(
            route = Screen.Search.route,
            title = stringResource(R.string.search),
            selectedIcon = RhythmIcons.SearchFilled,
            unselectedIcon = RhythmIcons.Search,
            onClick = {
                navigateToTopLevel(Screen.Search.route)
            }
        )
    )

    val settingsItem = LocalNavRailItem(
        route = Screen.Settings.route,
        title = stringResource(R.string.settings_backup_settings),
        selectedIcon = RhythmIcons.SettingsFilled,
        unselectedIcon = RhythmIcons.Settings,
        onClick = {
            navigateToTopLevel(Screen.Settings.route)
        }
    )

    if (isFloating) {
        val floatingItems = coreItems + settingsItem
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Vertical + WindowInsetsSides.Start))
                .padding(start = 12.dp, top = 16.dp, bottom = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainer,
                tonalElevation = 3.dp,
                shadowElevation = 4.dp,
                shape = RoundedCornerShape(28.dp),
                modifier = Modifier
                    .wrapContentHeight()
                    .width(80.dp)
            ) {
                Column(
                    modifier = Modifier
                        .wrapContentHeight()
                        .padding(vertical = 16.dp, horizontal = 2.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically)
                ) {
                    floatingItems.forEach { item ->
                        val isSelected = when {
                            item.route == Screen.Home.route -> currentRoute == Screen.Home.route
                            item.route.substringBefore("?") == Screen.Library.route.substringBefore("?") -> currentRoute.startsWith("library")
                            item.route == Screen.Search.route -> currentRoute == Screen.Search.route
                            item.route == Screen.RhythmStats.route -> currentRoute == Screen.RhythmStats.route
                            item.route == Screen.Settings.route -> currentRoute == Screen.Settings.route
                            else -> false
                        }
                        LocalNavigationRailItemWithAnimation(
                            item = item,
                            isSelected = isSelected,
                            haptic = haptic,
                            context = context
                        )
                    }
                }
            }
        }
    } else {
        val railContainerColor = MaterialTheme.colorScheme.surfaceContainer
        val railWidth = 84.dp

        Surface(
            color = railContainerColor,
            tonalElevation = 1.dp,
            shadowElevation = 0.dp,
            shape = RectangleShape,
            modifier = Modifier
                .fillMaxHeight()
                .width(railWidth)
                .windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Vertical + WindowInsetsSides.Start))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(vertical = 16.dp, horizontal = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .wrapContentHeight(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically)
                ) {
                    coreItems.forEach { item ->
                        val isSelected = when {
                            item.route == Screen.Home.route -> currentRoute == Screen.Home.route
                            item.route.substringBefore("?") == Screen.Library.route.substringBefore("?") -> currentRoute.startsWith("library")
                            item.route == Screen.Search.route -> currentRoute == Screen.Search.route
                            item.route == Screen.RhythmStats.route -> currentRoute == Screen.RhythmStats.route
                            else -> false
                        }
                        LocalNavigationRailItemWithAnimation(
                            item = item,
                            isSelected = isSelected,
                            haptic = haptic,
                            context = context
                        )
                    }
                }

                Box(
                    modifier = Modifier.align(Alignment.BottomCenter)
                ) {
                    LocalNavigationRailItemWithAnimation(
                        item = settingsItem,
                        isSelected = currentRoute == Screen.Settings.route,
                        haptic = haptic,
                        context = context
                    )
                }
            }
        }
    }
}

/**
 * Data class for local navigation rail items
 */
private data class LocalNavRailItem(
    val route: String,
    val title: String,
    val selectedIcon: MaterialSymbolIcon,
    val unselectedIcon: MaterialSymbolIcon,
    val onClick: () -> Unit
)

/**
 * Local navigation rail item with animated selection indicator and spring physics
 */
@Composable
private fun LocalNavigationRailItemWithAnimation(
    item: LocalNavRailItem,
    isSelected: Boolean,
    haptic: androidx.compose.ui.hapticfeedback.HapticFeedback,
    context: android.content.Context
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val animatedScale by animateFloatAsState(
        targetValue = when {
            isPressed -> 0.90f
            isSelected -> 1.02f
            else -> 1.0f
        },
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "scale_${item.title}"
    )

    val iconColor by animateColorAsState(
        targetValue = if (isSelected)
            MaterialTheme.colorScheme.onPrimaryContainer
        else
            MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "iconColor_${item.title}"
    )

    val textColor by animateColorAsState(
        targetValue = if (isSelected)
            MaterialTheme.colorScheme.onSurface
        else
            MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "textColor_${item.title}"
    )

    val pillWidth by animateDpAsState(
        targetValue = if (isSelected) 56.dp else 0.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "pillWidth_${item.title}"
    )

    Column(
        modifier = Modifier
            .width(76.dp)
            .graphicsLayer {
                scaleX = animatedScale
                scaleY = animatedScale
            }
            .clip(RoundedCornerShape(16.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = null
            ) {
                HapticUtils.performHapticFeedback(context, haptic, HapticType.HEAVY)
                item.onClick()
            }
            .padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .height(32.dp)
                .width(56.dp),
            contentAlignment = Alignment.Center
        ) {
            if (isSelected) {
                Box(
                    modifier = Modifier
                        .width(pillWidth)
                        .height(32.dp)
                        .background(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = CircleShape
                        )
                )
            }

            Icon(
                imageVector = if (isSelected) item.selectedIcon else item.unselectedIcon,
                contentDescription = item.title,
                tint = iconColor,
                modifier = Modifier.size(24.dp)
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = item.title,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
            color = textColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}


