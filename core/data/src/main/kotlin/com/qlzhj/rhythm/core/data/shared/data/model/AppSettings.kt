package com.qlzhj.rhythm.core.data.shared.data.model

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import android.util.Log
import com.qlzhj.rhythm.core.data.R
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.qlzhj.rhythm.core.common.GsonUtils
import com.qlzhj.rhythm.core.common.ArtistSeparator
import com.qlzhj.rhythm.core.common.StoragePathUtils
import com.qlzhj.rhythm.core.common.AppStrings
import java.io.File
import java.util.Date // Import Date for timestamp
import androidx.core.content.edit
import androidx.core.net.toUri
import com.qlzhj.rhythm.core.model.AutoEQProfile
import com.qlzhj.rhythm.core.model.CanvasNetworkMode
import com.qlzhj.rhythm.core.model.CustomEqualizerPreset
import com.qlzhj.rhythm.core.model.LyricsSourcePreference
import com.qlzhj.rhythm.core.model.MediaScanMode
import com.qlzhj.rhythm.core.model.Playlist
import com.qlzhj.rhythm.core.model.Song

/**
 * Data class to represent a single crash log entry
 */
data class CrashLogEntry(
    val timestamp: Long,
    val log: String
)

/**
 * Enum for album view types in the library
 */
enum class AlbumViewType {
    LIST, GRID
}

/**
 * Enum for artist view types in the library
 */
enum class ArtistViewType {
    LIST, GRID
}

/**
 * Enum for playlist view types in the library
 */
enum class PlaylistViewType {
    LIST, GRID
}

/**
 * Enum for artist artwork source preferences
 */
enum class ArtistArtworkSource {
    PREFER_LOCAL_THEN_API,
    LOCAL_ONLY,
    API_ONLY,
    DISABLED;

    companion object {
        fun fromName(name: String?): ArtistArtworkSource {
            return entries.find { it.name.equals(name, ignoreCase = true) } ?: PREFER_LOCAL_THEN_API
        }
    }
}

data class RhythmAuraPolicyBand(
    val minAge: Int,
    val maxAge: Int,
    val maxVolumeThreshold: Float,
    val recommendedDailyMinutes: Int,
    val stopPlaybackOnZeroVolume: Boolean,
    val enforceHapticFeedback: Boolean
)

typealias RhythmGuardPolicyBand = RhythmAuraPolicyBand

private val RHYTHM_AURA_POLICY_BANDS = listOf(
    RhythmAuraPolicyBand(8, 12, 0.50f, 40, true, true),
    RhythmAuraPolicyBand(13, 15, 0.56f, 55, true, true),
    RhythmAuraPolicyBand(16, 17, 0.60f, 70, true, true),
    RhythmAuraPolicyBand(18, 25, 0.68f, 95, false, false),
    RhythmAuraPolicyBand(26, 40, 0.72f, 120, false, false),
    RhythmAuraPolicyBand(41, 55, 0.70f, 105, false, false),
    RhythmAuraPolicyBand(56, 80, 0.65f, 90, true, true)
)

private val RHYTHM_GUARD_POLICY_BANDS = RHYTHM_AURA_POLICY_BANDS


/**
 * Priority order for lyrics APIs
 */
enum class LyricsApiPriority(val displayName: String) {
    LYRICALLY_FIRST("Lyrically"),
    LRCLIB_FIRST("LRCLib"),
    BETTERLYRICS_FIRST("Better Lyrics");

    companion object {
        fun fromOrdinal(ordinal: Int): LyricsApiPriority {
            return values().getOrElse(ordinal) { LYRICALLY_FIRST }
        }
    }
}

/**
 * Singleton class to manage all app settings using SharedPreferences
 */
class AppSettings private constructor(context: Context) : com.qlzhj.rhythm.core.network.network.NetworkSettingsProvider {
    companion object {
        private const val PREFS_NAME = "rhythm_preferences"

        /**
         * Background warm-up of the settings SharedPreferences file so the first
         * main-thread reads (AppSettings init) don't block on the async XML load
         * (B-phase B2 startup preheat). Call as early as possible in Application.
         */
        fun preheat(context: Context) {
            Thread {
                android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_BACKGROUND)
                try {
                    context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getString("", null)
                } catch (_: Exception) {
                }
            }.start()
        }
        
        // Playback Settings
        internal const val KEY_GAPLESS_PLAYBACK = "gapless_playback"
        internal const val KEY_CROSSFADE = "crossfade"
        internal const val KEY_CROSSFADE_DURATION = "crossfade_duration"
        internal const val KEY_CROSSFADE_REPEAT_ONE = "crossfade_repeat_one"
        internal const val KEY_CROSSFADE_ON_SKIP = "crossfade_on_skip"
        internal const val KEY_AUDIO_NORMALIZATION = "audio_normalization"
        internal const val KEY_REPLAY_GAIN = "replay_gain"
        internal const val KEY_REPLAY_GAIN_MODE = "replay_gain_mode"
        internal const val KEY_REPLAY_GAIN_DRC = "replay_gain_drc"
        internal const val KEY_REPLAY_GAIN_PREAMP = "replay_gain_preamp"
        internal const val KEY_REPLAY_GAIN_PREAMP_UNTAGGED = "replay_gain_preamp_untagged"
        internal const val KEY_SKIP_SILENCE = "skip_silence_enabled"
        internal const val KEY_AUDIO_ROUTING_MODE = "audio_routing_mode" // "default", "app", "system"
        internal const val KEY_RESUME_ON_DEVICE_RECONNECT = "resume_on_device_reconnect"
        internal const val KEY_AUDIO_OFFLOAD_ENABLED = "audio_offload_enabled"

        internal const val KEY_AUDIO_ENGINE_SOFTWARE_ENABLED = "audio_engine_software_enabled"
        internal const val KEY_AUDIO_ENGINE_PREFER_HARDWARE = "audio_engine_prefer_hardware"
        internal const val KEY_AUDIO_ENGINE_FLOAT_OUTPUT = "audio_engine_float_output"
        internal const val KEY_AUDIO_ENGINE_FLOAT_OUTPUT_MIGRATED = "audio_engine_float_output_v2_migrated"
        internal const val KEY_AUDIO_ENGINE_DISABLED_DECODERS = "audio_engine_disabled_decoders"
        internal const val KEY_DECODER_EXTENSIONS_ENABLED = "audio_decoder_extensions_enabled"
        internal const val KEY_DISABLED_DECODER_EXTENSIONS = "audio_disabled_decoder_extensions"
        
        // Battery Saver Settings
        internal const val KEY_BATTERY_SAVER_ENABLED = "battery_saver_enabled"
        internal const val KEY_BATTERY_SAVER_MODE = "battery_saver_mode" // "auto" or "manual"
        internal const val KEY_BATTERY_SAVER_DISABLE_HAPTICS = "battery_saver_disable_haptics"
        internal const val KEY_BATTERY_SAVER_ENABLE_OFFLOAD = "battery_saver_enable_offload"
        internal const val KEY_BATTERY_SAVER_DISABLE_MARQUEE = "battery_saver_disable_marquee"
        internal const val KEY_BATTERY_SAVER_DISABLE_LOSSLESS_ARTWORK = "battery_saver_disable_lossless_artwork"
        internal const val KEY_BATTERY_SAVER_DISABLE_AUTO_FETCH_ARTWORK = "battery_saver_disable_auto_fetch_artwork"
        internal const val KEY_GLOBAL_MARQUEE_ENABLED = "global_marquee_enabled"
        
        internal const val KEY_PRELOAD_LIMIT = "preload_limit"
        
        // Lyrics Settings
        internal const val KEY_SHOW_LYRICS = "show_lyrics"
        internal const val KEY_ONLINE_ONLY_LYRICS = "online_only_lyrics" // Deprecated, kept for migration
        internal const val KEY_LYRICS_SOURCE_PREFERENCE = "lyrics_source_preference"
        internal const val KEY_SHOW_LYRICS_BACKGROUND_ARTWORK = "show_lyrics_background_artwork"
        internal const val KEY_SHOW_LYRICS_TRANSLATION = "show_lyrics_translation"
        internal const val KEY_SHOW_LYRICS_ROMANIZATION = "show_lyrics_romanization"
        internal const val KEY_KEEP_SCREEN_ON_LYRICS = "keep_screen_on_lyrics"
        internal const val KEY_TAP_LYRICS_TO_FULL_SCREEN = "tap_lyrics_to_full_screen"
        internal const val KEY_LYRICS_API_PRIORITY = "lyrics_api_priority"
        internal const val KEY_LYRICS_API_FALLBACK_RETRY = "lyrics_api_fallback_retry"
        internal const val KEY_AUTO_HIDE_LYRICS_CONTROLS = "auto_hide_lyrics_controls"
        internal const val KEY_LYRIC_BOLD = "lyric_bold"
        internal const val KEY_TRIM_LYRICS = "trim_lyrics"
        internal const val KEY_LYRIC_NO_ANIMATION = "lyric_no_animation"
        internal const val KEY_TRANSLATION_AUTO_WORD = "translation_auto_word"
        
        // Theme Settings
        internal const val KEY_USE_SYSTEM_THEME = "use_system_theme"
        internal const val KEY_DARK_MODE = "dark_mode"
        internal const val KEY_AMOLED_THEME = "amoled_theme"
        internal const val KEY_FLOATING_NAVIGATION_BAR = "floating_navigation_bar"
        internal const val KEY_USE_DYNAMIC_COLORS = "use_dynamic_colors"
        internal const val KEY_CUSTOM_COLOR_SCHEME = "custom_color_scheme"
        internal const val KEY_CUSTOM_FONT = "custom_font"
        internal const val KEY_COLOR_SOURCE = "color_source" // ALBUM_ART, MONET, or CUSTOM
        internal const val KEY_EXTRACTED_ALBUM_COLORS = "extracted_album_colors" // JSON string with color values
        internal const val KEY_FONT_SOURCE = "font_source" // SYSTEM or CUSTOM
        internal const val KEY_CUSTOM_FONT_PATH = "custom_font_path" // Path to imported font file
        internal const val KEY_CUSTOM_FONT_FAMILY = "custom_font_family" // Display name of custom font
        
        // Player Theme Settings
        internal const val KEY_PLAYER_THEME_ID = "player_theme_id" // ID of the selected player theme (default, compact, large, minimal)
        internal const val KEY_MINI_PLAYER_THEME_ID = "miniplayer_theme_id"
        internal const val KEY_USE_EXPERIMENTAL_PLAYER_UI = "use_experimental_player_ui"
        internal const val KEY_ENABLE_ALBUM_EDITING = "enable_album_editing"
        
        // Library Settings
        internal const val KEY_ALBUM_VIEW_TYPE = "album_view_type"
        private const val KEY_ARTIST_VIEW_TYPE = "artist_view_type"
        private const val KEY_PLAYLIST_VIEW_TYPE = "playlist_view_type"
        internal const val KEY_ALBUM_SORT_ORDER = "album_sort_order"
        private const val KEY_PLAYLIST_SORT_ORDER = "playlist_sort_order"
        private const val KEY_PLAYLIST_DETAIL_SORT_ORDER = "playlist_detail_sort_order"
        internal const val KEY_LIBRARY_TAB_ORDER = "library_tab_order"
        internal const val KEY_LIBRARY_COMBINE_DISCS = "library_combine_discs"
        internal const val KEY_PLAYER_CHIP_ORDER = "player_chip_order"
        internal const val KEY_HIDDEN_LIBRARY_TABS = "hidden_library_tabs"
        internal const val KEY_HIDDEN_PLAYER_CHIPS = "hidden_player_chips"
        internal const val KEY_EXPRESSIVE_BOTTOM_BUTTONS_NORMAL = "expressive_bottom_buttons_normal"
        internal const val KEY_EXPRESSIVE_HIDDEN_BOTTOM_BUTTONS_NORMAL = "expressive_hidden_bottom_buttons_normal"
        internal const val KEY_EXPRESSIVE_BOTTOM_BUTTONS_MERGE = "expressive_bottom_buttons_merge"
        internal const val KEY_EXPRESSIVE_HIDDEN_BOTTOM_BUTTONS_MERGE = "expressive_hidden_bottom_buttons_merge"
        const val MAX_EXPRESSIVE_BOTTOM_PINS_NORMAL = 2
        const val MAX_EXPRESSIVE_BOTTOM_BUTTONS_MERGE = 5
        val fixedBottomButtonsNormal = listOf("LYRICS", "FAVORITE")
        internal const val KEY_GROUP_BY_ALBUM_ARTIST = "group_by_album_artist" // New setting for album artist grouping
        internal const val KEY_PREFER_SONG_ARTWORK = "prefer_song_artwork" // Prefer per-song embedded artwork over shared album art
        internal const val KEY_IGNORE_MEDIASTORE_COVERS = "ignore_mediastore_covers" // Legacy key kept for migration compatibility
        internal const val KEY_LOSSLESS_ARTWORK = "lossless_artwork" // Show cover art without downscaling/compression
        internal const val KEY_SHOW_LIBRARY_BOTTOM_BAR_ALWAYS = "show_library_bottom_bar_always"
        
        // Audio Device Settings
        internal const val KEY_AUTO_CONNECT_DEVICE = "auto_connect_device"
        internal const val KEY_USE_SYSTEM_VOLUME = "use_system_volume"
        internal const val KEY_APP_VOLUME = "app_volume"
        internal const val KEY_STOP_PLAYBACK_ON_ZERO_VOLUME = "stop_playback_on_zero_volume"
        internal const val KEY_DISMISSED_AUTOEQ_SUGGESTIONS = "dismissed_autoeq_suggestions"
        
        // Equalizer Settings
        internal const val KEY_EQUALIZER_ENABLED = "equalizer_enabled"
        internal const val KEY_EQUALIZER_PRESET = "equalizer_preset"
        internal const val KEY_EQUALIZER_BAND_LEVELS = "equalizer_band_levels"
        internal const val KEY_AUTOEQ_PROFILE = "autoeq_profile"
        private const val KEY_USER_AUDIO_DEVICES = "user_audio_devices"
        internal const val KEY_ACTIVE_AUDIO_DEVICE_ID = "active_audio_device_id"
        internal const val KEY_BASS_BOOST_ENABLED = "bass_boost_enabled"
        internal const val KEY_BASS_BOOST_STRENGTH = "bass_boost_strength"
        internal const val KEY_BASS_BOOST_AVAILABLE = "bass_boost_available"
        internal const val KEY_VIRTUALIZER_ENABLED = "virtualizer_enabled"
        internal const val KEY_VIRTUALIZER_STRENGTH = "virtualizer_strength"
        internal const val KEY_MONO_AUDIO_ENABLED = "mono_audio_enabled"
        internal const val KEY_CUSTOM_EQUALIZER_PRESETS = "custom_equalizer_presets"
        internal const val KEY_EQUALIZER_PRESET_ORDER = "equalizer_preset_order"
        internal const val KEY_HIDDEN_EQUALIZER_PRESETS = "hidden_equalizer_presets"
        internal const val KEY_PINNED_AUTOEQ_PROFILES = "pinned_autoeq_profiles"
        internal const val KEY_CUSTOM_AUTOEQ_PROFILES = "custom_autoeq_profiles"
        internal const val KEY_SPEAKER_AUTOEQ_BYPASS = "speaker_autoeq_bypass"
        
        // Cache Settings
        internal const val KEY_MAX_CACHE_SIZE = "max_cache_size"

        
        // Search History
        internal const val KEY_SEARCH_HISTORY = "search_history"
        internal const val KEY_SHOW_KEYBOARD_ON_SEARCH_OPEN = "show_keyboard_on_search_open"
        
        // Playlists
        private const val KEY_PLAYLISTS = "playlists"
        private const val KEY_FAVORITE_SONGS = "favorite_songs"
        private const val KEY_DEFAULT_PLAYLISTS_ENABLED = "default_playlists_enabled"
        private const val KEY_SHOW_LIKED_IN_PLAYLISTS = "show_liked_in_playlists"
        private const val KEY_SMART_PLAYLIST_RECENTLY_ADDED = "smart_playlist_recently_added"
        private const val KEY_SMART_PLAYLIST_MOST_PLAYED = "smart_playlist_most_played"
        private const val KEY_SMART_PLAYLIST_ON_REPEAT = "smart_playlist_on_repeat"
        private const val KEY_SMART_PLAYLIST_FORGOTTEN_FAVORITES = "smart_playlist_forgotten_favorites"
        private const val KEY_SMART_PLAYLIST_RECENTLY_PLAYED = "smart_playlist_recently_played"
        
        // User Statistics
        internal const val KEY_LISTENING_TIME = "listening_time"
        internal const val KEY_SONGS_PLAYED = "songs_played"
        internal const val KEY_UNIQUE_ARTISTS = "unique_artists"
        internal const val KEY_GENRE_PREFERENCES = "genre_preferences"
        internal const val KEY_TIME_BASED_PREFERENCES = "time_based_preferences"

        // Rhythm Guard (listening health)
        internal const val KEY_RHYTHM_GUARD_MODE = "rhythm_guard_mode"
        internal const val KEY_RHYTHM_GUARD_AGE = "rhythm_guard_age"
        internal const val KEY_RHYTHM_GUARD_MANUAL_WARNINGS_ENABLED = "rhythm_guard_manual_warnings_enabled"
        internal const val KEY_RHYTHM_GUARD_MANUAL_VOLUME_THRESHOLD = "rhythm_guard_manual_volume_threshold"
        internal const val KEY_RHYTHM_GUARD_APPLY_VOLUME_LIMIT_ON_SPEAKER = "rhythm_guard_apply_volume_limit_on_speaker"
        internal const val KEY_RHYTHM_GUARD_LAST_AUTO_APPLIED_AT = "rhythm_guard_last_auto_applied_at"
        internal const val KEY_RHYTHM_GUARD_ALERT_THRESHOLD_MINUTES = "rhythm_guard_alert_threshold_minutes"
        internal const val KEY_RHYTHM_GUARD_WARNING_TIMEOUT_MINUTES = "rhythm_guard_warning_timeout_minutes"
        internal const val KEY_RHYTHM_GUARD_POST_TIMEOUT_COOLDOWN_MINUTES = "rhythm_guard_post_timeout_cooldown_minutes"
        internal const val KEY_RHYTHM_GUARD_BREAK_RESUME_MINUTES = "rhythm_guard_break_resume_minutes"
        internal const val KEY_RHYTHM_GUARD_TIMEOUT_UNTIL_MS = "rhythm_guard_timeout_until_ms"
        internal const val KEY_RHYTHM_GUARD_TIMEOUT_REASON = "rhythm_guard_timeout_reason"
        internal const val KEY_RHYTHM_GUARD_TIMEOUT_STARTED_AT_MS = "rhythm_guard_timeout_started_at_ms"
        internal const val KEY_RHYTHM_GUARD_TIMEOUT_COOLDOWN_UNTIL_MS = "rhythm_guard_timeout_cooldown_until_ms"
        internal const val KEY_RHYTHM_GUARD_NEXT_ALLOWED_LIMIT_MINUTES = "rhythm_guard_next_allowed_limit_minutes"
        internal const val KEY_RHYTHM_GUARD_FIRST_BREAK_SEEN = "rhythm_guard_first_break_seen"
        internal const val KEY_HOME_SHOW_RHYTHM_GUARD = "home_show_rhythm_guard"

        // Legacy keys kept for migration compatibility.
        internal const val KEY_RHYTHM_AURA_MODE = "rhythm_aura_mode"
        internal const val KEY_RHYTHM_AURA_AGE = "rhythm_aura_age"
        internal const val KEY_RHYTHM_AURA_MANUAL_WARNINGS_ENABLED = "rhythm_aura_manual_warnings_enabled"
        internal const val KEY_RHYTHM_AURA_MANUAL_VOLUME_THRESHOLD = "rhythm_aura_manual_volume_threshold"
        internal const val KEY_RHYTHM_AURA_LAST_AUTO_APPLIED_AT = "rhythm_aura_last_auto_applied_at"

        const val RHYTHM_GUARD_MODE_OFF = "OFF"
        const val RHYTHM_GUARD_MODE_AUTO = "AUTO"
        const val RHYTHM_GUARD_MODE_MANUAL = "MANUAL"

        @Deprecated("Use AppSettings.RHYTHM_GUARD_MODE_OFF")
        const val RHYTHM_AURA_MODE_OFF = RHYTHM_GUARD_MODE_OFF
        @Deprecated("Use AppSettings.RHYTHM_GUARD_MODE_AUTO")
        const val RHYTHM_AURA_MODE_AUTO = RHYTHM_GUARD_MODE_AUTO
        @Deprecated("Use AppSettings.RHYTHM_GUARD_MODE_MANUAL")
        const val RHYTHM_AURA_MODE_MANUAL = RHYTHM_GUARD_MODE_MANUAL
        
        // Recently Played
        internal const val KEY_RECENTLY_PLAYED = "recently_played"
        private const val KEY_RECENTLY_PLAYED_SONG_CACHE = "recently_played_song_cache"
        internal const val KEY_LAST_PLAYED_TIMESTAMP = "last_played_timestamp"
        
        // API Integration
        private const val KEY_INTEGRATIONS_ENABLED = "integrations_enabled"
        private const val KEY_DEEZER_API_ENABLED = "deezer_api_enabled"
        private const val KEY_LRCLIB_API_ENABLED = "lrclib_api_enabled"
        private const val KEY_BETTERLYRICS_API_ENABLED = "better_lyrics_api_enabled"
        private const val KEY_YTMUSIC_API_ENABLED = "ytmusic_api_enabled"
        private const val KEY_SPOTIFY_API_ENABLED = "spotify_api_enabled"
        private const val KEY_SPOTIFY_CLIENT_ID = "spotify_client_id"
        private const val KEY_SPOTIFY_CLIENT_SECRET = "spotify_client_secret"
        private const val KEY_LYRICALLY_API_ENABLED = "lyrically_api_enabled"
        private const val KEY_WIKIPEDIA_API_ENABLED = "wikipedia_api_enabled"
        internal const val KEY_AUTO_FETCH_ARTWORK = "auto_fetch_artwork"
        private const val KEY_ARTIST_ARTWORK_SOURCE = "artist_artwork_source"
        private const val KEY_APPLE_CANVAS_ENABLED = "apple_canvas_enabled"
        private const val KEY_APPLE_CANVAS_NETWORK_MODE = "apple_canvas_network_mode"
        
        // General Broadcast Status Settings (for Tasker, KWGT, etc.)
        private const val KEY_BROADCAST_STATUS_ENABLED = "broadcast_status_enabled"
        internal const val KEY_BLUETOOTH_LYRICS_ENABLED = "bluetooth_lyrics_enabled"
        
        // Enhanced User Preferences
        private const val KEY_FAVORITE_GENRES = "favorite_genres"
        internal const val KEY_DAILY_LISTENING_STATS = "daily_listening_stats"
        internal const val KEY_WEEKLY_TOP_ARTISTS = "weekly_top_artists"
        internal const val KEY_MOOD_PREFERENCES = "mood_preferences"
        
        // Song Play Counts
        private const val KEY_SONG_PLAY_COUNTS = "song_play_counts"

        // Onboarding
        internal const val KEY_ONBOARDING_COMPLETED = "onboarding_completed"
        internal const val KEY_INITIAL_MEDIA_SCAN_COMPLETED = "initial_media_scan_completed"
        internal const val KEY_GENRE_DETECTION_COMPLETED = "genre_detection_completed"
        internal const val KEY_AUDIO_METADATA_EXTRACTION_COMPLETED = "audio_metadata_extraction_completed"
        internal const val KEY_EMBEDDED_ARTWORK_EXTRACTION_COMPLETED = "embedded_artwork_extraction_completed"
        internal const val KEY_EMBEDDED_ARTWORK_EXTRACTION_LOSSLESS_STATUS = "embedded_artwork_extraction_lossless_status"

        // App Updater Settings
        private const val KEY_AUTO_CHECK_FOR_UPDATES = "auto_check_for_updates"
        internal const val KEY_UPDATE_CHANNEL = "update_channel" // New key for update channel
        internal const val KEY_UPDATE_SOURCE = "update_source" // New key for OTA source flavor
        internal const val KEY_UPDATES_ENABLED = "updates_enabled" // Master switch for updates
        internal const val KEY_UPDATE_NOTIFICATIONS_ENABLED = "update_notifications_enabled" // Push-style notifications
        internal const val KEY_UPDATE_STATUS_NOTIFICATIONS_ENABLED = "update_status_notifications_enabled" // Notify for no-update/error states
        internal const val KEY_MEDIA_SCAN_MODE = "media_scan_mode" // Mode for media scanning: "blacklist" or "whitelist"
        internal const val KEY_INCLUDE_HIDDEN_WHITELISTED_MEDIA = "include_hidden_whitelisted_media"
        internal const val KEY_UPDATE_CHECK_INTERVAL_HOURS = "update_check_interval_hours" // Configurable interval

        // Beta Program
        internal const val KEY_HAS_SHOWN_BETA_POPUP = "has_shown_beta_popup"

        // Crash Reporting
        internal const val KEY_LAST_CRASH_LOG = "last_crash_log"
        internal const val KEY_CRASH_LOG_HISTORY = "crash_log_history" // New key for crash log history
        
        // Haptic Feedback
        private const val KEY_HAPTIC_FEEDBACK_ENABLED = "haptic_feedback_enabled"
        
        // Notification Settings
    internal const val KEY_USE_CUSTOM_NOTIFICATION = "use_custom_notification"
    internal const val KEY_LIBRARY_OPERATIONS_NOTIFICATIONS_ENABLED = "library_operations_notifications_enabled"
    internal const val KEY_SLEEP_TIMER_NOTIFICATIONS_ENABLED = "sleep_timer_notifications_enabled"
    internal const val KEY_STREAMING_NOTIFICATIONS_ENABLED = "streaming_notifications_enabled"
    internal const val KEY_RHYTHM_GUARD_ALERT_NOTIFICATIONS_ENABLED = "rhythm_guard_alert_notifications_enabled"
    internal const val KEY_RHYTHM_GUARD_TIMER_NOTIFICATIONS_ENABLED = "rhythm_guard_timer_notifications_enabled"
    internal const val KEY_RHYTHM_PULSE_NOTIFICATIONS_ENABLED = "rhythm_pulse_notifications_enabled"
    internal const val KEY_RHYTHM_PULSE_NOTIFICATION_INTERVAL_HOURS = "rhythm_pulse_notification_interval_hours"
    
    // UI Settings
    private const val KEY_DEFAULT_SCREEN = "default_screen"
    internal const val KEY_FORCE_PLAYER_COMPACT_MODE = "force_player_compact_mode"
    
        // Codec Monitoring & Enhanced Seeking
        internal const val KEY_CODEC_MONITORING_ENABLED = "codec_monitoring_enabled"
        internal const val KEY_SHOW_CODEC_NOTIFICATIONS = "show_codec_notifications"
        internal const val KEY_ENHANCED_SEEKING_ENABLED = "enhanced_seeking_enabled"
        
        // Media3 1.9.0 Features
        internal const val KEY_TRACK_ERROR_CHECKER_ENABLED = "track_error_checker_enabled"
        
        // Festive Theme Settings
        internal const val KEY_FESTIVE_THEME_ENABLED = "festive_theme_enabled"
        internal const val KEY_FESTIVE_THEME_TYPE = "festive_theme_type"
        internal const val KEY_FESTIVE_THEME_INTENSITY = "festive_theme_intensity"
        internal const val KEY_FESTIVE_THEME_AUTO_DETECT = "festive_theme_auto_detect"
        private const val KEY_FESTIVE_SNOWFLAKE_SIZE = "festive_snowflake_size"
        private const val KEY_FESTIVE_SNOWFLAKE_AREA = "festive_snowflake_area"
        
        // Festive Decoration Position Settings
        private const val KEY_FESTIVE_SHOW_TOP_LIGHTS = "festive_show_top_lights"
        private const val KEY_FESTIVE_SHOW_SIDE_GARLAND = "festive_show_side_garland"
        private const val KEY_FESTIVE_SHOW_BOTTOM_SNOW = "festive_show_bottom_snow"
        private const val KEY_FESTIVE_SHOW_SNOWFALL = "festive_show_snowfall"
        
        // Blacklisted Songs
        internal const val KEY_BLACKLISTED_SONGS = "blacklisted_songs"
        
        // Blacklisted Folders
        internal const val KEY_BLACKLISTED_FOLDERS = "blacklisted_folders"
        
        // Whitelisted Songs
        internal const val KEY_WHITELISTED_SONGS = "whitelisted_songs"
        
        // Whitelisted Folders
        internal const val KEY_WHITELISTED_FOLDERS = "whitelisted_folders"

        // Pinned Folders (Explorer)
        internal const val KEY_PINNED_FOLDERS = "pinned_folders"
        
        // Playlist Playback Behavior
        
        // Backup and Restore
        internal const val KEY_LAST_BACKUP_TIMESTAMP = "last_backup_timestamp"
        internal const val KEY_AUTO_BACKUP_ENABLED = "auto_backup_enabled"
        internal const val KEY_BACKUP_LOCATION = "backup_location"
        
        // Sleep Timer
        internal const val KEY_SLEEP_TIMER_ACTIVE = "sleep_timer_active"
        internal const val KEY_SLEEP_TIMER_REMAINING_SECONDS = "sleep_timer_remaining_seconds"
        internal const val KEY_SLEEP_TIMER_ACTION = "sleep_timer_action"
        
        // Media Scan Tracking
        internal const val KEY_LAST_SCAN_TIMESTAMP = "last_scan_timestamp"
        internal const val KEY_LAST_SCAN_DURATION = "last_scan_duration"
        internal const val KEY_PENDING_FULL_MEDIA_RESCAN = "pending_full_media_rescan"
        internal const val KEY_LAST_EMBEDDED_ART_SELF_HEAL_MS = "last_embedded_art_self_heal_ms"
        
        // Media Scan Filtering
        internal const val KEY_ALLOWED_FORMATS = "allowed_formats"
        internal const val KEY_MINIMUM_BITRATE = "minimum_bitrate"
        internal const val KEY_MINIMUM_DURATION = "minimum_duration"
        
        // Library Sort Order
        internal const val KEY_SONGS_SORT_ORDER = "songs_sort_order"
        
        // Alphabet Bar Settings
        internal const val KEY_SHOW_ALPHABET_BAR = "show_alphabet_bar"
        private const val KEY_SHOW_SCROLL_TO_TOP = "show_scroll_to_top"
        
        // App Mode Settings (Local vs Streaming)
        internal const val KEY_APP_MODE = "app_mode" // "LOCAL" or "STREAMING"
        internal const val KEY_STREAMING_SERVICE = "streaming_service" // "SPOTIFY", "APPLE_MUSIC", etc.
        internal const val KEY_STREAMING_QUALITY = "streaming_quality" // "LOW", "MEDIUM", "HIGH", "LOSSLESS"
        internal const val KEY_ALLOW_CELLULAR_STREAMING = "allow_cellular_streaming"
        internal const val KEY_OFFLINE_MODE = "offline_mode"
        internal const val KEY_REMEMBER_STREAMING_PASSWORDS = "remember_streaming_passwords"
        
