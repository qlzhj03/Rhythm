package chromahub.rhythm.app.shared.presentation.screens.settings

/**
 * String route constants for settings destinations. Extracted from
 * SettingsScreen (PR-5) so :core:ui's SettingsSearch and the settings screens
 * share one source of truth.
 */
object SettingsRoutes {
    const val NOTIFICATIONS = "notifications_settings"
    const val LABS = "labs_settings"
    const val EXPERIMENTAL_FEATURES = LABS
    const val ABOUT = "about_screen"
    const val UPDATES = "updates_screen"
    const val MEDIA_SCAN = "media_scan_settings"
    const val ARTIST_SEPARATORS = "artist_separators_settings"
    const val PLAYLISTS = "playlist_settings"
    const val DEFAULT_PLAYLISTS = "default_playlists_settings"
    const val API_MANAGEMENT = "api_management_settings"
    const val CACHE_MANAGEMENT = "cache_management_settings"
    const val BACKUP_RESTORE = "backup_restore_settings"
    const val THEME_CUSTOMIZATION = "theme_customization_settings"
    const val PLAYER_CUSTOMIZATION = "player_customization_settings"
    const val MINIPLAYER_CUSTOMIZATION = "miniplayer_customization_settings"
    const val EQUALIZER = "equalizer_settings"
    const val SLEEP_TIMER = "sleep_timer_settings"
    const val CRASH_LOG_HISTORY = "crash_log_history_settings"
    const val QUEUE = "queue_settings"
    const val PLAYBACK = "playback_settings"
    const val LYRICS = "lyrics_settings"
    const val WIDGET = "widget_settings"
    const val HOME_SCREEN = "home_screen_settings"
    const val GESTURES = "gestures_settings"
    const val RHYTHM_STATS = "rhythm_stats"
    // Local-NavHost destinations the settings scaffold links into (B1 PR-9):
    // the local feature's Screen sealed class lives in :feature:local, which
    // :feature:settings no longer depends on, so the literals live here.
    const val LOCAL_EQUALIZER = "equalizer"
    const val EXPRESSIVE_SHAPES = "expressive_shapes_settings"
    const val LIBRARY_SETTINGS = "library_settings"
    const val RHYTHM_GUARD = "rhythm_guard_settings"
    @Deprecated("Use RHYTHM_GUARD")
    const val RHYTHM_AURA = RHYTHM_GUARD
    const val GO_SETTINGS = "go_settings"
    const val BATTERY_SAVER = "battery_saver_settings"
    const val REPLAY_GAIN = "replay_gain_settings"
    const val CROSSFADE = "crossfade_settings"
    const val NATIVE_CAPABILITIES = "tuner_native_capabilities_settings"
    const val EXTERNAL_CAPABILITIES = "tuner_external_capabilities_settings"
    const val DECODER_EXTENSIONS = "tuner_decoder_extensions_settings"
    const val DEVICE_CAPABILITIES = NATIVE_CAPABILITIES
    const val AUDIO_CAPABILITIES = EXTERNAL_CAPABILITIES
    const val AUDIO_ENGINE = EXTERNAL_CAPABILITIES
    const val FALLBACK_ENGINES = DECODER_EXTENSIONS
}
