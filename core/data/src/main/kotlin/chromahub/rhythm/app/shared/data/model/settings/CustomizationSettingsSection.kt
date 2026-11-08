package chromahub.rhythm.app.shared.data.model.settings

import android.content.SharedPreferences
import androidx.core.content.edit
import chromahub.rhythm.app.shared.data.model.AppSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
/**
 * Home/player/miniplayer/gesture/expressive-shape customization state and
 * write operations (B-phase B2 domain split).
 */
 */
internal class CustomizationSettingsSection(
    private val prefs: SharedPreferences,
    private val updateDerived: () -> Unit,
) {

    // Default section order for home screen
    private val defaultHomeSectionOrder = listOf(
        "DISCOVER", "RECENTLY_PLAYED", "ARTISTS", "RHYTHM_GUARD",
        "NEW_RELEASES", "RECENTLY_ADDED", "RECOMMENDED", "STATS", "MOOD"
    )

    // Player Chip Order (Add to Playlist and Edit chips are not reorderable - they stay fixed)
    internal val defaultChipOrder = listOf("FAVORITE", "SPEED", "EQUALIZER", "SLEEP_TIMER", "LYRICS", "ALBUM", "ARTIST", "SHARE")

    val allExpressiveBottomButtons = listOf(
        "LYRICS", "FAVORITE", "DEVICE", "QUEUE", "MORE",
        "SHUFFLE", "REPEAT", "EQUALIZER", "SPEED", "SLEEP_TIMER",
        "ADD_TO_PLAYLIST", "ALBUM", "ARTIST", "SONG_INFO", "SHARE"
    )
    val defaultExpressiveBottomButtonsNormal = listOf("DEVICE", "QUEUE", "MORE")
    val defaultExpressiveBottomButtonsMerge = listOf("LYRICS", "FAVORITE", "DEVICE", "QUEUE", "MORE")

    val allExpressiveBottomButtonsNormal = allExpressiveBottomButtons.filterNot { it in AppSettings.fixedBottomButtonsNormal }

    internal val _expressiveHiddenBottomButtonsNormal = MutableStateFlow(
        prefs.getString(AppSettings.KEY_EXPRESSIVE_HIDDEN_BOTTOM_BUTTONS_NORMAL, null)
            ?.split(",")
            ?.filter { it.isNotBlank() }
            ?.filter { it in allExpressiveBottomButtons && it !in AppSettings.fixedBottomButtonsNormal && it != "MORE" }
            ?.toSet()
            ?: emptySet()
    )
    val expressiveHiddenBottomButtonsNormal: StateFlow<Set<String>> = _expressiveHiddenBottomButtonsNormal.asStateFlow()

    internal val _expressiveHiddenBottomButtonsMerge = MutableStateFlow(
        prefs.getString(AppSettings.KEY_EXPRESSIVE_HIDDEN_BOTTOM_BUTTONS_MERGE, null)
            ?.split(",")
            ?.filter { it.isNotBlank() }
            ?.filter { it in allExpressiveBottomButtons }
            ?.toSet()
            ?: emptySet()
    )
    val expressiveHiddenBottomButtonsMerge: StateFlow<Set<String>> = _expressiveHiddenBottomButtonsMerge.asStateFlow()
    internal val _albumBottomSheetDiscFilter = MutableStateFlow(prefs.getInt(AppSettings.KEY_ALBUM_BOTTOM_SHEET_DISC_FILTER, 0).coerceAtLeast(0))

    val albumBottomSheetDiscFilter: StateFlow<Int> get() = _albumBottomSheetDiscFilter.asStateFlow()
    fun setAlbumBottomSheetDiscFilter(value: Int) {
        val normalizedValue = value.coerceAtLeast(0)
        _albumBottomSheetDiscFilter.value = normalizedValue
        prefs.edit { putInt(AppSettings.KEY_ALBUM_BOTTOM_SHEET_DISC_FILTER, normalizedValue) }
    }
    internal val _albumBottomSheetGradientBlur = MutableStateFlow(prefs.getBoolean(AppSettings.KEY_ALBUM_BOTTOM_SHEET_GRADIENT_BLUR, true))

    val albumBottomSheetGradientBlur: StateFlow<Boolean> get() = _albumBottomSheetGradientBlur.asStateFlow()
    fun setAlbumBottomSheetGradientBlur(value: Boolean) {
        _albumBottomSheetGradientBlur.value = value
        prefs.edit { putBoolean(AppSettings.KEY_ALBUM_BOTTOM_SHEET_GRADIENT_BLUR, value) }
    }
    internal val _expressiveBottomButtonsMerge = MutableStateFlow(
        prefs.getString(AppSettings.KEY_EXPRESSIVE_BOTTOM_BUTTONS_MERGE, null)
            ?.split(",")
            ?.filter { it.isNotBlank() }
            ?.distinct()
            ?.filter { it in allExpressiveBottomButtons }
            ?.takeIf { it.isNotEmpty() }
            ?.let { savedList ->
                val result = savedList.toMutableList()
                defaultExpressiveBottomButtonsMerge.forEachIndexed { defaultIndex, button ->
                    if (!result.contains(button)) {
                        result.add(defaultIndex.coerceAtMost(result.size), button)
                    }
                }
                result
            }
            ?: defaultExpressiveBottomButtonsMerge
    )

    val expressiveBottomButtonsMerge: StateFlow<List<String>> get() = _expressiveBottomButtonsMerge.asStateFlow()
    fun setExpressiveBottomButtonsMerge(order: List<String>) {
        val sanitized = order.filter { it in allExpressiveBottomButtons }
        val orderString = sanitized.joinToString(",")
        prefs.edit { putString(AppSettings.KEY_EXPRESSIVE_BOTTOM_BUTTONS_MERGE, orderString) }
        _expressiveBottomButtonsMerge.value = sanitized
    }
    internal val _expressiveBottomButtonsNormal = MutableStateFlow(
        prefs.getString(AppSettings.KEY_EXPRESSIVE_BOTTOM_BUTTONS_NORMAL, null)
            ?.split(",")
            ?.filter { it.isNotBlank() }
            ?.distinct()
            ?.filter { it in allExpressiveBottomButtons && it !in AppSettings.fixedBottomButtonsNormal }
            ?.takeIf { it.isNotEmpty() }
            ?.let { savedList ->
                val result = savedList.toMutableList()
                defaultExpressiveBottomButtonsNormal.forEachIndexed { defaultIndex, button ->
                    if (!result.contains(button)) {
                        result.add(defaultIndex.coerceAtMost(result.size), button)
                    }
                }
                if (!result.contains("MORE")) {
                    result.add("MORE")
                }
                result
            }
            ?: defaultExpressiveBottomButtonsNormal
    )

    val expressiveBottomButtonsNormal: StateFlow<List<String>> get() = _expressiveBottomButtonsNormal.asStateFlow()
    fun setExpressiveBottomButtonsNormal(order: List<String>) {
        val sanitized = order.filter { it in allExpressiveBottomButtons && it !in AppSettings.fixedBottomButtonsNormal }.toMutableList()
        if (!sanitized.contains("MORE")) {
            sanitized.add("MORE")
        }
        val orderString = sanitized.joinToString(",")
        prefs.edit { putString(AppSettings.KEY_EXPRESSIVE_BOTTOM_BUTTONS_NORMAL, orderString) }
        _expressiveBottomButtonsNormal.value = sanitized
    }
    internal val _expressiveShapeAlbumArt = MutableStateFlow(prefs.getString(AppSettings.KEY_EXPRESSIVE_SHAPE_ALBUM_ART, "GHOSTISH") ?: "GHOSTISH")

    val expressiveShapeAlbumArt: StateFlow<String> get() = _expressiveShapeAlbumArt.asStateFlow()
    fun setExpressiveShapeAlbumArt(value: String) {
        _expressiveShapeAlbumArt.value = value
        prefs.edit { putString(AppSettings.KEY_EXPRESSIVE_SHAPE_ALBUM_ART, value) }
        // Auto-switch to custom preset when individual shape is changed
        if (_expressiveShapePreset.value != "CUSTOM") {
            setExpressiveShapePreset("CUSTOM")
        }
    }
    internal val _expressiveShapeArtistArt = MutableStateFlow(prefs.getString(AppSettings.KEY_EXPRESSIVE_SHAPE_ARTIST_ART, "PIXEL_CIRCLE") ?: "PIXEL_CIRCLE")

    val expressiveShapeArtistArt: StateFlow<String> get() = _expressiveShapeArtistArt.asStateFlow()
    fun setExpressiveShapeArtistArt(value: String) {
        _expressiveShapeArtistArt.value = value
        prefs.edit { putString(AppSettings.KEY_EXPRESSIVE_SHAPE_ARTIST_ART, value) }
        if (_expressiveShapePreset.value != "CUSTOM") {
            setExpressiveShapePreset("CUSTOM")
        }
    }
    internal val _expressiveShapeMiniPlayer = MutableStateFlow(prefs.getString(AppSettings.KEY_EXPRESSIVE_SHAPE_MINI_PLAYER, "COOKIE_4") ?: "COOKIE_4")

    val expressiveShapeMiniPlayer: StateFlow<String> get() = _expressiveShapeMiniPlayer.asStateFlow()
    fun setExpressiveShapeMiniPlayer(value: String) {
        _expressiveShapeMiniPlayer.value = value
        prefs.edit { putString(AppSettings.KEY_EXPRESSIVE_SHAPE_MINI_PLAYER, value) }
        if (_expressiveShapePreset.value != "CUSTOM") {
            setExpressiveShapePreset("CUSTOM")
        }
    }
    internal val _expressiveShapePlayerArt = MutableStateFlow(prefs.getString(AppSettings.KEY_EXPRESSIVE_SHAPE_PLAYER_ART, "BUN") ?: "BUN")

    val expressiveShapePlayerArt: StateFlow<String> get() = _expressiveShapePlayerArt.asStateFlow()
    fun setExpressiveShapePlayerArt(value: String) {
        _expressiveShapePlayerArt.value = value
        prefs.edit { putString(AppSettings.KEY_EXPRESSIVE_SHAPE_PLAYER_ART, value) }
        if (_expressiveShapePreset.value != "CUSTOM") {
            setExpressiveShapePreset("CUSTOM")
        }
    }
    internal val _expressiveShapePlayerControls = MutableStateFlow(prefs.getString(AppSettings.KEY_EXPRESSIVE_SHAPE_PLAYER_CONTROLS, "SUNNY") ?: "SUNNY")

    val expressiveShapePlayerControls: StateFlow<String> get() = _expressiveShapePlayerControls.asStateFlow()
    fun setExpressiveShapePlayerControls(value: String) {
        _expressiveShapePlayerControls.value = value
        prefs.edit { putString(AppSettings.KEY_EXPRESSIVE_SHAPE_PLAYER_CONTROLS, value) }
        if (_expressiveShapePreset.value != "CUSTOM") {
            setExpressiveShapePreset("CUSTOM")
        }
    }
    internal val _expressiveShapePlaylistArt = MutableStateFlow(prefs.getString(AppSettings.KEY_EXPRESSIVE_SHAPE_PLAYLIST_ART, "CLOVER_4_LEAF") ?: "CLOVER_4_LEAF")

    val expressiveShapePlaylistArt: StateFlow<String> get() = _expressiveShapePlaylistArt.asStateFlow()
    fun setExpressiveShapePlaylistArt(value: String) {
        _expressiveShapePlaylistArt.value = value
        prefs.edit { putString(AppSettings.KEY_EXPRESSIVE_SHAPE_PLAYLIST_ART, value) }
        if (_expressiveShapePreset.value != "CUSTOM") {
            setExpressiveShapePreset("CUSTOM")
        }
    }
    internal val _expressiveShapePreset = MutableStateFlow(prefs.getString(AppSettings.KEY_EXPRESSIVE_SHAPE_PRESET, "DEFAULT") ?: "DEFAULT")

    val expressiveShapePreset: StateFlow<String> get() = _expressiveShapePreset.asStateFlow()
    fun setExpressiveShapePreset(value: String) {
        _expressiveShapePreset.value = value
        prefs.edit { putString(AppSettings.KEY_EXPRESSIVE_SHAPE_PRESET, value) }
    }
    internal val _expressiveShapeSongArt = MutableStateFlow(prefs.getString(AppSettings.KEY_EXPRESSIVE_SHAPE_SONG_ART, "CLOVER_8_LEAF") ?: "CLOVER_8_LEAF")

    val expressiveShapeSongArt: StateFlow<String> get() = _expressiveShapeSongArt.asStateFlow()
    fun setExpressiveShapeSongArt(value: String) {
        _expressiveShapeSongArt.value = value
        prefs.edit { putString(AppSettings.KEY_EXPRESSIVE_SHAPE_SONG_ART, value) }
        if (_expressiveShapePreset.value != "CUSTOM") {
            setExpressiveShapePreset("CUSTOM")
        }
    }
    internal val _expressiveShapesEnabled = MutableStateFlow(prefs.getBoolean(AppSettings.KEY_EXPRESSIVE_SHAPES_ENABLED, true))

    val expressiveShapesEnabled: StateFlow<Boolean> get() = _expressiveShapesEnabled.asStateFlow()
    fun setExpressiveShapesEnabled(value: Boolean) {
        _expressiveShapesEnabled.value = value
        prefs.edit { putBoolean(AppSettings.KEY_EXPRESSIVE_SHAPES_ENABLED, value) }
    }
    internal val _gestureArtworkDoubleTap = MutableStateFlow(prefs.getBoolean(AppSettings.KEY_GESTURE_ARTWORK_DOUBLE_TAP, true))

    val gestureArtworkDoubleTap: StateFlow<Boolean> get() = _gestureArtworkDoubleTap.asStateFlow()
    fun setGestureArtworkDoubleTap(value: Boolean) {
        _gestureArtworkDoubleTap.value = value
        prefs.edit { putBoolean(AppSettings.KEY_GESTURE_ARTWORK_DOUBLE_TAP, value) }
    }
    internal val _gestureArtworkSingleTap = MutableStateFlow(prefs.getBoolean(AppSettings.KEY_GESTURE_ARTWORK_SINGLE_TAP, true))

    val gestureArtworkSingleTap: StateFlow<Boolean> get() = _gestureArtworkSingleTap.asStateFlow()
    fun setGestureArtworkSingleTap(value: Boolean) {
        _gestureArtworkSingleTap.value = value
        prefs.edit { putBoolean(AppSettings.KEY_GESTURE_ARTWORK_SINGLE_TAP, value) }
    }
    internal val _gestureLibrarySwipeTabs = MutableStateFlow(prefs.getBoolean(AppSettings.KEY_GESTURE_LIBRARY_SWIPE_TABS, true))

    val gestureLibrarySwipeTabs: StateFlow<Boolean> get() = _gestureLibrarySwipeTabs.asStateFlow()
    fun setGestureLibrarySwipeTabs(value: Boolean) {
        _gestureLibrarySwipeTabs.value = value
        prefs.edit { putBoolean(AppSettings.KEY_GESTURE_LIBRARY_SWIPE_TABS, value) }
    }
    internal val _gesturePlayerSwipeDismiss = MutableStateFlow(prefs.getBoolean(AppSettings.KEY_GESTURE_PLAYER_SWIPE_DISMISS, true))

    val gesturePlayerSwipeDismiss: StateFlow<Boolean> get() = _gesturePlayerSwipeDismiss.asStateFlow()
    fun setGesturePlayerSwipeDismiss(value: Boolean) {
        _gesturePlayerSwipeDismiss.value = value
        prefs.edit { putBoolean(AppSettings.KEY_GESTURE_PLAYER_SWIPE_DISMISS, value) }
    }
    internal val _gesturePlayerSwipeTracks = MutableStateFlow(prefs.getBoolean(AppSettings.KEY_GESTURE_PLAYER_SWIPE_TRACKS, true))

    val gesturePlayerSwipeTracks: StateFlow<Boolean> get() = _gesturePlayerSwipeTracks.asStateFlow()
    fun setGesturePlayerSwipeTracks(value: Boolean) {
        _gesturePlayerSwipeTracks.value = value
        prefs.edit { putBoolean(AppSettings.KEY_GESTURE_PLAYER_SWIPE_TRACKS, value) }
    }
    internal val _gestureQueueSwipeToRemove = MutableStateFlow(prefs.getBoolean(AppSettings.KEY_GESTURE_QUEUE_SWIPE_TO_REMOVE, true))

    val gestureQueueSwipeToRemove: StateFlow<Boolean> get() = _gestureQueueSwipeToRemove.asStateFlow()
    fun setGestureQueueSwipeToRemove(value: Boolean) {
        _gestureQueueSwipeToRemove.value = value
        prefs.edit { putBoolean(AppSettings.KEY_GESTURE_QUEUE_SWIPE_TO_REMOVE, value) }
    }
    internal val _homeAppIconVisibility = MutableStateFlow(prefs.getInt(AppSettings.KEY_HOME_APP_ICON_VISIBILITY, 1))

    val homeAppIconVisibility: StateFlow<Int> get() = _homeAppIconVisibility.asStateFlow()
    fun setHomeAppIconVisibility(value: Int) {
        _homeAppIconVisibility.value = value
        prefs.edit { putInt(AppSettings.KEY_HOME_APP_ICON_VISIBILITY, value) }
    }
    internal val _homeArtistsCount = MutableStateFlow(prefs.getInt(AppSettings.KEY_HOME_ARTISTS_COUNT, 10))

    val homeArtistsCount: StateFlow<Int> get() = _homeArtistsCount.asStateFlow()
    fun setHomeArtistsCount(value: Int) {
        if (value in 4..20) {
            _homeArtistsCount.value = value
            prefs.edit { putInt(AppSettings.KEY_HOME_ARTISTS_COUNT, value) }
        }
    }
    internal val _homeDiscoverAutoScroll = MutableStateFlow(prefs.getBoolean(AppSettings.KEY_HOME_DISCOVER_AUTO_SCROLL, true))

    val homeDiscoverAutoScroll: StateFlow<Boolean> get() = _homeDiscoverAutoScroll.asStateFlow()
    fun setHomeDiscoverAutoScroll(value: Boolean) {
        _homeDiscoverAutoScroll.value = value
        prefs.edit { putBoolean(AppSettings.KEY_HOME_DISCOVER_AUTO_SCROLL, value) }
    }
    internal val _homeDiscoverAutoScrollInterval = MutableStateFlow(prefs.getInt(AppSettings.KEY_HOME_DISCOVER_AUTO_SCROLL_INTERVAL, 5))

    val homeDiscoverAutoScrollInterval: StateFlow<Int> get() = _homeDiscoverAutoScrollInterval.asStateFlow()
    fun setHomeDiscoverAutoScrollInterval(value: Int) {
        if (value in 2..15) {
            _homeDiscoverAutoScrollInterval.value = value
            prefs.edit { putInt(AppSettings.KEY_HOME_DISCOVER_AUTO_SCROLL_INTERVAL, value) }
        }
    }
    internal val _homeDiscoverItemCount = MutableStateFlow(prefs.getInt(AppSettings.KEY_HOME_DISCOVER_ITEM_COUNT, 6))

    val homeDiscoverItemCount: StateFlow<Int> get() = _homeDiscoverItemCount.asStateFlow()
    fun setHomeDiscoverItemCount(value: Int) {
        if (value in 3..12) {
            _homeDiscoverItemCount.value = value
            prefs.edit { putInt(AppSettings.KEY_HOME_DISCOVER_ITEM_COUNT, value) }
        }
    }
    internal val _homeDiscoverShowAlbumName = MutableStateFlow(prefs.getBoolean(AppSettings.KEY_HOME_DISCOVER_SHOW_ALBUM_NAME, true))

    val homeDiscoverShowAlbumName: StateFlow<Boolean> get() = _homeDiscoverShowAlbumName.asStateFlow()
    fun setHomeDiscoverShowAlbumName(value: Boolean) {
        _homeDiscoverShowAlbumName.value = value
        prefs.edit { putBoolean(AppSettings.KEY_HOME_DISCOVER_SHOW_ALBUM_NAME, value) }
    }
    internal val _homeDiscoverShowArtistName = MutableStateFlow(prefs.getBoolean(AppSettings.KEY_HOME_DISCOVER_SHOW_ARTIST_NAME, true))

    val homeDiscoverShowArtistName: StateFlow<Boolean> get() = _homeDiscoverShowArtistName.asStateFlow()
    fun setHomeDiscoverShowArtistName(value: Boolean) {
        _homeDiscoverShowArtistName.value = value
        prefs.edit { putBoolean(AppSettings.KEY_HOME_DISCOVER_SHOW_ARTIST_NAME, value) }
    }
    internal val _homeDiscoverShowGradient = MutableStateFlow(prefs.getBoolean(AppSettings.KEY_HOME_DISCOVER_SHOW_GRADIENT, true))

    val homeDiscoverShowGradient: StateFlow<Boolean> get() = _homeDiscoverShowGradient.asStateFlow()
    fun setHomeDiscoverShowGradient(value: Boolean) {
        _homeDiscoverShowGradient.value = value
        prefs.edit { putBoolean(AppSettings.KEY_HOME_DISCOVER_SHOW_GRADIENT, value) }
    }
    internal val _homeDiscoverShowPlayButton = MutableStateFlow(prefs.getBoolean(AppSettings.KEY_HOME_DISCOVER_SHOW_PLAY_BUTTON, true))

    val homeDiscoverShowPlayButton: StateFlow<Boolean> get() = _homeDiscoverShowPlayButton.asStateFlow()
    fun setHomeDiscoverShowPlayButton(value: Boolean) {
        _homeDiscoverShowPlayButton.value = value
        prefs.edit { putBoolean(AppSettings.KEY_HOME_DISCOVER_SHOW_PLAY_BUTTON, value) }
    }
    internal val _homeDiscoverShowYear = MutableStateFlow(prefs.getBoolean(AppSettings.KEY_HOME_DISCOVER_SHOW_YEAR, true))

    val homeDiscoverShowYear: StateFlow<Boolean> get() = _homeDiscoverShowYear.asStateFlow()
    fun setHomeDiscoverShowYear(value: Boolean) {
        _homeDiscoverShowYear.value = value
        prefs.edit { putBoolean(AppSettings.KEY_HOME_DISCOVER_SHOW_YEAR, value) }
    }
    internal val _homeHeaderDisplayMode = MutableStateFlow(prefs.getInt(AppSettings.KEY_HOME_HEADER_DISPLAY_MODE, 1))

    val homeHeaderDisplayMode: StateFlow<Int> get() = _homeHeaderDisplayMode.asStateFlow()
    fun setHomeHeaderDisplayMode(value: Int) {
        _homeHeaderDisplayMode.value = value
        prefs.edit { putInt(AppSettings.KEY_HOME_HEADER_DISPLAY_MODE, value) }
    }
    internal val _homeNewReleasesCount = MutableStateFlow(prefs.getInt(AppSettings.KEY_HOME_NEW_RELEASES_COUNT, 10))

    val homeNewReleasesCount: StateFlow<Int> get() = _homeNewReleasesCount.asStateFlow()
    fun setHomeNewReleasesCount(value: Int) {
        if (value in 4..20) {
            _homeNewReleasesCount.value = value
            prefs.edit { putInt(AppSettings.KEY_HOME_NEW_RELEASES_COUNT, value) }
        }
    }
    internal val _homeRecentlyAddedCount = MutableStateFlow(prefs.getInt(AppSettings.KEY_HOME_RECENTLY_ADDED_COUNT, 10))

    val homeRecentlyAddedCount: StateFlow<Int> get() = _homeRecentlyAddedCount.asStateFlow()
    fun setHomeRecentlyAddedCount(value: Int) {
        if (value in 4..20) {
            _homeRecentlyAddedCount.value = value
            prefs.edit { putInt(AppSettings.KEY_HOME_RECENTLY_ADDED_COUNT, value) }
        }
    }
    internal val _homeRecentlyPlayedCount = MutableStateFlow(prefs.getInt(AppSettings.KEY_HOME_RECENTLY_PLAYED_COUNT, 6))

    val homeRecentlyPlayedCount: StateFlow<Int> get() = _homeRecentlyPlayedCount.asStateFlow()
    fun setHomeRecentlyPlayedCount(value: Int) {
        if (value in 3..12) {
            _homeRecentlyPlayedCount.value = value
            prefs.edit { putInt(AppSettings.KEY_HOME_RECENTLY_PLAYED_COUNT, value) }
        }
    }
    internal val _homeRecommendedCount = MutableStateFlow(prefs.getInt(AppSettings.KEY_HOME_RECOMMENDED_COUNT, 4))

    val homeRecommendedCount: StateFlow<Int> get() = _homeRecommendedCount.asStateFlow()
    fun setHomeRecommendedCount(value: Int) {
        if (value in 2..8) {
            _homeRecommendedCount.value = value
            prefs.edit { putInt(AppSettings.KEY_HOME_RECOMMENDED_COUNT, value) }
        }
    }
    internal val _homeSectionOrder = MutableStateFlow(
        prefs.getString(AppSettings.KEY_HOME_SECTION_ORDER, null)
            ?.split(",")
            ?.map(String::trim)
            ?.filter { it.isNotBlank() && it != "GREETING" }
            ?.takeIf { it.isNotEmpty() }
            ?: defaultHomeSectionOrder
    )

    val homeSectionOrder: StateFlow<List<String>> get() = _homeSectionOrder.asStateFlow()
    fun setHomeSectionOrder(value: List<String>) {
        _homeSectionOrder.value = value
        prefs.edit { putString(AppSettings.KEY_HOME_SECTION_ORDER, value.joinToString(",")) }
    }
    internal val _homeShowAppIcon = MutableStateFlow(prefs.getBoolean(AppSettings.KEY_HOME_SHOW_APP_ICON, false))

    val homeShowAppIcon: StateFlow<Boolean> get() = _homeShowAppIcon.asStateFlow()
    fun setHomeShowAppIcon(value: Boolean) {
        _homeShowAppIcon.value = value
        prefs.edit { putBoolean(AppSettings.KEY_HOME_SHOW_APP_ICON, value) }
    }
    internal val _homeShowArtists = MutableStateFlow(prefs.getBoolean(AppSettings.KEY_HOME_SHOW_ARTISTS, true))

    val homeShowArtists: StateFlow<Boolean> get() = _homeShowArtists.asStateFlow()
    fun setHomeShowArtists(value: Boolean) {
        _homeShowArtists.value = value
        prefs.edit { putBoolean(AppSettings.KEY_HOME_SHOW_ARTISTS, value) }
    }
    internal val _homeShowDiscoverCarousel = MutableStateFlow(prefs.getBoolean(AppSettings.KEY_HOME_SHOW_DISCOVER_CAROUSEL, true))

    val homeShowDiscoverCarousel: StateFlow<Boolean> get() = _homeShowDiscoverCarousel.asStateFlow()
    fun setHomeShowDiscoverCarousel(value: Boolean) {
        _homeShowDiscoverCarousel.value = value
        prefs.edit { putBoolean(AppSettings.KEY_HOME_SHOW_DISCOVER_CAROUSEL, value) }
    }
    internal val _homeShowListeningStats = MutableStateFlow(prefs.getBoolean(AppSettings.KEY_HOME_SHOW_LISTENING_STATS, true))

    val homeShowListeningStats: StateFlow<Boolean> get() = _homeShowListeningStats.asStateFlow()
    fun setHomeShowListeningStats(value: Boolean) {
        _homeShowListeningStats.value = value
        prefs.edit { putBoolean(AppSettings.KEY_HOME_SHOW_LISTENING_STATS, value) }
    }
    internal val _homeShowNewReleases = MutableStateFlow(prefs.getBoolean(AppSettings.KEY_HOME_SHOW_NEW_RELEASES, true))

    val homeShowNewReleases: StateFlow<Boolean> get() = _homeShowNewReleases.asStateFlow()
    fun setHomeShowNewReleases(value: Boolean) {
        _homeShowNewReleases.value = value
        prefs.edit { putBoolean(AppSettings.KEY_HOME_SHOW_NEW_RELEASES, value) }
    }
    internal val _homeShowRecentlyAdded = MutableStateFlow(prefs.getBoolean(AppSettings.KEY_HOME_SHOW_RECENTLY_ADDED, true))

    val homeShowRecentlyAdded: StateFlow<Boolean> get() = _homeShowRecentlyAdded.asStateFlow()
    fun setHomeShowRecentlyAdded(value: Boolean) {
        _homeShowRecentlyAdded.value = value
        prefs.edit { putBoolean(AppSettings.KEY_HOME_SHOW_RECENTLY_ADDED, value) }
    }
    internal val _homeShowRecentlyPlayed = MutableStateFlow(prefs.getBoolean(AppSettings.KEY_HOME_SHOW_RECENTLY_PLAYED, true))

    val homeShowRecentlyPlayed: StateFlow<Boolean> get() = _homeShowRecentlyPlayed.asStateFlow()
    fun setHomeShowRecentlyPlayed(value: Boolean) {
        _homeShowRecentlyPlayed.value = value
        prefs.edit { putBoolean(AppSettings.KEY_HOME_SHOW_RECENTLY_PLAYED, value) }
    }
    internal val _homeShowRecommended = MutableStateFlow(prefs.getBoolean(AppSettings.KEY_HOME_SHOW_RECOMMENDED, true))

    val homeShowRecommended: StateFlow<Boolean> get() = _homeShowRecommended.asStateFlow()
    fun setHomeShowRecommended(value: Boolean) {
        _homeShowRecommended.value = value
        prefs.edit { putBoolean(AppSettings.KEY_HOME_SHOW_RECOMMENDED, value) }
    }
    internal val _homeShowRhythmGuard = MutableStateFlow(
        prefs.getBoolean(AppSettings.KEY_HOME_SHOW_RHYTHM_GUARD, true)
    )

    val homeShowRhythmGuard: StateFlow<Boolean> get() = _homeShowRhythmGuard.asStateFlow()
    fun setHomeShowRhythmGuard(value: Boolean) {
        _homeShowRhythmGuard.value = value
        prefs.edit { putBoolean(AppSettings.KEY_HOME_SHOW_RHYTHM_GUARD, value) }
    }
    internal val _miniPlayerAlwaysShowTablet = MutableStateFlow(prefs.getBoolean(AppSettings.KEY_MINIPLAYER_ALWAYS_SHOW_TABLET, false))

    val miniPlayerAlwaysShowTablet: StateFlow<Boolean> get() = _miniPlayerAlwaysShowTablet.asStateFlow()
    fun setMiniPlayerAlwaysShowTablet(value: Boolean) {
        _miniPlayerAlwaysShowTablet.value = value
        prefs.edit { putBoolean(AppSettings.KEY_MINIPLAYER_ALWAYS_SHOW_TABLET, value) }
    }
    internal val _miniPlayerArtworkSize = MutableStateFlow(prefs.getInt(AppSettings.KEY_MINIPLAYER_ARTWORK_SIZE, 56))

    val miniPlayerArtworkSize: StateFlow<Int> get() = _miniPlayerArtworkSize.asStateFlow()
    fun setMiniPlayerArtworkSize(value: Int) {
        _miniPlayerArtworkSize.value = value
        prefs.edit { putInt(AppSettings.KEY_MINIPLAYER_ARTWORK_SIZE, value) }
    }
    internal val _miniPlayerCornerRadius = MutableStateFlow(prefs.getInt(AppSettings.KEY_MINIPLAYER_CORNER_RADIUS, 14))

    val miniPlayerCornerRadius: StateFlow<Int> get() = _miniPlayerCornerRadius.asStateFlow()
    fun setMiniPlayerCornerRadius(value: Int) {
        _miniPlayerCornerRadius.value = value
        prefs.edit { putInt(AppSettings.KEY_MINIPLAYER_CORNER_RADIUS, value) }
    }
    internal val _miniPlayerProgressStyle = MutableStateFlow(prefs.getString(AppSettings.KEY_MINIPLAYER_PROGRESS_STYLE, "NORMAL") ?: "NORMAL")

    val miniPlayerProgressStyle: StateFlow<String> get() = _miniPlayerProgressStyle.asStateFlow()
    fun setMiniPlayerProgressStyle(value: String) {
        _miniPlayerProgressStyle.value = value
        prefs.edit { putString(AppSettings.KEY_MINIPLAYER_PROGRESS_STYLE, value) }
    }
    internal val _miniPlayerShowArtwork = MutableStateFlow(prefs.getBoolean(AppSettings.KEY_MINIPLAYER_SHOW_ARTWORK, true))

    val miniPlayerShowArtwork: StateFlow<Boolean> get() = _miniPlayerShowArtwork.asStateFlow()
    fun setMiniPlayerShowArtwork(value: Boolean) {
        _miniPlayerShowArtwork.value = value
        prefs.edit { putBoolean(AppSettings.KEY_MINIPLAYER_SHOW_ARTWORK, value) }
    }
    internal val _miniPlayerShowProgress = MutableStateFlow(prefs.getBoolean(AppSettings.KEY_MINIPLAYER_SHOW_PROGRESS, true))

    val miniPlayerShowProgress: StateFlow<Boolean> get() = _miniPlayerShowProgress.asStateFlow()
    fun setMiniPlayerShowProgress(value: Boolean) {
        _miniPlayerShowProgress.value = value
        prefs.edit { putBoolean(AppSettings.KEY_MINIPLAYER_SHOW_PROGRESS, value) }
    }
    internal val _miniPlayerShowTime = MutableStateFlow(prefs.getBoolean(AppSettings.KEY_MINIPLAYER_SHOW_TIME, true))

    val miniPlayerShowTime: StateFlow<Boolean> get() = _miniPlayerShowTime.asStateFlow()
    fun setMiniPlayerShowTime(value: Boolean) {
        _miniPlayerShowTime.value = value
        prefs.edit { putBoolean(AppSettings.KEY_MINIPLAYER_SHOW_TIME, value) }
    }
    internal val _miniPlayerSwipeDismiss = MutableStateFlow(prefs.getBoolean(AppSettings.KEY_MINIPLAYER_SWIPE_DISMISS, true))

    val miniPlayerSwipeDismiss: StateFlow<Boolean> get() = _miniPlayerSwipeDismiss.asStateFlow()
    fun setMiniPlayerSwipeDismiss(value: Boolean) {
        _miniPlayerSwipeDismiss.value = value
        prefs.edit { putBoolean(AppSettings.KEY_MINIPLAYER_SWIPE_DISMISS, value) }
    }
    internal val _miniPlayerSwipeGestures = MutableStateFlow(prefs.getBoolean(AppSettings.KEY_MINIPLAYER_SWIPE_GESTURES, true))

    val miniPlayerSwipeGestures: StateFlow<Boolean> get() = _miniPlayerSwipeGestures.asStateFlow()
    fun setMiniPlayerSwipeGestures(value: Boolean) {
        _miniPlayerSwipeGestures.value = value
        prefs.edit { putBoolean(AppSettings.KEY_MINIPLAYER_SWIPE_GESTURES, value) }
    }
    internal val _miniPlayerSwipeTracks = MutableStateFlow(prefs.getBoolean(AppSettings.KEY_MINIPLAYER_SWIPE_TRACKS, true))

    val miniPlayerSwipeTracks: StateFlow<Boolean> get() = _miniPlayerSwipeTracks.asStateFlow()
    fun setMiniPlayerSwipeTracks(value: Boolean) {
        _miniPlayerSwipeTracks.value = value
        prefs.edit { putBoolean(AppSettings.KEY_MINIPLAYER_SWIPE_TRACKS, value) }
    }
    internal val _miniPlayerThemeId = MutableStateFlow(prefs.getString(AppSettings.KEY_MINI_PLAYER_THEME_ID, "EXPRESSIVE") ?: "EXPRESSIVE")

    val miniPlayerThemeId: StateFlow<String> get() = _miniPlayerThemeId.asStateFlow()
    fun setMiniPlayerThemeId(themeId: String) {
        prefs.edit { putString(AppSettings.KEY_MINI_PLAYER_THEME_ID, themeId) }
        _miniPlayerThemeId.value = themeId
    }
    internal val _playerAccentBackgroundEnabled = MutableStateFlow(prefs.getBoolean(AppSettings.KEY_PLAYER_ACCENT_BACKGROUND_ENABLED, true))

    val playerAccentBackgroundEnabled: StateFlow<Boolean> get() = _playerAccentBackgroundEnabled.asStateFlow()
    fun setPlayerAccentBackgroundEnabled(enabled: Boolean) {
        val disableAmbient = enabled && _playerAmbientBackdropEnabled.value
        prefs.edit {
            putBoolean(AppSettings.KEY_PLAYER_ACCENT_BACKGROUND_ENABLED, enabled)
            if (disableAmbient) putBoolean(AppSettings.KEY_PLAYER_AMBIENT_BACKDROP_ENABLED, false)
        }
        _playerAccentBackgroundEnabled.value = enabled
        // Ambient and accent backgrounds are mutually exclusive — enabling one disables the other.
        if (disableAmbient) _playerAmbientBackdropEnabled.value = false
    }
    internal val _playerAmbientBackdropEnabled = MutableStateFlow(prefs.getBoolean(AppSettings.KEY_PLAYER_AMBIENT_BACKDROP_ENABLED, false))

    val playerAmbientBackdropEnabled: StateFlow<Boolean> get() = _playerAmbientBackdropEnabled.asStateFlow()
    fun setPlayerAmbientBackdropEnabled(enabled: Boolean) {
        val disableAccent = enabled && _playerAccentBackgroundEnabled.value
        prefs.edit {
            putBoolean(AppSettings.KEY_PLAYER_AMBIENT_BACKDROP_ENABLED, enabled)
            if (disableAccent) putBoolean(AppSettings.KEY_PLAYER_ACCENT_BACKGROUND_ENABLED, false)
        }
        _playerAmbientBackdropEnabled.value = enabled
        // Ambient and accent backgrounds are mutually exclusive — enabling one disables the other.
        if (disableAccent) _playerAccentBackgroundEnabled.value = false
    }
    internal val _playerAmbientBackdropIntensity = MutableStateFlow(prefs.getFloat(AppSettings.KEY_PLAYER_AMBIENT_BACKDROP_INTENSITY, 0.85f))

    val playerAmbientBackdropIntensity: StateFlow<Float> get() = _playerAmbientBackdropIntensity.asStateFlow()
    fun setPlayerAmbientBackdropIntensity(value: Float) {
        _playerAmbientBackdropIntensity.value = value.coerceIn(0f, 1f)
        prefs.edit { putFloat(AppSettings.KEY_PLAYER_AMBIENT_BACKDROP_INTENSITY, _playerAmbientBackdropIntensity.value) }
    }
    internal val _playerAmbientInfiniteZoom = MutableStateFlow(prefs.getBoolean(AppSettings.KEY_PLAYER_AMBIENT_INFINITE_ZOOM, true))

    val playerAmbientInfiniteZoom: StateFlow<Boolean> get() = _playerAmbientInfiniteZoom.asStateFlow()
    fun setPlayerAmbientInfiniteZoom(enabled: Boolean) {
        _playerAmbientInfiniteZoom.value = enabled
        prefs.edit { putBoolean(AppSettings.KEY_PLAYER_AMBIENT_INFINITE_ZOOM, enabled) }
    }
    internal val _playerArtworkCornerRadius = MutableStateFlow(prefs.getInt(AppSettings.KEY_PLAYER_ARTWORK_CORNER_RADIUS, 28))

    val playerArtworkCornerRadius: StateFlow<Int> get() = _playerArtworkCornerRadius.asStateFlow()
    fun setPlayerArtworkCornerRadius(value: Int) {
        _playerArtworkCornerRadius.value = value.coerceIn(0, 40)
        prefs.edit { putInt(AppSettings.KEY_PLAYER_ARTWORK_CORNER_RADIUS, _playerArtworkCornerRadius.value) }
    }
    internal val _playerChipOrder = MutableStateFlow(
        prefs.getString(AppSettings.KEY_PLAYER_CHIP_ORDER, null)
            ?.split(",")
            ?.filter { it.isNotBlank() }
            ?.map { if (it == "PITCH" || it == "SPEED_PITCH") "SPEED" else it }
            ?.distinct()
            ?.filter { it in defaultChipOrder }
            ?.takeIf { it.isNotEmpty() }
            ?.let { existingChips ->
                var updated = existingChips
                if (!updated.contains("SHARE")) {
                    updated = updated + "SHARE"
                }
                updated
            }
            ?: defaultChipOrder
    )

    val playerChipOrder: StateFlow<List<String>> get() = _playerChipOrder.asStateFlow()
    fun setPlayerChipOrder(chipOrder: List<String>) {
        val sanitizedOrder = chipOrder.filter { it in defaultChipOrder }
        val orderString = sanitizedOrder.joinToString(",")
        prefs.edit { putString(AppSettings.KEY_PLAYER_CHIP_ORDER, orderString) }
        _playerChipOrder.value = sanitizedOrder
    }
    internal val _playerGlassIntensity = MutableStateFlow(prefs.getFloat(AppSettings.KEY_PLAYER_GLASS_INTENSITY, 1.0f))

    val playerGlassIntensity: StateFlow<Float> get() = _playerGlassIntensity.asStateFlow()
    fun setPlayerGlassIntensity(value: Float) {
        _playerGlassIntensity.value = value
        prefs.edit { putFloat(AppSettings.KEY_PLAYER_GLASS_INTENSITY, value) }
    }
    internal val _playerLyricsAlignment = MutableStateFlow(prefs.getString(AppSettings.KEY_PLAYER_LYRICS_ALIGNMENT, "CENTER") ?: "CENTER")

    val playerLyricsAlignment: StateFlow<String> get() = _playerLyricsAlignment.asStateFlow()
    fun setPlayerLyricsAlignment(value: String) {
        _playerLyricsAlignment.value = value
        prefs.edit { putString(AppSettings.KEY_PLAYER_LYRICS_ALIGNMENT, value) }
    }
    internal val _playerLyricsTextSize = MutableStateFlow(prefs.getFloat(AppSettings.KEY_PLAYER_LYRICS_TEXT_SIZE, 1.0f))

    val playerLyricsTextSize: StateFlow<Float> get() = _playerLyricsTextSize.asStateFlow()
    fun setPlayerLyricsTextSize(value: Float) {
        _playerLyricsTextSize.value = value.coerceIn(0.5f, 2.0f)
        prefs.edit { putFloat(AppSettings.KEY_PLAYER_LYRICS_TEXT_SIZE, _playerLyricsTextSize.value) }
    }
    internal val _playerLyricsTransition = MutableStateFlow(prefs.getInt(AppSettings.KEY_PLAYER_LYRICS_TRANSITION, 2)) // 2 = Scale

    val playerLyricsTransition: StateFlow<Int> get() = _playerLyricsTransition.asStateFlow()
    fun setPlayerLyricsTransition(value: Int) {
        _playerLyricsTransition.value = value
        prefs.edit { putInt(AppSettings.KEY_PLAYER_LYRICS_TRANSITION, value) }
    }
    internal val _playerMergeControlsToBottom = MutableStateFlow(prefs.getBoolean(AppSettings.KEY_PLAYER_MERGE_CONTROLS_TO_BOTTOM, true))

    val playerMergeControlsToBottom: StateFlow<Boolean> get() = _playerMergeControlsToBottom.asStateFlow()
    fun setPlayerMergeControlsToBottom(enabled: Boolean) {
        prefs.edit { putBoolean(AppSettings.KEY_PLAYER_MERGE_CONTROLS_TO_BOTTOM, enabled) }
        _playerMergeControlsToBottom.value = enabled
    }
    internal val _playerProgressStyle = MutableStateFlow(prefs.getString(AppSettings.KEY_PLAYER_PROGRESS_STYLE, "WAVY") ?: "WAVY")

    val playerProgressStyle: StateFlow<String> get() = _playerProgressStyle.asStateFlow()
    fun setPlayerProgressStyle(value: String) {
        _playerProgressStyle.value = value
        prefs.edit { putString(AppSettings.KEY_PLAYER_PROGRESS_STYLE, value) }
    }
    internal val _playerProgressThumbRotate = MutableStateFlow(prefs.getBoolean(AppSettings.KEY_PLAYER_PROGRESS_THUMB_ROTATE, false))

    val playerProgressThumbRotate: StateFlow<Boolean> get() = _playerProgressThumbRotate.asStateFlow()
    fun setPlayerProgressThumbRotate(value: Boolean) {
        _playerProgressThumbRotate.value = value
        prefs.edit { putBoolean(AppSettings.KEY_PLAYER_PROGRESS_THUMB_ROTATE, value) }
    }
    internal val _playerProgressThumbStyle = MutableStateFlow(
        migrateThumbStyle(prefs.getString(AppSettings.KEY_PLAYER_PROGRESS_THUMB_STYLE, "DEFAULT") ?: "DEFAULT")
    )

    val playerProgressThumbStyle: StateFlow<String> get() = _playerProgressThumbStyle.asStateFlow()
    fun setPlayerProgressThumbStyle(value: String) {
        _playerProgressThumbStyle.value = value
        prefs.edit { putString(AppSettings.KEY_PLAYER_PROGRESS_THUMB_STYLE, value) }
    }
    internal val _playerShowArtBelowLyrics = MutableStateFlow(prefs.getBoolean(AppSettings.KEY_PLAYER_SHOW_ART_BELOW_LYRICS, true))

    val playerShowArtBelowLyrics: StateFlow<Boolean> get() = _playerShowArtBelowLyrics.asStateFlow()
    fun setPlayerShowArtBelowLyrics(value: Boolean) {
        _playerShowArtBelowLyrics.value = value
        prefs.edit { putBoolean(AppSettings.KEY_PLAYER_SHOW_ART_BELOW_LYRICS, value) }
    }
    internal val _playerShowAudioQualityBadges = MutableStateFlow(prefs.getBoolean(AppSettings.KEY_PLAYER_SHOW_AUDIO_QUALITY_BADGES, true))

    val playerShowAudioQualityBadges: StateFlow<Boolean> get() = _playerShowAudioQualityBadges.asStateFlow()
    fun setPlayerShowAudioQualityBadges(value: Boolean) {
        _playerShowAudioQualityBadges.value = value
        prefs.edit { putBoolean(AppSettings.KEY_PLAYER_SHOW_AUDIO_QUALITY_BADGES, value) }
    }
    internal val _playerShowGradientOverlay = MutableStateFlow(prefs.getBoolean(AppSettings.KEY_PLAYER_SHOW_GRADIENT_OVERLAY, true))

    val playerShowGradientOverlay: StateFlow<Boolean> get() = _playerShowGradientOverlay.asStateFlow()
    fun setPlayerShowGradientOverlay(value: Boolean) {
        _playerShowGradientOverlay.value = value
        prefs.edit { putBoolean(AppSettings.KEY_PLAYER_SHOW_GRADIENT_OVERLAY, value) }
    }
    internal val _playerShowSeekButtons = MutableStateFlow(prefs.getBoolean(AppSettings.KEY_PLAYER_SHOW_SEEK_BUTTONS, true))

    val playerShowSeekButtons: StateFlow<Boolean> get() = _playerShowSeekButtons.asStateFlow()
    fun setPlayerShowSeekButtons(value: Boolean) {
        _playerShowSeekButtons.value = value
        prefs.edit { putBoolean(AppSettings.KEY_PLAYER_SHOW_SEEK_BUTTONS, value) }
    }
    internal val _playerShowSongInfoOnArtwork = MutableStateFlow(prefs.getBoolean(AppSettings.KEY_PLAYER_SHOW_SONG_INFO_ON_ARTWORK, true))

    val playerShowSongInfoOnArtwork: StateFlow<Boolean> get() = _playerShowSongInfoOnArtwork.asStateFlow()
    fun setPlayerShowSongInfoOnArtwork(value: Boolean) {
        _playerShowSongInfoOnArtwork.value = value
        prefs.edit { putBoolean(AppSettings.KEY_PLAYER_SHOW_SONG_INFO_ON_ARTWORK, value) }
    }
    internal val _playerTextAlignment = MutableStateFlow(prefs.getString(AppSettings.KEY_PLAYER_TEXT_ALIGNMENT, "CENTER") ?: "CENTER")

    val playerTextAlignment: StateFlow<String> get() = _playerTextAlignment.asStateFlow()
    fun setPlayerTextAlignment(value: String) {
        _playerTextAlignment.value = value
        prefs.edit { putString(AppSettings.KEY_PLAYER_TEXT_ALIGNMENT, value) }
    }
    internal val _playerThemeId = MutableStateFlow(prefs.getString(AppSettings.KEY_PLAYER_THEME_ID, "EXPRESSIVE") ?: "EXPRESSIVE")

    val playerThemeId: StateFlow<String> get() = _playerThemeId.asStateFlow()
    fun setPlayerThemeId(themeId: String) {
        prefs.edit { putString(AppSettings.KEY_PLAYER_THEME_ID, themeId) }
        _playerThemeId.value = themeId
    }
    internal val _hiddenPlayerChips = MutableStateFlow(
        prefs.getString(AppSettings.KEY_HIDDEN_PLAYER_CHIPS, null)
            ?.split(",")
            ?.filter { it.isNotBlank() }
            ?.toSet()
            ?: emptySet()
    )

    val hiddenPlayerChips: StateFlow<Set<String>> get() = _hiddenPlayerChips.asStateFlow()
    fun setHiddenPlayerChips(hiddenChips: Set<String>) {
        val sanitizedHiddenChips = hiddenChips.filter { it in defaultChipOrder }.toSet()
        val hiddenString = sanitizedHiddenChips.joinToString(",")
        prefs.edit { putString(AppSettings.KEY_HIDDEN_PLAYER_CHIPS, hiddenString) }
        _hiddenPlayerChips.value = sanitizedHiddenChips
    }


    // Player Progress Thumb Style (migrates legacy names to the M3-native set)
    private fun migrateThumbStyle(value: String): String = when (value) {
        "GLOW", "ARROW" -> "DEFAULT"
        "LINE" -> "PILL"
        "OUTLINE", "DOT", "RING" -> "CIRCLE"
        else -> value
    }

    fun setExpressiveHiddenBottomButtonsNormal(hidden: Set<String>) {
        val hiddenStr = hidden.joinToString(",")
        prefs.edit { putString(AppSettings.KEY_EXPRESSIVE_HIDDEN_BOTTOM_BUTTONS_NORMAL, hiddenStr) }
        _expressiveHiddenBottomButtonsNormal.value = hidden
    }

    fun setExpressiveHiddenBottomButtonsMerge(hidden: Set<String>) {
        val hiddenStr = hidden.joinToString(",")
        prefs.edit { putString(AppSettings.KEY_EXPRESSIVE_HIDDEN_BOTTOM_BUTTONS_MERGE, hiddenStr) }
        _expressiveHiddenBottomButtonsMerge.value = hidden
    }
}