        // Queue & Playback Behavior
        internal const val KEY_SHUFFLE_USES_EXOPLAYER = "shuffle_uses_exoplayer"
        internal const val KEY_AUTO_ADD_TO_QUEUE = "auto_add_to_queue"
        internal const val KEY_RESPECT_ALBUM_ON_PLAY = "respect_album_on_play"
        internal const val KEY_CLEAR_QUEUE_ON_NEW_SONG = "clear_queue_on_new_song"
        internal const val KEY_CONTEXT_QUEUE_PREFERENCE = "context_queue_preference" // ARTIST_FIRST | GENRE_FIRST | ARTIST_THEN_GENRE
        internal const val KEY_CONTEXT_QUEUE_PERSISTENCE = "context_queue_persistence" // EPHEMERAL | PERSISTENT
        internal const val KEY_CONTEXT_QUEUE_SIZE = "context_queue_size" // default number of contextual tracks to build
        internal const val KEY_HIDE_PLAYED_SONGS_IN_QUEUE = "hide_played_songs_in_queue"
        internal const val KEY_SHOW_QUEUE_DIALOG = "show_queue_dialog"
        internal const val KEY_LIST_QUEUE_ACTION_BEHAVIOR = "list_queue_action_behavior" // "replace", "ask", "play_next", "add_to_end"
        internal const val KEY_REPEAT_MODE_PERSISTENCE = "repeat_mode_persistence"
        internal const val KEY_SHUFFLE_MODE_PERSISTENCE = "shuffle_mode_persistence"
        internal const val KEY_KEEP_SHUFFLE_ON_SELECTION = "keep_shuffle_on_selection"
        internal const val KEY_SAVED_SHUFFLE_STATE = "saved_shuffle_state"
        internal const val KEY_SAVED_REPEAT_MODE = "saved_repeat_mode"
        internal const val KEY_PLAYBACK_SPEED = "playback_speed"
        internal const val KEY_DEFAULT_PLAYBACK_SPEED = "default_playback_speed"
        internal const val KEY_USE_DEFAULT_PLAYBACK_SPEED = "use_default_playback_speed"
        internal const val KEY_PLAYBACK_PITCH = "playback_pitch"
        internal const val KEY_SYNC_SPEED_AND_PITCH = "sync_speed_and_pitch"
        internal const val KEY_USE_HOURS_IN_TIME_FORMAT = "use_hours_in_time_format"
        internal const val KEY_SHOW_REMAINING_TIME = "show_remaining_time"
        internal const val KEY_EXPRESSIVE_COLORS = "expressive_colors"
        internal const val KEY_THEME_INTENSITY = "theme_intensity"
        internal const val KEY_STOP_PLAYBACK_ON_APP_CLOSE = "stop_playback_on_app_close"
        internal const val KEY_QUEUE_PERSISTENCE_ENABLED = "queue_persistence_enabled" // Enable/disable queue persistence
        internal const val KEY_SAVED_QUEUE = "saved_queue" // Queue persistence - list of song IDs
        internal const val KEY_SAVED_QUEUE_INDEX = "saved_queue_index" // Current position in queue
        internal const val KEY_SAVED_PLAYBACK_POSITION = "saved_playback_position" // Current playback position in ms
        internal const val KEY_SAVED_ORIGINAL_QUEUE = "saved_original_queue" // Original un-shuffled queue
        internal const val KEY_SAVED_ORIGINAL_QUEUE_SOURCE = "saved_original_queue_source" // Queue source name
        internal const val KEY_HIDE_PLAYED_QUEUE_SONGS = "hide_played_queue_songs" // Hide already-played songs in queue
        
        // Widget Settings
        internal const val KEY_WIDGET_SHOW_ALBUM_ART = "widget_show_album_art"
        internal const val KEY_WIDGET_SHOW_ARTIST = "widget_show_artist"
        internal const val KEY_WIDGET_SHOW_ALBUM = "widget_show_album"
        internal const val KEY_WIDGET_CORNER_RADIUS = "widget_corner_radius"
        internal const val KEY_WIDGET_AUTO_UPDATE = "widget_auto_update"
        internal const val KEY_WIDGET_SHOW_FAVORITE_BUTTON = "widget_show_favorite_button"
        internal const val KEY_WIDGET_THEME = "widget_theme"
        // Rhythm Cookie widget corner actions: 0=skip, 1=shuffle, 2=repeat, 3=favorite, 4=none
        internal const val KEY_WIDGET_COOKIE_BOTTOM_LEFT = "widget_cookie_bottom_left"
        internal const val KEY_WIDGET_COOKIE_BOTTOM_RIGHT = "widget_cookie_bottom_right"
        // Rhythm Stats widget: 0=all time, 1=today, 2=week, 3=month
        internal const val KEY_WIDGET_STATS_RANGE = "widget_stats_range"
        // Rhythm Stats gem: 0=longest streak, 1=current streak, 2=active days, 3=total sessions
        internal const val KEY_WIDGET_STATS_GEM = "widget_stats_gem"
        
        // Global Header Settings
        private const val KEY_HEADER_COLLAPSE_BEHAVIOR = "header_collapse_behavior" // 0=Normal, 1=Always Collapsed (applies to all screens)
        
        // Home Screen Customization Settings - Header
        internal const val KEY_HOME_HEADER_DISPLAY_MODE = "home_header_display_mode" // 0=Icon Only, 1=Name Only, 2=Both
        internal const val KEY_HOME_SHOW_APP_ICON = "home_show_app_icon" // Deprecated - kept for migration
        internal const val KEY_HOME_APP_ICON_VISIBILITY = "home_app_icon_visibility" // 0=Both, 1=Expanded, 2=Collapsed
        
        // Home Screen Customization Settings - Section Visibility
        internal const val KEY_HOME_SHOW_RECENTLY_PLAYED = "home_show_recently_played"
        internal const val KEY_HOME_SHOW_DISCOVER_CAROUSEL = "home_show_discover_carousel"
        internal const val KEY_HOME_SHOW_ARTISTS = "home_show_artists"
        internal const val KEY_HOME_SHOW_NEW_RELEASES = "home_show_new_releases"
        internal const val KEY_HOME_SHOW_RECENTLY_ADDED = "home_show_recently_added"
        internal const val KEY_HOME_SHOW_RECOMMENDED = "home_show_recommended"
        internal const val KEY_HOME_SHOW_LISTENING_STATS = "home_show_listening_stats"
        
        // Home Screen Customization Settings - Discover Widget
        internal const val KEY_HOME_DISCOVER_AUTO_SCROLL = "home_discover_auto_scroll"
        internal const val KEY_HOME_DISCOVER_AUTO_SCROLL_INTERVAL = "home_discover_auto_scroll_interval"
        internal const val KEY_HOME_DISCOVER_ITEM_COUNT = "home_discover_item_count"
        internal const val KEY_HOME_DISCOVER_SHOW_ALBUM_NAME = "home_discover_show_album_name"
        internal const val KEY_HOME_DISCOVER_SHOW_ARTIST_NAME = "home_discover_show_artist_name"
        internal const val KEY_HOME_DISCOVER_SHOW_YEAR = "home_discover_show_year"
        internal const val KEY_HOME_DISCOVER_SHOW_PLAY_BUTTON = "home_discover_show_play_button"
        internal const val KEY_HOME_DISCOVER_SHOW_GRADIENT = "home_discover_show_gradient"
        
        // Home Screen Customization Settings - Section Item Counts
        internal const val KEY_HOME_RECENTLY_PLAYED_COUNT = "home_recently_played_count"
        internal const val KEY_HOME_ARTISTS_COUNT = "home_artists_count"
        internal const val KEY_HOME_NEW_RELEASES_COUNT = "home_new_releases_count"
        internal const val KEY_HOME_RECENTLY_ADDED_COUNT = "home_recently_added_count"
        internal const val KEY_HOME_RECOMMENDED_COUNT = "home_recommended_count"
        
        // Home Screen Customization Settings - Card Appearance
        internal const val KEY_HOME_SECTION_ORDER = "home_section_order"

        // Streaming Home Screen Customization
        private const val KEY_STREAMING_HOME_SHOW_RHYTHM_GUARD = "streaming_home_show_rhythm_guard"
        private const val KEY_STREAMING_HOME_SHOW_RHYTHM_STATS = "streaming_home_show_rhythm_stats"
        private const val KEY_STREAMING_HOME_SHOW_RECENTLY_PLAYED = "streaming_home_show_recently_played"
        private const val KEY_STREAMING_HOME_SHOW_ARTISTS = "streaming_home_show_artists"
        private const val KEY_STREAMING_HOME_SHOW_RECOMMENDED = "streaming_home_show_recommended"
        private const val KEY_STREAMING_HOME_SHOW_NEW_RELEASES = "streaming_home_show_new_releases"
        private const val KEY_STREAMING_HOME_SHOW_PLAYLISTS = "streaming_home_show_playlists"
        private const val KEY_STREAMING_HOME_SECTION_ORDER = "streaming_home_section_order"

        internal const val KEY_ALBUM_BOTTOM_SHEET_GRADIENT_BLUR = "album_bottom_sheet_gradient_blur"
        internal const val KEY_ALBUM_BOTTOM_SHEET_DISC_FILTER = "album_bottom_sheet_disc_filter"
        internal const val KEY_ALBUM_HIDE_ABOUT = "album_hide_about"
        
        // Artist Separator Settings
        const val DEFAULT_ARTIST_SEPARATOR_DELIMITERS = ";/"
        internal const val KEY_ARTIST_SEPARATOR_ENABLED = "artist_separator_enabled"
        internal const val KEY_ARTIST_SEPARATOR_DELIMITERS = "artist_separator_delimiters" // Delimiter string or JSON array
        internal const val KEY_ARTIST_SEPARATOR_CUSTOM_DELIMITERS = "artist_separator_custom_delimiters"
        internal const val KEY_ARTIST_SEPARATOR_CACHE_SIGNATURE = "artist_separator_cache_signature"
        
        // Player Screen Customization Settings
        internal const val KEY_PLAYER_SHOW_GRADIENT_OVERLAY = "player_show_gradient_overlay"
        internal const val KEY_PLAYER_AMBIENT_BACKDROP_ENABLED = "player_ambient_backdrop_enabled" // Ambient backdrop from artwork
        internal const val KEY_PLAYER_AMBIENT_BACKDROP_INTENSITY = "player_ambient_backdrop_intensity" // Float 0.0-1.0, controls container transparency
        internal const val KEY_PLAYER_AMBIENT_INFINITE_ZOOM = "player_ambient_infinite_zoom" // Boolean, continuous slow zoom on ambient backdrop
        internal const val KEY_PLAYER_ACCENT_BACKGROUND_ENABLED = "player_accent_background_enabled" // Use accent color as player bg (normal mode)
        internal const val KEY_PLAYER_MERGE_CONTROLS_TO_BOTTOM = "player_merge_controls_to_bottom" // Merge lyrics/favorite into centered bottom icon controls
        internal const val KEY_PLAYER_GLASS_INTENSITY = "player_glass_intensity" // Float 0.0-2.0, glass effect opacity multiplier
        internal const val KEY_PLAYER_LYRICS_TRANSITION = "player_lyrics_transition" // 0=SlideVertical, 1=Fade, 2=Scale, 3=SlideHorizontal
        internal const val KEY_PLAYER_LYRICS_TEXT_SIZE = "player_lyrics_text_size" // Float sp multiplier, default 1.0
        internal const val KEY_PLAYER_LYRICS_ALIGNMENT = "player_lyrics_alignment" // "CENTER", "START", "END"
        internal const val KEY_PLAYER_SHOW_ART_BELOW_LYRICS = "player_show_art_below_lyrics" // Boolean
        internal const val KEY_PLAYER_SHOW_SEEK_BUTTONS = "player_show_seek_buttons"
        internal const val KEY_PLAYER_TEXT_ALIGNMENT = "player_text_alignment" // "START", "CENTER", "END"
        internal const val KEY_PLAYER_SHOW_SONG_INFO_ON_ARTWORK = "player_show_song_info_on_artwork"
        internal const val KEY_PLAYER_ARTWORK_CORNER_RADIUS = "player_artwork_corner_radius" // 0-40 dp
        internal const val KEY_PLAYER_SHOW_AUDIO_QUALITY_BADGES = "player_show_audio_quality_badges"
        private const val KEY_EXPRESSIVE_QUALITY_BADGE_ALWAYS_VISIBLE = "expressive_quality_badge_always_visible"
        internal const val KEY_PLAYER_PROGRESS_STYLE = "player_progress_style" // "NORMAL", "WAVY", "ROUNDED", "THIN", "THICK"
        internal const val KEY_PLAYER_PROGRESS_THUMB_STYLE = "player_progress_thumb_style" // "NONE", "DEFAULT", "CIRCLE", "SQUARE", "PILL", "DIAMOND", "FLOWER", "HEART", "COOKIE", "PUFFY"
        internal const val KEY_PLAYER_PROGRESS_THUMB_ROTATE = "player_progress_thumb_rotate" // Boolean
        
        // MiniPlayer Customization Settings
        internal const val KEY_MINIPLAYER_PROGRESS_STYLE = "miniplayer_progress_style" // "NORMAL", "WAVY", "ROUNDED", "THIN", "GRADIENT"
        internal const val KEY_MINIPLAYER_SHOW_PROGRESS = "miniplayer_show_progress"
        internal const val KEY_MINIPLAYER_SHOW_ARTWORK = "miniplayer_show_artwork"
        internal const val KEY_MINIPLAYER_ARTWORK_SIZE = "miniplayer_artwork_size" // 40-72 dp
        internal const val KEY_MINIPLAYER_CORNER_RADIUS = "miniplayer_corner_radius" // 0-28 dp
        internal const val KEY_MINIPLAYER_SHOW_TIME = "miniplayer_show_time"
        internal const val KEY_MINIPLAYER_SWIPE_GESTURES = "miniplayer_swipe_gestures"
        internal const val KEY_MINIPLAYER_ALWAYS_SHOW_TABLET = "miniplayer_always_show_tablet"
        
        // Gesture Settings
        internal const val KEY_GESTURE_PLAYER_SWIPE_DISMISS = "gesture_player_swipe_dismiss" // Swipe down to dismiss full player
        internal const val KEY_GESTURE_PLAYER_SWIPE_TRACKS = "gesture_player_swipe_tracks" // Swipe left/right to change tracks in full player
        internal const val KEY_GESTURE_ARTWORK_DOUBLE_TAP = "gesture_artwork_double_tap" // Double tap on artwork to play/pause
        internal const val KEY_GESTURE_ARTWORK_SINGLE_TAP = "gesture_artwork_single_tap" // Tap artwork to toggle lyrics
        internal const val KEY_GESTURE_QUEUE_SWIPE_TO_REMOVE = "gesture_queue_swipe_to_remove" // Swipe to remove from queue
        internal const val KEY_GESTURE_LIBRARY_SWIPE_TABS = "gesture_library_swipe_tabs" // Swipe between library tabs
        internal const val KEY_MINIPLAYER_SWIPE_TRACKS = "miniplayer_swipe_tracks" // Swipe horizontally on miniplayer to change tracks
        internal const val KEY_MINIPLAYER_SWIPE_DISMISS = "miniplayer_swipe_dismiss" // Swipe vertically on miniplayer to open/dismiss
        internal const val KEY_TAP_LYRICS_TO_SEEK = "tap_lyrics_to_seek" // Tap lyric line to seek playback
        
        // Expressive MaterialShapes Settings (M3 Expressive API)
        internal const val KEY_EXPRESSIVE_SHAPES_ENABLED = "expressive_shapes_enabled" // Master toggle for expressive shapes
        internal const val KEY_EXPRESSIVE_SHAPE_PRESET = "expressive_shape_preset" // Preset: DEFAULT, PLAYFUL, ORGANIC, GEOMETRIC, RETRO, CUSTOM
        internal const val KEY_EXPRESSIVE_SHAPE_ALBUM_ART = "expressive_shape_album_art" // Shape for album artwork
        internal const val KEY_EXPRESSIVE_SHAPE_PLAYER_ART = "expressive_shape_player_art" // Shape for player artwork
        internal const val KEY_EXPRESSIVE_SHAPE_SONG_ART = "expressive_shape_song_art" // Shape for song artwork
        internal const val KEY_EXPRESSIVE_SHAPE_PLAYLIST_ART = "expressive_shape_playlist_art" // Shape for playlist artwork
        internal const val KEY_EXPRESSIVE_SHAPE_ARTIST_ART = "expressive_shape_artist_art" // Shape for artist artwork
        internal const val KEY_EXPRESSIVE_SHAPE_PLAYER_CONTROLS = "expressive_shape_player_controls" // Shape for player controls
        internal const val KEY_EXPRESSIVE_SHAPE_MINI_PLAYER = "expressive_shape_mini_player" // Shape for mini player
        private const val KEY_SHOW_SETTINGS_SUGGESTIONS = "show_settings_suggestions"
        private const val KEY_INITIAL_SETTINGS_SUBROUTE = "initial_settings_subroute"
        private const val KEY_INITIAL_STREAMING_ROUTE = "initial_streaming_route"
        
        const val KEY_SONG_LYRICS_PREFERENCES = "song_lyrics_preferences"
        const val KEY_SONG_CUSTOM_LRC_FILES = "song_custom_lrc_files"
        const val KEY_LRC_RENAME_BEHAVIOR = "lrc_rename_behavior"
        
        @Volatile
        private var INSTANCE: AppSettings? = null
        
