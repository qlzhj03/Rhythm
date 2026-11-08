package chromahub.rhythm.app.features.local.presentation.navigation

import android.net.Uri
import chromahub.rhythm.app.features.local.presentation.screens.LibraryTab
import com.qlzhj.rhythm.core.ui.shared.presentation.screens.settings.SettingsRoutes

/**
 * Screen route constants for the inner navigation graph. Extracted from
 * LocalNavigation (PR-5) so the player screens in :feature:local can build
 * routes while the NavHost assembly itself stays in :app's composition root.
 */
sealed class Screen(val route: String) {
    object Home : Screen("home")
    object Search : Screen("search")
    object Library : Screen("library?tab={tab}") {
        fun createRoute(tab: LibraryTab = LibraryTab.SONGS): String = "library?tab=${tab.name.lowercase()}"
    }
    object Player : Screen("player")
    object Settings : Screen("settings")
    object AddToPlaylist : Screen("add_to_playlist")
    object PlaylistDetail : Screen("playlist/{playlistId}") {
        fun createRoute(playlistId: String) = "playlist/$playlistId"
    }
    object ArtistDetail : Screen("artist/{artistName}") {
        fun createRoute(artistName: String) = "artist/${Uri.encode(artistName)}"
    }
    object AlbumDetail : Screen("album/{albumId}?albumName={albumName}") {
        fun createRoute(albumId: String, albumName: String): String {
            val safeId = if (albumId.contains('/')) albumId.replace('/', '_') else albumId
            return "album/${Uri.encode(safeId)}?albumName=${Uri.encode(albumName)}"
        }
    }
    
    // Tuner Settings Subroutes
    object TunerNotifications : Screen("tuner_notifications_settings")
    object TunerLabs : Screen("tuner_labs_settings")
    val TunerExperimentalFeatures = TunerLabs
    object TunerAbout : Screen("tuner_about_screen")
    object TunerUpdates : Screen("tuner_updates_screen")
    object TunerMediaScan : Screen("tuner_media_scan_settings")
    object TunerPlaylists : Screen("tuner_playlist_settings")
    object TunerApiManagement : Screen("tuner_api_management_settings")
    object TunerCacheManagement : Screen("tuner_cache_management_settings")
    object TunerBackupRestore : Screen("tuner_backup_restore_settings")
    object TunerThemeCustomization : Screen("tuner_theme_customization_settings")
    object TunerEqualizer : Screen("tuner_equalizer_settings")
    object TunerSleepTimer : Screen("tuner_sleep_timer_settings")
    object TunerCrashLogHistory : Screen("tuner_crash_log_history_settings")
    object TunerQueue : Screen("tuner_queue_settings")
    object TunerPlayback : Screen("tuner_playback_settings")
    object TunerHomeScreen : Screen("tuner_home_screen_settings")
    object TunerExpressiveShapes : Screen("tuner_expressive_shapes_settings")
    object TunerPlayerCustomization : Screen("tuner_player_customization_settings")
    object TunerMiniPlayerCustomization : Screen("tuner_miniplayer_customization_settings")
    object TunerBatterySaver : Screen("tuner_battery_saver_settings")
    object TunerLibrarySettings : Screen("tuner_library_settings")
    object TunerRhythmGuard : Screen("tuner_rhythm_guard_settings")
    object TunerLyrics : Screen("tuner_lyrics_settings")
    object TunerGestures : Screen("tuner_gestures_settings")
    object TunerWidget : Screen("tuner_widget_settings")
    object TunerArtistSeparators : Screen("tuner_artist_separators_settings")
    object TunerGoSettings : Screen("tuner_go_settings")
    object TunerReplayGain : Screen("tuner_replay_gain_settings")
    object TunerCrossfade : Screen("tuner_crossfade_settings")
    object TunerDefaultPlaylists : Screen("tuner_default_playlists_settings")
    object TunerNativeCapabilities : Screen(SettingsRoutes.NATIVE_CAPABILITIES)
    val TunerDeviceCapabilities = TunerNativeCapabilities
    object TunerExternalCapabilities : Screen(SettingsRoutes.EXTERNAL_CAPABILITIES)
    val TunerAudioCapabilities = TunerExternalCapabilities
    val TunerAudioEngine = TunerExternalCapabilities
    object TunerDecoderExtensions : Screen(SettingsRoutes.DECODER_EXTENSIONS)
    val TunerFallbackEngines = TunerDecoderExtensions
    
    // Stats Screen
    object RhythmStats : Screen("rhythm_stats")
    object Equalizer : Screen("equalizer")
}
