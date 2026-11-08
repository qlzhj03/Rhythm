package chromahub.rhythm.app.shared.presentation.screens.settings

import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SheetValue
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.lerp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.navArgument
import chromahub.rhythm.app.R
import com.qlzhj.rhythm.core.designsystem.LocalMiniPlayerPadding
import com.qlzhj.rhythm.core.designsystem.icons.Icon
import com.qlzhj.rhythm.core.designsystem.icons.MaterialSymbolIcon
import com.qlzhj.rhythm.core.designsystem.icons.RhythmIcons
import com.qlzhj.rhythm.core.designsystem.theme.RhythmTheme
import com.qlzhj.rhythm.core.designsystem.utils.HapticType
import com.qlzhj.rhythm.core.designsystem.utils.HapticUtils
import com.qlzhj.rhythm.core.designsystem.utils.LazyListStateSaver
import com.qlzhj.rhythm.core.designsystem.utils.windowScreenHeightDp
import com.qlzhj.rhythm.core.designsystem.utils.windowScreenWidthDp
import com.qlzhj.rhythm.core.data.core.domain.repository.BackupRestoreController
import com.qlzhj.rhythm.core.data.core.domain.repository.LibraryAdminController
import com.qlzhj.rhythm.core.data.core.domain.repository.LibraryCacheManager
import com.qlzhj.rhythm.core.data.core.domain.repository.PlaylistAdminController
import chromahub.rhythm.app.infrastructure.service.PlaybackSettingsController
import chromahub.rhythm.app.infrastructure.service.PlaybackStateSource
import chromahub.rhythm.app.infrastructure.service.SleepTimerController
import chromahub.rhythm.app.features.local.presentation.components.settings.LanguageSwitcherBottomSheet
import com.qlzhj.rhythm.core.data.shared.data.model.AppSettings
import com.qlzhj.rhythm.core.data.shared.data.repository.PlaybackStatsRepository
import chromahub.rhythm.app.shared.presentation.components.Material3SettingsGroup
import chromahub.rhythm.app.shared.presentation.components.Material3SettingsItem
import chromahub.rhythm.app.shared.presentation.components.SettingsBadgePalette
import chromahub.rhythm.app.shared.presentation.components.SettingsPalettes
import chromahub.rhythm.app.shared.presentation.components.bottomsheets.AdaptiveSheetScrollContainer
import chromahub.rhythm.app.shared.presentation.components.bottomsheets.RhythmAdaptiveModalSheet
import chromahub.rhythm.app.shared.presentation.components.bottomsheets.SheetAdaptiveType
import chromahub.rhythm.app.shared.presentation.components.bottomsheets.StandardBottomSheetHeader
import chromahub.rhythm.app.shared.presentation.components.bottomsheets.groupedBottomSheetItemShape
import chromahub.rhythm.app.shared.presentation.components.common.CollapsibleHeaderScreen
import chromahub.rhythm.app.shared.presentation.components.player.SleepTimerBottomSheetNew
import com.qlzhj.rhythm.core.common.shared.presentation.navigation.RhythmGuardRiskLevel
import com.qlzhj.rhythm.core.common.shared.presentation.navigation.rhythmGuardResolveRiskLevel
import kotlin.math.absoluteValue
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// Wrapper function for navigation
@OptIn(ExperimentalAnimationApi::class)
@Composable
fun SettingsScreenWrapper(
    onBack: () -> Unit,
    appSettings: com.qlzhj.rhythm.core.data.shared.data.model.AppSettings,
    navController: androidx.navigation.NavController,
    playbackState: PlaybackStateSource,
    playbackSettings: PlaybackSettingsController,
    playlistAdmin: PlaylistAdminController,
    libraryAdmin: LibraryAdminController,
    cacheManager: LibraryCacheManager,
    backupRestore: BackupRestoreController,
    sleepTimer: SleepTimerController
) {
    val isTablet = windowScreenWidthDp() >= 600
    val isLandscapeTablet = isTablet && windowScreenWidthDp() > windowScreenHeightDp()

    var routeStack by rememberSaveable { mutableStateOf(emptyList<String>()) }
    var showSleepTimerBottomSheet by rememberSaveable { mutableStateOf(false) }
    val currentSong by playbackState.currentSong.collectAsState()
    val isPlaying by playbackState.isPlaying.collectAsState()

    // Hoist the main settings scroll state to persist across navigation
    val mainSettingsScrollState = rememberSaveable(
        saver = LazyListStateSaver
    ) {
        LazyListState()
    }

    // Handle back navigation
    val handleBack: () -> Unit = {
        if (routeStack.isNotEmpty()) {
            routeStack = routeStack.dropLast(1)
        } else {
            onBack()
        }
    }

    // Consume any initial subroute requested by external callers (e.g., open settings -> specific pane)
    LaunchedEffect(Unit) {
        val pending = appSettings.consumeInitialSettingsSubroute()
        if (pending == SettingsRoutes.GO_SETTINGS) {
            // Exit-navigation (D-phase D2): Go settings lives in :app's NavHost.
            navController.navigate(SettingsRoutes.STREAMING_GO_SETTINGS)
        } else if (!pending.isNullOrBlank()) {
            routeStack = listOf(pending)
        }
    }

    // Handle system back gestures when in subsettings
    BackHandler(enabled = routeStack.isNotEmpty()) {
        handleBack()
    }

    val onNavigateToSubsetting: (String) -> Unit = { route: String ->
        if (route == SettingsRoutes.RHYTHM_STATS) {
            // Both modes render the same local graph (RhythmNavigation hosts
            // LocalNavigation for STREAMING too), so the stats destination is
            // always present; the old streaming-route probe was never registered.
            navController.navigate(SettingsRoutes.RHYTHM_STATS)
        } else if (route == SettingsRoutes.EQUALIZER) {
            navController.navigate(SettingsRoutes.LOCAL_EQUALIZER)
        } else if (route == SettingsRoutes.GO_SETTINGS) {
            // Exit-navigation (D-phase D2): Go settings is hosted in :app's
            // NavHost (Screen.TunerGoSettings) instead of embedding
            // :feature:streaming's GoSettingsScreen here.
            navController.navigate(SettingsRoutes.STREAMING_GO_SETTINGS)
        } else if (route == SettingsRoutes.SLEEP_TIMER) {
            showSleepTimerBottomSheet = true
        } else {
            if (routeStack.lastOrNull() != route) {
                routeStack = routeStack + route
            }
        }
    }

    if (isLandscapeTablet) {
        // Tablet layout: Master-detail with settings always visible on left
        Row(modifier = Modifier.fillMaxSize()) {
            // Master pane - always visible settings list
            Surface(
                modifier = Modifier
                    .weight(0.35f)
                    .fillMaxHeight(),
            ) {
                SettingsScreen(
                    onBackClick = handleBack,
                    onNavigateTo = { route ->
                        if (route == SettingsRoutes.RHYTHM_STATS || route == SettingsRoutes.EQUALIZER ||
                            route == SettingsRoutes.GO_SETTINGS || route == SettingsRoutes.SLEEP_TIMER
                        ) {
                            onNavigateToSubsetting(route)
                        } else {
                            routeStack = listOf(route)
                        }
                    },
                    scrollState = mainSettingsScrollState,
                    isTablet = true
                )
            }

            // Divider

            // Detail pane - subsettings or placeholder
            Surface(
                modifier = Modifier
                    .weight(0.65f)
                    .fillMaxHeight(),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 0.dp
            ) {
                AnimatedContent(
                    targetState = routeStack,
                    transitionSpec = {
                        val isBacking = targetState.size < initialState.size
                        if (!isBacking && targetState.isNotEmpty()) {
                            slideInHorizontally(
                                initialOffsetX = { it },
                                animationSpec = tween(
                                    durationMillis = 400,
                                    easing = androidx.compose.animation.core.EaseOutCubic
                                )
                            ) + fadeIn(
                                animationSpec = tween(
                                    durationMillis = 350,
                                    delayMillis = 50
                                )
                            ) togetherWith
                            slideOutHorizontally(
                                targetOffsetX = { -it / 4 },
                                animationSpec = tween(
                                    durationMillis = 350,
                                    easing = androidx.compose.animation.core.EaseInCubic
                                )
                            ) + fadeOut(
                                animationSpec = tween(durationMillis = 250)
                            )
                        } else {
                            slideInHorizontally(
                                initialOffsetX = { -it / 4 },
                                animationSpec = tween(
                                    durationMillis = 400,
                                    easing = androidx.compose.animation.core.EaseOutCubic
                                )
                            ) + fadeIn(
                                animationSpec = tween(
                                    durationMillis = 350,
                                    delayMillis = 50
                                )
                            ) togetherWith
                            slideOutHorizontally(
                                targetOffsetX = { it },
                                animationSpec = tween(
                                    durationMillis = 350,
                                    easing = androidx.compose.animation.core.EaseInCubic
                                )
                            ) + fadeOut(
                                animationSpec = tween(durationMillis = 250)
                            )
                        }
                    },
                    label = "tablet_detail_navigation",
                    contentKey = { it.lastOrNull() ?: "placeholder" }
                ) { stack ->
                    val route = stack.lastOrNull()
                    when (route) {
                        SettingsRoutes.NOTIFICATIONS -> NotificationsSettingsScreen(onBackClick = handleBack)
                        SettingsRoutes.PLAYLISTS -> PlaylistsSettingsScreen(
                            onBackClick = handleBack,
                            onNavigateTo = onNavigateToSubsetting,
                            playlistAdmin = playlistAdmin
                        )
                        SettingsRoutes.DEFAULT_PLAYLISTS -> DefaultPlaylistsSettingsScreen(
                            onBackClick = handleBack,
                            playlistAdmin = playlistAdmin
                        )
                        SettingsRoutes.MEDIA_SCAN -> MediaScanSettingsScreen(onBackClick = handleBack, libraryAdmin = libraryAdmin)
                        SettingsRoutes.ARTIST_SEPARATORS -> ArtistSeparatorsSettingsScreen(onBackClick = handleBack)
                        SettingsRoutes.ABOUT -> chromahub.rhythm.app.shared.presentation.screens.settings.AboutScreen(
                            onBackClick = handleBack,
                            onNavigateToUpdates = { onNavigateToSubsetting(SettingsRoutes.UPDATES) },
                            onNavigateTo = onNavigateToSubsetting
                        )
                        SettingsRoutes.NATIVE_CAPABILITIES, SettingsRoutes.DEVICE_CAPABILITIES -> NativeCapabilitiesSettingsScreen(
                            onBackClick = handleBack
                        )
                        SettingsRoutes.EXTERNAL_CAPABILITIES, SettingsRoutes.AUDIO_CAPABILITIES, SettingsRoutes.AUDIO_ENGINE -> ExternalCapabilitiesSettingsScreen(
                            onBackClick = handleBack,
                            onNavigateTo = onNavigateToSubsetting
                        )
                        SettingsRoutes.DECODER_EXTENSIONS, SettingsRoutes.FALLBACK_ENGINES -> DecoderExtensionsSettingsScreen(
                            onBackClick = handleBack
                        )
                        SettingsRoutes.UPDATES -> UpdatesSettingsScreen(onBackClick = handleBack)
                        SettingsRoutes.LABS, SettingsRoutes.EXPERIMENTAL_FEATURES -> LabsSettingsScreen(
                            onBackClick = handleBack,
                            onNavigateTo = onNavigateToSubsetting,
                            onNavigateToGoSettings = { onNavigateToSubsetting(SettingsRoutes.GO_SETTINGS) }
                        )
                        SettingsRoutes.API_MANAGEMENT -> ApiManagementSettingsScreen(onBackClick = handleBack)
                        SettingsRoutes.CACHE_MANAGEMENT -> CacheManagementSettingsScreen(
                            onBackClick = handleBack,
                            cacheManager = cacheManager,
                            libraryAdmin = libraryAdmin
                        )
                        SettingsRoutes.BACKUP_RESTORE -> BackupRestoreSettingsScreen(
                            onBackClick = handleBack,
                            libraryAdmin = libraryAdmin,
                            backupRestore = backupRestore,
                            playlistAdmin = playlistAdmin
                        )
                        SettingsRoutes.THEME_CUSTOMIZATION -> ThemeCustomizationSettingsScreen(onBackClick = handleBack)
                        SettingsRoutes.PLAYER_CUSTOMIZATION -> PlayerCustomizationSettingsScreen(onBackClick = handleBack)
                        SettingsRoutes.MINIPLAYER_CUSTOMIZATION -> MiniPlayerCustomizationSettingsScreen(onBackClick = handleBack)
                        SettingsRoutes.CRASH_LOG_HISTORY -> CrashLogHistorySettingsScreen(onBackClick = handleBack, appSettings = appSettings)
                        SettingsRoutes.QUEUE -> QueueSettingsScreen(onBackClick = handleBack)
                        SettingsRoutes.PLAYBACK -> PlaybackSettingsScreen(
                            onBackClick = handleBack,
                            onNavigateTo = onNavigateToSubsetting,
                            playbackSettings = playbackSettings
                        )
                        SettingsRoutes.REPLAY_GAIN -> ReplayGainSettingsScreen(
                            onBackClick = handleBack,
                            onNavigateTo = onNavigateToSubsetting
                        )
                        SettingsRoutes.CROSSFADE -> CrossfadeSettingsScreen(
                            onBackClick = handleBack,
                            onNavigateTo = onNavigateToSubsetting
                        )
                        SettingsRoutes.LYRICS -> LyricsSettingsScreen(onBackClick = handleBack)
                        SettingsRoutes.WIDGET -> WidgetSettingsScreen(onBackClick = handleBack)
                        SettingsRoutes.HOME_SCREEN -> HomeScreenCustomizationSettingsScreen(onBackClick = handleBack)
                        SettingsRoutes.GESTURES -> GesturesSettingsScreen(onBackClick = handleBack)
                        SettingsRoutes.EXPRESSIVE_SHAPES -> ExpressiveShapesSettingsScreen(onBackClick = handleBack)
                        SettingsRoutes.LIBRARY_SETTINGS -> LibrarySettingsScreen(onBackClick = handleBack)
                        SettingsRoutes.RHYTHM_GUARD -> RhythmGuardSettingsScreen(onBackClick = handleBack)
                        SettingsRoutes.BATTERY_SAVER -> PerformanceSettingsScreen(onBackClick = handleBack)
                        else -> PlaceholderSettingsScreen()
                    }
                }
            }
        }
    } else {
        // Phone layout: Traditional navigation with AnimatedContent
        AnimatedContent(
            targetState = routeStack,
            transitionSpec = {
                val isBacking = targetState.size < initialState.size
                if (!isBacking && targetState.isNotEmpty()) {
                    slideInHorizontally(
                        initialOffsetX = { it },
                        animationSpec = tween(
                            durationMillis = 400,
                            easing = androidx.compose.animation.core.EaseOutCubic
                        )
                    ) + fadeIn(
                        animationSpec = tween(
                            durationMillis = 350,
                            delayMillis = 50
                        )
                    ) + scaleIn(
                        initialScale = 0.92f,
                        animationSpec = tween(
                            durationMillis = 400,
                            easing = androidx.compose.animation.core.EaseOutCubic
                        )
                    ) togetherWith
                    slideOutHorizontally(
                        targetOffsetX = { -it / 4 },
                        animationSpec = tween(
                            durationMillis = 350,
                            easing = androidx.compose.animation.core.EaseInCubic
                        )
                    ) + fadeOut(
                        animationSpec = tween(durationMillis = 250)
                    ) + scaleOut(
                        targetScale = 0.95f,
                        animationSpec = tween(
                            durationMillis = 350,
                            easing = androidx.compose.animation.core.EaseInCubic
                        )
                    )
                } else {
                    slideInHorizontally(
                        initialOffsetX = { -it / 4 },
                        animationSpec = tween(
                            durationMillis = 400,
                            easing = androidx.compose.animation.core.EaseOutCubic
                        )
                    ) + fadeIn(
                        animationSpec = tween(
                            durationMillis = 350,
                            delayMillis = 50
                        )
                    ) + scaleIn(
                        initialScale = 0.95f,
                        animationSpec = tween(
                            durationMillis = 400,
                            easing = androidx.compose.animation.core.EaseOutCubic
                        )
                    ) togetherWith
                    slideOutHorizontally(
                        targetOffsetX = { it },
                        animationSpec = tween(
                            durationMillis = 350,
                            easing = androidx.compose.animation.core.EaseInCubic
                        )
                    ) + fadeOut(
                        animationSpec = tween(durationMillis = 250)
                    ) + scaleOut(
                        targetScale = 0.92f,
                        animationSpec = tween(
                            durationMillis = 350,
                            easing = androidx.compose.animation.core.EaseInCubic
                        )
                    )
                }
            },
            label = "settings_navigation",
            contentKey = { it.lastOrNull() ?: "main_settings" }
        ) { stack ->
            val route = stack.lastOrNull()
            when (route) {
                SettingsRoutes.NOTIFICATIONS -> NotificationsSettingsScreen(onBackClick = handleBack)
                SettingsRoutes.PLAYLISTS -> PlaylistsSettingsScreen(
                    onBackClick = handleBack,
                    onNavigateTo = onNavigateToSubsetting,
                    playlistAdmin = playlistAdmin
                )
                SettingsRoutes.DEFAULT_PLAYLISTS -> DefaultPlaylistsSettingsScreen(
                    onBackClick = handleBack,
                    playlistAdmin = playlistAdmin
                )
                SettingsRoutes.MEDIA_SCAN -> MediaScanSettingsScreen(onBackClick = handleBack, libraryAdmin = libraryAdmin)
                SettingsRoutes.ARTIST_SEPARATORS -> ArtistSeparatorsSettingsScreen(onBackClick = handleBack)
                SettingsRoutes.ABOUT -> chromahub.rhythm.app.shared.presentation.screens.settings.AboutScreen(
                    onBackClick = handleBack,
                    onNavigateToUpdates = { onNavigateToSubsetting(SettingsRoutes.UPDATES) },
                    onNavigateTo = onNavigateToSubsetting
                )
                SettingsRoutes.NATIVE_CAPABILITIES, SettingsRoutes.DEVICE_CAPABILITIES -> NativeCapabilitiesSettingsScreen(
                    onBackClick = handleBack
                )
                SettingsRoutes.EXTERNAL_CAPABILITIES, SettingsRoutes.AUDIO_CAPABILITIES, SettingsRoutes.AUDIO_ENGINE -> ExternalCapabilitiesSettingsScreen(
                    onBackClick = handleBack,
                    onNavigateTo = onNavigateToSubsetting
                )
                SettingsRoutes.DECODER_EXTENSIONS, SettingsRoutes.FALLBACK_ENGINES -> DecoderExtensionsSettingsScreen(
                    onBackClick = handleBack
                )
                SettingsRoutes.UPDATES -> UpdatesSettingsScreen(onBackClick = handleBack)
                SettingsRoutes.LABS, SettingsRoutes.EXPERIMENTAL_FEATURES -> LabsSettingsScreen(
                    onBackClick = handleBack,
                    onNavigateTo = onNavigateToSubsetting,
                    onNavigateToGoSettings = { onNavigateToSubsetting(SettingsRoutes.GO_SETTINGS) }
                )
                SettingsRoutes.API_MANAGEMENT -> ApiManagementSettingsScreen(onBackClick = handleBack)
                SettingsRoutes.CACHE_MANAGEMENT -> CacheManagementSettingsScreen(
                            onBackClick = handleBack,
                            cacheManager = cacheManager,
                            libraryAdmin = libraryAdmin
                        )
                SettingsRoutes.BACKUP_RESTORE -> BackupRestoreSettingsScreen(
                            onBackClick = handleBack,
                            libraryAdmin = libraryAdmin,
                            backupRestore = backupRestore,
                            playlistAdmin = playlistAdmin
                        )
                SettingsRoutes.THEME_CUSTOMIZATION -> ThemeCustomizationSettingsScreen(onBackClick = handleBack)
                SettingsRoutes.PLAYER_CUSTOMIZATION -> PlayerCustomizationSettingsScreen(onBackClick = handleBack)
                SettingsRoutes.MINIPLAYER_CUSTOMIZATION -> MiniPlayerCustomizationSettingsScreen(onBackClick = handleBack)
                SettingsRoutes.CRASH_LOG_HISTORY -> CrashLogHistorySettingsScreen(onBackClick = handleBack, appSettings = appSettings)
                SettingsRoutes.QUEUE -> QueueSettingsScreen(onBackClick = handleBack)
                SettingsRoutes.PLAYBACK -> PlaybackSettingsScreen(
                    onBackClick = handleBack,
                    onNavigateTo = onNavigateToSubsetting,
                    playbackSettings = playbackSettings
                )
                SettingsRoutes.REPLAY_GAIN -> ReplayGainSettingsScreen(
                    onBackClick = handleBack,
                    onNavigateTo = onNavigateToSubsetting
                )
                SettingsRoutes.CROSSFADE -> CrossfadeSettingsScreen(
                    onBackClick = handleBack,
                    onNavigateTo = onNavigateToSubsetting
                )
                SettingsRoutes.LYRICS -> LyricsSettingsScreen(onBackClick = handleBack)
                SettingsRoutes.WIDGET -> WidgetSettingsScreen(onBackClick = handleBack)
                SettingsRoutes.HOME_SCREEN -> HomeScreenCustomizationSettingsScreen(onBackClick = handleBack)
                SettingsRoutes.GESTURES -> GesturesSettingsScreen(onBackClick = handleBack)
                SettingsRoutes.EXPRESSIVE_SHAPES -> ExpressiveShapesSettingsScreen(onBackClick = handleBack)
                SettingsRoutes.LIBRARY_SETTINGS -> LibrarySettingsScreen(onBackClick = handleBack)
                SettingsRoutes.RHYTHM_GUARD -> RhythmGuardSettingsScreen(onBackClick = handleBack)
                SettingsRoutes.BATTERY_SAVER -> PerformanceSettingsScreen(onBackClick = handleBack)
                else -> SettingsScreen(
                    onBackClick = handleBack,
                    onNavigateTo = onNavigateToSubsetting,
                    scrollState = mainSettingsScrollState
                )
            }
        }
    }

    if (showSleepTimerBottomSheet) {
        SleepTimerBottomSheetNew(
            onDismiss = { showSleepTimerBottomSheet = false },
            currentSong = currentSong,
            isPlaying = isPlaying,
            sleepTimer = sleepTimer,
            appSettings = appSettings
        )
    }
}