        fun getInstance(context: Context): AppSettings {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: AppSettings(context.applicationContext).also { INSTANCE = it }
            }
        }

        fun defaultAllowedFormats(): Set<String> = setOf(
            "mp3", "flac", "ogg", "m4a", "opus", "opa", "wav", "aac", "alac", "aiff", "aif", "wma",
            "mka", "ac3", "ac4", "oga", "mid", "midi", "adts", "m4b", "eac", "eac3", "mhm", "mhm1",
            "dts", "dtshd", "dtsx", "truehd", "ape", "wv", "tta", "tak", "dsf", "dff", "dsd"
        )
    }
    
    private val context: Context = context.applicationContext
    val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val platformSection = com.qlzhj.rhythm.core.data.shared.data.model.settings.PlatformSettingsSection(prefs, { updateDerivedSettings() })
    private val lyricsSection = com.qlzhj.rhythm.core.data.shared.data.model.settings.LyricsSettingsSection(prefs, { updateDerivedSettings() })
    private val streamingModeSection = com.qlzhj.rhythm.core.data.shared.data.model.settings.StreamingModeSettingsSection(prefs)
    private val themeSection = com.qlzhj.rhythm.core.data.shared.data.model.settings.ThemeSettingsSection(prefs)
    private val playbackQueueSection = com.qlzhj.rhythm.core.data.shared.data.model.settings.PlaybackQueueSettingsSection(prefs)
    private val guardStatsSection = com.qlzhj.rhythm.core.data.shared.data.model.settings.GuardStatsSettingsSection(prefs)
    val whitelistedSongs: StateFlow<List<String>> get() = librarySection.whitelistedSongs
    val whitelistedFolders: StateFlow<List<String>> get() = librarySection.whitelistedFolders
    val pinnedFolders: StateFlow<List<String>> get() = librarySection.pinnedFolders
    val blacklistedSongs: StateFlow<List<String>> get() = librarySection.blacklistedSongs
    val blacklistedFolders: StateFlow<List<String>> get() = librarySection.blacklistedFolders
    fun normalizeStoragePath(path: String) = librarySection.normalizeStoragePath(path)
    private val librarySection = com.qlzhj.rhythm.core.data.shared.data.model.settings.LibrarySettingsSection(prefs, { updateDerivedSettings() })
    private val customizationSection = com.qlzhj.rhythm.core.data.shared.data.model.settings.CustomizationSettingsSection(prefs, { updateDerivedSettings() })
    val defaultEqualizerPresetOrder get() = audioEqSection.defaultEqualizerPresetOrder
    val pinnedAutoEQProfiles: StateFlow<List<String>> get() = audioEqSection.pinnedAutoEQProfiles
    val customAutoEQProfiles: StateFlow<List<AutoEQProfile>> get() = audioEqSection.customAutoEQProfiles
    val customEqualizerPresets: StateFlow<List<CustomEqualizerPreset>> get() = audioEqSection.customEqualizerPresets
    private val audioEqSection = com.qlzhj.rhythm.core.data.shared.data.model.settings.AudioEqSettingsSection(prefs, { updateDerivedSettings() })
    private val widgetSection = com.qlzhj.rhythm.core.data.shared.data.model.settings.WidgetSettingsSection(prefs)

    // Settings-driven background jobs (auto-backup / update / rhythm pulse),
    // carved out to SettingsJobScheduler in B-phase B2 (lazy: flows above must exist).
    private val jobScheduler by lazy {
        com.qlzhj.rhythm.core.data.core.domain.settings.SettingsJobScheduler(
            context = context,
            updatesEnabled = updatesEnabled,
            autoCheckForUpdates = autoCheckForUpdates,
            updateNotificationsEnabled = updateNotificationsEnabled,
            updateStatusNotificationsEnabled = updateStatusNotificationsEnabled,
            updateCheckIntervalHours = updateCheckIntervalHours,
            rhythmPulseNotificationIntervalHours = rhythmPulseNotificationIntervalHours,
        )
    }

    fun setInitialSettingsSubroute(route: String?) {
        prefs.edit { putString(AppSettings.KEY_INITIAL_SETTINGS_SUBROUTE, route) }
    }

    fun consumeInitialSettingsSubroute(): String? {
        val v = prefs.getString(AppSettings.KEY_INITIAL_SETTINGS_SUBROUTE, null)
        if (v != null) prefs.edit { remove(AppSettings.KEY_INITIAL_SETTINGS_SUBROUTE) }
        return v
    }

    fun setInitialStreamingRoute(route: String?) {
        prefs.edit { putString(AppSettings.KEY_INITIAL_STREAMING_ROUTE, route) }
    }

    fun consumeInitialStreamingRoute(): String? {
        val v = prefs.getString(AppSettings.KEY_INITIAL_STREAMING_ROUTE, null)
        if (v != null) prefs.edit { remove(AppSettings.KEY_INITIAL_STREAMING_ROUTE) }
        return v
    }
    
    // Playback Settings
    val audioOffloadEnabled: StateFlow<Boolean> get() = audioEqSection.audioOffloadEnabled
    fun setAudioOffloadEnabled(enable: Boolean) = audioEqSection.setAudioOffloadEnabled(enable)
    
    val preloadLimit: StateFlow<Int> get() = platformSection.preloadLimit
    fun setPreloadLimit(limit: Int) = platformSection.setPreloadLimit(limit)
    
    val gaplessPlayback: StateFlow<Boolean> get() = platformSection.gaplessPlayback
    fun setGaplessPlayback(enable: Boolean) = platformSection.setGaplessPlayback(enable)
    
    val resumeOnDeviceReconnect: StateFlow<Boolean> get() = audioEqSection.resumeOnDeviceReconnect
    fun setResumeOnDeviceReconnect(enable: Boolean) = audioEqSection.setResumeOnDeviceReconnect(enable)
    
    val crossfade: StateFlow<Boolean> get() = audioEqSection.crossfade
    fun setCrossfade(enable: Boolean) = audioEqSection.setCrossfade(enable)
    
    val crossfadeDuration: StateFlow<Float> get() = audioEqSection.crossfadeDuration
    fun setCrossfadeDuration(duration: Float) = audioEqSection.setCrossfadeDuration(duration)

    val crossfadeRepeatOne: StateFlow<Boolean> get() = audioEqSection.crossfadeRepeatOne
    fun setCrossfadeRepeatOne(enabled: Boolean) = audioEqSection.setCrossfadeRepeatOne(enabled)
    
    val crossfadeOnSkip: StateFlow<Boolean> get() = audioEqSection.crossfadeOnSkip
    fun setCrossfadeOnSkip(enable: Boolean) = audioEqSection.setCrossfadeOnSkip(enable)
    
    val audioNormalization: StateFlow<Boolean> get() = audioEqSection.audioNormalization
    fun setAudioNormalization(enable: Boolean) = audioEqSection.setAudioNormalization(enable)
    
    val replayGain: StateFlow<Boolean> get() = audioEqSection.replayGain
    fun setReplayGain(enable: Boolean) = audioEqSection.setReplayGain(enable)

    val replayGainMode: StateFlow<Int> get() = audioEqSection.replayGainMode
    fun setReplayGainMode(mode: Int) = audioEqSection.setReplayGainMode(mode)

    val replayGainDrc: StateFlow<Boolean> get() = audioEqSection.replayGainDrc
    fun setReplayGainDrc(enable: Boolean) = audioEqSection.setReplayGainDrc(enable)

    val replayGainPreamp: StateFlow<Float> get() = audioEqSection.replayGainPreamp
    fun setReplayGainPreamp(preamp: Float) = audioEqSection.setReplayGainPreamp(preamp)

    val replayGainPreampUntagged: StateFlow<Float> get() = audioEqSection.replayGainPreampUntagged
    fun setReplayGainPreampUntagged(preamp: Float) = audioEqSection.setReplayGainPreampUntagged(preamp)
    
    val skipSilenceEnabled: StateFlow<Boolean> get() = platformSection.skipSilenceEnabled
    fun setSkipSilenceEnabled(enable: Boolean) = platformSection.setSkipSilenceEnabled(enable)
    
    val audioRoutingMode: StateFlow<String> get() = audioEqSection.audioRoutingMode
    fun setAudioRoutingMode(mode: String) = audioEqSection.setAudioRoutingMode(mode)

    val audioEngineSoftwareEnabled: StateFlow<Boolean> get() = audioEqSection.audioEngineSoftwareEnabled
    fun setAudioEngineSoftwareEnabled(enabled: Boolean) = audioEqSection.setAudioEngineSoftwareEnabled(enabled)

    val audioEnginePreferHardware: StateFlow<Boolean> get() = audioEqSection.audioEnginePreferHardware
    fun setAudioEnginePreferHardware(prefer: Boolean) = audioEqSection.setAudioEnginePreferHardware(prefer)

    val audioEngineFloatOutput: StateFlow<Boolean> get() = audioEqSection.audioEngineFloatOutput
    fun setAudioEngineFloatOutput(enable: Boolean) = audioEqSection.setAudioEngineFloatOutput(enable)

    val audioEngineDisabledDecoders: StateFlow<Set<String>> get() = audioEqSection.audioEngineDisabledDecoders
    fun setAudioEngineDisabledDecoders(disabledSet: Set<String>) = audioEqSection.setAudioEngineDisabledDecoders(disabledSet)

    val decoderExtensionsEnabled: StateFlow<Boolean> get() = audioEqSection.decoderExtensionsEnabled
    fun setDecoderExtensionsEnabled(enabled: Boolean) = audioEqSection.setDecoderExtensionsEnabled(enabled)

    val disabledDecoderExtensions: StateFlow<Set<String>> get() = audioEqSection.disabledDecoderExtensions
    fun setDisabledDecoderExtensions(disabledSet: Set<String>) = audioEqSection.setDisabledDecoderExtensions(disabledSet)
    
    // Lyrics Settings
    val showLyrics: StateFlow<Boolean> get() = lyricsSection.showLyrics
    fun setShowLyrics(show: Boolean) = lyricsSection.setShowLyrics(show)
    
    // Migrate from old online_only_lyrics setting to new preference system
    
    val lyricsSourcePreference: StateFlow<LyricsSourcePreference> get() = lyricsSection.lyricsSourcePreference
    fun setLyricsSourcePreference(preference: LyricsSourcePreference) = lyricsSection.setLyricsSourcePreference(preference)
    
    // Keep for backward compatibility but make it read from new preference
    val onlineOnlyLyrics: StateFlow<Boolean> get() = lyricsSection.onlineOnlyLyrics
    @Deprecated("Use setLyricsSourcePreference instead")
    fun setOnlineOnlyLyrics(onlineOnly: Boolean) = lyricsSection.setOnlineOnlyLyrics(onlineOnly)
    
    val showLyricsBackgroundArtwork: StateFlow<Boolean> get() = lyricsSection.showLyricsBackgroundArtwork
    fun setShowLyricsBackgroundArtwork(show: Boolean) = lyricsSection.setShowLyricsBackgroundArtwork(show)

    val showLyricsTranslation: StateFlow<Boolean> get() = lyricsSection.showLyricsTranslation
    fun setShowLyricsTranslation(show: Boolean) = lyricsSection.setShowLyricsTranslation(show)
    
    val showLyricsRomanization: StateFlow<Boolean> get() = lyricsSection.showLyricsRomanization
    fun setShowLyricsRomanization(show: Boolean) = lyricsSection.setShowLyricsRomanization(show)
    
    val keepScreenOnLyrics: StateFlow<Boolean> get() = lyricsSection.keepScreenOnLyrics
    fun setKeepScreenOnLyrics(enabled: Boolean) = lyricsSection.setKeepScreenOnLyrics(enabled)
    
    val tapLyricsToFullScreen: StateFlow<Boolean> get() = lyricsSection.tapLyricsToFullScreen
    fun setTapLyricsToFullScreen(enable: Boolean) = lyricsSection.setTapLyricsToFullScreen(enable)

    val lyricsApiPriority: StateFlow<LyricsApiPriority> get() = lyricsSection.lyricsApiPriority
    fun setLyricsApiPriority(priority: LyricsApiPriority) = lyricsSection.setLyricsApiPriority(priority)

    val lyricsApiFallbackRetry: StateFlow<Boolean> get() = lyricsSection.lyricsApiFallbackRetry
    fun setLyricsApiFallbackRetry(enable: Boolean) = lyricsSection.setLyricsApiFallbackRetry(enable)
    
    val autoHideLyricsControls: StateFlow<Boolean> get() = lyricsSection.autoHideLyricsControls
    fun setAutoHideLyricsControls(enabled: Boolean) = lyricsSection.setAutoHideLyricsControls(enabled)
    
    val lyricBold: StateFlow<Boolean> get() = lyricsSection.lyricBold
    fun setLyricBold(enable: Boolean) = lyricsSection.setLyricBold(enable)

    val trimLyrics: StateFlow<Boolean> get() = platformSection.trimLyrics
    fun setTrimLyrics(enable: Boolean) = platformSection.setTrimLyrics(enable)

    val lyricNoAnimation: StateFlow<Boolean> get() = lyricsSection.lyricNoAnimation
    fun setLyricNoAnimation(enable: Boolean) = lyricsSection.setLyricNoAnimation(enable)

    val translationAutoWord: StateFlow<Boolean> get() = lyricsSection.translationAutoWord
    fun setTranslationAutoWord(enable: Boolean) = lyricsSection.setTranslationAutoWord(enable)
    
    // Theme Settings
    val useSystemTheme: StateFlow<Boolean> get() = themeSection.useSystemTheme
    fun setUseSystemTheme(use: Boolean) = themeSection.setUseSystemTheme(use)
    
    val darkMode: StateFlow<Boolean> get() = themeSection.darkMode
    fun setDarkMode(dark: Boolean) = themeSection.setDarkMode(dark)
    
    val amoledTheme: StateFlow<Boolean> get() = themeSection.amoledTheme
    fun setAmoledTheme(amoled: Boolean) = themeSection.setAmoledTheme(amoled)

    val floatingNavigationBar: StateFlow<Boolean> get() = themeSection.floatingNavigationBar
    fun setFloatingNavigationBar(enabled: Boolean) = themeSection.setFloatingNavigationBar(enabled)
    
    val useDynamicColors: StateFlow<Boolean> get() = themeSection.useDynamicColors
    fun setUseDynamicColors(use: Boolean) = themeSection.setUseDynamicColors(use)
    
    // Artist Separator Settings
    val artistSeparatorEnabled: StateFlow<Boolean> get() = librarySection.artistSeparatorEnabled
    fun setArtistSeparatorEnabled(enabled: Boolean) = librarySection.setArtistSeparatorEnabled(enabled)
    
    // Default delimiters: ; /
    val artistSeparatorDelimiters: StateFlow<String> get() = librarySection.artistSeparatorDelimiters
    fun setArtistSeparatorDelimiters(delimiters: String) = librarySection.setArtistSeparatorDelimiters(delimiters)

    val artistSeparatorCustomDelimiters: StateFlow<List<String>> get() = librarySection.artistSeparatorCustomDelimiters
    fun setArtistSeparatorCustomDelimiters(tokens: List<String>) = librarySection.setArtistSeparatorCustomDelimiters(tokens)
    
    val customColorScheme: StateFlow<String> get() = themeSection.customColorScheme
    fun setCustomColorScheme(scheme: String) = themeSection.setCustomColorScheme(scheme)
    
    val customFont: StateFlow<String> get() = themeSection.customFont
    fun setCustomFont(font: String) = themeSection.setCustomFont(font)
    
    val colorSource: StateFlow<String> get() = themeSection.colorSource
    fun setColorSource(source: String) = themeSection.setColorSource(source)
    
    val extractedAlbumColors: StateFlow<String?> get() = librarySection.extractedAlbumColors
    fun setExtractedAlbumColors(colorsJson: String?) = librarySection.setExtractedAlbumColors(colorsJson)
    
    val fontSource: StateFlow<String> get() = themeSection.fontSource
    fun setFontSource(source: String) = themeSection.setFontSource(source)
    
    val customFontPath: StateFlow<String?> get() = themeSection.customFontPath
    fun setCustomFontPath(path: String?) = themeSection.setCustomFontPath(path)
    
    val customFontFamily: StateFlow<String> get() = themeSection.customFontFamily
    fun setCustomFontFamily(family: String) = themeSection.setCustomFontFamily(family)
    
    // Player Theme Settings
    val playerThemeId: StateFlow<String> get() = customizationSection.playerThemeId
    fun setPlayerThemeId(themeId: String) = customizationSection.setPlayerThemeId(themeId)
    
    val miniPlayerThemeId: StateFlow<String> get() = customizationSection.miniPlayerThemeId
    fun setMiniPlayerThemeId(themeId: String) = customizationSection.setMiniPlayerThemeId(themeId)
    
    // Library Settings
    val albumViewType: StateFlow<AlbumViewType> get() = librarySection.albumViewType
    fun setAlbumViewType(viewType: AlbumViewType) = librarySection.setAlbumViewType(viewType)
    
    private val _artistViewType = MutableStateFlow(
        ArtistViewType.valueOf(prefs.getString(AppSettings.KEY_ARTIST_VIEW_TYPE, ArtistViewType.GRID.name) ?: ArtistViewType.GRID.name)
    )
    val artistViewType: StateFlow<ArtistViewType> = _artistViewType.asStateFlow()
    
    private val _playlistViewType = MutableStateFlow(
        PlaylistViewType.valueOf(prefs.getString(AppSettings.KEY_PLAYLIST_VIEW_TYPE, PlaylistViewType.LIST.name) ?: PlaylistViewType.LIST.name)
    )
    val playlistViewType: StateFlow<PlaylistViewType> = _playlistViewType.asStateFlow()
    
    // Album Sort Order
    val albumSortOrder: StateFlow<String> get() = librarySection.albumSortOrder
    fun setAlbumSortOrder(sortOrder: String) = librarySection.setAlbumSortOrder(sortOrder)
    
    // Playlist Sort Order
    private val _playlistSortOrder = MutableStateFlow(prefs.getString(KEY_PLAYLIST_SORT_ORDER, "NAME_ASC") ?: "NAME_ASC")
    val playlistSortOrder: StateFlow<String> = _playlistSortOrder.asStateFlow()
    
    // Playlist Detail Sort Order (for songs within a playlist)
    private val _playlistDetailSortOrder = MutableStateFlow(prefs.getString(KEY_PLAYLIST_DETAIL_SORT_ORDER, "TITLE_ASC") ?: "TITLE_ASC")
    val playlistDetailSortOrder: StateFlow<String> = _playlistDetailSortOrder.asStateFlow()
    
    // Artist Collaboration Mode
    
    // Library Tab Order
    val libraryTabOrder: StateFlow<List<String>> get() = librarySection.libraryTabOrder
    fun setLibraryTabOrder(tabOrder: List<String>) = librarySection.setLibraryTabOrder(tabOrder)

    val libraryCombineDiscs: StateFlow<Boolean> get() = librarySection.libraryCombineDiscs
    fun setLibraryCombineDiscs(enabled: Boolean) = librarySection.setLibraryCombineDiscs(enabled)
    
    val playerChipOrder: StateFlow<List<String>> get() = customizationSection.playerChipOrder
    fun setPlayerChipOrder(chipOrder: List<String>) = customizationSection.setPlayerChipOrder(chipOrder)
    
    // Default Landing Screen
    private val _defaultScreen = MutableStateFlow(prefs.getString(KEY_DEFAULT_SCREEN, "home") ?: "home")
    val defaultScreen: StateFlow<String> = _defaultScreen.asStateFlow()
    
    // Hidden Library Tabs
    val hiddenLibraryTabs: StateFlow<Set<String>> get() = librarySection.hiddenLibraryTabs
    fun setHiddenLibraryTabs(hiddenTabs: Set<String>) = librarySection.setHiddenLibraryTabs(hiddenTabs)
    
    // Hidden Player Chips
    val hiddenPlayerChips: StateFlow<Set<String>> get() = customizationSection.hiddenPlayerChips
    fun setHiddenPlayerChips(hiddenChips: Set<String>) = customizationSection.setHiddenPlayerChips(hiddenChips)

    // Expressive Player Bottom Buttons


    val expressiveBottomButtonsNormal: StateFlow<List<String>> get() = customizationSection.expressiveBottomButtonsNormal
    fun setExpressiveBottomButtonsNormal(order: List<String>) = customizationSection.setExpressiveBottomButtonsNormal(order)


    val expressiveBottomButtonsMerge: StateFlow<List<String>> get() = customizationSection.expressiveBottomButtonsMerge
    fun setExpressiveBottomButtonsMerge(order: List<String>) = customizationSection.setExpressiveBottomButtonsMerge(order)
    val expressiveHiddenBottomButtonsNormal: StateFlow<Set<String>> get() = customizationSection.expressiveHiddenBottomButtonsNormal
    val allExpressiveBottomButtons get() = customizationSection.allExpressiveBottomButtons
    val defaultExpressiveBottomButtonsNormal get() = customizationSection.defaultExpressiveBottomButtonsNormal
    val defaultExpressiveBottomButtonsMerge get() = customizationSection.defaultExpressiveBottomButtonsMerge
    fun setExpressiveHiddenBottomButtonsNormal(hidden: Set<String>) = customizationSection.setExpressiveHiddenBottomButtonsNormal(hidden)
    fun setExpressiveHiddenBottomButtonsMerge(hidden: Set<String>) = customizationSection.setExpressiveHiddenBottomButtonsMerge(hidden)
    val expressiveHiddenBottomButtonsMerge: StateFlow<Set<String>> get() = customizationSection.expressiveHiddenBottomButtonsMerge

    
    
    // Group By Album Artist
    val groupByAlbumArtist: StateFlow<Boolean> get() = librarySection.groupByAlbumArtist
    fun setGroupByAlbumArtist(enable: Boolean) = librarySection.setGroupByAlbumArtist(enable)
    
    // Prefer per-song embedded artwork over shared MediaStore album art.
    val preferSongArtwork: StateFlow<Boolean> get() = librarySection.preferSongArtwork
    fun setPreferSongArtwork(enabled: Boolean) = librarySection.setPreferSongArtwork(enabled)

    @Deprecated("Use preferSongArtwork")

    // Lossless Artwork - Show cover art as-is without compression
    val losslessArtwork: StateFlow<Boolean> get() = librarySection.losslessArtwork
    fun setLosslessArtwork(enabled: Boolean) = librarySection.setLosslessArtwork(enabled)

    // Show Library Section Headers

    // Show Library Bottom Bar Always
    val showLibraryBottomBarAlways: StateFlow<Boolean> get() = librarySection.showLibraryBottomBarAlways
    fun setShowLibraryBottomBarAlways(enable: Boolean) = librarySection.setShowLibraryBottomBarAlways(enable)
    
    // Alphabet Bar Settings
    val showAlphabetBar: StateFlow<Boolean> get() = librarySection.showAlphabetBar
    fun setShowAlphabetBar(show: Boolean) = librarySection.setShowAlphabetBar(show)
    
    private val _showScrollToTop = MutableStateFlow(prefs.getBoolean(KEY_SHOW_SCROLL_TO_TOP, false))
    val showScrollToTop: StateFlow<Boolean> = _showScrollToTop.asStateFlow()
    
    // App Mode Settings (Local vs Streaming)
    val appMode: StateFlow<String> get() = streamingModeSection.appMode
    fun setAppMode(mode: String) = streamingModeSection.setAppMode(mode)
    
    val streamingService: StateFlow<String> get() = streamingModeSection.streamingService
    fun setStreamingService(service: String) = streamingModeSection.setStreamingService(service)
    
    // Default to LOSSLESS so the server streams the original file at full quality.
    // Users can downgrade in Go Settings if they want to save data.
    val streamingQuality: StateFlow<String> get() = streamingModeSection.streamingQuality
    fun setStreamingQuality(quality: String) = streamingModeSection.setStreamingQuality(quality)
    
    val allowCellularStreaming: StateFlow<Boolean> get() = streamingModeSection.allowCellularStreaming
    fun setAllowCellularStreaming(allow: Boolean) = streamingModeSection.setAllowCellularStreaming(allow)
    
    val offlineMode: StateFlow<Boolean> get() = streamingModeSection.offlineMode
    fun setOfflineMode(enabled: Boolean) = streamingModeSection.setOfflineMode(enabled)

    val rememberStreamingPasswords: StateFlow<Boolean> get() = streamingModeSection.rememberStreamingPasswords
    fun setRememberStreamingPasswords(enabled: Boolean) = streamingModeSection.setRememberStreamingPasswords(enabled)
    
    // Audio Device Settings
    
    val autoConnectDevice: StateFlow<Boolean> get() = audioEqSection.autoConnectDevice
    fun setAutoConnectDevice(enable: Boolean) = audioEqSection.setAutoConnectDevice(enable)
    
    val useSystemVolume: StateFlow<Boolean> get() = audioEqSection.useSystemVolume
    fun setUseSystemVolume(enable: Boolean) = audioEqSection.setUseSystemVolume(enable)
    
    val appVolume: StateFlow<Float> get() = audioEqSection.appVolume
    fun setAppVolume(volume: Float) = audioEqSection.setAppVolume(volume)
    
    val stopPlaybackOnZeroVolume: StateFlow<Boolean> get() = audioEqSection.stopPlaybackOnZeroVolume
    fun setStopPlaybackOnZeroVolume(enable: Boolean) = audioEqSection.setStopPlaybackOnZeroVolume(enable)
    
    // Equalizer Settings
    val equalizerEnabled: StateFlow<Boolean> get() = audioEqSection.equalizerEnabled
    fun setEqualizerEnabled(enable: Boolean) = audioEqSection.setEqualizerEnabled(enable)
    
    val equalizerPreset: StateFlow<String> get() = audioEqSection.equalizerPreset
    fun setEqualizerPreset(preset: String) = audioEqSection.setEqualizerPreset(preset)
    
    val equalizerBandLevels: StateFlow<String> get() = audioEqSection.equalizerBandLevels
    fun setEqualizerBandLevels(levels: String) = audioEqSection.setEqualizerBandLevels(levels)
    
    val autoEQProfile: StateFlow<String> get() = audioEqSection.autoEQProfile
    fun setAutoEQProfile(profileName: String) = audioEqSection.setAutoEQProfile(profileName)
    
    private val _userAudioDevices = MutableStateFlow(prefs.getString(KEY_USER_AUDIO_DEVICES, null))
    val userAudioDevices: StateFlow<String?> = _userAudioDevices.asStateFlow()
    
    val activeAudioDeviceId: StateFlow<String?> get() = audioEqSection.activeAudioDeviceId
    fun setActiveAudioDeviceId(deviceId: String?) = audioEqSection.setActiveAudioDeviceId(deviceId)
    
    val dismissedAutoEQSuggestions: StateFlow<String?> get() = audioEqSection.dismissedAutoEQSuggestions
    fun setDismissedAutoEQSuggestions(dismissedDevices: String?) = audioEqSection.setDismissedAutoEQSuggestions(dismissedDevices)
    
    val bassBoostEnabled: StateFlow<Boolean> get() = audioEqSection.bassBoostEnabled
    fun setBassBoostEnabled(enable: Boolean) = audioEqSection.setBassBoostEnabled(enable)
    
    val bassBoostStrength: StateFlow<Int> get() = audioEqSection.bassBoostStrength
    fun setBassBoostStrength(strength: Int) = audioEqSection.setBassBoostStrength(strength)
    
    val virtualizerEnabled: StateFlow<Boolean> get() = audioEqSection.virtualizerEnabled
    fun setVirtualizerEnabled(enable: Boolean) = audioEqSection.setVirtualizerEnabled(enable)
    
    val virtualizerStrength: StateFlow<Int> get() = audioEqSection.virtualizerStrength
    fun setVirtualizerStrength(strength: Int) = audioEqSection.setVirtualizerStrength(strength)
    
    val monoAudioEnabled: StateFlow<Boolean> get() = audioEqSection.monoAudioEnabled
    fun setMonoAudioEnabled(enable: Boolean) = audioEqSection.setMonoAudioEnabled(enable)
    


    val equalizerPresetOrder: StateFlow<List<String>> get() = audioEqSection.equalizerPresetOrder
    fun setEqualizerPresetOrder(order: List<String>) = audioEqSection.setEqualizerPresetOrder(order)

    fun setHiddenEqualizerPresets(hidden: Set<String>) = audioEqSection.setHiddenEqualizerPresets(hidden)
    val hiddenEqualizerPresets: StateFlow<Set<String>> get() = audioEqSection.hiddenEqualizerPresets

    

    val speakerAutoEQBypass: StateFlow<Boolean> get() = audioEqSection.speakerAutoEQBypass
    fun setSpeakerAutoEQBypass(bypass: Boolean) = audioEqSection.setSpeakerAutoEQBypass(bypass)
    
    // Sleep Timer
    val sleepTimerActive: StateFlow<Boolean> get() = playbackQueueSection.sleepTimerActive
    fun setSleepTimerActive(active: Boolean) = playbackQueueSection.setSleepTimerActive(active)
    
    val sleepTimerRemainingSeconds: StateFlow<Long> get() = playbackQueueSection.sleepTimerRemainingSeconds
    fun setSleepTimerRemainingSeconds(seconds: Long) = playbackQueueSection.setSleepTimerRemainingSeconds(seconds)
    
    val sleepTimerAction: StateFlow<String> get() = playbackQueueSection.sleepTimerAction
    fun setSleepTimerAction(action: String) = playbackQueueSection.setSleepTimerAction(action)
    
    // Queue & Playback Behavior Settings
    val shuffleUsesExoplayer: StateFlow<Boolean> get() = playbackQueueSection.shuffleUsesExoplayer
    fun setShuffleUsesExoplayer(useExoplayer: Boolean) = playbackQueueSection.setShuffleUsesExoplayer(useExoplayer)
    
    val autoAddToQueue: StateFlow<Boolean> get() = playbackQueueSection.autoAddToQueue
    fun setAutoAddToQueue(autoAdd: Boolean) = playbackQueueSection.setAutoAddToQueue(autoAdd)
    val respectAlbumOnPlay: StateFlow<Boolean> get() = playbackQueueSection.respectAlbumOnPlay
    fun setRespectAlbumOnPlay(respect: Boolean) = playbackQueueSection.setRespectAlbumOnPlay(respect)
    
    val clearQueueOnNewSong: StateFlow<Boolean> get() = playbackQueueSection.clearQueueOnNewSong
    fun setClearQueueOnNewSong(clearQueue: Boolean) = playbackQueueSection.setClearQueueOnNewSong(clearQueue)


    
    val showQueueDialog: StateFlow<Boolean> get() = playbackQueueSection.showQueueDialog
    fun setShowQueueDialog(show: Boolean) = playbackQueueSection.setShowQueueDialog(show)

    val listQueueActionBehavior: StateFlow<String> get() = playbackQueueSection.listQueueActionBehavior
    fun setListQueueActionBehavior(behavior: String) = playbackQueueSection.setListQueueActionBehavior(behavior)
    
    val hidePlayedQueueSongs: StateFlow<Boolean> get() = playbackQueueSection.hidePlayedQueueSongs
    fun setHidePlayedQueueSongs(hide: Boolean) = playbackQueueSection.setHidePlayedQueueSongs(hide)
    
    val repeatModePersistence: StateFlow<Boolean> get() = playbackQueueSection.repeatModePersistence
    fun setRepeatModePersistence(persist: Boolean) = playbackQueueSection.setRepeatModePersistence(persist)
    
    val shuffleModePersistence: StateFlow<Boolean> get() = playbackQueueSection.shuffleModePersistence
    fun setShuffleModePersistence(persist: Boolean) = playbackQueueSection.setShuffleModePersistence(persist)
    
    val keepShuffleOnSelection: StateFlow<Boolean> get() = playbackQueueSection.keepShuffleOnSelection
    fun setKeepShuffleOnSelection(keep: Boolean) = playbackQueueSection.setKeepShuffleOnSelection(keep)
    
    val savedShuffleState: StateFlow<Boolean> get() = playbackQueueSection.savedShuffleState
    fun setSavedShuffleState(enabled: Boolean) = playbackQueueSection.setSavedShuffleState(enabled)
    
    val savedRepeatMode: StateFlow<Int> get() = playbackQueueSection.savedRepeatMode
    fun setSavedRepeatMode(mode: Int) = playbackQueueSection.setSavedRepeatMode(mode)
    
    val playbackSpeed: StateFlow<Float> get() = playbackQueueSection.playbackSpeed
    fun setPlaybackSpeed(speed: Float) = playbackQueueSection.setPlaybackSpeed(speed)

    val defaultPlaybackSpeed: StateFlow<Float> get() = playbackQueueSection.defaultPlaybackSpeed
    fun setDefaultPlaybackSpeed(speed: Float) = playbackQueueSection.setDefaultPlaybackSpeed(speed)

    val useDefaultPlaybackSpeed: StateFlow<Boolean> get() = playbackQueueSection.useDefaultPlaybackSpeed
    fun setUseDefaultPlaybackSpeed(enabled: Boolean) = playbackQueueSection.setUseDefaultPlaybackSpeed(enabled)

    val playbackPitch: StateFlow<Float> get() = playbackQueueSection.playbackPitch
    fun setPlaybackPitch(pitch: Float) = playbackQueueSection.setPlaybackPitch(pitch)

    val syncSpeedAndPitch: StateFlow<Boolean> get() = playbackQueueSection.syncSpeedAndPitch
    fun setSyncSpeedAndPitch(sync: Boolean) = playbackQueueSection.setSyncSpeedAndPitch(sync)
    
    // Time Format Settings - Show hours:minutes:seconds for longer tracks (>60 min)
    val useHoursInTimeFormat: StateFlow<Boolean> get() = playbackQueueSection.useHoursInTimeFormat
    fun setUseHoursInTimeFormat(enabled: Boolean) = playbackQueueSection.setUseHoursInTimeFormat(enabled)
    
    val showRemainingTime: StateFlow<Boolean> get() = playbackQueueSection.showRemainingTime
    fun setShowRemainingTime(enabled: Boolean) = playbackQueueSection.setShowRemainingTime(enabled)
    
    val expressiveColors: StateFlow<Boolean> get() = themeSection.expressiveColors
    fun setExpressiveColors(enabled: Boolean) = themeSection.setExpressiveColors(enabled)

    val themeIntensity: StateFlow<String> get() = themeSection.themeIntensity
    fun setThemeIntensity(intensity: String) = themeSection.setThemeIntensity(intensity)
    
    // Stop Playback on App Close
    val stopPlaybackOnAppClose: StateFlow<Boolean> get() = playbackQueueSection.stopPlaybackOnAppClose
    fun setStopPlaybackOnAppClose(enabled: Boolean) = playbackQueueSection.setStopPlaybackOnAppClose(enabled)
    
    // Queue Persistence
    val queuePersistenceEnabled: StateFlow<Boolean> get() = playbackQueueSection.queuePersistenceEnabled
    fun setQueuePersistenceEnabled(enabled: Boolean) = playbackQueueSection.setQueuePersistenceEnabled(enabled)

    // Context queue preferences
    val contextQueuePreference: StateFlow<String> get() = playbackQueueSection.contextQueuePreference
    fun setContextQueuePreference(pref: String) = playbackQueueSection.setContextQueuePreference(pref)

    val contextQueuePersistence: StateFlow<String> get() = playbackQueueSection.contextQueuePersistence
    fun setContextQueuePersistence(value: String) = playbackQueueSection.setContextQueuePersistence(value)

    val contextQueueSize: StateFlow<Int> get() = playbackQueueSection.contextQueueSize
    fun setContextQueueSize(size: Int) = playbackQueueSection.setContextQueueSize(size)
    
    val savedQueue: StateFlow<List<String>> get() = playbackQueueSection.savedQueue
    fun setSavedQueue(songIds: List<String>) = playbackQueueSection.setSavedQueue(songIds)
    
    val savedQueueIndex: StateFlow<Int> get() = playbackQueueSection.savedQueueIndex
    fun setSavedQueueIndex(index: Int) = playbackQueueSection.setSavedQueueIndex(index)
    
    val savedPlaybackPosition: StateFlow<Long> get() = playbackQueueSection.savedPlaybackPosition
    fun setSavedPlaybackPosition(position: Long) = playbackQueueSection.setSavedPlaybackPosition(position)

    val savedOriginalQueue: StateFlow<List<String>> get() = playbackQueueSection.savedOriginalQueue
    fun setSavedOriginalQueue(songIds: List<String>) = playbackQueueSection.setSavedOriginalQueue(songIds)

    val savedOriginalQueueSource: StateFlow<String?> get() = playbackQueueSection.savedOriginalQueueSource
    fun setSavedOriginalQueueSource(sourceName: String?) = playbackQueueSection.setSavedOriginalQueueSource(sourceName)
    
    // Cache Settings
    val maxCacheSize: StateFlow<Long> get() = platformSection.maxCacheSize
    fun setMaxCacheSize(size: Long) = platformSection.setMaxCacheSize(size)
    
    // Search History
    val searchHistory: StateFlow<String?> get() = platformSection.searchHistory
    fun setSearchHistory(history: String?) = platformSection.setSearchHistory(history)

    // Search UX
    val showKeyboardOnSearchOpen: StateFlow<Boolean> get() = platformSection.showKeyboardOnSearchOpen
    fun setShowKeyboardOnSearchOpen(enabled: Boolean) = platformSection.setShowKeyboardOnSearchOpen(enabled)
    
    // Playlists
    private val _playlists = MutableStateFlow<String?>(prefs.getString(KEY_PLAYLISTS, null))
    val playlists: StateFlow<String?> = _playlists.asStateFlow()

    private val _favoriteSongs = MutableStateFlow<String?>(prefs.getString(KEY_FAVORITE_SONGS, null))
    val favoriteSongs: StateFlow<String?> = _favoriteSongs.asStateFlow()
    
    // Song Lyrics Preferences - Map of songId to source preference ("online", "embedded", "lrc")
    val songLyricsPreferences: StateFlow<Map<String, String>> get() = lyricsSection.songLyricsPreferences

    // Custom LRC Files - Map of songId to custom LRC filename
    val songCustomLrcFiles: StateFlow<Map<String, String>> get() = lyricsSection.songCustomLrcFiles

    // LRC Rename Behavior - "ask", "always", "never"
    val lrcRenameBehavior: StateFlow<String> get() = lyricsSection.lrcRenameBehavior
    fun setLrcRenameBehavior(value: String) = lyricsSection.setLrcRenameBehavior(value)
    
    private val _defaultPlaylistsEnabled = MutableStateFlow(prefs.getBoolean(KEY_DEFAULT_PLAYLISTS_ENABLED, true))
    val defaultPlaylistsEnabled: StateFlow<Boolean> = _defaultPlaylistsEnabled.asStateFlow()

    private val _showLikedInPlaylists = MutableStateFlow(prefs.getBoolean(KEY_SHOW_LIKED_IN_PLAYLISTS, false))
    val showLikedInPlaylists: StateFlow<Boolean> = _showLikedInPlaylists.asStateFlow()

    private val _smartPlaylistRecentlyAdded = MutableStateFlow(prefs.getBoolean(KEY_SMART_PLAYLIST_RECENTLY_ADDED, true))
    val smartPlaylistRecentlyAdded: StateFlow<Boolean> = _smartPlaylistRecentlyAdded.asStateFlow()

    private val _smartPlaylistMostPlayed = MutableStateFlow(prefs.getBoolean(KEY_SMART_PLAYLIST_MOST_PLAYED, true))
    val smartPlaylistMostPlayed: StateFlow<Boolean> = _smartPlaylistMostPlayed.asStateFlow()

    private val _smartPlaylistOnRepeat = MutableStateFlow(prefs.getBoolean(KEY_SMART_PLAYLIST_ON_REPEAT, true))
    val smartPlaylistOnRepeat: StateFlow<Boolean> = _smartPlaylistOnRepeat.asStateFlow()

    private val _smartPlaylistForgottenFavorites = MutableStateFlow(prefs.getBoolean(KEY_SMART_PLAYLIST_FORGOTTEN_FAVORITES, true))
    val smartPlaylistForgottenFavorites: StateFlow<Boolean> = _smartPlaylistForgottenFavorites.asStateFlow()

    private val _smartPlaylistRecentlyPlayed = MutableStateFlow(prefs.getBoolean(KEY_SMART_PLAYLIST_RECENTLY_PLAYED, true))
    val smartPlaylistRecentlyPlayed: StateFlow<Boolean> = _smartPlaylistRecentlyPlayed.asStateFlow()
    
    // User Statistics
    val listeningTime: StateFlow<Long> get() = guardStatsSection.listeningTime
    fun setListeningTime(time: Long) = guardStatsSection.setListeningTime(time)
    
    val songsPlayed: StateFlow<Int> get() = guardStatsSection.songsPlayed
    fun setSongsPlayed(count: Int) = guardStatsSection.setSongsPlayed(count)

    val rhythmGuardMode: StateFlow<String> get() = guardStatsSection.rhythmGuardMode

    val rhythmGuardAge: StateFlow<Int> get() = guardStatsSection.rhythmGuardAge

    val rhythmGuardManualWarningsEnabled: StateFlow<Boolean> get() = guardStatsSection.rhythmGuardManualWarningsEnabled
    @Deprecated("Use setRhythmGuardAge")
    fun setRhythmGuardManualWarningsEnabled(enabled: Boolean) = guardStatsSection.setRhythmGuardManualWarningsEnabled(enabled)

    val rhythmGuardManualVolumeThreshold: StateFlow<Float> get() = guardStatsSection.rhythmGuardManualVolumeThreshold
    @Deprecated("Use setRhythmGuardManualWarningsEnabled")
    fun setRhythmGuardManualVolumeThreshold(threshold: Float) = guardStatsSection.setRhythmGuardManualVolumeThreshold(threshold)

    val rhythmGuardApplyVolumeLimitOnSpeaker: StateFlow<Boolean> get() = guardStatsSection.rhythmGuardApplyVolumeLimitOnSpeaker
    fun setRhythmGuardApplyVolumeLimitOnSpeaker(enabled: Boolean) = guardStatsSection.setRhythmGuardApplyVolumeLimitOnSpeaker(enabled)

    val rhythmGuardLastAutoAppliedAt: StateFlow<Long> get() = guardStatsSection.rhythmGuardLastAutoAppliedAt
    @Deprecated("Use setRhythmGuardManualVolumeThreshold")
    fun setRhythmGuardLastAutoAppliedAt(timestamp: Long) = guardStatsSection.setRhythmGuardLastAutoAppliedAt(timestamp)

    val rhythmGuardAlertThresholdMinutes: StateFlow<Int> get() = guardStatsSection.rhythmGuardAlertThresholdMinutes
    @Deprecated("Use setRhythmGuardLastAutoAppliedAt")
    fun setRhythmGuardAlertThresholdMinutes(minutes: Int) = guardStatsSection.setRhythmGuardAlertThresholdMinutes(minutes)

    val rhythmGuardWarningTimeoutMinutes: StateFlow<Int> get() = guardStatsSection.rhythmGuardWarningTimeoutMinutes
    fun setRhythmGuardWarningTimeoutMinutes(minutes: Int) = guardStatsSection.setRhythmGuardWarningTimeoutMinutes(minutes)

    val rhythmGuardPostTimeoutCooldownMinutes: StateFlow<Int> get() = guardStatsSection.rhythmGuardPostTimeoutCooldownMinutes
    fun setRhythmGuardPostTimeoutCooldownMinutes(minutes: Int) = guardStatsSection.setRhythmGuardPostTimeoutCooldownMinutes(minutes)

    val rhythmGuardBreakResumeMinutes: StateFlow<Int> get() = guardStatsSection.rhythmGuardBreakResumeMinutes
    fun setRhythmGuardBreakResumeMinutes(minutes: Int) = guardStatsSection.setRhythmGuardBreakResumeMinutes(minutes)

    val rhythmGuardTimeoutUntilMs: StateFlow<Long> get() = guardStatsSection.rhythmGuardTimeoutUntilMs

    val rhythmGuardTimeoutReason: StateFlow<String> get() = guardStatsSection.rhythmGuardTimeoutReason

    val rhythmGuardTimeoutStartedAtMs: StateFlow<Long> get() = guardStatsSection.rhythmGuardTimeoutStartedAtMs

    val rhythmGuardTimeoutCooldownUntilMs: StateFlow<Long> get() = guardStatsSection.rhythmGuardTimeoutCooldownUntilMs
    fun setRhythmGuardTimeoutCooldownUntilMs(untilEpochMs: Long) = guardStatsSection.setRhythmGuardTimeoutCooldownUntilMs(untilEpochMs)
    
    val rhythmGuardNextAllowedLimitMinutes: StateFlow<Int> get() = guardStatsSection.rhythmGuardNextAllowedLimitMinutes
    fun setRhythmGuardNextAllowedLimitMinutes(value: Int) = guardStatsSection.setRhythmGuardNextAllowedLimitMinutes(value)

    val rhythmGuardFirstBreakSeen: StateFlow<Boolean> get() = guardStatsSection.rhythmGuardFirstBreakSeen
    fun setRhythmGuardFirstBreakSeen(seen: Boolean) = guardStatsSection.setRhythmGuardFirstBreakSeen(seen)

    val homeShowRhythmGuard: StateFlow<Boolean> get() = customizationSection.homeShowRhythmGuard
    fun setHomeShowRhythmGuard(value: Boolean) = customizationSection.setHomeShowRhythmGuard(value)
    
    val uniqueArtists: StateFlow<Int> get() = librarySection.uniqueArtists
    fun setUniqueArtists(count: Int) = librarySection.setUniqueArtists(count)
    
    val genrePreferences: StateFlow<Map<String, Int>> get() = librarySection.genrePreferences
    fun setGenrePreferences(preferences: Map<String, Int>) = librarySection.setGenrePreferences(preferences)
    
    val timeBasedPreferences: StateFlow<Map<Int, List<String>>> get() = guardStatsSection.timeBasedPreferences
    fun setTimeBasedPreferences(preferences: Map<Int, List<String>>) = guardStatsSection.setTimeBasedPreferences(preferences)

    private data class RecentSongSnapshot(
        val id: String,
        val title: String,
        val artist: String,
        val album: String,
        val duration: Long,
        val uri: String,
        val artworkUri: String?
    )

    private fun Song.toRecentSongSnapshot(): RecentSongSnapshot {
        return RecentSongSnapshot(
            id = id,
            title = title,
            artist = artist,
            album = album,
            duration = duration,
            uri = uri.toString(),
            artworkUri = artworkUri?.toString()
        )
    }

    private fun RecentSongSnapshot.toSongOrNull(): Song? {
        return runCatching {
            Song(
                id = id,
                title = title,
                artist = artist,
                album = album,
                duration = duration,
                uri = uri.toUri(),
                artworkUri = artworkUri?.takeIf { it.isNotBlank() }?.let(Uri::parse)
            )
        }.getOrNull()
    }

    private fun loadRecentlyPlayedSongCache(): Map<String, Song> {
        return try {
            val json = prefs.getString(AppSettings.KEY_RECENTLY_PLAYED_SONG_CACHE, null)
            if (json.isNullOrBlank()) {
                emptyMap()
            } else {
                Gson().fromJson<List<RecentSongSnapshot>>(
                    json,
                    object : TypeToken<List<RecentSongSnapshot>>() {}.type
                )
                    .orEmpty()
                    .mapNotNull { snapshot ->
                        snapshot.toSongOrNull()?.let { song -> song.id to song }
                    }
                    .toMap()
            }
        } catch (_: Exception) {
            emptyMap()
        }
    }
    
    // Recently Played
    val recentlyPlayed: StateFlow<List<String>> get() = playbackQueueSection.recentlyPlayed

    private val _recentlyPlayedSongCache = MutableStateFlow(loadRecentlyPlayedSongCache())
    val recentlyPlayedSongCache: StateFlow<Map<String, Song>> = _recentlyPlayedSongCache.asStateFlow()
    
    val lastPlayedTimestamp: StateFlow<Long> get() = playbackQueueSection.lastPlayedTimestamp
    
    private val _integrationsEnabled = MutableStateFlow(prefs.getBoolean(KEY_INTEGRATIONS_ENABLED, false))
    override val integrationsEnabled: StateFlow<Boolean> = _integrationsEnabled.asStateFlow()

    // API Enable/Disable States
    private val _deezerApiEnabled = MutableStateFlow(prefs.getBoolean(KEY_DEEZER_API_ENABLED, false))
    override val deezerApiEnabled: StateFlow<Boolean> = _deezerApiEnabled.asStateFlow()
    
    private val _lrclibApiEnabled = MutableStateFlow(prefs.getBoolean(KEY_LRCLIB_API_ENABLED, false))
    override val lrclibApiEnabled: StateFlow<Boolean> = _lrclibApiEnabled.asStateFlow()
    
    private val _betterLyricsApiEnabled = MutableStateFlow(prefs.getBoolean(KEY_BETTERLYRICS_API_ENABLED, false))
    override val betterLyricsApiEnabled: StateFlow<Boolean> = _betterLyricsApiEnabled.asStateFlow()
    
    private val _ytMusicApiEnabled = MutableStateFlow(prefs.getBoolean(KEY_YTMUSIC_API_ENABLED, false))
    override val ytMusicApiEnabled: StateFlow<Boolean> = _ytMusicApiEnabled.asStateFlow()
    
    private val _spotifyApiEnabled = MutableStateFlow(prefs.getBoolean(KEY_SPOTIFY_API_ENABLED, false))
    override val spotifyApiEnabled: StateFlow<Boolean> = _spotifyApiEnabled.asStateFlow()
    
    private val _lyricallyApiEnabled = MutableStateFlow(prefs.getBoolean(KEY_LYRICALLY_API_ENABLED, false))
    override val lyricallyApiEnabled: StateFlow<Boolean> = _lyricallyApiEnabled.asStateFlow()

    private val _wikipediaApiEnabled = MutableStateFlow(prefs.getBoolean(KEY_WIKIPEDIA_API_ENABLED, false))
    override val wikipediaApiEnabled: StateFlow<Boolean> = _wikipediaApiEnabled.asStateFlow()
    
    val autoFetchArtwork: StateFlow<Boolean> get() = librarySection.autoFetchArtwork
    fun setAutoFetchArtwork(enabled: Boolean) = librarySection.setAutoFetchArtwork(enabled)

    private val _artistArtworkSource = MutableStateFlow(
        ArtistArtworkSource.fromName(prefs.getString(AppSettings.KEY_ARTIST_ARTWORK_SOURCE, ArtistArtworkSource.PREFER_LOCAL_THEN_API.name))
    )
    val artistArtworkSource: StateFlow<ArtistArtworkSource> = _artistArtworkSource.asStateFlow()

    private val _appleCanvasEnabled = MutableStateFlow(prefs.getBoolean(KEY_APPLE_CANVAS_ENABLED, false))
    val appleCanvasEnabled: StateFlow<Boolean> = _appleCanvasEnabled.asStateFlow()

    private val _appleCanvasNetworkMode = MutableStateFlow(
        CanvasNetworkMode.fromOrdinal(prefs.getInt(AppSettings.KEY_APPLE_CANVAS_NETWORK_MODE, CanvasNetworkMode.BOTH.ordinal))
    )
    val appleCanvasNetworkMode: StateFlow<CanvasNetworkMode> = _appleCanvasNetworkMode.asStateFlow()
    
    private val _spotifyClientId = MutableStateFlow(prefs.getString(KEY_SPOTIFY_CLIENT_ID, "") ?: "")
    override val spotifyClientId: StateFlow<String> = _spotifyClientId.asStateFlow()
    
    private val _spotifyClientSecret = MutableStateFlow(prefs.getString(KEY_SPOTIFY_CLIENT_SECRET, "") ?: "")
    override val spotifyClientSecret: StateFlow<String> = _spotifyClientSecret.asStateFlow()


    // General Broadcast Status Settings
    private val _broadcastStatusEnabled = MutableStateFlow(prefs.getBoolean(KEY_BROADCAST_STATUS_ENABLED, false))
    val broadcastStatusEnabled: StateFlow<Boolean> = _broadcastStatusEnabled.asStateFlow()

    val bluetoothLyricsEnabled: StateFlow<Boolean> get() = lyricsSection.bluetoothLyricsEnabled
    fun setBluetoothLyricsEnabled(enabled: Boolean) = lyricsSection.setBluetoothLyricsEnabled(enabled)

    // Enhanced User Preferences
    private val _favoriteGenres = MutableStateFlow<Map<String, Int>>(
        try {
            val json = prefs.getString(AppSettings.KEY_FAVORITE_GENRES, null)
            if (json != null) {
                Gson().fromJson(json, object : TypeToken<Map<String, Int>>() {}.type)
            } else {
                emptyMap()
            }
        } catch (e: Exception) {
            emptyMap()
        }
    )
    val favoriteGenres: StateFlow<Map<String, Int>> = _favoriteGenres.asStateFlow()
    
    val dailyListeningStats: StateFlow<Map<String, Long>> get() = guardStatsSection.dailyListeningStats
    
    val weeklyTopArtists: StateFlow<Map<String, Int>> get() = guardStatsSection.weeklyTopArtists
    
    val moodPreferences: StateFlow<Map<String, List<String>>> get() = guardStatsSection.moodPreferences

    private val _songPlayCounts = MutableStateFlow<Map<String, Int>>(
        try {
            val json = prefs.getString(AppSettings.KEY_SONG_PLAY_COUNTS, null)
            if (json != null) {
                Gson().fromJson(json, object : TypeToken<Map<String, Int>>() {}.type)
            } else {
                emptyMap()
            }
        } catch (e: Exception) {
            emptyMap()
        }
    )
    val songPlayCounts: StateFlow<Map<String, Int>> = _songPlayCounts.asStateFlow()

    // Onboarding
    val onboardingCompleted: StateFlow<Boolean> get() = platformSection.onboardingCompleted
    fun setOnboardingCompleted(completed: Boolean) = platformSection.setOnboardingCompleted(completed)

    val initialMediaScanCompleted: StateFlow<Boolean> get() = librarySection.initialMediaScanCompleted
    fun setInitialMediaScanCompleted(completed: Boolean) = librarySection.setInitialMediaScanCompleted(completed)

    val genreDetectionCompleted: StateFlow<Boolean> get() = librarySection.genreDetectionCompleted
    fun setGenreDetectionCompleted(completed: Boolean) = librarySection.setGenreDetectionCompleted(completed)

    val audioMetadataExtractionCompleted: StateFlow<Boolean> get() = librarySection.audioMetadataExtractionCompleted
    fun setAudioMetadataExtractionCompleted(completed: Boolean) = librarySection.setAudioMetadataExtractionCompleted(completed)

    val embeddedArtworkExtractionCompleted: StateFlow<Boolean> get() = librarySection.embeddedArtworkExtractionCompleted
    fun setEmbeddedArtworkExtractionCompleted(completed: Boolean) = librarySection.setEmbeddedArtworkExtractionCompleted(completed)

    val embeddedArtworkExtractionLosslessStatus: StateFlow<Boolean> get() = librarySection.embeddedArtworkExtractionLosslessStatus
    fun setEmbeddedArtworkExtractionLosslessStatus(lossless: Boolean) = librarySection.setEmbeddedArtworkExtractionLosslessStatus(lossless)

    // App Updater Settings
private val _autoCheckForUpdates = MutableStateFlow(prefs.getBoolean(AppSettings.KEY_AUTO_CHECK_FOR_UPDATES, true))
    val autoCheckForUpdates: StateFlow<Boolean> = _autoCheckForUpdates.asStateFlow()
    
    val updateChannel: StateFlow<String> get() = platformSection.updateChannel
    fun setUpdateChannel(channel: String) = platformSection.setUpdateChannel(channel)

    val updateSource: StateFlow<String> get() = platformSection.updateSource
    fun setUpdateSource(source: String) = platformSection.setUpdateSource(source)
    
    val updatesEnabled: StateFlow<Boolean> get() = platformSection.updatesEnabled
    
    val updateNotificationsEnabled: StateFlow<Boolean> get() = platformSection.updateNotificationsEnabled

    val updateStatusNotificationsEnabled: StateFlow<Boolean> get() = platformSection.updateStatusNotificationsEnabled

    // Media Scan Mode
    val mediaScanMode: StateFlow<MediaScanMode> get() = librarySection.mediaScanMode
    fun setMediaScanMode(mode: MediaScanMode) = librarySection.setMediaScanMode(mode)

    val includeHiddenWhitelistedMedia: StateFlow<Boolean> get() = librarySection.includeHiddenWhitelistedMedia
    fun setIncludeHiddenWhitelistedMedia(include: Boolean) = librarySection.setIncludeHiddenWhitelistedMedia(include)

    val updateCheckIntervalHours: StateFlow<Int> get() = platformSection.updateCheckIntervalHours

    // Beta Program
    val hasShownBetaPopup: StateFlow<Boolean> get() = platformSection.hasShownBetaPopup
    fun setHasShownBetaPopup(shown: Boolean) = platformSection.setHasShownBetaPopup(shown)

    // Crash Reporting
    val lastCrashLog: StateFlow<String?> get() = platformSection.lastCrashLog
    fun setLastCrashLog(log: String?) = platformSection.setLastCrashLog(log)

    val crashLogHistory: StateFlow<List<CrashLogEntry>> get() = platformSection.crashLogHistory
    
    // Haptic Feedback Settings
    private val _hapticFeedbackEnabled = MutableStateFlow(prefs.getBoolean(KEY_HAPTIC_FEEDBACK_ENABLED, true))
    val hapticFeedbackEnabled: StateFlow<Boolean> = _hapticFeedbackEnabled.asStateFlow()
    
    // Notification Settings
    val useCustomNotification: StateFlow<Boolean> get() = platformSection.useCustomNotification
    fun setUseCustomNotification(enabled: Boolean) = platformSection.setUseCustomNotification(enabled)

    val libraryOperationsNotificationsEnabled: StateFlow<Boolean> get() = librarySection.libraryOperationsNotificationsEnabled
    fun setLibraryOperationsNotificationsEnabled(enabled: Boolean) = librarySection.setLibraryOperationsNotificationsEnabled(enabled)

    val sleepTimerNotificationsEnabled: StateFlow<Boolean> get() = platformSection.sleepTimerNotificationsEnabled
    fun setSleepTimerNotificationsEnabled(enabled: Boolean) = platformSection.setSleepTimerNotificationsEnabled(enabled)

    val streamingNotificationsEnabled: StateFlow<Boolean> get() = platformSection.streamingNotificationsEnabled
    fun setStreamingNotificationsEnabled(enabled: Boolean) = platformSection.setStreamingNotificationsEnabled(enabled)

    val rhythmGuardAlertNotificationsEnabled: StateFlow<Boolean> get() = guardStatsSection.rhythmGuardAlertNotificationsEnabled
    fun setRhythmGuardAlertNotificationsEnabled(enabled: Boolean) = guardStatsSection.setRhythmGuardAlertNotificationsEnabled(enabled)

    val rhythmGuardTimerNotificationsEnabled: StateFlow<Boolean> get() = guardStatsSection.rhythmGuardTimerNotificationsEnabled
    fun setRhythmGuardTimerNotificationsEnabled(enabled: Boolean) = guardStatsSection.setRhythmGuardTimerNotificationsEnabled(enabled)

    val rhythmPulseNotificationsEnabled: StateFlow<Boolean> get() = platformSection.rhythmPulseNotificationsEnabled

    val rhythmPulseNotificationIntervalHours: StateFlow<Int> get() = platformSection.rhythmPulseNotificationIntervalHours
    
    // UI Settings
    
    val forcePlayerCompactMode: StateFlow<Boolean> get() = platformSection.forcePlayerCompactMode
    fun setForcePlayerCompactMode(enabled: Boolean) = platformSection.setForcePlayerCompactMode(enabled)

    val useExperimentalPlayerUi: StateFlow<Boolean> get() = platformSection.useExperimentalPlayerUi
    fun setUseExperimentalPlayerUi(enabled: Boolean) = platformSection.setUseExperimentalPlayerUi(enabled)

    val enableAlbumEditing: StateFlow<Boolean> get() = librarySection.enableAlbumEditing
    fun setEnableAlbumEditing(enabled: Boolean) = librarySection.setEnableAlbumEditing(enabled)
    
    // Festive Theme Settings
    val festiveThemeEnabled: StateFlow<Boolean> get() = themeSection.festiveThemeEnabled
    fun setFestiveThemeEnabled(enabled: Boolean) = themeSection.setFestiveThemeEnabled(enabled)
    
    val festiveThemeType: StateFlow<String> get() = themeSection.festiveThemeType
    fun setFestiveThemeType(type: String) = themeSection.setFestiveThemeType(type)
    
    val festiveThemeIntensity: StateFlow<Float> get() = themeSection.festiveThemeIntensity
    fun setFestiveThemeIntensity(intensity: Float) = themeSection.setFestiveThemeIntensity(intensity)
    
    val festiveThemeAutoDetect: StateFlow<Boolean> get() = themeSection.festiveThemeAutoDetect
    fun setFestiveThemeAutoDetect(autoDetect: Boolean) = themeSection.setFestiveThemeAutoDetect(autoDetect)
    
    private val _festiveSnowflakeSize = MutableStateFlow(prefs.getFloat(KEY_FESTIVE_SNOWFLAKE_SIZE, 1.0f))
    val festiveSnowflakeSize: StateFlow<Float> = _festiveSnowflakeSize.asStateFlow()
    
    private val _festiveSnowflakeArea = MutableStateFlow(prefs.getString(KEY_FESTIVE_SNOWFLAKE_AREA, "FULL_SCREEN") ?: "FULL_SCREEN")
    val festiveSnowflakeArea: StateFlow<String> = _festiveSnowflakeArea.asStateFlow()
    
    // Developer & Debugging Settings
    val trackErrorCheckerEnabled: StateFlow<Boolean> get() = platformSection.trackErrorCheckerEnabled
    fun setTrackErrorCheckerEnabled(enabled: Boolean) = platformSection.setTrackErrorCheckerEnabled(enabled)

    val codecMonitoringEnabled: StateFlow<Boolean> get() = platformSection.codecMonitoringEnabled
    fun setCodecMonitoringEnabled(enabled: Boolean) = platformSection.setCodecMonitoringEnabled(enabled)
    
    val audioDeviceLoggingEnabled: StateFlow<Boolean> get() = audioEqSection.audioDeviceLoggingEnabled
    fun setAudioDeviceLoggingEnabled(enabled: Boolean) = audioEqSection.setAudioDeviceLoggingEnabled(enabled)
    
    val showCodecNotifications: StateFlow<Boolean> get() = platformSection.showCodecNotifications
    fun setShowCodecNotifications(enabled: Boolean) = platformSection.setShowCodecNotifications(enabled)
    
    val enhancedSeekingEnabled: StateFlow<Boolean> get() = platformSection.enhancedSeekingEnabled
    fun setEnhancedSeekingEnabled(enabled: Boolean) = platformSection.setEnhancedSeekingEnabled(enabled)
    
    // Media3 1.9.0 Features
    
    

    // Battery Saver StateFlows
    val batterySaverEnabled: StateFlow<Boolean> get() = platformSection.batterySaverEnabled
    fun setBatterySaverEnabled(enabled: Boolean) = platformSection.setBatterySaverEnabled(enabled)

    val batterySaverMode: StateFlow<String> get() = platformSection.batterySaverMode
    fun setBatterySaverMode(mode: String) = platformSection.setBatterySaverMode(mode)

    val batterySaverDisableHaptics: StateFlow<Boolean> get() = platformSection.batterySaverDisableHaptics
    fun setBatterySaverDisableHaptics(disable: Boolean) = platformSection.setBatterySaverDisableHaptics(disable)

    val batterySaverEnableOffload: StateFlow<Boolean> get() = platformSection.batterySaverEnableOffload
    fun setBatterySaverEnableOffload(enable: Boolean) = platformSection.setBatterySaverEnableOffload(enable)

    val batterySaverDisableMarquee: StateFlow<Boolean> get() = platformSection.batterySaverDisableMarquee
    fun setBatterySaverDisableMarquee(disable: Boolean) = platformSection.setBatterySaverDisableMarquee(disable)

    val batterySaverDisableLosslessArtwork: StateFlow<Boolean> get() = platformSection.batterySaverDisableLosslessArtwork
    fun setBatterySaverDisableLosslessArtwork(disable: Boolean) = platformSection.setBatterySaverDisableLosslessArtwork(disable)

    val batterySaverDisableAutoFetchArtwork: StateFlow<Boolean> get() = platformSection.batterySaverDisableAutoFetchArtwork
    fun setBatterySaverDisableAutoFetchArtwork(disable: Boolean) = platformSection.setBatterySaverDisableAutoFetchArtwork(disable)

    val globalMarqueeEnabled: StateFlow<Boolean> get() = platformSection.globalMarqueeEnabled
    fun setGlobalMarqueeEnabled(enabled: Boolean) = platformSection.setGlobalMarqueeEnabled(enabled)

    // Derived settings
    private val _isHapticEnabled = MutableStateFlow(calculateIsHapticEnabled())
    val isHapticEnabled: StateFlow<Boolean> = _isHapticEnabled.asStateFlow()

    private val _isAudioOffloadActive = MutableStateFlow(calculateIsAudioOffloadActive())
    val isAudioOffloadActive: StateFlow<Boolean> = _isAudioOffloadActive.asStateFlow()

    private val _isMarqueeActive = MutableStateFlow(calculateIsMarqueeActive())
    val isMarqueeActive: StateFlow<Boolean> = _isMarqueeActive.asStateFlow()

    private val _isLosslessArtworkActive = MutableStateFlow(calculateIsLosslessArtworkActive())
    val isLosslessArtworkActive: StateFlow<Boolean> = _isLosslessArtworkActive.asStateFlow()

    private val _isAutoFetchArtworkActive = MutableStateFlow(calculateIsAutoFetchArtworkActive())
    val isAutoFetchArtworkActive: StateFlow<Boolean> = _isAutoFetchArtworkActive.asStateFlow()

    private fun calculateIsHapticEnabled(): Boolean {
        val masterEnabled = platformSection._batterySaverEnabled.value
        val mode = platformSection._batterySaverMode.value
        val disableHapticsInManual = platformSection._batterySaverDisableHaptics.value
        val baseHaptics = _hapticFeedbackEnabled.value
        
        return baseHaptics && !(masterEnabled && (mode == "auto" || disableHapticsInManual))
    }

    private fun calculateIsAudioOffloadActive(): Boolean {
        val masterEnabled = platformSection._batterySaverEnabled.value
        val mode = platformSection._batterySaverMode.value
        val enableOffloadInManual = platformSection._batterySaverEnableOffload.value
        val baseOffload = audioEqSection._audioOffloadEnabled.value
        
        return baseOffload || (masterEnabled && (mode == "auto" || enableOffloadInManual))
    }

    private fun calculateIsMarqueeActive(): Boolean {
        val masterEnabled = platformSection._batterySaverEnabled.value
        val mode = platformSection._batterySaverMode.value
        val disableMarqueeInManual = platformSection._batterySaverDisableMarquee.value
        val baseMarquee = platformSection._globalMarqueeEnabled.value
        
        return baseMarquee && !(masterEnabled && (mode == "auto" || disableMarqueeInManual))
    }

    private fun calculateIsLosslessArtworkActive(): Boolean {
        val masterEnabled = platformSection._batterySaverEnabled.value
        val mode = platformSection._batterySaverMode.value
        val disableLosslessInManual = platformSection._batterySaverDisableLosslessArtwork.value
        val baseLossless = librarySection._losslessArtwork.value
        
        return baseLossless && !(masterEnabled && (mode == "auto" || disableLosslessInManual))
    }

    private fun calculateIsAutoFetchArtworkActive(): Boolean {
        val masterEnabled = platformSection._batterySaverEnabled.value
        val mode = platformSection._batterySaverMode.value
        val disableAutoFetchInManual = platformSection._batterySaverDisableAutoFetchArtwork.value
        val baseAutoFetch = librarySection._autoFetchArtwork.value
        
        return baseAutoFetch && !(masterEnabled && (mode == "auto" || disableAutoFetchInManual))
    }

    fun updateDerivedSettings() {
        _isHapticEnabled.value = calculateIsHapticEnabled()
        _isAudioOffloadActive.value = calculateIsAudioOffloadActive()
        _isMarqueeActive.value = calculateIsMarqueeActive()
        _isLosslessArtworkActive.value = calculateIsLosslessArtworkActive()
        _isAutoFetchArtworkActive.value = calculateIsAutoFetchArtworkActive()
    }
    
    // Festive Decoration Position Settings
    private val _festiveShowTopLights = MutableStateFlow(prefs.getBoolean(KEY_FESTIVE_SHOW_TOP_LIGHTS, true))
    val festiveShowTopLights: StateFlow<Boolean> = _festiveShowTopLights.asStateFlow()
    
    private val _festiveShowSideGarland = MutableStateFlow(prefs.getBoolean(KEY_FESTIVE_SHOW_SIDE_GARLAND, true))
    val festiveShowSideGarland: StateFlow<Boolean> = _festiveShowSideGarland.asStateFlow()
    
    private val _festiveShowBottomSnow = MutableStateFlow(prefs.getBoolean(KEY_FESTIVE_SHOW_BOTTOM_SNOW, true))
    val festiveShowBottomSnow: StateFlow<Boolean> = _festiveShowBottomSnow.asStateFlow()
    
    private val _festiveShowSnowfall = MutableStateFlow(prefs.getBoolean(KEY_FESTIVE_SHOW_SNOWFALL, true))
    val festiveShowSnowfall: StateFlow<Boolean> = _festiveShowSnowfall.asStateFlow()
    
    // Blacklisted Songs
    
    // Blacklisted Folders
    
    // Whitelisted Songs
    
    // Whitelisted Folders

    // Pinned Folders (Explorer)

    // Backup and Restore Settings
    val lastBackupTimestamp: StateFlow<Long> get() = platformSection.lastBackupTimestamp
    fun setLastBackupTimestamp(timestamp: Long) = platformSection.setLastBackupTimestamp(timestamp)
    
    val autoBackupEnabled: StateFlow<Boolean> get() = platformSection.autoBackupEnabled
    
    val backupLocation: StateFlow<String?> get() = platformSection.backupLocation
    fun setBackupLocation(location: String?) = platformSection.setBackupLocation(location)
    
    // Media Scan Tracking
    val lastScanTimestamp: StateFlow<Long> get() = librarySection.lastScanTimestamp
    fun setLastScanTimestamp(timestamp: Long) = librarySection.setLastScanTimestamp(timestamp)
    
    val lastScanDuration: StateFlow<Long> get() = librarySection.lastScanDuration
    fun setLastScanDuration(duration: Long) = librarySection.setLastScanDuration(duration)
    
    // Media Scan Filtering
    val allowedFormats: StateFlow<Set<String>> get() = librarySection.allowedFormats
    fun setAllowedFormats(formats: Set<String>) = librarySection.setAllowedFormats(formats)
    
    val minimumBitrate: StateFlow<Int> get() = platformSection.minimumBitrate
    
    val minimumDuration: StateFlow<Long> get() = librarySection.minimumDuration
    fun setMinimumDuration(duration: Long) = librarySection.setMinimumDuration(duration)
    
    // Library Sort Order
    val songsSortOrder: StateFlow<String> get() = librarySection.songsSortOrder
    fun setSongsSortOrder(sortOrder: String) = librarySection.setSongsSortOrder(sortOrder)
    
    /**
     * Initialize scheduled workers after all StateFlow properties are initialized
     * This must be done after all MutableStateFlow declarations to avoid NullPointerException
     */
    init {
        migrateLegacyArtworkPreferenceIfNeeded()
        normalizeArtworkPreferenceStateIfNeeded()

        // Defer non-critical WorkManager scheduling to background thread
        Thread {
            android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_BACKGROUND)
            if (prefs.getBoolean(AppSettings.KEY_AUTO_BACKUP_ENABLED, false)) {
                jobScheduler.scheduleAutoBackup()
            }
            if (prefs.getBoolean(AppSettings.KEY_UPDATES_ENABLED, false) &&
                prefs.getBoolean(AppSettings.KEY_AUTO_CHECK_FOR_UPDATES, false) &&
                (
                    prefs.getBoolean(AppSettings.KEY_UPDATE_NOTIFICATIONS_ENABLED, false) ||
                        prefs.getBoolean(AppSettings.KEY_UPDATE_STATUS_NOTIFICATIONS_ENABLED, false)
                    )) {
                jobScheduler.scheduleUpdateNotificationWorker()
            }
            if (prefs.getBoolean(AppSettings.KEY_RHYTHM_PULSE_NOTIFICATIONS_ENABLED, false)) {
                jobScheduler.scheduleRhythmPulseNotificationWorker()
            }
        }.apply { isDaemon = true }.start()
    }

    private fun migrateLegacyArtworkPreferenceIfNeeded() {
        if (prefs.contains(AppSettings.KEY_PREFER_SONG_ARTWORK)) {
            return
        }

        if (!prefs.contains(AppSettings.KEY_IGNORE_MEDIASTORE_COVERS)) {
            return
        }

        val legacyValue = prefs.getBoolean(AppSettings.KEY_IGNORE_MEDIASTORE_COVERS, false)
        prefs.edit { putBoolean(AppSettings.KEY_PREFER_SONG_ARTWORK, legacyValue) }
        librarySection._preferSongArtwork.value = legacyValue
        Log.d("AppSettings", "Migrated legacy ignore_mediastore_covers to prefer_song_artwork")
    }

    private fun normalizeArtworkPreferenceStateIfNeeded() {
        if (librarySection._losslessArtwork.value && !librarySection._preferSongArtwork.value) {
            prefs.edit { putBoolean(AppSettings.KEY_LOSSLESS_ARTWORK, false) }
            librarySection._losslessArtwork.value = false
            Log.d("AppSettings", "Normalized artwork preferences by disabling lossless mode while song-based artwork is off")
        }
    }
    

    // Battery Saver Methods







    


    
    
    

    
    
    





    fun toggleAudioEngineDecoder(decoderName: String, enabled: Boolean) = audioEqSection.toggleAudioEngineDecoder(decoderName, enabled)

    fun resetAudioEngineDecoders() = audioEqSection.resetAudioEngineDecoders()



    fun toggleDecoderExtension(extensionId: String, enabled: Boolean) = audioEqSection.toggleDecoderExtension(extensionId, enabled)

    fun resetDecoderExtensions() = audioEqSection.resetDecoderExtensions()
    
    




    
    // Lyrics Settings Methods
    
    
    

    
    
    

    
    fun isLyricsApiPrioritySet(): Boolean {
        return prefs.contains(AppSettings.KEY_LYRICS_API_PRIORITY)
    }






    
    // Theme Settings Methods
    
    
    

    
    // Artist Separator Settings Methods
    


    fun getArtistSeparatorCacheSignature() = librarySection.getArtistSeparatorCacheSignature()

    fun setArtistSeparatorCacheSignature(signature: String?) = librarySection.setArtistSeparatorCacheSignature(signature)
    
    
    
    
    
    
    
    
    // Player Theme Settings Methods
    
    
    // Library Settings Methods
    
    fun setArtistViewType(viewType: ArtistViewType) {
        prefs.edit { putString(AppSettings.KEY_ARTIST_VIEW_TYPE, viewType.name) }
        _artistViewType.value = viewType
    }
    
    fun setPlaylistViewType(viewType: PlaylistViewType) {
        prefs.edit { putString(AppSettings.KEY_PLAYLIST_VIEW_TYPE, viewType.name) }
        _playlistViewType.value = viewType
    }
    
    
    fun setPlaylistSortOrder(sortOrder: String) {
        prefs.edit { putString(AppSettings.KEY_PLAYLIST_SORT_ORDER, sortOrder) }
        _playlistSortOrder.value = sortOrder
    }
    
    fun setPlaylistDetailSortOrder(sortOrder: String) {
        prefs.edit { putString(AppSettings.KEY_PLAYLIST_DETAIL_SORT_ORDER, sortOrder) }
        _playlistDetailSortOrder.value = sortOrder
    }
    
    
    
    fun resetLibraryTabOrder() = librarySection.resetLibraryTabOrder()

    
    
    fun resetPlayerChipOrder() {
        prefs.edit { remove(AppSettings.KEY_PLAYER_CHIP_ORDER) }
        customizationSection._playerChipOrder.value = customizationSection.defaultChipOrder
    }
    
    fun setDefaultScreen(screen: String) {
        prefs.edit { putString(AppSettings.KEY_DEFAULT_SCREEN, screen) }
        _defaultScreen.value = screen
    }
    
    



    fun resetExpressiveBottomButtonsNormal() {
        prefs.edit {
            remove(AppSettings.KEY_EXPRESSIVE_BOTTOM_BUTTONS_NORMAL)
            remove(AppSettings.KEY_EXPRESSIVE_HIDDEN_BOTTOM_BUTTONS_NORMAL)
        }
        customizationSection._expressiveBottomButtonsNormal.value = customizationSection.defaultExpressiveBottomButtonsNormal
        customizationSection._expressiveHiddenBottomButtonsNormal.value = emptySet()
    }



    fun resetExpressiveBottomButtonsMerge() {
        prefs.edit {
            remove(AppSettings.KEY_EXPRESSIVE_BOTTOM_BUTTONS_MERGE)
            remove(AppSettings.KEY_EXPRESSIVE_HIDDEN_BOTTOM_BUTTONS_MERGE)
        }
        customizationSection._expressiveBottomButtonsMerge.value = customizationSection.defaultExpressiveBottomButtonsMerge
        customizationSection._expressiveHiddenBottomButtonsMerge.value = emptySet()
    }


    






    fun requestFullMediaRescanOnNextLaunch(reason: String = "unspecified") = librarySection.requestFullMediaRescanOnNextLaunch(reason)

    fun consumePendingFullMediaRescanRequest() = librarySection.consumePendingFullMediaRescanRequest()

    /**
     * Self-heal guard gate: allows the one-time embedded artwork re-extraction
     * at most once per 24h, so a wiped artwork cache self-heals on the next
     * launch without ever turning into a per-launch extraction loop.
     */
    fun shouldRunEmbeddedArtSelfHeal() = librarySection.shouldRunEmbeddedArtSelfHeal()

    fun markEmbeddedArtSelfHealDone() = librarySection.markEmbeddedArtSelfHealDone()
    
    
    fun setShowScrollToTop(show: Boolean) {
        prefs.edit { putBoolean(AppSettings.KEY_SHOW_SCROLL_TO_TOP, show) }
        _showScrollToTop.value = show
    }
    
    // App Mode setter methods
    
    
    
    

    
    
    // Audio Device Settings Methods
    
    
    
    
    
    // Equalizer Settings Methods
    
    
    
    
    fun setUserAudioDevices(devicesJson: String?) {
        prefs.edit { putString(AppSettings.KEY_USER_AUDIO_DEVICES, devicesJson) }
        _userAudioDevices.value = devicesJson
    }
    
    
    
    
    
    fun setBassBoostAvailable(available: Boolean) = audioEqSection.setBassBoostAvailable(available)
    
    fun isBassBoostAvailable() = audioEqSection.isBassBoostAvailable()
    val isBassBoostAvailableFlow: StateFlow<Boolean> get() = audioEqSection.isBassBoostAvailable
    
    
    

    fun saveCustomEqualizerPreset(preset: CustomEqualizerPreset) = audioEqSection.saveCustomEqualizerPreset(preset)

    fun deleteCustomEqualizerPreset(id: String) = audioEqSection.deleteCustomEqualizerPreset(id)


    fun resetEqualizerPresetOrder() = audioEqSection.resetEqualizerPresetOrder()


    fun pinAutoEQProfile(name: String) = audioEqSection.pinAutoEQProfile(name)

    fun unpinAutoEQProfile(name: String) = audioEqSection.unpinAutoEQProfile(name)

    fun saveCustomAutoEQProfile(profile: AutoEQProfile) = audioEqSection.saveCustomAutoEQProfile(profile)

    fun saveCustomAutoEQProfiles(profiles: List<AutoEQProfile>) = audioEqSection.saveCustomAutoEQProfiles(profiles)

    fun deleteCustomAutoEQProfile(name: String) = audioEqSection.deleteCustomAutoEQProfile(name)

    
    // Sleep Timer Methods
    
    
    
    // Queue & Playback Behavior Methods
    
    
    

    

    
    
    
    
    
    
    




    
    // Time Format Settings Methods
    
    

    
    // Stop Playback on App Close Methods
    
    // Queue Persistence Methods
    
    
    



    fun clearSavedOriginalQueue() = playbackQueueSection.clearSavedOriginalQueue()
    
    fun clearSavedQueue() = playbackQueueSection.clearSavedQueue()

    // Context queue settings


    
    // Cache Settings Methods
    
    // Search History Methods


    // Playlists
    fun setPlaylists(playlistsJson: String?) {
        if (playlistsJson == null) {
            prefs.edit { remove(AppSettings.KEY_PLAYLISTS) }
        } else {
            prefs.edit { putString(AppSettings.KEY_PLAYLISTS, playlistsJson) }
        }
        _playlists.value = playlistsJson
    }

    fun setFavoriteSongs(favoriteSongsJson: String?) {
        if (favoriteSongsJson == null) {
            prefs.edit { remove(AppSettings.KEY_FAVORITE_SONGS) } // Use apply() to prevent ANR
        } else {
            prefs.edit { putString(AppSettings.KEY_FAVORITE_SONGS, favoriteSongsJson) } // Use apply() to prevent ANR
        }
        _favoriteSongs.value = favoriteSongsJson
    }
    
    fun setDefaultPlaylistsEnabled(enabled: Boolean) {
        prefs.edit { putBoolean(AppSettings.KEY_DEFAULT_PLAYLISTS_ENABLED, enabled) }
        _defaultPlaylistsEnabled.value = enabled
    }

    fun setShowLikedInPlaylists(enabled: Boolean) {
        prefs.edit { putBoolean(AppSettings.KEY_SHOW_LIKED_IN_PLAYLISTS, enabled) }
        _showLikedInPlaylists.value = enabled
    }

    fun setSmartPlaylistRecentlyAdded(enabled: Boolean) {
        prefs.edit { putBoolean(AppSettings.KEY_SMART_PLAYLIST_RECENTLY_ADDED, enabled) }
        _smartPlaylistRecentlyAdded.value = enabled
    }

    fun setSmartPlaylistMostPlayed(enabled: Boolean) {
        prefs.edit { putBoolean(AppSettings.KEY_SMART_PLAYLIST_MOST_PLAYED, enabled) }
        _smartPlaylistMostPlayed.value = enabled
    }

    fun setSmartPlaylistOnRepeat(enabled: Boolean) {
        prefs.edit { putBoolean(AppSettings.KEY_SMART_PLAYLIST_ON_REPEAT, enabled) }
        _smartPlaylistOnRepeat.value = enabled
    }

    fun setSmartPlaylistForgottenFavorites(enabled: Boolean) {
        prefs.edit { putBoolean(AppSettings.KEY_SMART_PLAYLIST_FORGOTTEN_FAVORITES, enabled) }
        _smartPlaylistForgottenFavorites.value = enabled
    }

    fun setSmartPlaylistRecentlyPlayed(enabled: Boolean) {
        prefs.edit { putBoolean(AppSettings.KEY_SMART_PLAYLIST_RECENTLY_PLAYED, enabled) }
        _smartPlaylistRecentlyPlayed.value = enabled
    }
    
    // User Statistics Methods
    

    fun setRhythmGuardMode(mode: String) {
        val normalizedMode = sanitizeRhythmGuardMode(mode)
        prefs.edit {
    putString(AppSettings.KEY_RHYTHM_GUARD_MODE, normalizedMode)
    putString(AppSettings.KEY_RHYTHM_AURA_MODE, normalizedMode)
}
        guardStatsSection._rhythmAuraMode.value = normalizedMode

        if (normalizedMode == AppSettings.RHYTHM_GUARD_MODE_AUTO) {
            applyRhythmGuardAutoProfileForAge(guardStatsSection._rhythmAuraAge.value)
        }
    }

    @Deprecated("Use setRhythmGuardMode")

    fun setRhythmGuardAge(age: Int) {
        val safeAge = age.coerceIn(8, 80)
        prefs.edit {
    putInt(AppSettings.KEY_RHYTHM_GUARD_AGE, safeAge)
    putInt(AppSettings.KEY_RHYTHM_AURA_AGE, safeAge)
}
        guardStatsSection._rhythmAuraAge.value = safeAge

        if (guardStatsSection._rhythmAuraMode.value == AppSettings.RHYTHM_GUARD_MODE_AUTO) {
            applyRhythmGuardAutoProfileForAge(safeAge)
        }
    }













    fun setRhythmGuardListeningTimeout(untilEpochMs: Long, reason: String = "", startedAtEpochMs: Long = System.currentTimeMillis()) = guardStatsSection.setRhythmGuardListeningTimeout(untilEpochMs, reason, startedAtEpochMs)

    fun clearRhythmGuardListeningTimeout() = guardStatsSection.clearRhythmGuardListeningTimeout()


    fun setRhythmGuardTimeoutCooldownWithLimit(cooldownUntilMs: Long, nextLimitMinutes: Int) = guardStatsSection.setRhythmGuardTimeoutCooldownWithLimit(cooldownUntilMs, nextLimitMinutes)

    fun clearRhythmGuardTimeoutCooldown() = guardStatsSection.clearRhythmGuardTimeoutCooldown()

    fun isRhythmGuardTimeoutActive(nowMs: Long = System.currentTimeMillis()) = guardStatsSection.isRhythmGuardTimeoutActive(nowMs)

    fun getRhythmGuardPolicyBands(): List<RhythmGuardPolicyBand> = RHYTHM_GUARD_POLICY_BANDS

    @Deprecated("Use getRhythmGuardPolicyBands")
    fun getRhythmAuraPolicyBands(): List<RhythmAuraPolicyBand> = RHYTHM_AURA_POLICY_BANDS

    fun getRhythmGuardPolicy(age: Int = guardStatsSection._rhythmAuraAge.value): RhythmGuardPolicyBand {
        val safeAge = age.coerceIn(8, 80)
        return RHYTHM_GUARD_POLICY_BANDS.firstOrNull { safeAge in it.minAge..it.maxAge }
            ?: RHYTHM_GUARD_POLICY_BANDS.last()
    }


    @Deprecated("Use getRhythmGuardPolicy")
    fun estimateRhythmGuardTodayListeningMinutes(dailyListeningStats: Map<String, Long>, songsPlayed: Int, listeningTimeMs: Long) = guardStatsSection.estimateRhythmGuardTodayListeningMinutes(dailyListeningStats, songsPlayed, listeningTimeMs)

    @Deprecated("Use estimateRhythmGuardTodayListeningMinutes")
    fun estimateRhythmAuraTodayListeningMinutes(dailyListeningStats: Map<String, Long>, songsPlayed: Int, listeningTimeMs: Long) = guardStatsSection.estimateRhythmAuraTodayListeningMinutes(dailyListeningStats, songsPlayed, listeningTimeMs)

    fun applyRhythmGuardAutoProfileForAge(age: Int) {
        val safeAge = age.coerceIn(8, 80)
        val policy = getRhythmGuardPolicy(safeAge)

        setAudioNormalization(true)
        setReplayGain(false)
        setUseSystemVolume(true)
        setStopPlaybackOnZeroVolume(policy.stopPlaybackOnZeroVolume)

        if (policy.enforceHapticFeedback && !_hapticFeedbackEnabled.value) {
            setHapticFeedbackEnabled(true)
        }

        setRhythmGuardLastAutoAppliedAt(System.currentTimeMillis())
    }

    @Deprecated("Use applyRhythmGuardAutoProfileForAge")
    fun applyRhythmAuraAutoProfileForAge(age: Int) = applyRhythmGuardAutoProfileForAge(age)

    private fun sanitizeRhythmGuardMode(mode: String?) = guardStatsSection.sanitizeRhythmGuardMode(mode)

    @Deprecated("Use sanitizeRhythmGuardMode")
    private fun sanitizeRhythmAuraMode(mode: String?) = guardStatsSection.sanitizeRhythmAuraMode(mode)
    
    
    

    // Recently Played Methods
    fun updateRecentlyPlayed(songIds: List<String>) {
        val json = Gson().toJson(songIds)
        prefs.edit { putString(AppSettings.KEY_RECENTLY_PLAYED, json) }
        playbackQueueSection._recentlyPlayed.value = songIds
    }

    fun updateRecentlyPlayedSongCache(songs: List<Song>) {
        val snapshots = songs
            .distinctBy { it.id }
            .take(50)
            .map { it.toRecentSongSnapshot() }

        prefs.edit { putString(AppSettings.KEY_RECENTLY_PLAYED_SONG_CACHE, Gson().toJson(snapshots)) }
        _recentlyPlayedSongCache.value = snapshots
            .mapNotNull { snapshot ->
                snapshot.toSongOrNull()?.let { song -> song.id to song }
            }
            .toMap()
    }
    
    fun updateLastPlayedTimestamp(timestamp: Long) {
        prefs.edit { putLong(AppSettings.KEY_LAST_PLAYED_TIMESTAMP, timestamp) }
        playbackQueueSection._lastPlayedTimestamp.value = timestamp
    }
    
    
    // API Enable/Disable Methods
    fun setAppleCanvasEnabled(enabled: Boolean) {
        prefs.edit { putBoolean(AppSettings.KEY_APPLE_CANVAS_ENABLED, enabled) }
        _appleCanvasEnabled.value = enabled
        if (enabled) setIntegrationsEnabled(true)
    }

    fun setAppleCanvasNetworkMode(mode: CanvasNetworkMode) {
        prefs.edit { putInt(AppSettings.KEY_APPLE_CANVAS_NETWORK_MODE, mode.ordinal) }
        _appleCanvasNetworkMode.value = mode
    }

    fun setArtistArtworkSource(source: ArtistArtworkSource) {
        prefs.edit { putString(AppSettings.KEY_ARTIST_ARTWORK_SOURCE, source.name) }
        _artistArtworkSource.value = source
    }

    fun setIntegrationsEnabled(enabled: Boolean) {
        prefs.edit { putBoolean(AppSettings.KEY_INTEGRATIONS_ENABLED, enabled) }
        _integrationsEnabled.value = enabled
    }

    fun setDeezerApiEnabled(enabled: Boolean) {
        prefs.edit { putBoolean(AppSettings.KEY_DEEZER_API_ENABLED, enabled) }
        _deezerApiEnabled.value = enabled
        if (enabled) setIntegrationsEnabled(true)
    }
    
    fun setLrcLibApiEnabled(enabled: Boolean) {
        prefs.edit { putBoolean(AppSettings.KEY_LRCLIB_API_ENABLED, enabled) }
        _lrclibApiEnabled.value = enabled
        if (enabled) setIntegrationsEnabled(true)
    }
    
    fun setBetterLyricsApiEnabled(enabled: Boolean) {
        prefs.edit { putBoolean(AppSettings.KEY_BETTERLYRICS_API_ENABLED, enabled) }
        _betterLyricsApiEnabled.value = enabled
        if (enabled) setIntegrationsEnabled(true)
    }
    
    fun setYTMusicApiEnabled(enabled: Boolean) {
        prefs.edit { putBoolean(AppSettings.KEY_YTMUSIC_API_ENABLED, enabled) }
        _ytMusicApiEnabled.value = enabled
        if (enabled) setIntegrationsEnabled(true)
    }
    
    fun setSpotifyApiEnabled(enabled: Boolean) {
        prefs.edit { putBoolean(AppSettings.KEY_SPOTIFY_API_ENABLED, enabled) }
        _spotifyApiEnabled.value = enabled
        if (enabled) setIntegrationsEnabled(true)
    }
    
    fun setLyricallyApiEnabled(enabled: Boolean) {
        prefs.edit { putBoolean(AppSettings.KEY_LYRICALLY_API_ENABLED, enabled) }
        _lyricallyApiEnabled.value = enabled
        if (enabled) setIntegrationsEnabled(true)
    }

    fun setWikipediaApiEnabled(enabled: Boolean) {
        prefs.edit { putBoolean(AppSettings.KEY_WIKIPEDIA_API_ENABLED, enabled) }
        _wikipediaApiEnabled.value = enabled
        if (enabled) setIntegrationsEnabled(true)
    }
    
    
    fun setSpotifyClientId(clientId: String) {
        prefs.edit { putString(AppSettings.KEY_SPOTIFY_CLIENT_ID, clientId) }
        _spotifyClientId.value = clientId
    }
    
    fun setSpotifyClientSecret(clientSecret: String) {
        prefs.edit { putString(AppSettings.KEY_SPOTIFY_CLIENT_SECRET, clientSecret) }
        _spotifyClientSecret.value = clientSecret
    }


    // General Broadcast Status Methods
    fun setBroadcastStatusEnabled(enabled: Boolean) {
        prefs.edit { putBoolean(AppSettings.KEY_BROADCAST_STATUS_ENABLED, enabled) }
        _broadcastStatusEnabled.value = enabled
    }


    // Enhanced User Preferences Methods
    fun updateFavoriteGenres(genres: Map<String, Int>) {
        val json = Gson().toJson(genres)
        prefs.edit { putString(AppSettings.KEY_FAVORITE_GENRES, json) }
        _favoriteGenres.value = genres
    }
    
    fun updateDailyListeningStats(stats: Map<String, Long>) {
        val json = Gson().toJson(stats)
        prefs.edit { putString(AppSettings.KEY_DAILY_LISTENING_STATS, json) }
        guardStatsSection._dailyListeningStats.value = stats
    }
    
    fun updateWeeklyTopArtists(artists: Map<String, Int>) {
        val json = Gson().toJson(artists)
        prefs.edit { putString(AppSettings.KEY_WEEKLY_TOP_ARTISTS, json) }
        guardStatsSection._weeklyTopArtists.value = artists
    }
    
    fun updateMoodPreferences(preferences: Map<String, List<String>>) {
        val json = Gson().toJson(preferences)
        prefs.edit { putString(AppSettings.KEY_MOOD_PREFERENCES, json) }
        guardStatsSection._moodPreferences.value = preferences
    }

    // Song Play Counts Methods
    fun setSongPlayCounts(counts: Map<String, Int>) {
        val json = Gson().toJson(counts)
        prefs.edit { putString(AppSettings.KEY_SONG_PLAY_COUNTS, json) }
        _songPlayCounts.value = counts
    }

    // Onboarding Methods






    // App Updater Settings Methods
    fun setAutoCheckForUpdates(enable: Boolean) {
        prefs.edit { putBoolean(AppSettings.KEY_AUTO_CHECK_FOR_UPDATES, enable) }
        _autoCheckForUpdates.value = enable

        if (jobScheduler.shouldRunUpdateNotificationWorker()) {
            jobScheduler.scheduleUpdateNotificationWorker()
        } else {
            jobScheduler.cancelUpdateNotificationWorker()
        }
    }



    fun setUpdatesEnabled(enable: Boolean) {
        prefs.edit { putBoolean(AppSettings.KEY_UPDATES_ENABLED, enable) }
        platformSection._updatesEnabled.value = enable
        
        // Update WorkManager scheduling based on new state
        if (jobScheduler.shouldRunUpdateNotificationWorker()) {
            jobScheduler.scheduleUpdateNotificationWorker()
        } else {
            jobScheduler.cancelUpdateNotificationWorker()
        }
    }

    fun setUpdateNotificationsEnabled(enable: Boolean) {
        prefs.edit { putBoolean(AppSettings.KEY_UPDATE_NOTIFICATIONS_ENABLED, enable) }
        platformSection._updateNotificationsEnabled.value = enable
        
        // Update WorkManager scheduling
        if (jobScheduler.shouldRunUpdateNotificationWorker()) {
            jobScheduler.scheduleUpdateNotificationWorker()
        } else {
            jobScheduler.cancelUpdateNotificationWorker()
        }
    }

    fun setUpdateStatusNotificationsEnabled(enable: Boolean) {
        prefs.edit { putBoolean(AppSettings.KEY_UPDATE_STATUS_NOTIFICATIONS_ENABLED, enable) }
        platformSection._updateStatusNotificationsEnabled.value = enable

        if (jobScheduler.shouldRunUpdateNotificationWorker()) {
            jobScheduler.scheduleUpdateNotificationWorker()
        } else {
            jobScheduler.cancelUpdateNotificationWorker()
        }
    }



    fun setUpdateCheckIntervalHours(hours: Int) {
        prefs.edit { putInt(AppSettings.KEY_UPDATE_CHECK_INTERVAL_HOURS, hours) }
        platformSection._updateCheckIntervalHours.value = hours

        if (jobScheduler.shouldRunUpdateNotificationWorker()) {
            jobScheduler.scheduleUpdateNotificationWorker()
        }
    }

    // Beta Program Methods

    // Crash Reporting Methods

    fun addCrashLogEntry(log: String) {
        val currentHistory = platformSection._crashLogHistory.value.toMutableList()
        val newEntry = CrashLogEntry(System.currentTimeMillis(), log)
        currentHistory.add(0, newEntry) // Add to the beginning
        // Keep only the last 6 crash logs to prevent excessive storage
        val limitedHistory = currentHistory.take(6)
        val json = Gson().toJson(limitedHistory)
        // Update in-memory state first for immediate UI feedback
        platformSection._crashLogHistory.value = limitedHistory
        // Use commit() instead of apply() for crash logs to ensure persistence before process exits
        // This is critical - crash logs must be written synchronously or they'll be lost
        try {
            prefs.edit { putString(AppSettings.KEY_CRASH_LOG_HISTORY, json) }
        } catch (e: Exception) {
            // If commit fails (shouldn't happen), at least log it
            Log.e("CrashLog", "Failed to persist crash log to SharedPreferences", e)
        }
    }

    fun clearCrashLogHistory() {
        prefs.edit { remove(AppSettings.KEY_CRASH_LOG_HISTORY) }
        platformSection._crashLogHistory.value = emptyList()
    }
    
    // Haptic Feedback Methods
    fun setHapticFeedbackEnabled(enabled: Boolean) {
        prefs.edit { putBoolean(AppSettings.KEY_HAPTIC_FEEDBACK_ENABLED, enabled) }
        _hapticFeedbackEnabled.value = enabled
        updateDerivedSettings()
    }
    
    // Notification Settings Methods






    fun setRhythmPulseNotificationsEnabled(enabled: Boolean) {
        prefs.edit { putBoolean(AppSettings.KEY_RHYTHM_PULSE_NOTIFICATIONS_ENABLED, enabled) }
        platformSection._rhythmPulseNotificationsEnabled.value = enabled

        if (enabled) {
            jobScheduler.scheduleRhythmPulseNotificationWorker()
        } else {
            jobScheduler.cancelRhythmPulseNotificationWorker()
        }
    }

    fun setRhythmPulseNotificationIntervalHours(hours: Int) {
        val safeHours = hours.coerceIn(6, 72)
        prefs.edit { putInt(AppSettings.KEY_RHYTHM_PULSE_NOTIFICATION_INTERVAL_HOURS, safeHours) }
        platformSection._rhythmPulseNotificationIntervalHours.value = safeHours

        if (platformSection._rhythmPulseNotificationsEnabled.value) {
            jobScheduler.scheduleRhythmPulseNotificationWorker()
        }
    }
    


    
    // Codec Monitoring & Enhanced Seeking Methods
    
    
    
    
    
    // Media3 1.9.0 Feature Methods
    
    
    
    // UI Settings Methods
    
    // Festive Theme Settings Methods
    
    
    
    
    fun setFestiveSnowflakeSize(size: Float) {
        prefs.edit { putFloat(AppSettings.KEY_FESTIVE_SNOWFLAKE_SIZE, size) }
        _festiveSnowflakeSize.value = size
    }
    
    fun setFestiveSnowflakeArea(area: String) {
        prefs.edit { putString(AppSettings.KEY_FESTIVE_SNOWFLAKE_AREA, area) }
        _festiveSnowflakeArea.value = area
    }
    
    // Festive Decoration Position Methods
    fun setFestiveShowTopLights(show: Boolean) {
        prefs.edit { putBoolean(AppSettings.KEY_FESTIVE_SHOW_TOP_LIGHTS, show) }
        _festiveShowTopLights.value = show
    }
    
    fun setFestiveShowSideGarland(show: Boolean) {
        prefs.edit { putBoolean(AppSettings.KEY_FESTIVE_SHOW_SIDE_GARLAND, show) }
        _festiveShowSideGarland.value = show
    }
    
    fun setFestiveShowBottomSnow(show: Boolean) {
        prefs.edit { putBoolean(AppSettings.KEY_FESTIVE_SHOW_BOTTOM_SNOW, show) }
        _festiveShowBottomSnow.value = show
    }
    
    fun setFestiveShowSnowfall(show: Boolean) {
        prefs.edit { putBoolean(AppSettings.KEY_FESTIVE_SHOW_SNOWFALL, show) }
        _festiveShowSnowfall.value = show
    }
    
    // Blacklisted Songs Methods
    fun addToBlacklist(songId: String) = librarySection.addToBlacklist(songId)
    
    fun removeFromBlacklist(songId: String) = librarySection.removeFromBlacklist(songId)
    
    fun isBlacklisted(songId: String) = librarySection.isBlacklisted(songId)
    
    fun clearBlacklist() = librarySection.clearBlacklist()
    
    // Blacklisted Folders Methods
    fun addFolderToBlacklist(folderPath: String) = librarySection.addFolderToBlacklist(folderPath)
    
    fun removeFolderFromBlacklist(folderPath: String) = librarySection.removeFolderFromBlacklist(folderPath)
    
    fun isFolderBlacklisted(folderPath: String) = librarySection.isFolderBlacklisted(folderPath)
    
    fun clearFolderBlacklist() = librarySection.clearFolderBlacklist()
    
    // Bulk operations for better synchronization
    fun removeFolderAndRelatedSongs(folderPath: String, songsInFolder: List<String>) = librarySection.removeFolderAndRelatedSongs(folderPath, songsInFolder)
    
    fun addFolderAndOptionalSong(folderPath: String, songId: String?) = librarySection.addFolderAndOptionalSong(folderPath, songId)
    
    // Helper method to check if a song would be filtered by current blacklist rules
    fun isEffectivelyBlacklisted(songId: String, songPath: String?) = librarySection.isEffectivelyBlacklisted(songId, songPath)
    
    // Whitelisted Songs Methods
    fun addToWhitelist(songId: String) = librarySection.addToWhitelist(songId)
    
    fun removeFromWhitelist(songId: String) = librarySection.removeFromWhitelist(songId)
    
    fun isSongWhitelisted(songId: String) = librarySection.isSongWhitelisted(songId)
    
    fun clearWhitelist() = librarySection.clearWhitelist()
    
    // Song Rating Methods
    // Whitelisted Folders Methods
    fun addFolderToWhitelist(folderPath: String) = librarySection.addFolderToWhitelist(folderPath)
    
    fun removeFolderFromWhitelist(folderPath: String) = librarySection.removeFolderFromWhitelist(folderPath)
    
    fun isFolderWhitelisted(folderPath: String) = librarySection.isFolderWhitelisted(folderPath)
    
    fun clearFolderWhitelist() = librarySection.clearFolderWhitelist()

    // Pinned Folders Methods (Explorer)
    fun addFolderToPinned(folderPath: String) = librarySection.addFolderToPinned(folderPath)

    fun removeFolderFromPinned(folderPath: String) = librarySection.removeFolderFromPinned(folderPath)

    fun isFolderPinned(folderPath: String) = librarySection.isFolderPinned(folderPath)

    fun clearPinnedFolders() = librarySection.clearPinnedFolders()
    
    
    // Helper method to check if a song would be filtered by current whitelist rules
    fun isEffectivelyWhitelisted(songId: String, songPath: String?) = librarySection.isEffectivelyWhitelisted(songId, songPath)
    
    // Backup and Restore Methods
    
    fun setAutoBackupEnabled(enabled: Boolean) {
        prefs.edit { putBoolean(AppSettings.KEY_AUTO_BACKUP_ENABLED, enabled) }
        platformSection._autoBackupEnabled.value = enabled
        
        // Schedule or cancel auto-backup worker
        if (enabled) {
            jobScheduler.scheduleAutoBackup()
            // Trigger an immediate backup when enabling auto-backup
            triggerImmediateBackup()
            Log.d("AppSettings", "Auto-backup enabled: triggering immediate backup")
        } else {
            jobScheduler.cancelAutoBackup()
        }
    }
    
    // Media Scanning Methods
    
    
    
    fun setMinimumBitrate(bitrate: Int) {
        val changed = platformSection._minimumBitrate.value != bitrate
        prefs.edit { putInt(AppSettings.KEY_MINIMUM_BITRATE, bitrate) }
        platformSection._minimumBitrate.value = bitrate
        Log.d("AppSettings", "Minimum bitrate set to: ${bitrate}kbps")
        if (changed) {
            librarySection.requestFullMediaRescanOnNextLaunch(reason = "minimum_bitrate_changed")
        }
    }
    
    
    /** Trigger an immediate one-time backup (useful for testing) */
    fun triggerImmediateBackup() = jobScheduler.triggerImmediateBackup()
    

    data class BackupRestoreSections(
        val includeGeneralSettings: Boolean = true,
        val includeLibraryData: Boolean = true,
        val includeStatsAndRhythmGuard: Boolean = true
    ) {
        val hasAtLeastOneSectionSelected: Boolean
            get() = includeGeneralSettings || includeLibraryData || includeStatsAndRhythmGuard
    }

    private fun isLibraryBackupKey(key: String): Boolean {
        return key == AppSettings.KEY_PLAYLISTS ||
            key == AppSettings.KEY_FAVORITE_SONGS ||
            key == AppSettings.KEY_BLACKLISTED_SONGS ||
            key == AppSettings.KEY_BLACKLISTED_FOLDERS ||
            key == AppSettings.KEY_WHITELISTED_SONGS ||
            key == AppSettings.KEY_WHITELISTED_FOLDERS ||
            key == AppSettings.KEY_PINNED_FOLDERS ||
            key == AppSettings.KEY_SONG_LYRICS_PREFERENCES ||
            key == AppSettings.KEY_SONG_CUSTOM_LRC_FILES
    }

    fun isRhythmGuardTransientRuntimeKey(key: String): Boolean {
        return key == AppSettings.KEY_RHYTHM_GUARD_TIMEOUT_UNTIL_MS ||
            key == AppSettings.KEY_RHYTHM_GUARD_TIMEOUT_REASON ||
            key == AppSettings.KEY_RHYTHM_GUARD_TIMEOUT_STARTED_AT_MS ||
            key == AppSettings.KEY_RHYTHM_GUARD_TIMEOUT_COOLDOWN_UNTIL_MS ||
            key == AppSettings.KEY_RHYTHM_GUARD_NEXT_ALLOWED_LIMIT_MINUTES
    }

    fun isStatsAndRhythmGuardBackupKey(key: String): Boolean {
        if (isRhythmGuardTransientRuntimeKey(key)) {
            return false
        }

        return key == AppSettings.KEY_LAST_PLAYED_TIMESTAMP ||
            key == AppSettings.KEY_RECENTLY_PLAYED ||
            key == AppSettings.KEY_LISTENING_TIME ||
            key == AppSettings.KEY_SONGS_PLAYED ||
            key == AppSettings.KEY_UNIQUE_ARTISTS ||
            key == AppSettings.KEY_GENRE_PREFERENCES ||
            key == AppSettings.KEY_FAVORITE_GENRES ||
            key == AppSettings.KEY_DAILY_LISTENING_STATS ||
            key == AppSettings.KEY_WEEKLY_TOP_ARTISTS ||
            key == AppSettings.KEY_MOOD_PREFERENCES ||
            key == AppSettings.KEY_SONG_PLAY_COUNTS ||
            key == AppSettings.KEY_HOME_SHOW_LISTENING_STATS ||
            key == AppSettings.KEY_RHYTHM_GUARD_MODE ||
            key == AppSettings.KEY_RHYTHM_GUARD_AGE ||
            key == AppSettings.KEY_RHYTHM_GUARD_MANUAL_WARNINGS_ENABLED ||
            key == AppSettings.KEY_RHYTHM_GUARD_MANUAL_VOLUME_THRESHOLD ||
            key == AppSettings.KEY_RHYTHM_GUARD_LAST_AUTO_APPLIED_AT ||
            key == AppSettings.KEY_RHYTHM_GUARD_ALERT_THRESHOLD_MINUTES ||
            key == AppSettings.KEY_RHYTHM_GUARD_WARNING_TIMEOUT_MINUTES ||
            key == AppSettings.KEY_RHYTHM_GUARD_BREAK_RESUME_MINUTES ||
            key == AppSettings.KEY_RHYTHM_AURA_MODE ||
            key == AppSettings.KEY_RHYTHM_AURA_AGE ||
            key.startsWith("rhythm_guard_") ||
            key.startsWith("rhythm_aura_")
    }

    fun shouldIncludeKeyInBackupSections(
        key: String,
        sections: BackupRestoreSections
    ): Boolean {
        if (isRhythmGuardTransientRuntimeKey(key)) {
            return false
        }

        return when {
            isLibraryBackupKey(key) -> sections.includeLibraryData
            isStatsAndRhythmGuardBackupKey(key) -> sections.includeStatsAndRhythmGuard
            else -> sections.includeGeneralSettings
        }
    }
    
    sealed class BackupValidationResult {
        object Valid : BackupValidationResult()
        data class Invalid(val reason: String) : BackupValidationResult()
    }

    /**
     * Validates a backup JSON payload for structure, header keys, and parseable playlists data
     */
    fun validateBackupJson(
        backupJson: String,
        sections: BackupRestoreSections = BackupRestoreSections()
    ): BackupValidationResult {
        if (backupJson.isBlank()) {
            return BackupValidationResult.Invalid("Backup JSON payload is empty")
        }
        return try {
            val type = object : TypeToken<Map<String, Any?>>() {}.type
            val map: Map<String, Any?>? = GsonUtils.gson.fromJson(backupJson, type)
            if (map == null || map.isEmpty()) {
                return BackupValidationResult.Invalid("Backup content is not a valid non-empty JSON object")
            }

            if (!map.containsKey("preferences")) {
                return BackupValidationResult.Invalid("Missing required 'preferences' key")
            }

            if (!map.containsKey("backup_version")) {
                return BackupValidationResult.Invalid("Missing required 'backup_version' key")
            }

            val effectiveSections = if (sections.hasAtLeastOneSectionSelected) sections else BackupRestoreSections()

            if (effectiveSections.includeLibraryData && map.containsKey("playlists_data")) {
                val playlistsData = map["playlists_data"] as? String
                if (!playlistsData.isNullOrBlank()) {
                    try {
                        val playlistListType = object : TypeToken<List<Playlist>>() {}.type
                        val parsedPlaylists: List<Playlist>? = GsonUtils.gson.fromJson(playlistsData, playlistListType)
                        if (parsedPlaylists == null) {
                            return BackupValidationResult.Invalid("playlists_data cannot be parsed into Playlist objects")
                        }
                    } catch (e: Exception) {
                        return BackupValidationResult.Invalid("Corrupted playlist structure inside backup: ${e.message}")
                    }
                }
            }

            BackupValidationResult.Valid
        } catch (e: Exception) {
            BackupValidationResult.Invalid("JSON parsing exception: ${e.message}")
        }
    }

    /**
     * Creates a complete backup of all app data as JSON
     */
    fun createBackup(sections: BackupRestoreSections = BackupRestoreSections()): String {
        val backupData = mutableMapOf<String, Any?>()
        val preferencesTypes = mutableMapOf<String, String>()

        val effectiveSections = if (sections.hasAtLeastOneSectionSelected) {
            sections
        } else {
            BackupRestoreSections()
        }
        
        // Get all preferences
        val allPrefs = prefs.all
        
        // Filter based on selected backup sections.
        val filteredPrefs = allPrefs.filterKeys { key ->
            shouldIncludeKeyInBackupSections(key, effectiveSections)
        }
        
        // Store type information for each preference
        filteredPrefs.forEach { (key, value) ->
            preferencesTypes[key] = when (value) {
                is Boolean -> "Boolean"
                is Float -> "Float"
                is Int -> "Int"
                is Long -> "Long"
                is String -> "String"
                is Set<*> -> "StringSet"
                else -> "Unknown"
            }
        }
        
        backupData["preferences"] = filteredPrefs
        backupData["preferences_types"] = preferencesTypes
        backupData["timestamp"] = System.currentTimeMillis()
        backupData["app_version"] = "1.0.0" // You might want to get this dynamically
        backupData["backup_version"] = 4 // Includes explicit stats, Rhythm Guard payload, and playback_history.json
        backupData["selected_sections"] = mapOf(
            "general_settings" to effectiveSections.includeGeneralSettings,
            "library_data" to effectiveSections.includeLibraryData,
            "stats_rhythm_guard" to effectiveSections.includeStatsAndRhythmGuard
        )
        
        // Explicitly include playlist and favorite songs data even if already in preferences
        // This ensures they are properly backed up and restored
        if (effectiveSections.includeLibraryData) {
            try {
            val playlistsJson = try {
                kotlinx.coroutines.runBlocking {
                    val db = com.qlzhj.rhythm.core.data.features.local.data.database.RhythmDatabase.getInstance(context)
                    val playlistDao = db.playlistDao()
                    val dbPlaylists = playlistDao.getAllPlaylists()
                    if (dbPlaylists.isNotEmpty()) {
                        val songDao = db.songDao()
                        val modelPlaylists = dbPlaylists.map { entity ->
                            val songIds = playlistDao.getSongIdsForPlaylist(entity.id)
                            val playlistSongs = songIds.map { songId ->
                                songDao.getSongById(songId)?.let { songEntity ->
                                    Song(
                                        id = songEntity.id,
                                        title = songEntity.title,
                                        artist = songEntity.artist,
                                        album = songEntity.album,
                                        albumId = songEntity.albumId,
                                        duration = songEntity.duration,
                                        uri = (songEntity.uri).toUri(),
                                        artworkUri = songEntity.artworkUri?.let { it.toUri() },
                                        trackNumber = songEntity.trackNumber,
                                        year = songEntity.year,
                                        genre = songEntity.genre,
                                        dateAdded = songEntity.dateAdded,
                                        dateModified = songEntity.dateModified.takeIf { it > 0L } ?: songEntity.dateAdded,
                                        albumArtist = songEntity.albumArtist,
                                        bitrate = songEntity.bitrate,
                                        sampleRate = songEntity.sampleRate,
                                        channels = songEntity.channels,
                                        codec = songEntity.codec,
                                        discNumber = songEntity.discNumber,
                                        path = songEntity.path
                                    )
                                } ?: Song(
                                    id = songId,
                                    title = AppStrings.unresolvedTrack(),
                                    artist = AppStrings.unknownArtist(),
                                    album = AppStrings.unknownAlbum(),
                                    albumId = "",
                                    duration = 0L,
                                    uri = Uri.EMPTY,
                                    artworkUri = null,
                                    trackNumber = 0,
                                    year = 0,
                                    genre = null,
                                    dateAdded = entity.dateCreated,
                                    dateModified = entity.dateModified,
                                    albumArtist = null,
                                    bitrate = null,
                                    sampleRate = null,
                                    channels = null,
                                    codec = null,
                                    discNumber = 1,
                                    path = null
                                )
                            }
                            Playlist(
                                id = entity.id,
                                name = entity.name,
                                songs = playlistSongs,
                                dateCreated = entity.dateCreated,
                                dateModified = entity.dateModified,
                                artworkUri = entity.artworkUri?.let { it.toUri() }
                            )
                        }
                        GsonUtils.gson.toJson(modelPlaylists)
                    } else {
                        prefs.getString(AppSettings.KEY_PLAYLISTS, "[]")
                    }
                }
            } catch (e: Exception) {
                Log.e("AppSettings", "Error reading database playlists for backup", e)
                prefs.getString(AppSettings.KEY_PLAYLISTS, "[]")
            }
            val favoriteSongsJson = prefs.getString(AppSettings.KEY_FAVORITE_SONGS, null)
            
            backupData["playlists_data"] = playlistsJson ?: "[]"
            Log.d("AppSettings", "Including playlists data in backup: ${backupData["playlists_data"].toString().length} characters")
            
            if (favoriteSongsJson != null) {
                backupData["favorite_songs_data"] = favoriteSongsJson
                Log.d("AppSettings", "Including favorite songs data in backup: ${favoriteSongsJson.length} characters")
            }
            
            // Also include blacklisted songs and folders
            val blacklistedSongsJson = prefs.getString(AppSettings.KEY_BLACKLISTED_SONGS, null)
            val blacklistedFoldersJson = prefs.getString(AppSettings.KEY_BLACKLISTED_FOLDERS, null)
            
            if (blacklistedSongsJson != null) {
                backupData["blacklisted_songs_data"] = blacklistedSongsJson
            }
            
            if (blacklistedFoldersJson != null) {
                backupData["blacklisted_folders_data"] = blacklistedFoldersJson
            }
            
            // Include whitelisted songs and folders
            val whitelistedSongsJson = prefs.getString(AppSettings.KEY_WHITELISTED_SONGS, null)
            val whitelistedFoldersJson = prefs.getString(AppSettings.KEY_WHITELISTED_FOLDERS, null)
            
            if (whitelistedSongsJson != null) {
                backupData["whitelisted_songs_data"] = whitelistedSongsJson
            }
            
            if (whitelistedFoldersJson != null) {
                backupData["whitelisted_folders_data"] = whitelistedFoldersJson
            }
            
            // Include pinned folders
            val pinnedFoldersJson = prefs.getString(AppSettings.KEY_PINNED_FOLDERS, null)
            if (pinnedFoldersJson != null) {
                backupData["pinned_folders_data"] = pinnedFoldersJson
            }
            
            } catch (e: Exception) {
                Log.e("AppSettings", "Error including playlist data in backup", e)
            }
        }

        if (effectiveSections.includeStatsAndRhythmGuard) {
            try {
                val statsPrefs = allPrefs.filterKeys { key ->
                    isStatsAndRhythmGuardBackupKey(key)
                }
                val statsPreferenceTypes = mutableMapOf<String, String>()
                statsPrefs.forEach { (key, value) ->
                    statsPreferenceTypes[key] = when (value) {
                        is Boolean -> "Boolean"
                        is Float -> "Float"
                        is Int -> "Int"
                        is Long -> "Long"
                        is String -> "String"
                        is Set<*> -> "StringSet"
                        else -> "Unknown"
                    }
                }

                backupData["stats_rhythm_guard_data"] = statsPrefs
                backupData["stats_rhythm_guard_types"] = statsPreferenceTypes
                Log.d("AppSettings", "Including stats & Rhythm Guard data in backup: ${statsPrefs.size} keys")

                val historyFile = File(context.filesDir, "playback_history.json")
                if (historyFile.exists()) {
                    val historyJson = historyFile.readText()
                    if (historyJson.isNotBlank()) {
                        backupData["playback_history"] = historyJson
                        Log.d("AppSettings", "Including playback_history.json in backup: ${historyJson.length} chars")
                    }
                }
            } catch (e: Exception) {
                Log.e("AppSettings", "Error including stats & Rhythm Guard data in backup", e)
            }
        }
        
        val backupJsonString = GsonUtils.gson.toJson(backupData)
        val validation = validateBackupJson(backupJsonString, effectiveSections)
        if (validation is BackupValidationResult.Invalid) {
            Log.e("AppSettings", "Created backup payload failed validation: ${validation.reason}")
            throw IllegalStateException("Backup payload validation failed: ${validation.reason}")
        }
        return backupJsonString
    }
    
    /**
     * Restores app data from a backup JSON string
     */
    fun restoreFromBackup(
        backupJson: String,
        sections: BackupRestoreSections = BackupRestoreSections()
    ): Boolean {
        val validation = validateBackupJson(backupJson, sections)
        if (validation is BackupValidationResult.Invalid) {
            Log.e("AppSettings", "Restore rejected due to invalid backup payload: ${validation.reason}")
            return false
        }

        return try {
            val type = object : com.google.gson.reflect.TypeToken<Map<String, Any?>>() {}.type
            val backupData = GsonUtils.gson.fromJson<Map<String, Any?>>(backupJson, type) ?: return false
            val preferences = (backupData["preferences"] as? Map<*, *>)
                ?.mapNotNull { (key, value) ->
                    (key as? String)?.let { safeKey -> safeKey to value }
                }
                ?.toMap()
                ?: return false
            val preferencesTypes = (backupData["preferences_types"] as? Map<*, *>)
                ?.mapNotNull { (key, value) ->
                    val safeKey = key as? String ?: return@mapNotNull null
                    val safeValue = value as? String ?: return@mapNotNull null
                    safeKey to safeValue
                }
                ?.toMap()
                ?: emptyMap()
            val backupVersion = (backupData["backup_version"] as? Double)?.toInt() ?: 1

            if (!sections.hasAtLeastOneSectionSelected) {
                Log.w("AppSettings", "Restore skipped: no backup sections selected")
                return false
            }
            
            Log.d("AppSettings", "Backup version: $backupVersion")
            
            // Clear existing preferences (optional - you might want to merge instead)
            prefs.edit {
            
            // Restore all preferences with proper type handling
            preferences.forEach { (key, value) ->
                if (!shouldIncludeKeyInBackupSections(key, sections) || isRhythmGuardTransientRuntimeKey(key)) {
                    return@forEach
                }

                val originalType = preferencesTypes[key]
                applyBackupPreferenceValue(this, key, value, originalType)
                // Migrate legacy "rhythm_aura_" keys to new "rhythm_guard_" equivalents
                if (key.startsWith("rhythm_aura_") && !preferences.containsKey(key.replaceFirst("rhythm_aura_", "rhythm_guard_"))) {
                    val guardKey = key.replaceFirst("rhythm_aura_", "rhythm_guard_")
                    if (shouldIncludeKeyInBackupSections(guardKey, sections)) {
                        applyBackupPreferenceValue(this, guardKey, value, originalType)
                        Log.d("AppSettings", "Migrated legacy key $key -> $guardKey during restore")
                    }
                }
            }

            if (sections.includeStatsAndRhythmGuard) {
                val statsData = (backupData["stats_rhythm_guard_data"] as? Map<*, *>)
                    ?.mapNotNull { (key, value) ->
                        (key as? String)?.let { safeKey -> safeKey to value }
                    }
                    ?.toMap()
                    ?: emptyMap()
                val statsTypes = (backupData["stats_rhythm_guard_types"] as? Map<*, *>)
                    ?.mapNotNull { (key, value) ->
                        val safeKey = key as? String ?: return@mapNotNull null
                        val safeValue = value as? String ?: return@mapNotNull null
                        safeKey to safeValue
                    }
                    ?.toMap()
                    ?: emptyMap()

                statsData.forEach { (key, value) ->
                    if (isStatsAndRhythmGuardBackupKey(key) && !isRhythmGuardTransientRuntimeKey(key)) {
                        applyBackupPreferenceValue(this, key, value, statsTypes[key] ?: preferencesTypes[key])
                        // Also migrate legacy aura keys to rhythm_guard_ counterparts
                        if (key.startsWith("rhythm_aura_") && !statsData.containsKey(key.replaceFirst("rhythm_aura_", "rhythm_guard_"))) {
                            val guardKey = key.replaceFirst("rhythm_aura_", "rhythm_guard_")
                            applyBackupPreferenceValue(this, guardKey, value, statsTypes[guardKey] ?: statsTypes[key] ?: preferencesTypes[key])
                            Log.d("AppSettings", "Migrated legacy stats key $key -> $guardKey during restore")
                        }
                    }
                }

                val playbackHistoryJson = backupData["playback_history"] as? String
                if (!playbackHistoryJson.isNullOrBlank()) {
                    try {
                        val historyFile = File(context.filesDir, "playback_history.json")
                        historyFile.writeText(playbackHistoryJson)
                        Log.d("AppSettings", "Restored playback_history.json: ${playbackHistoryJson.length} chars")
                    } catch (e: Exception) {
                        Log.e("AppSettings", "Failed to restore playback_history.json", e)
                    }
                }
            }
            
            // Handle backup version 2 and above - explicit playlist data restoration
            if (backupVersion >= 2 && sections.includeLibraryData) {
                Log.d("AppSettings", "Restoring playlist data from backup version $backupVersion")
                
                // Restore playlists data explicitly
                val playlistsData = backupData["playlists_data"] as? String
                if (playlistsData != null) {
                    try {
                        val playlistListType = object : TypeToken<List<Playlist>>() {}.type
                        val restoredPlaylists: List<Playlist> = GsonUtils.gson.fromJson(playlistsData, playlistListType) ?: emptyList()
                        
                        kotlinx.coroutines.runBlocking {
                            val db = com.qlzhj.rhythm.core.data.features.local.data.database.RhythmDatabase.getInstance(context)
                            val playlistDao = db.playlistDao()
                            val songDao = db.songDao()
                            
                            playlistDao.deleteAllPlaylists()
                            playlistDao.deleteAllPlaylistSongs()
                            
                            val songEntitiesToInsert = mutableListOf<com.qlzhj.rhythm.core.data.features.local.data.database.entity.SongEntity>()
                            
                            restoredPlaylists.forEach { playlist ->
                                val playlistEntity = com.qlzhj.rhythm.core.data.features.local.data.database.entity.PlaylistEntity(
                                    id = playlist.id,
                                    name = playlist.name,
                                    dateCreated = playlist.dateCreated,
                                    dateModified = playlist.dateModified,
                                    artworkUri = playlist.artworkUri?.toString()
                                )
                                playlistDao.insertPlaylist(playlistEntity)
                                
                                playlist.songs.forEach { song ->
                                    val songEntity = com.qlzhj.rhythm.core.data.features.local.data.database.entity.SongEntity(
                                        id = song.id,
                                        title = song.title,
                                        artist = song.artist,
                                        album = song.album,
                                        albumId = song.albumId,
                                        duration = song.duration,
                                        uri = song.uri.toString(),
                                        artworkUri = song.artworkUri?.toString(),
                                        trackNumber = song.trackNumber,
                                        year = song.year,
                                        genre = song.genre,
                                        dateAdded = song.dateAdded,
                                        dateModified = song.dateModified,
                                        albumArtist = song.albumArtist,
                                        bitrate = song.bitrate,
                                        sampleRate = song.sampleRate,
                                        channels = song.channels,
                                        codec = song.codec,
                                        discNumber = song.discNumber,
                                        path = song.path
                                    )
                                    songEntitiesToInsert.add(songEntity)
                                }
                                
                                val songEntities = playlist.songs.mapIndexed { index, song ->
                                    com.qlzhj.rhythm.core.data.features.local.data.database.entity.PlaylistSongEntity(
                                        playlistId = playlist.id,
                                        songId = song.id,
                                        orderIndex = index
                                    )
                                }
                                playlistDao.insertPlaylistSongs(songEntities)
                            }
                            
                            if (songEntitiesToInsert.isNotEmpty()) {
                                val distinctSongs = songEntitiesToInsert.distinctBy { it.id }
                                songDao.insertAll(distinctSongs)
                                Log.d("AppSettings", "Restored ${distinctSongs.size} unique song metadata records directly to Room database")
                            }
                        }
                        Log.d("AppSettings", "Restored playlists directly to Room database")
                    } catch (e: Exception) {
                        Log.e("AppSettings", "Failed to restore playlists to Room database", e)
                        putString(AppSettings.KEY_PLAYLISTS, playlistsData)
                    }
                }
                
                // Restore favorite songs data explicitly
                val favoriteSongsData = backupData["favorite_songs_data"] as? String
                if (favoriteSongsData != null) {
                    putString(AppSettings.KEY_FAVORITE_SONGS, favoriteSongsData)
                    Log.d("AppSettings", "Restored favorite songs data: ${favoriteSongsData.length} characters")
                }
                
                // Restore blacklisted songs and folders
                val blacklistedSongsData = backupData["blacklisted_songs_data"] as? String
                if (blacklistedSongsData != null) {
                    putString(AppSettings.KEY_BLACKLISTED_SONGS, blacklistedSongsData)
                    Log.d("AppSettings", "Restored blacklisted songs data")
                }
                
                val blacklistedFoldersData = backupData["blacklisted_folders_data"] as? String
                if (blacklistedFoldersData != null) {
                    putString(AppSettings.KEY_BLACKLISTED_FOLDERS, blacklistedFoldersData)
                    Log.d("AppSettings", "Restored blacklisted folders data")
                }
                
                // Restore whitelisted songs and folders
                val whitelistedSongsData = backupData["whitelisted_songs_data"] as? String
                if (whitelistedSongsData != null) {
                    putString(AppSettings.KEY_WHITELISTED_SONGS, whitelistedSongsData)
                    Log.d("AppSettings", "Restored whitelisted songs data")
                }
                
                val whitelistedFoldersData = backupData["whitelisted_folders_data"] as? String
                if (whitelistedFoldersData != null) {
                    putString(AppSettings.KEY_WHITELISTED_FOLDERS, whitelistedFoldersData)
                    Log.d("AppSettings", "Restored whitelisted folders data")
                }
                
                // Restore pinned folders
                val pinnedFoldersData = backupData["pinned_folders_data"] as? String
                if (pinnedFoldersData != null) {
                    putString(AppSettings.KEY_PINNED_FOLDERS, pinnedFoldersData)
                    Log.d("AppSettings", "Restored pinned folders data")
                }
            } else if (backupVersion < 2) {
                Log.d("AppSettings", "Backup version $backupVersion - using preferences-based restoration")
            }
            
            }
            
            // Refresh all StateFlows to reflect the restored data
            refreshAllStateFlows()
            guardStatsSection.clearRhythmGuardListeningTimeout()
            guardStatsSection.clearRhythmGuardTimeoutCooldown()
            
            Log.d("AppSettings", "Backup restoration completed successfully")
            true
        } catch (e: Exception) {
            Log.e("AppSettings", "Failed to restore backup", e)
            false
        }
    }

    fun applyBackupPreferenceValue(
        editor: SharedPreferences.Editor,
        key: String,
        value: Any?,
        originalType: String?
    ) {
        val existingValue = prefs.all[key]
        val resolvedType = originalType ?: when (existingValue) {
            is Boolean -> "Boolean"
            is Float -> "Float"
            is Int -> "Int"
            is Long -> "Long"
            is String -> "String"
            is Set<*> -> "StringSet"
            else -> {
                when {
                    key == "listening_time" || key.endsWith("_timestamp") || key.endsWith("_ms") || key.endsWith("_at") -> "Long"
                    key == "songs_played" || key == "unique_artists" || key == "rhythm_guard_age" || key.endsWith("_count") || key.endsWith("_limit") || key.endsWith("_index") -> "Int"
                    key == "gapless_playback" || key == "audioEqSection._crossfade" || key.endsWith("_enabled") || key.endsWith("_active") -> "Boolean"
                    key == "crossfade_duration" || key.endsWith("_strength") || key.endsWith("_threshold") || key.endsWith("_speed") || key.endsWith("_pitch") -> "Float"
                    else -> null
                }
            }
        }

        when (value) {
            is Boolean -> editor.putBoolean(key, value)
            is Float -> {
                when (resolvedType) {
                    "Long" -> editor.putLong(key, value.toLong())
                    "Int" -> editor.putInt(key, value.toInt())
                    else -> editor.putFloat(key, value)
                }
            }
            is Int -> {
                when (resolvedType) {
                    "Long" -> editor.putLong(key, value.toLong())
                    "Float" -> editor.putFloat(key, value.toFloat())
                    else -> editor.putInt(key, value)
                }
            }
            is Long -> {
                when (resolvedType) {
                    "Int" -> editor.putInt(key, value.toInt())
                    "Float" -> editor.putFloat(key, value.toFloat())
                    else -> editor.putLong(key, value)
                }
            }
            is String -> editor.putString(key, value)
            is List<*> -> {
                if (resolvedType == "StringSet") {
                    editor.putStringSet(key, value.mapNotNull { it?.toString() }.toSet())
                }
            }
            is Set<*> -> {
                if (resolvedType == "StringSet") {
                    editor.putStringSet(key, value.mapNotNull { it?.toString() }.toSet())
                }
            }
            is Double -> {
                when (resolvedType) {
                    "Float" -> editor.putFloat(key, value.toFloat())
                    "Long" -> editor.putLong(key, value.toLong())
                    "Int" -> editor.putInt(key, value.toInt())
                    "Boolean" -> editor.putBoolean(key, value != 0.0)
                    else -> editor.putFloat(key, value.toFloat())
                }
            }
        }
    }
    
    /**
     * Safely get a Long value from SharedPreferences, handling JSON restore type casting issues
     */
    private fun safeLong(key: String, defaultValue: Long): Long {
        return try {
            prefs.getLong(key, defaultValue)
        } catch (e: ClassCastException) {
            // Handle case where value was stored as Float/Double from JSON restore
            try {
                prefs.getFloat(key, defaultValue.toFloat()).toLong()
            } catch (e2: Exception) {
                // If all else fails, use default and fix the stored value
                prefs.edit { putLong(key, defaultValue) }
                defaultValue
            }
        }
    }
    
    /**
     * Helper function to safely set values with validation
     */
    private fun safeSetValue(key: String, value: Any, validator: ((Any) -> Boolean)? = null): Boolean {
        return try {
            if (validator != null && !validator(value)) {
                Log.w("AppSettings", "Invalid value for key $key: $value")
                return false
            }
            
            prefs.edit {
                when (value) {
                    is Boolean -> putBoolean(key, value)
                    is String -> putString(key, value)
                    is Int -> putInt(key, value)
                    is Long -> putLong(key, value)
                    is Float -> putFloat(key, value)
                    else -> {
                        Log.w("AppSettings", "Unsupported value type for key $key")
                        return false
                    }
                }
            }
            true
        } catch (e: Exception) {
            Log.e("AppSettings", "Error setting preference $key", e)
            false
        }
    }
    
    /**
     * Validates cache size value
     */
    private fun isValidCacheSize(size: Long) = platformSection.isValidCacheSize(size)
    
    
    /**
     * Validates update check interval
     */
    private fun isValidUpdateInterval(hours: Int): Boolean {
        return hours in 1..168 // 1 hour to 1 week
    }
    
    /**
     * Refreshes all StateFlows to reflect current SharedPreferences values
     */
    private fun refreshAllStateFlows() {
        // Playback Settings
        platformSection._gaplessPlayback.value = prefs.getBoolean(AppSettings.KEY_GAPLESS_PLAYBACK, true)
        audioEqSection._crossfade.value = prefs.getBoolean(AppSettings.KEY_CROSSFADE, true)
        audioEqSection._crossfadeDuration.value = prefs.getFloat(AppSettings.KEY_CROSSFADE_DURATION, 4f)
        audioEqSection._crossfadeRepeatOne.value = prefs.getBoolean(AppSettings.KEY_CROSSFADE_REPEAT_ONE, true)
        audioEqSection._audioNormalization.value = prefs.getBoolean(AppSettings.KEY_AUDIO_NORMALIZATION, true)
        audioEqSection._replayGain.value = prefs.getBoolean(AppSettings.KEY_REPLAY_GAIN, false)
        audioEqSection._replayGainMode.value = prefs.getInt(AppSettings.KEY_REPLAY_GAIN_MODE, 1)
        audioEqSection._replayGainDrc.value = prefs.getBoolean(AppSettings.KEY_REPLAY_GAIN_DRC, true)
        audioEqSection._replayGainPreamp.value = prefs.getFloat(AppSettings.KEY_REPLAY_GAIN_PREAMP, 0f)
        audioEqSection._replayGainPreampUntagged.value = prefs.getFloat(AppSettings.KEY_REPLAY_GAIN_PREAMP_UNTAGGED, 0f)
        audioEqSection._audioOffloadEnabled.value = prefs.getBoolean(AppSettings.KEY_AUDIO_OFFLOAD_ENABLED, false)
        platformSection._skipSilenceEnabled.value = prefs.getBoolean(AppSettings.KEY_SKIP_SILENCE, false)
        platformSection._batterySaverEnabled.value = prefs.getBoolean(AppSettings.KEY_BATTERY_SAVER_ENABLED, false)
        platformSection._batterySaverMode.value = prefs.getString(AppSettings.KEY_BATTERY_SAVER_MODE, "auto") ?: "auto"
        platformSection._batterySaverDisableHaptics.value = prefs.getBoolean(AppSettings.KEY_BATTERY_SAVER_DISABLE_HAPTICS, true)
        platformSection._batterySaverEnableOffload.value = prefs.getBoolean(AppSettings.KEY_BATTERY_SAVER_ENABLE_OFFLOAD, true)
        platformSection._batterySaverDisableMarquee.value = prefs.getBoolean(AppSettings.KEY_BATTERY_SAVER_DISABLE_MARQUEE, true)
        platformSection._batterySaverDisableLosslessArtwork.value = prefs.getBoolean(AppSettings.KEY_BATTERY_SAVER_DISABLE_LOSSLESS_ARTWORK, true)
        platformSection._batterySaverDisableAutoFetchArtwork.value = prefs.getBoolean(AppSettings.KEY_BATTERY_SAVER_DISABLE_AUTO_FETCH_ARTWORK, true)
        platformSection._globalMarqueeEnabled.value = prefs.getBoolean(AppSettings.KEY_GLOBAL_MARQUEE_ENABLED, true)
        updateDerivedSettings()
        
        // Theme Settings
        themeSection._useSystemTheme.value = prefs.getBoolean(AppSettings.KEY_USE_SYSTEM_THEME, true)
        themeSection._darkMode.value = prefs.getBoolean(AppSettings.KEY_DARK_MODE, true)
        themeSection._amoledTheme.value = prefs.getBoolean(AppSettings.KEY_AMOLED_THEME, false)
        themeSection._floatingNavigationBar.value = prefs.getBoolean(AppSettings.KEY_FLOATING_NAVIGATION_BAR, true)
        themeSection._useDynamicColors.value = prefs.getBoolean(AppSettings.KEY_USE_DYNAMIC_COLORS, false)
        themeSection._customColorScheme.value = prefs.getString(AppSettings.KEY_CUSTOM_COLOR_SCHEME, "Default") ?: "Default"
        themeSection._customFont.value = prefs.getString(AppSettings.KEY_CUSTOM_FONT, "Geom") ?: "Geom"
        
        // Library Settings
        librarySection._albumViewType.value = AlbumViewType.valueOf(prefs.getString(AppSettings.KEY_ALBUM_VIEW_TYPE, AlbumViewType.GRID.name) ?: AlbumViewType.GRID.name)
        _artistViewType.value = ArtistViewType.valueOf(prefs.getString(AppSettings.KEY_ARTIST_VIEW_TYPE, ArtistViewType.GRID.name) ?: ArtistViewType.GRID.name)
        librarySection._albumSortOrder.value = prefs.getString(AppSettings.KEY_ALBUM_SORT_ORDER, "TRACK_NUMBER") ?: "TRACK_NUMBER"
        librarySection._songsSortOrder.value = prefs.getString(AppSettings.KEY_SONGS_SORT_ORDER, "TITLE_ASC") ?: "TITLE_ASC"
        librarySection._libraryCombineDiscs.value = prefs.getBoolean(AppSettings.KEY_LIBRARY_COMBINE_DISCS, false)
        librarySection._preferSongArtwork.value = if (prefs.contains(AppSettings.KEY_PREFER_SONG_ARTWORK)) {
            prefs.getBoolean(AppSettings.KEY_PREFER_SONG_ARTWORK, true)
        } else {
            prefs.getBoolean(AppSettings.KEY_IGNORE_MEDIASTORE_COVERS, true)
        }
        customizationSection._albumBottomSheetDiscFilter.value = prefs.getInt(AppSettings.KEY_ALBUM_BOTTOM_SHEET_DISC_FILTER, 0).coerceAtLeast(0)
        customizationSection._albumBottomSheetGradientBlur.value = prefs.getBoolean(AppSettings.KEY_ALBUM_BOTTOM_SHEET_GRADIENT_BLUR, true)
        librarySection._showLibraryBottomBarAlways.value = prefs.getBoolean(AppSettings.KEY_SHOW_LIBRARY_BOTTOM_BAR_ALWAYS, false)
        
        // Audio Device Settings
        audioEqSection._autoConnectDevice.value = prefs.getBoolean(AppSettings.KEY_AUTO_CONNECT_DEVICE, true)
        audioEqSection._useSystemVolume.value = prefs.getBoolean(AppSettings.KEY_USE_SYSTEM_VOLUME, true)
        audioEqSection._appVolume.value = prefs.getFloat(AppSettings.KEY_APP_VOLUME, 1.0f)
        audioEqSection._stopPlaybackOnZeroVolume.value = prefs.getBoolean(AppSettings.KEY_STOP_PLAYBACK_ON_ZERO_VOLUME, false)
        
        // Cache Settings
        platformSection._maxCacheSize.value = safeLong(AppSettings.KEY_MAX_CACHE_SIZE, 300L * 1024L * 1024L)
        
        // Other settings
        lyricsSection._showLyrics.value = prefs.getBoolean(AppSettings.KEY_SHOW_LYRICS, true)
        lyricsSection._tapLyricsToFullScreen.value = prefs.getBoolean(AppSettings.KEY_TAP_LYRICS_TO_FULL_SCREEN, true)
        lyricsSection._lyricsApiPriority.value = LyricsApiPriority.fromOrdinal(
            prefs.getInt(AppSettings.KEY_LYRICS_API_PRIORITY, LyricsApiPriority.LYRICALLY_FIRST.ordinal)
        )
        lyricsSection._lyricsApiFallbackRetry.value = prefs.getBoolean(AppSettings.KEY_LYRICS_API_FALLBACK_RETRY, true)
        lyricsSection._onlineOnlyLyrics.value = prefs.getBoolean(AppSettings.KEY_ONLINE_ONLY_LYRICS, true)
        lyricsSection._lyricBold.value = prefs.getBoolean(AppSettings.KEY_LYRIC_BOLD, false)
        platformSection._trimLyrics.value = prefs.getBoolean(AppSettings.KEY_TRIM_LYRICS, true)
        lyricsSection._lyricNoAnimation.value = prefs.getBoolean(AppSettings.KEY_LYRIC_NO_ANIMATION, false)
        lyricsSection._translationAutoWord.value = prefs.getBoolean(AppSettings.KEY_TRANSLATION_AUTO_WORD, false)
        lyricsSection._showLyricsBackgroundArtwork.value = prefs.getBoolean(AppSettings.KEY_SHOW_LYRICS_BACKGROUND_ARTWORK, true)
        platformSection._searchHistory.value = prefs.getString(AppSettings.KEY_SEARCH_HISTORY, null)
        platformSection._showKeyboardOnSearchOpen.value = prefs.getBoolean(AppSettings.KEY_SHOW_KEYBOARD_ON_SEARCH_OPEN, true)
        _playlists.value = prefs.getString(AppSettings.KEY_PLAYLISTS, null)
        _favoriteSongs.value = prefs.getString(AppSettings.KEY_FAVORITE_SONGS, null)
        
        // User Statistics
        guardStatsSection._listeningTime.value = safeLong(AppSettings.KEY_LISTENING_TIME, 0L)
        guardStatsSection._songsPlayed.value = prefs.getInt(AppSettings.KEY_SONGS_PLAYED, 0)
        librarySection._uniqueArtists.value = prefs.getInt(AppSettings.KEY_UNIQUE_ARTISTS, 0)
        guardStatsSection._rhythmAuraMode.value = guardStatsSection.sanitizeRhythmGuardMode(
            prefs.getString(AppSettings.KEY_RHYTHM_GUARD_MODE, prefs.getString(AppSettings.KEY_RHYTHM_AURA_MODE, AppSettings.RHYTHM_GUARD_MODE_OFF))
        )
        guardStatsSection._rhythmAuraAge.value = prefs.getInt(AppSettings.KEY_RHYTHM_GUARD_AGE, prefs.getInt(AppSettings.KEY_RHYTHM_AURA_AGE, 18)).coerceIn(8, 80)
        guardStatsSection._rhythmAuraManualWarningsEnabled.value = prefs.getBoolean(
            AppSettings.KEY_RHYTHM_GUARD_MANUAL_WARNINGS_ENABLED,
            prefs.getBoolean(AppSettings.KEY_RHYTHM_AURA_MANUAL_WARNINGS_ENABLED, true)
        )
        guardStatsSection._rhythmAuraManualVolumeThreshold.value = prefs.getFloat(
            AppSettings.KEY_RHYTHM_GUARD_MANUAL_VOLUME_THRESHOLD,
            prefs.getFloat(AppSettings.KEY_RHYTHM_AURA_MANUAL_VOLUME_THRESHOLD, 0.68f)
        ).coerceIn(0.40f, 0.95f)
        guardStatsSection._rhythmGuardApplyVolumeLimitOnSpeaker.value = prefs.getBoolean(
            AppSettings.KEY_RHYTHM_GUARD_APPLY_VOLUME_LIMIT_ON_SPEAKER,
            false
        )
        guardStatsSection._rhythmAuraLastAutoAppliedAt.value = safeLong(
            AppSettings.KEY_RHYTHM_GUARD_LAST_AUTO_APPLIED_AT,
            safeLong(AppSettings.KEY_RHYTHM_AURA_LAST_AUTO_APPLIED_AT, 0L)
        )
        guardStatsSection._rhythmGuardAlertThresholdMinutes.value = prefs.getInt(AppSettings.KEY_RHYTHM_GUARD_ALERT_THRESHOLD_MINUTES, -1).coerceIn(-1, 24 * 60)
        guardStatsSection._rhythmGuardWarningTimeoutMinutes.value = prefs.getInt(AppSettings.KEY_RHYTHM_GUARD_WARNING_TIMEOUT_MINUTES, 2).coerceIn(1, 60)
        guardStatsSection._rhythmGuardPostTimeoutCooldownMinutes.value = prefs.getInt(AppSettings.KEY_RHYTHM_GUARD_POST_TIMEOUT_COOLDOWN_MINUTES, 10).coerceIn(1, 60)
        guardStatsSection._rhythmGuardBreakResumeMinutes.value = prefs.getInt(AppSettings.KEY_RHYTHM_GUARD_BREAK_RESUME_MINUTES, 15).coerceIn(1, 180)
        guardStatsSection._rhythmGuardTimeoutUntilMs.value = safeLong(AppSettings.KEY_RHYTHM_GUARD_TIMEOUT_UNTIL_MS, 0L).coerceAtLeast(0L)
        guardStatsSection._rhythmGuardTimeoutReason.value = prefs.getString(AppSettings.KEY_RHYTHM_GUARD_TIMEOUT_REASON, null).orEmpty()
        guardStatsSection._rhythmGuardTimeoutStartedAtMs.value = safeLong(AppSettings.KEY_RHYTHM_GUARD_TIMEOUT_STARTED_AT_MS, 0L).coerceAtLeast(0L)
        guardStatsSection._rhythmGuardTimeoutCooldownUntilMs.value = safeLong(AppSettings.KEY_RHYTHM_GUARD_TIMEOUT_COOLDOWN_UNTIL_MS, 0L).coerceAtLeast(0L)
        guardStatsSection._rhythmGuardNextAllowedLimitMinutes.value = prefs.getInt(AppSettings.KEY_RHYTHM_GUARD_NEXT_ALLOWED_LIMIT_MINUTES, 0)
        guardStatsSection._rhythmGuardFirstBreakSeen.value = prefs.getBoolean(AppSettings.KEY_RHYTHM_GUARD_FIRST_BREAK_SEEN, false)
        customizationSection._homeShowRhythmGuard.value = prefs.getBoolean(AppSettings.KEY_HOME_SHOW_RHYTHM_GUARD, true)
        
        // Enhanced User Preferences
        _favoriteGenres.value = try {
            val json = prefs.getString(AppSettings.KEY_FAVORITE_GENRES, null)
            if (json != null) Gson().fromJson(json, object : TypeToken<Map<String, Int>>() {}.type) else emptyMap()
        } catch (e: Exception) { emptyMap() }
        
        guardStatsSection._dailyListeningStats.value = try {
            val json = prefs.getString(AppSettings.KEY_DAILY_LISTENING_STATS, null)
            if (json != null) Gson().fromJson(json, object : TypeToken<Map<String, Long>>() {}.type) else emptyMap()
        } catch (e: Exception) { emptyMap() }
        
        guardStatsSection._weeklyTopArtists.value = try {
            val json = prefs.getString(AppSettings.KEY_WEEKLY_TOP_ARTISTS, null)
            if (json != null) Gson().fromJson(json, object : TypeToken<Map<String, Int>>() {}.type) else emptyMap()
        } catch (e: Exception) { emptyMap() }
        
        guardStatsSection._moodPreferences.value = try {
            val json = prefs.getString(AppSettings.KEY_MOOD_PREFERENCES, null)
            if (json != null) Gson().fromJson(json, object : TypeToken<Map<String, List<String>>>() {}.type) else emptyMap()
        } catch (e: Exception) { emptyMap() }
        
        _songPlayCounts.value = try {
            val json = prefs.getString(AppSettings.KEY_SONG_PLAY_COUNTS, null)
            if (json != null) Gson().fromJson(json, object : TypeToken<Map<String, Int>>() {}.type) else emptyMap()
        } catch (e: Exception) { emptyMap() }
        
        // Recently Played
        playbackQueueSection._recentlyPlayed.value = try {
            val json = prefs.getString(AppSettings.KEY_RECENTLY_PLAYED, null)
            if (json != null) Gson().fromJson(json, object : TypeToken<List<String>>() {}.type) else emptyList()
        } catch (e: Exception) { emptyList() }
        _recentlyPlayedSongCache.value = loadRecentlyPlayedSongCache()
        
        playbackQueueSection._lastPlayedTimestamp.value = safeLong(AppSettings.KEY_LAST_PLAYED_TIMESTAMP, 0L)
        
        // API Enable/Disable States
        _integrationsEnabled.value = prefs.getBoolean(AppSettings.KEY_INTEGRATIONS_ENABLED, false)
        _deezerApiEnabled.value = prefs.getBoolean(AppSettings.KEY_DEEZER_API_ENABLED, false)
        _lrclibApiEnabled.value = prefs.getBoolean(AppSettings.KEY_LRCLIB_API_ENABLED, false)
        _betterLyricsApiEnabled.value = prefs.getBoolean(AppSettings.KEY_BETTERLYRICS_API_ENABLED, false)
        _ytMusicApiEnabled.value = prefs.getBoolean(AppSettings.KEY_YTMUSIC_API_ENABLED, false)
        _spotifyApiEnabled.value = prefs.getBoolean(AppSettings.KEY_SPOTIFY_API_ENABLED, false)
        _lyricallyApiEnabled.value = prefs.getBoolean(AppSettings.KEY_LYRICALLY_API_ENABLED, false)
        _wikipediaApiEnabled.value = prefs.getBoolean(AppSettings.KEY_WIKIPEDIA_API_ENABLED, false)
        _appleCanvasEnabled.value = prefs.getBoolean(AppSettings.KEY_APPLE_CANVAS_ENABLED, false)
        _appleCanvasNetworkMode.value = CanvasNetworkMode.fromOrdinal(
            prefs.getInt(AppSettings.KEY_APPLE_CANVAS_NETWORK_MODE, CanvasNetworkMode.BOTH.ordinal)
        )
        librarySection._autoFetchArtwork.value = prefs.getBoolean(AppSettings.KEY_AUTO_FETCH_ARTWORK, true)
        _spotifyClientId.value = prefs.getString(AppSettings.KEY_SPOTIFY_CLIENT_ID, "") ?: ""
        _spotifyClientSecret.value = prefs.getString(AppSettings.KEY_SPOTIFY_CLIENT_SECRET, "") ?: ""

        // General Broadcast Status Settings
        _broadcastStatusEnabled.value = prefs.getBoolean(AppSettings.KEY_BROADCAST_STATUS_ENABLED, false)
        lyricsSection._bluetoothLyricsEnabled.value = prefs.getBoolean(AppSettings.KEY_BLUETOOTH_LYRICS_ENABLED, false)
        
        // App Updates
        _autoCheckForUpdates.value = prefs.getBoolean(AppSettings.KEY_AUTO_CHECK_FOR_UPDATES, true)
        platformSection._updateChannel.value = prefs.getString(AppSettings.KEY_UPDATE_CHANNEL, "stable") ?: "stable"
        platformSection._updateSource.value = prefs.getString(AppSettings.KEY_UPDATE_SOURCE, "installed") ?: "installed"
        platformSection._updatesEnabled.value = prefs.getBoolean(AppSettings.KEY_UPDATES_ENABLED, true)
        platformSection._updateNotificationsEnabled.value = prefs.getBoolean(AppSettings.KEY_UPDATE_NOTIFICATIONS_ENABLED, true)
        platformSection._updateStatusNotificationsEnabled.value = prefs.getBoolean(AppSettings.KEY_UPDATE_STATUS_NOTIFICATIONS_ENABLED, false)
        librarySection._mediaScanMode.value = MediaScanMode.fromValue(prefs.getString(AppSettings.KEY_MEDIA_SCAN_MODE, "blacklist") ?: "blacklist")
        librarySection._includeHiddenWhitelistedMedia.value = prefs.getBoolean(AppSettings.KEY_INCLUDE_HIDDEN_WHITELISTED_MEDIA, true)
        platformSection._updateCheckIntervalHours.value = prefs.getInt(AppSettings.KEY_UPDATE_CHECK_INTERVAL_HOURS, 6)
        
        // Re-schedule update notification worker if settings changed
        if (jobScheduler.shouldRunUpdateNotificationWorker()) {
            jobScheduler.scheduleUpdateNotificationWorker()
        } else {
            jobScheduler.cancelUpdateNotificationWorker()
        }
        
        // Beta Program
        platformSection._hasShownBetaPopup.value = prefs.getBoolean(AppSettings.KEY_HAS_SHOWN_BETA_POPUP, false)
        
        // Crash Reporting
        platformSection._lastCrashLog.value = prefs.getString(AppSettings.KEY_LAST_CRASH_LOG, null)
        platformSection._crashLogHistory.value = try {
            val json = prefs.getString(AppSettings.KEY_CRASH_LOG_HISTORY, null)
            if (json != null) Gson().fromJson(json, object : TypeToken<List<CrashLogEntry>>() {}.type) else emptyList()
        } catch (e: Exception) { emptyList() }
        
        // Other settings
        _hapticFeedbackEnabled.value = prefs.getBoolean(AppSettings.KEY_HAPTIC_FEEDBACK_ENABLED, true)
        platformSection._useCustomNotification.value = prefs.getBoolean(AppSettings.KEY_USE_CUSTOM_NOTIFICATION, false)
        guardStatsSection._rhythmGuardAlertNotificationsEnabled.value = prefs.getBoolean(AppSettings.KEY_RHYTHM_GUARD_ALERT_NOTIFICATIONS_ENABLED, true)
        guardStatsSection._rhythmGuardTimerNotificationsEnabled.value = prefs.getBoolean(AppSettings.KEY_RHYTHM_GUARD_TIMER_NOTIFICATIONS_ENABLED, true)
        platformSection._rhythmPulseNotificationsEnabled.value = prefs.getBoolean(AppSettings.KEY_RHYTHM_PULSE_NOTIFICATIONS_ENABLED, false)
        platformSection._rhythmPulseNotificationIntervalHours.value = prefs.getInt(AppSettings.KEY_RHYTHM_PULSE_NOTIFICATION_INTERVAL_HOURS, 24).coerceIn(6, 72)
        platformSection._forcePlayerCompactMode.value = prefs.getBoolean(AppSettings.KEY_FORCE_PLAYER_COMPACT_MODE, false)
        platformSection._useExperimentalPlayerUi.value = prefs.getBoolean(AppSettings.KEY_USE_EXPERIMENTAL_PLAYER_UI, false)
        librarySection._enableAlbumEditing.value = prefs.getBoolean(AppSettings.KEY_ENABLE_ALBUM_EDITING, false)
        platformSection._onboardingCompleted.value = prefs.getBoolean(AppSettings.KEY_ONBOARDING_COMPLETED, false)
        librarySection._initialMediaScanCompleted.value = prefs.getBoolean(AppSettings.KEY_INITIAL_MEDIA_SCAN_COMPLETED, false)
        librarySection._genreDetectionCompleted.value = prefs.getBoolean(AppSettings.KEY_GENRE_DETECTION_COMPLETED, false)

        if (platformSection._rhythmPulseNotificationsEnabled.value) {
            jobScheduler.scheduleRhythmPulseNotificationWorker()
        } else {
            jobScheduler.cancelRhythmPulseNotificationWorker()
        }
        
        // Blacklisted items
        librarySection._blacklistedSongs.value = try {
            val json = prefs.getString(AppSettings.KEY_BLACKLISTED_SONGS, null)
            if (json != null) Gson().fromJson(json, object : TypeToken<List<String>>() {}.type) else emptyList()
        } catch (e: Exception) { emptyList() }
        
        librarySection._blacklistedFolders.value = try {
            val json = prefs.getString(AppSettings.KEY_BLACKLISTED_FOLDERS, null)
            if (json != null) Gson().fromJson(json, object : TypeToken<List<String>>() {}.type) else emptyList()
        } catch (e: Exception) { emptyList() }
        
        // Backup settings
        platformSection._lastBackupTimestamp.value = safeLong(AppSettings.KEY_LAST_BACKUP_TIMESTAMP, 0L)
        platformSection._autoBackupEnabled.value = prefs.getBoolean(AppSettings.KEY_AUTO_BACKUP_ENABLED, false)
        platformSection._backupLocation.value = prefs.getString(AppSettings.KEY_BACKUP_LOCATION, null)
        
        // Ensure auto-backup is scheduled if enabled
        if (platformSection._autoBackupEnabled.value) {
            jobScheduler.scheduleAutoBackup()
        }
        
        // Sleep Timer settings
        playbackQueueSection._sleepTimerActive.value = prefs.getBoolean(AppSettings.KEY_SLEEP_TIMER_ACTIVE, false)
        playbackQueueSection._sleepTimerRemainingSeconds.value = prefs.getLong(AppSettings.KEY_SLEEP_TIMER_REMAINING_SECONDS, 0L)
        playbackQueueSection._sleepTimerAction.value = prefs.getString(AppSettings.KEY_SLEEP_TIMER_ACTION, "FADE_OUT") ?: "FADE_OUT"
        
        // Media Scan Tracking
        librarySection._lastScanTimestamp.value = safeLong(AppSettings.KEY_LAST_SCAN_TIMESTAMP, 0L)
        librarySection._lastScanDuration.value = safeLong(AppSettings.KEY_LAST_SCAN_DURATION, 0L)
        
        // Media Scan Filtering
        librarySection._allowedFormats.value = prefs.getStringSet(AppSettings.KEY_ALLOWED_FORMATS, AppSettings.defaultAllowedFormats())?.toSet() ?: AppSettings.defaultAllowedFormats()
        platformSection._minimumBitrate.value = prefs.getInt(AppSettings.KEY_MINIMUM_BITRATE, 0)
        librarySection._minimumDuration.value = safeLong(AppSettings.KEY_MINIMUM_DURATION, 0L)

        // Pinned Folders
        librarySection._pinnedFolders.value = try {
            val json = prefs.getString(AppSettings.KEY_PINNED_FOLDERS, null)
            if (json != null) Gson().fromJson(json, object : TypeToken<List<String>>() {}.type) else emptyList()
        } catch (e: Exception) { emptyList() }
        
        // Widget Settings
        widgetSection._widgetShowAlbumArt.value = prefs.getBoolean(AppSettings.KEY_WIDGET_SHOW_ALBUM_ART, true)
        widgetSection._widgetShowArtist.value = prefs.getBoolean(AppSettings.KEY_WIDGET_SHOW_ARTIST, true)
        widgetSection._widgetShowAlbum.value = prefs.getBoolean(AppSettings.KEY_WIDGET_SHOW_ALBUM, false)
        widgetSection._widgetCornerRadius.value = prefs.getInt(AppSettings.KEY_WIDGET_CORNER_RADIUS, 28)
        widgetSection._widgetAutoUpdate.value = prefs.getBoolean(AppSettings.KEY_WIDGET_AUTO_UPDATE, true)
        widgetSection._widgetShowFavoriteButton.value = prefs.getBoolean(AppSettings.KEY_WIDGET_SHOW_FAVORITE_BUTTON, true)
        widgetSection._widgetTheme.value = prefs.getInt(AppSettings.KEY_WIDGET_THEME, 0)
        widgetSection._widgetCookieBottomLeft.value = prefs.getInt(AppSettings.KEY_WIDGET_COOKIE_BOTTOM_LEFT, 3)
        widgetSection._widgetCookieBottomRight.value = prefs.getInt(AppSettings.KEY_WIDGET_COOKIE_BOTTOM_RIGHT, 4)
        widgetSection._widgetStatsRange.value = prefs.getInt(AppSettings.KEY_WIDGET_STATS_RANGE, 0)
        widgetSection._widgetStatsGem.value = prefs.getInt(AppSettings.KEY_WIDGET_STATS_GEM, 0)
        
        // Player Screen Customization Settings
        customizationSection._playerShowGradientOverlay.value = prefs.getBoolean(AppSettings.KEY_PLAYER_SHOW_GRADIENT_OVERLAY, true)
        customizationSection._playerLyricsTransition.value = prefs.getInt(AppSettings.KEY_PLAYER_LYRICS_TRANSITION, 2) // 2 = Scale
        customizationSection._playerAmbientBackdropEnabled.value = prefs.getBoolean(AppSettings.KEY_PLAYER_AMBIENT_BACKDROP_ENABLED, false)
        customizationSection._playerAmbientBackdropIntensity.value = prefs.getFloat(AppSettings.KEY_PLAYER_AMBIENT_BACKDROP_INTENSITY, 0.85f)
        customizationSection._playerAmbientInfiniteZoom.value = prefs.getBoolean(AppSettings.KEY_PLAYER_AMBIENT_INFINITE_ZOOM, true)
        customizationSection._playerAccentBackgroundEnabled.value = prefs.getBoolean(AppSettings.KEY_PLAYER_ACCENT_BACKGROUND_ENABLED, true)
        // Normalize legacy state: ambient and accent cannot both be on — ambient wins.
        if (customizationSection._playerAmbientBackdropEnabled.value && customizationSection._playerAccentBackgroundEnabled.value) {
            customizationSection._playerAccentBackgroundEnabled.value = false
            prefs.edit { putBoolean(AppSettings.KEY_PLAYER_ACCENT_BACKGROUND_ENABLED, false) }
        }
        customizationSection._playerMergeControlsToBottom.value = prefs.getBoolean(AppSettings.KEY_PLAYER_MERGE_CONTROLS_TO_BOTTOM, true)
        customizationSection._playerGlassIntensity.value = prefs.getFloat(AppSettings.KEY_PLAYER_GLASS_INTENSITY, 1.0f)
        customizationSection._playerLyricsTextSize.value = prefs.getFloat(AppSettings.KEY_PLAYER_LYRICS_TEXT_SIZE, 1.0f)
        customizationSection._playerLyricsAlignment.value = prefs.getString(AppSettings.KEY_PLAYER_LYRICS_ALIGNMENT, "CENTER") ?: "CENTER"
        customizationSection._playerShowArtBelowLyrics.value = prefs.getBoolean(AppSettings.KEY_PLAYER_SHOW_ART_BELOW_LYRICS, true)
        customizationSection._playerShowSeekButtons.value = prefs.getBoolean(AppSettings.KEY_PLAYER_SHOW_SEEK_BUTTONS, true)
        customizationSection._playerTextAlignment.value = prefs.getString(AppSettings.KEY_PLAYER_TEXT_ALIGNMENT, "CENTER") ?: "CENTER"
        customizationSection._playerShowSongInfoOnArtwork.value = prefs.getBoolean(AppSettings.KEY_PLAYER_SHOW_SONG_INFO_ON_ARTWORK, true)
        customizationSection._playerArtworkCornerRadius.value = prefs.getInt(AppSettings.KEY_PLAYER_ARTWORK_CORNER_RADIUS, 28)
        customizationSection._playerShowAudioQualityBadges.value = prefs.getBoolean(AppSettings.KEY_PLAYER_SHOW_AUDIO_QUALITY_BADGES, true)
        _expressiveQualityBadgeAlwaysVisible.value = prefs.getBoolean(AppSettings.KEY_EXPRESSIVE_QUALITY_BADGE_ALWAYS_VISIBLE, false)
        
        // Lyrics song preferences and renaming behavior
        lyricsSection._lrcRenameBehavior.value = prefs.getString(AppSettings.KEY_LRC_RENAME_BEHAVIOR, "ask") ?: "ask"
        lyricsSection._songLyricsPreferences.value = try {
            val json = prefs.getString(AppSettings.KEY_SONG_LYRICS_PREFERENCES, null)
            if (json != null) Gson().fromJson(json, object : TypeToken<Map<String, String>>() {}.type) else emptyMap()
        } catch (e: Exception) { emptyMap() }
        lyricsSection._songCustomLrcFiles.value = try {
            val json = prefs.getString(AppSettings.KEY_SONG_CUSTOM_LRC_FILES, null)
            if (json != null) Gson().fromJson(json, object : TypeToken<Map<String, String>>() {}.type) else emptyMap()
        } catch (e: Exception) { emptyMap() }
    }

    fun getSongLyricsPreference(songId: String): String? {
        return lyricsSection._songLyricsPreferences.value[songId]
    }

    fun setSongLyricsPreference(songId: String, preference: String?) {
        val currentPrefs = lyricsSection._songLyricsPreferences.value.toMutableMap()
        if (preference == null) {
            currentPrefs.remove(songId)
        } else {
            currentPrefs[songId] = preference
        }
        lyricsSection._songLyricsPreferences.value = currentPrefs
        prefs.edit { putString(AppSettings.KEY_SONG_LYRICS_PREFERENCES, Gson().toJson(currentPrefs)) }
    }

    fun getSongCustomLrcFile(songId: String): String? {
        return lyricsSection._songCustomLrcFiles.value[songId]
    }

    fun setSongCustomLrcFile(songId: String, path: String?) {
        val currentFiles = lyricsSection._songCustomLrcFiles.value.toMutableMap()
        if (path == null) {
            currentFiles.remove(songId)
        } else {
            currentFiles[songId] = path
        }
        lyricsSection._songCustomLrcFiles.value = currentFiles
        prefs.edit { putString(AppSettings.KEY_SONG_CUSTOM_LRC_FILES, Gson().toJson(currentFiles)) }
    }

    fun exportLyricsPreferencesToCsv(context: Context, uri: Uri): Boolean =
        com.qlzhj.rhythm.core.data.core.domain.settings.LyricsPreferenceIo.export(
            lyricsSection._songLyricsPreferences.value, lyricsSection._songCustomLrcFiles.value, context, uri
        )

    fun importLyricsPreferencesFromCsv(context: Context, uri: Uri): Boolean {
        val imported = com.qlzhj.rhythm.core.data.core.domain.settings.LyricsPreferenceIo.import(
            lyricsSection._songLyricsPreferences.value, lyricsSection._songCustomLrcFiles.value, context, uri
        ) ?: return false
        lyricsSection._songLyricsPreferences.value = imported.first
        prefs.edit { putString(AppSettings.KEY_SONG_LYRICS_PREFERENCES, Gson().toJson(imported.first)) }
        lyricsSection._songCustomLrcFiles.value = imported.second
        prefs.edit { putString(AppSettings.KEY_SONG_CUSTOM_LRC_FILES, Gson().toJson(imported.second)) }
        return true
    }



    
    // ==================== Widget Settings ====================
    
    val widgetShowAlbumArt: StateFlow<Boolean> get() = widgetSection.widgetShowAlbumArt
    fun setWidgetShowAlbumArt(value: Boolean) = widgetSection.setWidgetShowAlbumArt(value)
    
    val widgetShowArtist: StateFlow<Boolean> get() = widgetSection.widgetShowArtist
    fun setWidgetShowArtist(value: Boolean) = widgetSection.setWidgetShowArtist(value)
    
    val widgetShowAlbum: StateFlow<Boolean> get() = widgetSection.widgetShowAlbum
    fun setWidgetShowAlbum(value: Boolean) = widgetSection.setWidgetShowAlbum(value)
    
    val widgetCornerRadius: StateFlow<Int> get() = widgetSection.widgetCornerRadius
    fun setWidgetCornerRadius(value: Int) = widgetSection.setWidgetCornerRadius(value)
    
    val widgetAutoUpdate: StateFlow<Boolean> get() = widgetSection.widgetAutoUpdate
    fun setWidgetAutoUpdate(value: Boolean) = widgetSection.setWidgetAutoUpdate(value)
    
    val widgetShowFavoriteButton: StateFlow<Boolean> get() = widgetSection.widgetShowFavoriteButton
    fun setWidgetShowFavoriteButton(value: Boolean) = widgetSection.setWidgetShowFavoriteButton(value)
    
    val widgetTheme: StateFlow<Int> get() = widgetSection.widgetTheme
    fun setWidgetTheme(value: Int) = widgetSection.setWidgetTheme(value)
    
    val widgetCookieBottomLeft: StateFlow<Int> get() = widgetSection.widgetCookieBottomLeft
    fun setWidgetCookieBottomLeft(value: Int) = widgetSection.setWidgetCookieBottomLeft(value)
    
    val widgetCookieBottomRight: StateFlow<Int> get() = widgetSection.widgetCookieBottomRight
    fun setWidgetCookieBottomRight(value: Int) = widgetSection.setWidgetCookieBottomRight(value)
    
    val widgetStatsRange: StateFlow<Int> get() = widgetSection.widgetStatsRange
    fun setWidgetStatsRange(value: Int) = widgetSection.setWidgetStatsRange(value)
    
    val widgetStatsGem: StateFlow<Int> get() = widgetSection.widgetStatsGem
    fun setWidgetStatsGem(value: Int) = widgetSection.setWidgetStatsGem(value)
    
    // ==================== Global Header Settings ====================
    
    private val _headerCollapseBehavior = MutableStateFlow(prefs.getInt(KEY_HEADER_COLLAPSE_BEHAVIOR, 0))
    val headerCollapseBehavior: StateFlow<Int> = _headerCollapseBehavior.asStateFlow()
    fun setHeaderCollapseBehavior(value: Int) {
        _headerCollapseBehavior.value = value
        prefs.edit { putInt(AppSettings.KEY_HEADER_COLLAPSE_BEHAVIOR, value) }
    }
    
    // ==================== Home Screen Customization Settings ====================
    
    val homeHeaderDisplayMode: StateFlow<Int> get() = customizationSection.homeHeaderDisplayMode
    fun setHomeHeaderDisplayMode(value: Int) = customizationSection.setHomeHeaderDisplayMode(value)
    
    val homeShowAppIcon: StateFlow<Boolean> get() = customizationSection.homeShowAppIcon
    fun setHomeShowAppIcon(value: Boolean) = customizationSection.setHomeShowAppIcon(value)
    
    val homeAppIconVisibility: StateFlow<Int> get() = customizationSection.homeAppIconVisibility
    fun setHomeAppIconVisibility(value: Int) = customizationSection.setHomeAppIconVisibility(value)
    
    
    val homeShowRecentlyPlayed: StateFlow<Boolean> get() = customizationSection.homeShowRecentlyPlayed
    fun setHomeShowRecentlyPlayed(value: Boolean) = customizationSection.setHomeShowRecentlyPlayed(value)
    
    val homeShowDiscoverCarousel: StateFlow<Boolean> get() = customizationSection.homeShowDiscoverCarousel
    fun setHomeShowDiscoverCarousel(value: Boolean) = customizationSection.setHomeShowDiscoverCarousel(value)
    
    val homeShowArtists: StateFlow<Boolean> get() = customizationSection.homeShowArtists
    fun setHomeShowArtists(value: Boolean) = customizationSection.setHomeShowArtists(value)
    
    val homeShowNewReleases: StateFlow<Boolean> get() = customizationSection.homeShowNewReleases
    fun setHomeShowNewReleases(value: Boolean) = customizationSection.setHomeShowNewReleases(value)
    
    val homeShowRecentlyAdded: StateFlow<Boolean> get() = customizationSection.homeShowRecentlyAdded
    fun setHomeShowRecentlyAdded(value: Boolean) = customizationSection.setHomeShowRecentlyAdded(value)
    
    val homeShowRecommended: StateFlow<Boolean> get() = customizationSection.homeShowRecommended
    fun setHomeShowRecommended(value: Boolean) = customizationSection.setHomeShowRecommended(value)
    
    val homeShowListeningStats: StateFlow<Boolean> get() = customizationSection.homeShowListeningStats
    fun setHomeShowListeningStats(value: Boolean) = customizationSection.setHomeShowListeningStats(value)


    private val _streamingHomeShowRhythmGuard = MutableStateFlow(prefs.getBoolean(KEY_STREAMING_HOME_SHOW_RHYTHM_GUARD, true))
    val streamingHomeShowRhythmGuard: StateFlow<Boolean> = _streamingHomeShowRhythmGuard.asStateFlow()
    fun setStreamingHomeShowRhythmGuard(value: Boolean) {
        _streamingHomeShowRhythmGuard.value = value
        prefs.edit { putBoolean(AppSettings.KEY_STREAMING_HOME_SHOW_RHYTHM_GUARD, value) }
    }

    private val _streamingHomeShowRhythmStats = MutableStateFlow(prefs.getBoolean(KEY_STREAMING_HOME_SHOW_RHYTHM_STATS, true))
    val streamingHomeShowRhythmStats: StateFlow<Boolean> = _streamingHomeShowRhythmStats.asStateFlow()
    fun setStreamingHomeShowRhythmStats(value: Boolean) {
        _streamingHomeShowRhythmStats.value = value
        prefs.edit { putBoolean(AppSettings.KEY_STREAMING_HOME_SHOW_RHYTHM_STATS, value) }
    }

    private val _streamingHomeShowRecentlyPlayed = MutableStateFlow(prefs.getBoolean(KEY_STREAMING_HOME_SHOW_RECENTLY_PLAYED, true))
    val streamingHomeShowRecentlyPlayed: StateFlow<Boolean> = _streamingHomeShowRecentlyPlayed.asStateFlow()
    fun setStreamingHomeShowRecentlyPlayed(value: Boolean) {
        _streamingHomeShowRecentlyPlayed.value = value
        prefs.edit { putBoolean(AppSettings.KEY_STREAMING_HOME_SHOW_RECENTLY_PLAYED, value) }
    }

    private val _streamingHomeShowArtists = MutableStateFlow(prefs.getBoolean(KEY_STREAMING_HOME_SHOW_ARTISTS, true))
    val streamingHomeShowArtists: StateFlow<Boolean> = _streamingHomeShowArtists.asStateFlow()
    fun setStreamingHomeShowArtists(value: Boolean) {
        _streamingHomeShowArtists.value = value
        prefs.edit { putBoolean(AppSettings.KEY_STREAMING_HOME_SHOW_ARTISTS, value) }
    }

    private val _streamingHomeShowRecommended = MutableStateFlow(prefs.getBoolean(KEY_STREAMING_HOME_SHOW_RECOMMENDED, true))
    val streamingHomeShowRecommended: StateFlow<Boolean> = _streamingHomeShowRecommended.asStateFlow()
    fun setStreamingHomeShowRecommended(value: Boolean) {
        _streamingHomeShowRecommended.value = value
        prefs.edit { putBoolean(AppSettings.KEY_STREAMING_HOME_SHOW_RECOMMENDED, value) }
    }

    private val _streamingHomeShowNewReleases = MutableStateFlow(prefs.getBoolean(KEY_STREAMING_HOME_SHOW_NEW_RELEASES, true))
    val streamingHomeShowNewReleases: StateFlow<Boolean> = _streamingHomeShowNewReleases.asStateFlow()
    fun setStreamingHomeShowNewReleases(value: Boolean) {
        _streamingHomeShowNewReleases.value = value
        prefs.edit { putBoolean(AppSettings.KEY_STREAMING_HOME_SHOW_NEW_RELEASES, value) }
    }

    private val _streamingHomeShowPlaylists = MutableStateFlow(prefs.getBoolean(KEY_STREAMING_HOME_SHOW_PLAYLISTS, true))
    val streamingHomeShowPlaylists: StateFlow<Boolean> = _streamingHomeShowPlaylists.asStateFlow()
    fun setStreamingHomeShowPlaylists(value: Boolean) {
        _streamingHomeShowPlaylists.value = value
        prefs.edit { putBoolean(AppSettings.KEY_STREAMING_HOME_SHOW_PLAYLISTS, value) }
    }


    
    // ==================== Player Screen Customization Settings ====================
    
    val playerShowGradientOverlay: StateFlow<Boolean> get() = customizationSection.playerShowGradientOverlay
    fun setPlayerShowGradientOverlay(value: Boolean) = customizationSection.setPlayerShowGradientOverlay(value)

    val playerAmbientBackdropEnabled: StateFlow<Boolean> get() = customizationSection.playerAmbientBackdropEnabled
    fun setPlayerAmbientBackdropEnabled(enabled: Boolean) = customizationSection.setPlayerAmbientBackdropEnabled(enabled)

    val playerAccentBackgroundEnabled: StateFlow<Boolean> get() = customizationSection.playerAccentBackgroundEnabled
    fun setPlayerAccentBackgroundEnabled(enabled: Boolean) = customizationSection.setPlayerAccentBackgroundEnabled(enabled)

    val playerMergeControlsToBottom: StateFlow<Boolean> get() = customizationSection.playerMergeControlsToBottom
    fun setPlayerMergeControlsToBottom(enabled: Boolean) = customizationSection.setPlayerMergeControlsToBottom(enabled)

    val playerAmbientBackdropIntensity: StateFlow<Float> get() = customizationSection.playerAmbientBackdropIntensity
    fun setPlayerAmbientBackdropIntensity(value: Float) = customizationSection.setPlayerAmbientBackdropIntensity(value)

    val playerAmbientInfiniteZoom: StateFlow<Boolean> get() = customizationSection.playerAmbientInfiniteZoom
    fun setPlayerAmbientInfiniteZoom(enabled: Boolean) = customizationSection.setPlayerAmbientInfiniteZoom(enabled)

    val playerGlassIntensity: StateFlow<Float> get() = customizationSection.playerGlassIntensity
    fun setPlayerGlassIntensity(value: Float) = customizationSection.setPlayerGlassIntensity(value)
    




    val playerLyricsTransition: StateFlow<Int> get() = customizationSection.playerLyricsTransition
    fun setPlayerLyricsTransition(value: Int) = customizationSection.setPlayerLyricsTransition(value)

    val playerLyricsTextSize: StateFlow<Float> get() = customizationSection.playerLyricsTextSize
    fun setPlayerLyricsTextSize(value: Float) = customizationSection.setPlayerLyricsTextSize(value)

    val playerLyricsAlignment: StateFlow<String> get() = customizationSection.playerLyricsAlignment
    fun setPlayerLyricsAlignment(value: String) = customizationSection.setPlayerLyricsAlignment(value)

    val playerShowArtBelowLyrics: StateFlow<Boolean> get() = customizationSection.playerShowArtBelowLyrics
    fun setPlayerShowArtBelowLyrics(value: Boolean) = customizationSection.setPlayerShowArtBelowLyrics(value)
    
    val playerShowSeekButtons: StateFlow<Boolean> get() = customizationSection.playerShowSeekButtons
    fun setPlayerShowSeekButtons(value: Boolean) = customizationSection.setPlayerShowSeekButtons(value)
    
    val playerTextAlignment: StateFlow<String> get() = customizationSection.playerTextAlignment
    fun setPlayerTextAlignment(value: String) = customizationSection.setPlayerTextAlignment(value)
    
    val playerShowSongInfoOnArtwork: StateFlow<Boolean> get() = customizationSection.playerShowSongInfoOnArtwork
    fun setPlayerShowSongInfoOnArtwork(value: Boolean) = customizationSection.setPlayerShowSongInfoOnArtwork(value)
    
    val playerArtworkCornerRadius: StateFlow<Int> get() = customizationSection.playerArtworkCornerRadius
    fun setPlayerArtworkCornerRadius(value: Int) = customizationSection.setPlayerArtworkCornerRadius(value)
    
    val playerShowAudioQualityBadges: StateFlow<Boolean> get() = customizationSection.playerShowAudioQualityBadges
    fun setPlayerShowAudioQualityBadges(value: Boolean) = customizationSection.setPlayerShowAudioQualityBadges(value)

    private val _expressiveQualityBadgeAlwaysVisible = MutableStateFlow(prefs.getBoolean(KEY_EXPRESSIVE_QUALITY_BADGE_ALWAYS_VISIBLE, false))
    val expressiveQualityBadgeAlwaysVisible: StateFlow<Boolean> = _expressiveQualityBadgeAlwaysVisible.asStateFlow()
    fun setExpressiveQualityBadgeAlwaysVisible(value: Boolean) {
        _expressiveQualityBadgeAlwaysVisible.value = value
        prefs.edit { putBoolean(AppSettings.KEY_EXPRESSIVE_QUALITY_BADGE_ALWAYS_VISIBLE, value) }
    }
    
    // Player Progress Bar Style
    val playerProgressStyle: StateFlow<String> get() = customizationSection.playerProgressStyle
    fun setPlayerProgressStyle(value: String) = customizationSection.setPlayerProgressStyle(value)
    
    val playerProgressThumbStyle: StateFlow<String> get() = customizationSection.playerProgressThumbStyle
    fun setPlayerProgressThumbStyle(value: String) = customizationSection.setPlayerProgressThumbStyle(value)
    
    // Player Progress Thumb Rotate While Playing
    val playerProgressThumbRotate: StateFlow<Boolean> get() = customizationSection.playerProgressThumbRotate
    fun setPlayerProgressThumbRotate(value: Boolean) = customizationSection.setPlayerProgressThumbRotate(value)
    
    // ==================== MiniPlayer Customization Settings ====================
    
    val miniPlayerProgressStyle: StateFlow<String> get() = customizationSection.miniPlayerProgressStyle
    fun setMiniPlayerProgressStyle(value: String) = customizationSection.setMiniPlayerProgressStyle(value)
    
    val miniPlayerShowProgress: StateFlow<Boolean> get() = customizationSection.miniPlayerShowProgress
    fun setMiniPlayerShowProgress(value: Boolean) = customizationSection.setMiniPlayerShowProgress(value)
    
    val miniPlayerShowArtwork: StateFlow<Boolean> get() = customizationSection.miniPlayerShowArtwork
    fun setMiniPlayerShowArtwork(value: Boolean) = customizationSection.setMiniPlayerShowArtwork(value)
    
    val miniPlayerArtworkSize: StateFlow<Int> get() = customizationSection.miniPlayerArtworkSize
    fun setMiniPlayerArtworkSize(value: Int) = customizationSection.setMiniPlayerArtworkSize(value)
    
    val miniPlayerCornerRadius: StateFlow<Int> get() = customizationSection.miniPlayerCornerRadius
    fun setMiniPlayerCornerRadius(value: Int) = customizationSection.setMiniPlayerCornerRadius(value)
    
    val miniPlayerShowTime: StateFlow<Boolean> get() = customizationSection.miniPlayerShowTime
    fun setMiniPlayerShowTime(value: Boolean) = customizationSection.setMiniPlayerShowTime(value)
    
    
    
    
    val miniPlayerSwipeGestures: StateFlow<Boolean> get() = customizationSection.miniPlayerSwipeGestures
    fun setMiniPlayerSwipeGestures(value: Boolean) = customizationSection.setMiniPlayerSwipeGestures(value)
    
    
    val miniPlayerAlwaysShowTablet: StateFlow<Boolean> get() = customizationSection.miniPlayerAlwaysShowTablet
    fun setMiniPlayerAlwaysShowTablet(value: Boolean) = customizationSection.setMiniPlayerAlwaysShowTablet(value)
    
    val homeDiscoverAutoScroll: StateFlow<Boolean> get() = customizationSection.homeDiscoverAutoScroll
    fun setHomeDiscoverAutoScroll(value: Boolean) = customizationSection.setHomeDiscoverAutoScroll(value)
    
    val homeDiscoverAutoScrollInterval: StateFlow<Int> get() = customizationSection.homeDiscoverAutoScrollInterval
    fun setHomeDiscoverAutoScrollInterval(value: Int) = customizationSection.setHomeDiscoverAutoScrollInterval(value)
    
    val homeDiscoverItemCount: StateFlow<Int> get() = customizationSection.homeDiscoverItemCount
    fun setHomeDiscoverItemCount(value: Int) = customizationSection.setHomeDiscoverItemCount(value)
    
    val homeRecentlyPlayedCount: StateFlow<Int> get() = customizationSection.homeRecentlyPlayedCount
    fun setHomeRecentlyPlayedCount(value: Int) = customizationSection.setHomeRecentlyPlayedCount(value)
    
    val homeArtistsCount: StateFlow<Int> get() = customizationSection.homeArtistsCount
    fun setHomeArtistsCount(value: Int) = customizationSection.setHomeArtistsCount(value)
    
    val homeNewReleasesCount: StateFlow<Int> get() = customizationSection.homeNewReleasesCount
    fun setHomeNewReleasesCount(value: Int) = customizationSection.setHomeNewReleasesCount(value)
    
    val homeRecentlyAddedCount: StateFlow<Int> get() = customizationSection.homeRecentlyAddedCount
    fun setHomeRecentlyAddedCount(value: Int) = customizationSection.setHomeRecentlyAddedCount(value)
    
    val homeRecommendedCount: StateFlow<Int> get() = customizationSection.homeRecommendedCount
    fun setHomeRecommendedCount(value: Int) = customizationSection.setHomeRecommendedCount(value)
    
    
    

    
    
    // Discover Widget - Content Visibility Settings
    val homeDiscoverShowAlbumName: StateFlow<Boolean> get() = customizationSection.homeDiscoverShowAlbumName
    fun setHomeDiscoverShowAlbumName(value: Boolean) = customizationSection.setHomeDiscoverShowAlbumName(value)
    
    val homeDiscoverShowArtistName: StateFlow<Boolean> get() = customizationSection.homeDiscoverShowArtistName
    fun setHomeDiscoverShowArtistName(value: Boolean) = customizationSection.setHomeDiscoverShowArtistName(value)
    
    val homeDiscoverShowYear: StateFlow<Boolean> get() = customizationSection.homeDiscoverShowYear
    fun setHomeDiscoverShowYear(value: Boolean) = customizationSection.setHomeDiscoverShowYear(value)
    
    val homeDiscoverShowPlayButton: StateFlow<Boolean> get() = customizationSection.homeDiscoverShowPlayButton
    fun setHomeDiscoverShowPlayButton(value: Boolean) = customizationSection.setHomeDiscoverShowPlayButton(value)
    
    val homeDiscoverShowGradient: StateFlow<Boolean> get() = customizationSection.homeDiscoverShowGradient
    fun setHomeDiscoverShowGradient(value: Boolean) = customizationSection.setHomeDiscoverShowGradient(value)
    
    // Gesture Settings
    val gesturePlayerSwipeDismiss: StateFlow<Boolean> get() = customizationSection.gesturePlayerSwipeDismiss
    fun setGesturePlayerSwipeDismiss(value: Boolean) = customizationSection.setGesturePlayerSwipeDismiss(value)
    
    val gesturePlayerSwipeTracks: StateFlow<Boolean> get() = customizationSection.gesturePlayerSwipeTracks
    fun setGesturePlayerSwipeTracks(value: Boolean) = customizationSection.setGesturePlayerSwipeTracks(value)
    
    val gestureArtworkDoubleTap: StateFlow<Boolean> get() = customizationSection.gestureArtworkDoubleTap
    fun setGestureArtworkDoubleTap(value: Boolean) = customizationSection.setGestureArtworkDoubleTap(value)
    
    val gestureArtworkSingleTap: StateFlow<Boolean> get() = customizationSection.gestureArtworkSingleTap
    fun setGestureArtworkSingleTap(value: Boolean) = customizationSection.setGestureArtworkSingleTap(value)

    val gestureQueueSwipeToRemove: StateFlow<Boolean> get() = customizationSection.gestureQueueSwipeToRemove
    fun setGestureQueueSwipeToRemove(value: Boolean) = customizationSection.setGestureQueueSwipeToRemove(value)

    val gestureLibrarySwipeTabs: StateFlow<Boolean> get() = customizationSection.gestureLibrarySwipeTabs
    fun setGestureLibrarySwipeTabs(value: Boolean) = customizationSection.setGestureLibrarySwipeTabs(value)

    val miniPlayerSwipeTracks: StateFlow<Boolean> get() = customizationSection.miniPlayerSwipeTracks
    fun setMiniPlayerSwipeTracks(value: Boolean) = customizationSection.setMiniPlayerSwipeTracks(value)

    val miniPlayerSwipeDismiss: StateFlow<Boolean> get() = customizationSection.miniPlayerSwipeDismiss
    fun setMiniPlayerSwipeDismiss(value: Boolean) = customizationSection.setMiniPlayerSwipeDismiss(value)

    val tapLyricsToSeek: StateFlow<Boolean> get() = lyricsSection.tapLyricsToSeek
    fun setTapLyricsToSeek(value: Boolean) = lyricsSection.setTapLyricsToSeek(value)
    
    val homeSectionOrder: StateFlow<List<String>> get() = customizationSection.homeSectionOrder
    fun setHomeSectionOrder(value: List<String>) = customizationSection.setHomeSectionOrder(value)

    private val defaultStreamingHomeSectionOrder = listOf(
        "DISCOVER", "RECENTLY_PLAYED", "ARTISTS", "RHYTHM_GUARD", "RHYTHM_STATS", "NEW_RELEASES"
    )

    private fun normalizeStreamingHomeSectionOrder(rawSections: List<String>): List<String> {
        val normalized = rawSections
            .map(String::trim)
            .filter { it != "PLAYLISTS" && it.isNotBlank() }
            .map {
                when (it) {
                    "STATS" -> "RHYTHM_STATS"
                    "RECOMMENDED" -> "DISCOVER"
                    "GREETING" -> "DISCOVER"
                    else -> it
                }
            }

        return (normalized + defaultStreamingHomeSectionOrder).distinct()
    }

    private val _streamingHomeSectionOrder = MutableStateFlow(
        normalizeStreamingHomeSectionOrder(
            prefs.getString(AppSettings.KEY_STREAMING_HOME_SECTION_ORDER, null)
                ?.split(",")
                .orEmpty()
        )
    )
    val streamingHomeSectionOrder: StateFlow<List<String>> = _streamingHomeSectionOrder.asStateFlow()
    fun setStreamingHomeSectionOrder(value: List<String>) {
        val normalizedValue = normalizeStreamingHomeSectionOrder(value)
        _streamingHomeSectionOrder.value = normalizedValue
        prefs.edit { putString(AppSettings.KEY_STREAMING_HOME_SECTION_ORDER, normalizedValue.joinToString(",")) }
    }
    
    // Album Bottom Sheet Appearance Settings
    val albumBottomSheetGradientBlur: StateFlow<Boolean> get() = customizationSection.albumBottomSheetGradientBlur
    fun setAlbumBottomSheetGradientBlur(value: Boolean) = customizationSection.setAlbumBottomSheetGradientBlur(value)

    val albumBottomSheetDiscFilter: StateFlow<Int> get() = customizationSection.albumBottomSheetDiscFilter
    fun setAlbumBottomSheetDiscFilter(value: Int) = customizationSection.setAlbumBottomSheetDiscFilter(value)

    val albumHideAbout: StateFlow<Boolean> get() = librarySection.albumHideAbout
    fun setAlbumHideAbout(value: Boolean) = librarySection.setAlbumHideAbout(value)
    
    // ==================== Expressive MaterialShapes Settings ====================
    
    // Master toggle for expressive shapes feature
    val expressiveShapesEnabled: StateFlow<Boolean> get() = customizationSection.expressiveShapesEnabled
    fun setExpressiveShapesEnabled(value: Boolean) = customizationSection.setExpressiveShapesEnabled(value)
    
    // Shape preset selection
    val expressiveShapePreset: StateFlow<String> get() = customizationSection.expressiveShapePreset
    fun setExpressiveShapePreset(value: String) = customizationSection.setExpressiveShapePreset(value)




    private val _showSettingsSuggestions = MutableStateFlow(prefs.getBoolean(KEY_SHOW_SETTINGS_SUGGESTIONS, true))
    val showSettingsSuggestions: StateFlow<Boolean> = _showSettingsSuggestions.asStateFlow()
    fun setShowSettingsSuggestions(show: Boolean) {
        prefs.edit { putBoolean(AppSettings.KEY_SHOW_SETTINGS_SUGGESTIONS, show) }
        _showSettingsSuggestions.value = show
    }

    
    // Individual shape settings for each artwork target
    val expressiveShapeAlbumArt: StateFlow<String> get() = customizationSection.expressiveShapeAlbumArt
    fun setExpressiveShapeAlbumArt(value: String) = customizationSection.setExpressiveShapeAlbumArt(value)
    
    val expressiveShapePlayerArt: StateFlow<String> get() = customizationSection.expressiveShapePlayerArt
    fun setExpressiveShapePlayerArt(value: String) = customizationSection.setExpressiveShapePlayerArt(value)
    
    val expressiveShapeSongArt: StateFlow<String> get() = customizationSection.expressiveShapeSongArt
    fun setExpressiveShapeSongArt(value: String) = customizationSection.setExpressiveShapeSongArt(value)
    
    val expressiveShapePlaylistArt: StateFlow<String> get() = customizationSection.expressiveShapePlaylistArt
    fun setExpressiveShapePlaylistArt(value: String) = customizationSection.setExpressiveShapePlaylistArt(value)
    
    val expressiveShapeArtistArt: StateFlow<String> get() = customizationSection.expressiveShapeArtistArt
    fun setExpressiveShapeArtistArt(value: String) = customizationSection.setExpressiveShapeArtistArt(value)
    
    val expressiveShapePlayerControls: StateFlow<String> get() = customizationSection.expressiveShapePlayerControls
    fun setExpressiveShapePlayerControls(value: String) = customizationSection.setExpressiveShapePlayerControls(value)
    
    val expressiveShapeMiniPlayer: StateFlow<String> get() = customizationSection.expressiveShapeMiniPlayer
    fun setExpressiveShapeMiniPlayer(value: String) = customizationSection.setExpressiveShapeMiniPlayer(value)
    
    /**
     * Apply a preset to all artwork and player shape targets
     */
    fun applyExpressiveShapePreset(preset: String) {
        when (preset) {
            "DEFAULT" -> {
                customizationSection._expressiveShapeAlbumArt.value = "GHOSTISH"
                customizationSection._expressiveShapePlayerArt.value = "BUN"
                customizationSection._expressiveShapeSongArt.value = "CLOVER_8_LEAF"
                customizationSection._expressiveShapePlaylistArt.value = "CLOVER_4_LEAF"
                customizationSection._expressiveShapeArtistArt.value = "PIXEL_CIRCLE"
                customizationSection._expressiveShapePlayerControls.value = "SUNNY"
                customizationSection._expressiveShapeMiniPlayer.value = "COOKIE_4"
            }
            "FRIENDLY" -> {
                customizationSection._expressiveShapeAlbumArt.value = "CLOVER_8_LEAF"
                customizationSection._expressiveShapePlayerArt.value = "CLOVER_8_LEAF"
                customizationSection._expressiveShapeSongArt.value = "COOKIE_6"
                customizationSection._expressiveShapePlaylistArt.value = "OVAL"
                customizationSection._expressiveShapeArtistArt.value = "HEART"
                customizationSection._expressiveShapePlayerControls.value = "CIRCLE"
                customizationSection._expressiveShapeMiniPlayer.value = "OVAL"
            }
            "MODERN" -> {
                customizationSection._expressiveShapeAlbumArt.value = "SLANTED"
                customizationSection._expressiveShapePlayerArt.value = "SLANTED"
                customizationSection._expressiveShapeSongArt.value = "COOKIE_7"
                customizationSection._expressiveShapePlaylistArt.value = "DIAMOND"
                customizationSection._expressiveShapeArtistArt.value = "CIRCLE"
                customizationSection._expressiveShapePlayerControls.value = "PENTAGON"
                customizationSection._expressiveShapeMiniPlayer.value = "SLANTED"
            }
            "PLAYFUL" -> {
                customizationSection._expressiveShapeAlbumArt.value = "FLOWER"
                customizationSection._expressiveShapePlayerArt.value = "SOFT_BURST"
                customizationSection._expressiveShapeSongArt.value = "COOKIE_6"
                customizationSection._expressiveShapePlaylistArt.value = "CLOVER_4_LEAF"
                customizationSection._expressiveShapeArtistArt.value = "HEART"
                customizationSection._expressiveShapePlayerControls.value = "SUNNY"
                customizationSection._expressiveShapeMiniPlayer.value = "CLOVER_8_LEAF"
            }
            "ORGANIC" -> {
                customizationSection._expressiveShapeAlbumArt.value = "CLOVER_4_LEAF"
                customizationSection._expressiveShapePlayerArt.value = "FLOWER"
                customizationSection._expressiveShapeSongArt.value = "CLOVER_8_LEAF"
                customizationSection._expressiveShapePlaylistArt.value = "COOKIE_4"
                customizationSection._expressiveShapeArtistArt.value = "OVAL"
                customizationSection._expressiveShapePlayerControls.value = "CIRCLE"
                customizationSection._expressiveShapeMiniPlayer.value = "OVAL"
            }
            "GEOMETRIC" -> {
                customizationSection._expressiveShapeAlbumArt.value = "SQUARE"
                customizationSection._expressiveShapePlayerArt.value = "DIAMOND"
                customizationSection._expressiveShapeSongArt.value = "PENTAGON"
                customizationSection._expressiveShapePlaylistArt.value = "SQUARE"
                customizationSection._expressiveShapeArtistArt.value = "CIRCLE"
                customizationSection._expressiveShapePlayerControls.value = "PENTAGON"
                customizationSection._expressiveShapeMiniPlayer.value = "SQUARE"
            }
            "RETRO" -> {
                customizationSection._expressiveShapeAlbumArt.value = "PIXEL_CIRCLE"
                customizationSection._expressiveShapePlayerArt.value = "PIXEL_CIRCLE"
                customizationSection._expressiveShapeSongArt.value = "SQUARE"
                customizationSection._expressiveShapePlaylistArt.value = "PIXEL_TRIANGLE"
                customizationSection._expressiveShapeArtistArt.value = "PIXEL_CIRCLE"
                customizationSection._expressiveShapePlayerControls.value = "PIXEL_CIRCLE"
                customizationSection._expressiveShapeMiniPlayer.value = "SQUARE"
            }
            "CHEERFUL" -> {
                customizationSection._expressiveShapeAlbumArt.value = "FLOWER"
                customizationSection._expressiveShapePlayerArt.value = "SUNNY"
                customizationSection._expressiveShapeSongArt.value = "PUFFY"
                customizationSection._expressiveShapePlaylistArt.value = "CLOVER_4_LEAF"
                customizationSection._expressiveShapeArtistArt.value = "HEART"
                customizationSection._expressiveShapePlayerControls.value = "CIRCLE"
                customizationSection._expressiveShapeMiniPlayer.value = "PUFFY"
            }
            // CUSTOM - don't change individual shapes
        }
        // Save all shape values to preferences
        prefs.edit {
            putString(AppSettings.KEY_EXPRESSIVE_SHAPE_PRESET, preset)
            putString(AppSettings.KEY_EXPRESSIVE_SHAPE_ALBUM_ART, customizationSection._expressiveShapeAlbumArt.value)
            putString(AppSettings.KEY_EXPRESSIVE_SHAPE_PLAYER_ART, customizationSection._expressiveShapePlayerArt.value)
            putString(AppSettings.KEY_EXPRESSIVE_SHAPE_SONG_ART, customizationSection._expressiveShapeSongArt.value)
            putString(AppSettings.KEY_EXPRESSIVE_SHAPE_PLAYLIST_ART, customizationSection._expressiveShapePlaylistArt.value)
            putString(AppSettings.KEY_EXPRESSIVE_SHAPE_ARTIST_ART, customizationSection._expressiveShapeArtistArt.value)
            putString(AppSettings.KEY_EXPRESSIVE_SHAPE_PLAYER_CONTROLS, customizationSection._expressiveShapePlayerControls.value)
            putString(AppSettings.KEY_EXPRESSIVE_SHAPE_MINI_PLAYER, customizationSection._expressiveShapeMiniPlayer.value)
        }
        customizationSection._expressiveShapePreset.value = preset
    }
    
    /**
     * Synchronously commits all pending SharedPreferences edits to disk.
     * This blocks the calling thread until all outstanding asynchronous apply() writes
     * have been fully written to storage.
     */
    fun commit() {
        try {
            prefs.edit(commit = true) { }
            Log.d("AppSettings", "Synchronously committed all pending preference changes to disk")
        } catch (e: Exception) {
            Log.e("AppSettings", "Failed to commit preferences", e)
        }
    }

    /**
     * Randomize all expressive shape targets with randomly picked shapes.
     * Sets the preset to CUSTOM after randomizing.
     */
    fun randomizeExpressiveShapes() {
        val allShapes = listOf(
            "CIRCLE", "SQUARE", "OVAL", "PILL", "DIAMOND", "TRIANGLE", "PENTAGON",
            "FLOWER", "CLOVER_4_LEAF", "CLOVER_8_LEAF", "HEART", "BOOM", "SOFT_BOOM",
            "BURST", "SOFT_BURST", "SUNNY", "VERY_SUNNY",
            "COOKIE_4", "COOKIE_6", "COOKIE_7", "COOKIE_9", "COOKIE_12",
            "GHOSTISH", "PUFFY", "PUFFY_DIAMOND", "BUN", "FAN",
            "ARCH", "CLAM_SHELL", "GEM", "SLANTED", "PIXEL_CIRCLE", "PIXEL_TRIANGLE"
        )
        
        customizationSection._expressiveShapeAlbumArt.value = allShapes.random()
        customizationSection._expressiveShapePlayerArt.value = allShapes.random()
        customizationSection._expressiveShapeSongArt.value = allShapes.random()
        customizationSection._expressiveShapePlaylistArt.value = allShapes.random()
        customizationSection._expressiveShapeArtistArt.value = allShapes.random()
        customizationSection._expressiveShapePlayerControls.value = allShapes.random()
        customizationSection._expressiveShapeMiniPlayer.value = allShapes.random()
        
        // Save as CUSTOM preset
        prefs.edit {
            putString(AppSettings.KEY_EXPRESSIVE_SHAPE_PRESET, "CUSTOM")
            putString(AppSettings.KEY_EXPRESSIVE_SHAPE_ALBUM_ART, customizationSection._expressiveShapeAlbumArt.value)
            putString(AppSettings.KEY_EXPRESSIVE_SHAPE_PLAYER_ART, customizationSection._expressiveShapePlayerArt.value)
            putString(AppSettings.KEY_EXPRESSIVE_SHAPE_SONG_ART, customizationSection._expressiveShapeSongArt.value)
            putString(AppSettings.KEY_EXPRESSIVE_SHAPE_PLAYLIST_ART, customizationSection._expressiveShapePlaylistArt.value)
            putString(AppSettings.KEY_EXPRESSIVE_SHAPE_ARTIST_ART, customizationSection._expressiveShapeArtistArt.value)
            putString(AppSettings.KEY_EXPRESSIVE_SHAPE_PLAYER_CONTROLS, customizationSection._expressiveShapePlayerControls.value)
            putString(AppSettings.KEY_EXPRESSIVE_SHAPE_MINI_PLAYER, customizationSection._expressiveShapeMiniPlayer.value)
        }
        customizationSection._expressiveShapePreset.value = "CUSTOM"
    }
}
