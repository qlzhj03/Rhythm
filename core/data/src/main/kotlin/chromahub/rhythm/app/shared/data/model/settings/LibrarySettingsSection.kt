package chromahub.rhythm.app.shared.data.model.settings

import android.content.SharedPreferences
import androidx.core.content.edit
import android.util.Log
import chromahub.rhythm.app.core.common.ArtistSeparator
import chromahub.rhythm.app.core.common.StoragePathUtils
import com.google.gson.Gson
import chromahub.rhythm.app.shared.data.model.AlbumViewType
import com.google.gson.reflect.TypeToken
import chromahub.rhythm.app.core.model.MediaScanMode
import chromahub.rhythm.app.shared.data.model.AppSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
/**
 * Library / scan / folder / artwork settings state and operations
 * (B-phase B2 domain split).
 */
 */
internal class LibrarySettingsSection(
    private val prefs: SharedPreferences,
    private val updateDerived: () -> Unit,
) {


    internal val defaultTabOrder = listOf("SONGS", "LIKED", "PLAYLISTS", "ALBUMS", "ARTISTS", "ALBUM_ARTISTS", "EXPLORER")
    internal val _albumHideAbout = MutableStateFlow(prefs.getBoolean(AppSettings.KEY_ALBUM_HIDE_ABOUT, false))

    val albumHideAbout: StateFlow<Boolean> get() = _albumHideAbout.asStateFlow()
    fun setAlbumHideAbout(value: Boolean) {
        _albumHideAbout.value = value
        prefs.edit { putBoolean(AppSettings.KEY_ALBUM_HIDE_ABOUT, value) }
    }
    internal val _albumSortOrder = MutableStateFlow(prefs.getString(AppSettings.KEY_ALBUM_SORT_ORDER, "TRACK_NUMBER") ?: "TRACK_NUMBER")

    val albumSortOrder: StateFlow<String> get() = _albumSortOrder.asStateFlow()
    fun setAlbumSortOrder(sortOrder: String) {
        prefs.edit { putString(AppSettings.KEY_ALBUM_SORT_ORDER, sortOrder) }
        _albumSortOrder.value = sortOrder
    }
    internal val _albumViewType = MutableStateFlow(
        AlbumViewType.valueOf(prefs.getString(AppSettings.KEY_ALBUM_VIEW_TYPE, AlbumViewType.GRID.name) ?: AlbumViewType.GRID.name)
    )

    val albumViewType: StateFlow<AlbumViewType> get() = _albumViewType.asStateFlow()
    fun setAlbumViewType(viewType: AlbumViewType) {
        prefs.edit { putString(AppSettings.KEY_ALBUM_VIEW_TYPE, viewType.name) }
        _albumViewType.value = viewType
    }
    internal val _allowedFormats = MutableStateFlow(
        prefs.getStringSet(AppSettings.KEY_ALLOWED_FORMATS, AppSettings.defaultAllowedFormats())?.toSet() ?: AppSettings.defaultAllowedFormats()
    )

    val allowedFormats: StateFlow<Set<String>> get() = _allowedFormats.asStateFlow()
    fun setAllowedFormats(formats: Set<String>) {
        val changed = _allowedFormats.value != formats
        prefs.edit { putStringSet(AppSettings.KEY_ALLOWED_FORMATS, formats) }
        _allowedFormats.value = formats
        Log.d("AppSettings", "Allowed formats updated: $formats")
        if (changed) {
            requestFullMediaRescanOnNextLaunch(reason = "allowed_formats_changed")
        }
    }
    fun setArtistSeparatorCacheSignature(signature: String?) {
        prefs.edit {
            if (signature.isNullOrBlank()) {
                remove(AppSettings.KEY_ARTIST_SEPARATOR_CACHE_SIGNATURE)
            } else {
                putString(AppSettings.KEY_ARTIST_SEPARATOR_CACHE_SIGNATURE, signature)
            }
        }
    }
    internal val _artistSeparatorCustomDelimiters = MutableStateFlow<List<String>>(
        prefs.getString(AppSettings.KEY_ARTIST_SEPARATOR_CUSTOM_DELIMITERS, null)?.let {
            ArtistSeparator.parseDelimiters(it)
        } ?: emptyList()
    )

    val artistSeparatorCustomDelimiters: StateFlow<List<String>> get() = _artistSeparatorCustomDelimiters.asStateFlow()
    fun setArtistSeparatorCustomDelimiters(tokens: List<String>) {
        val clean = tokens.map { it.trim() }.filter { it.isNotEmpty() }.distinct()
        val json = if (clean.isEmpty()) "" else ArtistSeparator.serializeDelimiters(clean)
        prefs.edit {
            if (json.isEmpty()) {
                remove(AppSettings.KEY_ARTIST_SEPARATOR_CUSTOM_DELIMITERS)
            } else {
                putString(AppSettings.KEY_ARTIST_SEPARATOR_CUSTOM_DELIMITERS, json)
            }
        }
        _artistSeparatorCustomDelimiters.value = clean
    }
    internal val _artistSeparatorDelimiters = MutableStateFlow(
        prefs.getString(AppSettings.KEY_ARTIST_SEPARATOR_DELIMITERS, AppSettings.DEFAULT_ARTIST_SEPARATOR_DELIMITERS) ?: AppSettings.DEFAULT_ARTIST_SEPARATOR_DELIMITERS
    )

    val artistSeparatorDelimiters: StateFlow<String> get() = _artistSeparatorDelimiters.asStateFlow()
    fun setArtistSeparatorDelimiters(delimiters: String) {
        val sanitized = delimiters.trim().ifEmpty { AppSettings.DEFAULT_ARTIST_SEPARATOR_DELIMITERS }
        prefs.edit { putString(AppSettings.KEY_ARTIST_SEPARATOR_DELIMITERS, sanitized) }
        _artistSeparatorDelimiters.value = sanitized
    }
    internal val _artistSeparatorEnabled = MutableStateFlow(prefs.getBoolean(AppSettings.KEY_ARTIST_SEPARATOR_ENABLED, true))

    val artistSeparatorEnabled: StateFlow<Boolean> get() = _artistSeparatorEnabled.asStateFlow()
    fun setArtistSeparatorEnabled(enabled: Boolean) {
        prefs.edit { putBoolean(AppSettings.KEY_ARTIST_SEPARATOR_ENABLED, enabled) }
        _artistSeparatorEnabled.value = enabled
    }
    internal val _autoFetchArtwork = MutableStateFlow(prefs.getBoolean(AppSettings.KEY_AUTO_FETCH_ARTWORK, true))

    val autoFetchArtwork: StateFlow<Boolean> get() = _autoFetchArtwork.asStateFlow()
    fun setAutoFetchArtwork(enabled: Boolean) {
        prefs.edit { putBoolean(AppSettings.KEY_AUTO_FETCH_ARTWORK, enabled) }
        _autoFetchArtwork.value = enabled
        updateDerived()
    }
    internal val _embeddedArtworkExtractionCompleted = MutableStateFlow(
        prefs.getBoolean(AppSettings.KEY_EMBEDDED_ARTWORK_EXTRACTION_COMPLETED, false)
    )

    val embeddedArtworkExtractionCompleted: StateFlow<Boolean> get() = _embeddedArtworkExtractionCompleted.asStateFlow()
    fun setEmbeddedArtworkExtractionCompleted(completed: Boolean) {
        prefs.edit { putBoolean(AppSettings.KEY_EMBEDDED_ARTWORK_EXTRACTION_COMPLETED, completed) }
        _embeddedArtworkExtractionCompleted.value = completed
    }
    internal val _embeddedArtworkExtractionLosslessStatus = MutableStateFlow(
        prefs.getBoolean(AppSettings.KEY_EMBEDDED_ARTWORK_EXTRACTION_LOSSLESS_STATUS, false)
    )

    val embeddedArtworkExtractionLosslessStatus: StateFlow<Boolean> get() = _embeddedArtworkExtractionLosslessStatus.asStateFlow()
    fun setEmbeddedArtworkExtractionLosslessStatus(lossless: Boolean) {
        prefs.edit { putBoolean(AppSettings.KEY_EMBEDDED_ARTWORK_EXTRACTION_LOSSLESS_STATUS, lossless) }
        _embeddedArtworkExtractionLosslessStatus.value = lossless
    }
    internal val _enableAlbumEditing = MutableStateFlow(prefs.getBoolean(AppSettings.KEY_ENABLE_ALBUM_EDITING, false))

    val enableAlbumEditing: StateFlow<Boolean> get() = _enableAlbumEditing.asStateFlow()
    fun setEnableAlbumEditing(enabled: Boolean) {
        prefs.edit { putBoolean(AppSettings.KEY_ENABLE_ALBUM_EDITING, enabled) }
        _enableAlbumEditing.value = enabled
    }
    internal val _extractedAlbumColors = MutableStateFlow(prefs.getString(AppSettings.KEY_EXTRACTED_ALBUM_COLORS, null))

    val extractedAlbumColors: StateFlow<String?> get() = _extractedAlbumColors.asStateFlow()
    fun setExtractedAlbumColors(colorsJson: String?) {
        prefs.edit { putString(AppSettings.KEY_EXTRACTED_ALBUM_COLORS, colorsJson) }
        _extractedAlbumColors.value = colorsJson
    }
    internal val _genrePreferences = MutableStateFlow<Map<String, Int>>(
        try {
            val json = prefs.getString(AppSettings.KEY_GENRE_PREFERENCES, null)
            if (json != null) {
                Gson().fromJson(json, object : TypeToken<Map<String, Int>>() {}.type)
            } else {
                emptyMap()
            }
        } catch (e: Exception) {
            emptyMap()
        }
    )

    val genrePreferences: StateFlow<Map<String, Int>> get() = _genrePreferences.asStateFlow()
    fun setGenrePreferences(preferences: Map<String, Int>) {
        val json = Gson().toJson(preferences)
        prefs.edit { putString(AppSettings.KEY_GENRE_PREFERENCES, json) }
        _genrePreferences.value = preferences
    }
    internal val _groupByAlbumArtist = MutableStateFlow(false)

    val groupByAlbumArtist: StateFlow<Boolean> get() = _groupByAlbumArtist.asStateFlow()
    fun setGroupByAlbumArtist(enable: Boolean) {
        prefs.edit { putBoolean(AppSettings.KEY_GROUP_BY_ALBUM_ARTIST, false) }
        _groupByAlbumArtist.value = false
    }
    internal val _hiddenLibraryTabs = MutableStateFlow(
        (prefs.getString(AppSettings.KEY_HIDDEN_LIBRARY_TABS, null)
            ?.split(",")
            ?.filter { it.isNotBlank() }
            ?.toSet()
            ?: emptySet()) + if ("ALBUM_ARTISTS" in (prefs.getString(AppSettings.KEY_LIBRARY_TAB_ORDER, null) ?: "")) {
            // User already has an order that includes Album Artists (they enabled it explicitly)
            emptySet()
        } else {
            // Legacy or fresh installs: hide the Album Artists tab by default
            setOf("ALBUM_ARTISTS")
        }
    )

    val hiddenLibraryTabs: StateFlow<Set<String>> get() = _hiddenLibraryTabs.asStateFlow()
    fun setHiddenLibraryTabs(hiddenTabs: Set<String>) {
        val hiddenString = hiddenTabs.joinToString(",")
        prefs.edit { putString(AppSettings.KEY_HIDDEN_LIBRARY_TABS, hiddenString) }
        _hiddenLibraryTabs.value = hiddenTabs
    }
    internal val _includeHiddenWhitelistedMedia = MutableStateFlow(
        prefs.getBoolean(AppSettings.KEY_INCLUDE_HIDDEN_WHITELISTED_MEDIA, true)
    )

    val includeHiddenWhitelistedMedia: StateFlow<Boolean> get() = _includeHiddenWhitelistedMedia.asStateFlow()
    fun setIncludeHiddenWhitelistedMedia(include: Boolean) {
        val changed = _includeHiddenWhitelistedMedia.value != include
        prefs.edit { putBoolean(AppSettings.KEY_INCLUDE_HIDDEN_WHITELISTED_MEDIA, include) }
        _includeHiddenWhitelistedMedia.value = include
        if (changed) {
            requestFullMediaRescanOnNextLaunch(reason = "hidden_nomedia_scan_toggle_changed")
        }
    }
    internal val _initialMediaScanCompleted = MutableStateFlow(prefs.getBoolean(AppSettings.KEY_INITIAL_MEDIA_SCAN_COMPLETED, false))

    val initialMediaScanCompleted: StateFlow<Boolean> get() = _initialMediaScanCompleted.asStateFlow()
    fun setInitialMediaScanCompleted(completed: Boolean) {
        prefs.edit { putBoolean(AppSettings.KEY_INITIAL_MEDIA_SCAN_COMPLETED, completed) }
        _initialMediaScanCompleted.value = completed
    }
    internal val _lastScanDuration = MutableStateFlow(safeLong(AppSettings.KEY_LAST_SCAN_DURATION, 0L))

    val lastScanDuration: StateFlow<Long> get() = _lastScanDuration.asStateFlow()
    fun setLastScanDuration(duration: Long) {
        prefs.edit { putLong(AppSettings.KEY_LAST_SCAN_DURATION, duration) }
        _lastScanDuration.value = duration
    }
    internal val _lastScanTimestamp = MutableStateFlow(safeLong(AppSettings.KEY_LAST_SCAN_TIMESTAMP, 0L))

    val lastScanTimestamp: StateFlow<Long> get() = _lastScanTimestamp.asStateFlow()
    fun setLastScanTimestamp(timestamp: Long) {
        prefs.edit { putLong(AppSettings.KEY_LAST_SCAN_TIMESTAMP, timestamp) }
        _lastScanTimestamp.value = timestamp
    }
    internal val _libraryCombineDiscs = MutableStateFlow(prefs.getBoolean(AppSettings.KEY_LIBRARY_COMBINE_DISCS, false))

    val libraryCombineDiscs: StateFlow<Boolean> get() = _libraryCombineDiscs.asStateFlow()
    fun setLibraryCombineDiscs(enabled: Boolean) {
        prefs.edit { putBoolean(AppSettings.KEY_LIBRARY_COMBINE_DISCS, enabled) }
        _libraryCombineDiscs.value = enabled
    }
    internal val _libraryOperationsNotificationsEnabled = MutableStateFlow(
        prefs.getBoolean(AppSettings.KEY_LIBRARY_OPERATIONS_NOTIFICATIONS_ENABLED, true)
    )

    val libraryOperationsNotificationsEnabled: StateFlow<Boolean> get() = _libraryOperationsNotificationsEnabled.asStateFlow()
    fun setLibraryOperationsNotificationsEnabled(enabled: Boolean) {
        prefs.edit { putBoolean(AppSettings.KEY_LIBRARY_OPERATIONS_NOTIFICATIONS_ENABLED, enabled) }
        _libraryOperationsNotificationsEnabled.value = enabled
    }
    internal val _libraryTabOrder = MutableStateFlow(
        prefs.getString(AppSettings.KEY_LIBRARY_TAB_ORDER, null)
            ?.split(",")
            ?.filter { it.isNotBlank() }
            ?.takeIf { it.isNotEmpty() }
            ?.let { order ->
                val withLiked = if ("LIKED" in order) order
                else {
                    val songsIndex = order.indexOf("SONGS")
                    if (songsIndex >= 0) order.toMutableList().apply { add(songsIndex + 1, "LIKED") }
                    else order + "LIKED"
                }
                if ("ALBUM_ARTISTS" in withLiked) withLiked
                else {
                    val artistsIndex = withLiked.indexOf("ARTISTS")
                    if (artistsIndex >= 0) withLiked.toMutableList().apply { add(artistsIndex + 1, "ALBUM_ARTISTS") }
                    else withLiked + "ALBUM_ARTISTS"
                }
            }
            ?: defaultTabOrder
    )

    val libraryTabOrder: StateFlow<List<String>> get() = _libraryTabOrder.asStateFlow()
    fun setLibraryTabOrder(tabOrder: List<String>) {
        val orderString = tabOrder.joinToString(",")
        prefs.edit { putString(AppSettings.KEY_LIBRARY_TAB_ORDER, orderString) }
        _libraryTabOrder.value = tabOrder
    }
    internal val _losslessArtwork = MutableStateFlow(prefs.getBoolean(AppSettings.KEY_LOSSLESS_ARTWORK, true))

    val losslessArtwork: StateFlow<Boolean> get() = _losslessArtwork.asStateFlow()
    fun setLosslessArtwork(enabled: Boolean) {
        val changed = _losslessArtwork.value != enabled
        prefs.edit { putBoolean(AppSettings.KEY_LOSSLESS_ARTWORK, enabled) }
        _losslessArtwork.value = enabled
        updateDerived()

        // Lossless artwork requires per-song artwork mode to take effect.
        if (enabled && !_preferSongArtwork.value) {
            setPreferSongArtwork(true)
        }
    }
    internal val _mediaScanMode = MutableStateFlow(
        MediaScanMode.fromValue(prefs.getString(AppSettings.KEY_MEDIA_SCAN_MODE, "blacklist") ?: "blacklist")
    )

    val mediaScanMode: StateFlow<MediaScanMode> get() = _mediaScanMode.asStateFlow()
    fun setMediaScanMode(mode: MediaScanMode) {
        val changed = _mediaScanMode.value != mode
        prefs.edit { putString(AppSettings.KEY_MEDIA_SCAN_MODE, mode.value) }
        _mediaScanMode.value = mode
        if (changed) {
            requestFullMediaRescanOnNextLaunch(reason = "media_scan_mode_changed")
        }
    }
    internal val _minimumDuration = MutableStateFlow(safeLong(AppSettings.KEY_MINIMUM_DURATION, 0L)) // 0 = no filter

    val minimumDuration: StateFlow<Long> get() = _minimumDuration.asStateFlow()
    fun setMinimumDuration(duration: Long) {
        val changed = _minimumDuration.value != duration
        prefs.edit { putLong(AppSettings.KEY_MINIMUM_DURATION, duration) }
        _minimumDuration.value = duration
        Log.d("AppSettings", "Minimum duration set to: ${duration}ms")
        if (changed) {
            requestFullMediaRescanOnNextLaunch(reason = "minimum_duration_changed")
        }
    }
    internal val _preferSongArtwork = MutableStateFlow(
        if (prefs.contains(AppSettings.KEY_PREFER_SONG_ARTWORK)) {
            prefs.getBoolean(AppSettings.KEY_PREFER_SONG_ARTWORK, true)
        } else {
            prefs.getBoolean(AppSettings.KEY_IGNORE_MEDIASTORE_COVERS, true)
        }
    )

    val preferSongArtwork: StateFlow<Boolean> get() = _preferSongArtwork.asStateFlow()
    fun setPreferSongArtwork(enabled: Boolean) {
        val changed = _preferSongArtwork.value != enabled
        val disableLossless = !enabled && _losslessArtwork.value
        prefs.edit {
            putBoolean(AppSettings.KEY_PREFER_SONG_ARTWORK, enabled)
            putBoolean(AppSettings.KEY_IGNORE_MEDIASTORE_COVERS, enabled)
            putBoolean(AppSettings.KEY_LOSSLESS_ARTWORK, if (disableLossless) false else _losslessArtwork.value)
        }
        _preferSongArtwork.value = enabled
        if (disableLossless) {
            _losslessArtwork.value = false
        }
        updateDerived()
    }
    internal val _showAlphabetBar = MutableStateFlow(prefs.getBoolean(AppSettings.KEY_SHOW_ALPHABET_BAR, false))

    val showAlphabetBar: StateFlow<Boolean> get() = _showAlphabetBar.asStateFlow()
    fun setShowAlphabetBar(show: Boolean) {
        prefs.edit { putBoolean(AppSettings.KEY_SHOW_ALPHABET_BAR, show) }
        _showAlphabetBar.value = show
    }
    internal val _showLibraryBottomBarAlways = MutableStateFlow(prefs.getBoolean(AppSettings.KEY_SHOW_LIBRARY_BOTTOM_BAR_ALWAYS, false))

    val showLibraryBottomBarAlways: StateFlow<Boolean> get() = _showLibraryBottomBarAlways.asStateFlow()
    fun setShowLibraryBottomBarAlways(enable: Boolean) {
        prefs.edit { putBoolean(AppSettings.KEY_SHOW_LIBRARY_BOTTOM_BAR_ALWAYS, enable) }
        _showLibraryBottomBarAlways.value = enable
    }
    internal val _songsSortOrder = MutableStateFlow(prefs.getString(AppSettings.KEY_SONGS_SORT_ORDER, "TITLE_ASC") ?: "TITLE_ASC")

    val songsSortOrder: StateFlow<String> get() = _songsSortOrder.asStateFlow()
    fun setSongsSortOrder(sortOrder: String) {
        prefs.edit { putString(AppSettings.KEY_SONGS_SORT_ORDER, sortOrder) }
        _songsSortOrder.value = sortOrder
    }
    internal val _uniqueArtists = MutableStateFlow(prefs.getInt(AppSettings.KEY_UNIQUE_ARTISTS, 0))

    val uniqueArtists: StateFlow<Int> get() = _uniqueArtists.asStateFlow()
    fun setUniqueArtists(count: Int) {
        prefs.edit { putInt(AppSettings.KEY_UNIQUE_ARTISTS, count) }
        _uniqueArtists.value = count
    }
    fun resetLibraryTabOrder() {
        prefs.edit { remove(AppSettings.KEY_LIBRARY_TAB_ORDER) }
        _libraryTabOrder.value = defaultTabOrder
    }
    fun requestFullMediaRescanOnNextLaunch(reason: String = "unspecified") {
        prefs.edit {
    putBoolean(AppSettings.KEY_PENDING_FULL_MEDIA_RESCAN, true)
    putBoolean(AppSettings.KEY_INITIAL_MEDIA_SCAN_COMPLETED, false)
    putBoolean(AppSettings.KEY_GENRE_DETECTION_COMPLETED, false)
    putBoolean(AppSettings.KEY_EMBEDDED_ARTWORK_EXTRACTION_COMPLETED, false)
    putBoolean(AppSettings.KEY_EMBEDDED_ARTWORK_EXTRACTION_LOSSLESS_STATUS, false)
    putLong(AppSettings.KEY_LAST_SCAN_TIMESTAMP, 0L)
    putLong(AppSettings.KEY_LAST_SCAN_DURATION, 0L)
}

        _initialMediaScanCompleted.value = false
        _genreDetectionCompleted.value = false
        _audioMetadataExtractionCompleted.value = false
        _embeddedArtworkExtractionCompleted.value = false
        _embeddedArtworkExtractionLosslessStatus.value = false
        _lastScanTimestamp.value = 0L
        _lastScanDuration.value = 0L

        Log.i("AppSettings", "Full media rescan scheduled for next launch (reason=$reason)")
    }
    fun consumePendingFullMediaRescanRequest(): Boolean {
        val pending = prefs.getBoolean(AppSettings.KEY_PENDING_FULL_MEDIA_RESCAN, false)
        if (pending) {
            prefs.edit { putBoolean(AppSettings.KEY_PENDING_FULL_MEDIA_RESCAN, false) }
            Log.i("AppSettings", "Consuming pending full media rescan request")
        }
        return pending
    }
    fun shouldRunEmbeddedArtSelfHeal(): Boolean {
        val lastRun = prefs.getLong(AppSettings.KEY_LAST_EMBEDDED_ART_SELF_HEAL_MS, 0L)
        return System.currentTimeMillis() - lastRun >= 24L * 60 * 60 * 1000
    }
    fun markEmbeddedArtSelfHealDone() {
        prefs.edit { putLong(AppSettings.KEY_LAST_EMBEDDED_ART_SELF_HEAL_MS, System.currentTimeMillis()) }
        Log.i("AppSettings", "Marked embedded artwork self-heal as done")
    }
    fun addToBlacklist(songId: String) {
        val currentList = _blacklistedSongs.value.toMutableList()
        if (!currentList.contains(songId)) {
            currentList.add(songId)
            val json = Gson().toJson(currentList)
            prefs.edit { putString(AppSettings.KEY_BLACKLISTED_SONGS, json) }
            _blacklistedSongs.value = currentList
        }
    }
    fun removeFromBlacklist(songId: String) {
        val currentList = _blacklistedSongs.value.toMutableList()
        if (currentList.remove(songId)) {
            val json = Gson().toJson(currentList)
            prefs.edit { putString(AppSettings.KEY_BLACKLISTED_SONGS, json) }
            _blacklistedSongs.value = currentList
        }
    }
    fun isBlacklisted(songId: String): Boolean {
        return _blacklistedSongs.value.contains(songId)
    }
    fun clearBlacklist() {
        prefs.edit { remove(AppSettings.KEY_BLACKLISTED_SONGS) }
        _blacklistedSongs.value = emptyList()
    }
    fun addFolderToBlacklist(folderPath: String) {
        val normalized = StoragePathUtils.sanitizeAndNormalizePath(folderPath)
        if (normalized.isBlank()) return
        val currentList = _blacklistedFolders.value.toMutableList()
        if (!currentList.contains(normalized)) {
            currentList.add(normalized)
            val json = Gson().toJson(currentList)
            prefs.edit { putString(AppSettings.KEY_BLACKLISTED_FOLDERS, json) }
            _blacklistedFolders.value = currentList
        }
    }
    fun removeFolderFromBlacklist(folderPath: String) {
        val normalized = StoragePathUtils.sanitizeAndNormalizePath(folderPath)
        val currentList = _blacklistedFolders.value.toMutableList()
        if (currentList.remove(normalized) || currentList.remove(folderPath)) {
            val json = Gson().toJson(currentList)
            prefs.edit { putString(AppSettings.KEY_BLACKLISTED_FOLDERS, json) }
            _blacklistedFolders.value = currentList
        }
    }
    fun normalizeStoragePath(path: String): String {
        return StoragePathUtils.sanitizeAndNormalizePath(path)
    }
    private fun isFolderSubdirectoryOrEqual(parent: String, child: String): Boolean {
        return StoragePathUtils.isFolderSubdirectoryOrEqual(parent, child)
    }
    fun isFolderBlacklisted(folderPath: String): Boolean {
        return StoragePathUtils.isPathInFolders(folderPath, _blacklistedFolders.value)
    }
    fun clearFolderBlacklist() {
        prefs.edit { remove(AppSettings.KEY_BLACKLISTED_FOLDERS) }
        _blacklistedFolders.value = emptyList()
    }
    fun removeFolderAndRelatedSongs(folderPath: String, songsInFolder: List<String>) {
        // Remove folder from blacklist
        val currentFolders = _blacklistedFolders.value.toMutableList()
        if (currentFolders.remove(folderPath)) {
            val foldersJson = Gson().toJson(currentFolders)
            prefs.edit { putString(AppSettings.KEY_BLACKLISTED_FOLDERS, foldersJson) }
            _blacklistedFolders.value = currentFolders
        }
        
        // Remove related songs from individual blacklist
        val currentSongs = _blacklistedSongs.value.toMutableList()
        var songsRemoved = false
        songsInFolder.forEach { songId ->
            if (currentSongs.remove(songId)) {
                songsRemoved = true
            }
        }
        
        if (songsRemoved) {
            val songsJson = Gson().toJson(currentSongs)
            prefs.edit { putString(AppSettings.KEY_BLACKLISTED_SONGS, songsJson) }
            _blacklistedSongs.value = currentSongs
        }
    }
    fun addFolderAndOptionalSong(folderPath: String, songId: String?) {
        // Add folder to blacklist
        val currentFolders = _blacklistedFolders.value.toMutableList()
        if (!currentFolders.contains(folderPath)) {
            currentFolders.add(folderPath)
            val foldersJson = Gson().toJson(currentFolders)
            prefs.edit { putString(AppSettings.KEY_BLACKLISTED_FOLDERS, foldersJson) }
            _blacklistedFolders.value = currentFolders
        }
        
        // Optionally add song to individual blacklist for immediate synchronization
        songId?.let { id ->
            val currentSongs = _blacklistedSongs.value.toMutableList()
            if (!currentSongs.contains(id)) {
                currentSongs.add(id)
                val songsJson = Gson().toJson(currentSongs)
                prefs.edit { putString(AppSettings.KEY_BLACKLISTED_SONGS, songsJson) }
                _blacklistedSongs.value = currentSongs
            }
        }
    }
    fun isEffectivelyBlacklisted(songId: String, songPath: String?): Boolean {
        // Check individual song blacklist
        if (_blacklistedSongs.value.contains(songId)) return true
        
        // Check folder blacklist
        if (songPath != null) {
            return _blacklistedFolders.value.any { folderPath ->
                isFolderSubdirectoryOrEqual(folderPath, songPath)
            }
        }
        
        return false
    }
    fun addToWhitelist(songId: String) {
        val currentList = _whitelistedSongs.value.toMutableList()
        if (!currentList.contains(songId)) {
            currentList.add(songId)
            val json = Gson().toJson(currentList)
            prefs.edit { putString(AppSettings.KEY_WHITELISTED_SONGS, json) }
            _whitelistedSongs.value = currentList
        }
    }
    fun removeFromWhitelist(songId: String) {
        val currentList = _whitelistedSongs.value.toMutableList()
        if (currentList.remove(songId)) {
            val json = Gson().toJson(currentList)
            prefs.edit { putString(AppSettings.KEY_WHITELISTED_SONGS, json) }
            _whitelistedSongs.value = currentList
        }
    }
    fun isSongWhitelisted(songId: String): Boolean {
        return _whitelistedSongs.value.contains(songId)
    }
    fun clearWhitelist() {
        prefs.edit { remove(AppSettings.KEY_WHITELISTED_SONGS) }
        _whitelistedSongs.value = emptyList()
    }
    fun addFolderToWhitelist(folderPath: String) {
        val normalized = StoragePathUtils.sanitizeAndNormalizePath(folderPath)
        if (normalized.isBlank()) return
        val currentList = _whitelistedFolders.value.toMutableList()
        if (!currentList.contains(normalized)) {
            currentList.add(normalized)
            val json = Gson().toJson(currentList)
            prefs.edit { putString(AppSettings.KEY_WHITELISTED_FOLDERS, json) }
            _whitelistedFolders.value = currentList
            requestFullMediaRescanOnNextLaunch(reason = "whitelist_folder_added")
        }
    }
    fun removeFolderFromWhitelist(folderPath: String) {
        val normalized = StoragePathUtils.sanitizeAndNormalizePath(folderPath)
        val currentList = _whitelistedFolders.value.toMutableList()
        if (currentList.remove(normalized) || currentList.remove(folderPath)) {
            val json = Gson().toJson(currentList)
            prefs.edit { putString(AppSettings.KEY_WHITELISTED_FOLDERS, json) }
            _whitelistedFolders.value = currentList
            requestFullMediaRescanOnNextLaunch(reason = "whitelist_folder_removed")
        }
    }
    fun isFolderWhitelisted(folderPath: String): Boolean {
        return StoragePathUtils.isPathInFolders(folderPath, _whitelistedFolders.value)
    }
    fun clearFolderWhitelist() {
        val hadValues = _whitelistedFolders.value.isNotEmpty()
        prefs.edit { remove(AppSettings.KEY_WHITELISTED_FOLDERS) }
        _whitelistedFolders.value = emptyList()
        if (hadValues) {
            requestFullMediaRescanOnNextLaunch(reason = "whitelist_folders_cleared")
        }
    }
    fun addFolderToPinned(folderPath: String) {
        val currentList = _pinnedFolders.value.toMutableList()
        if (!currentList.contains(folderPath)) {
            currentList.add(folderPath)
            val json = Gson().toJson(currentList)
            prefs.edit { putString(AppSettings.KEY_PINNED_FOLDERS, json) }
            _pinnedFolders.value = currentList
        }
    }
    fun removeFolderFromPinned(folderPath: String) {
        val currentList = _pinnedFolders.value.toMutableList()
        if (currentList.remove(folderPath)) {
            val json = Gson().toJson(currentList)
            prefs.edit { putString(AppSettings.KEY_PINNED_FOLDERS, json) }
            _pinnedFolders.value = currentList
        }
    }
    fun isFolderPinned(folderPath: String): Boolean {
        return _pinnedFolders.value.contains(folderPath)
    }
    fun clearPinnedFolders() {
        prefs.edit { remove(AppSettings.KEY_PINNED_FOLDERS) }
        _pinnedFolders.value = emptyList()
    }
    fun isEffectivelyWhitelisted(songId: String, songPath: String?): Boolean {
        // If no whitelist exists, all songs are effectively whitelisted
        if (_whitelistedSongs.value.isEmpty() && _whitelistedFolders.value.isEmpty()) {
            return true
        }
        
        // Check individual song whitelist
        if (_whitelistedSongs.value.contains(songId)) return true
        
        // Check folder whitelist
        if (songPath != null) {
            return _whitelistedFolders.value.any { folderPath ->
                isFolderSubdirectoryOrEqual(folderPath, songPath)
            }
        }
        
        return false
    }
    fun getArtistSeparatorCacheSignature(): String? {
        return prefs.getString(AppSettings.KEY_ARTIST_SEPARATOR_CACHE_SIGNATURE, null)
    }
    internal val _whitelistedSongs = MutableStateFlow<List<String>>(
        try {
            val json = prefs.getString(AppSettings.KEY_WHITELISTED_SONGS, null)
            if (json != null) {
                val type = object : com.google.gson.reflect.TypeToken<List<String>>() {}.type
                Gson().fromJson(json, type) ?: emptyList()
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            emptyList()
        }
    )

    val whitelistedSongs: StateFlow<List<String>> get() = _whitelistedSongs.asStateFlow()
    internal val _whitelistedFolders = MutableStateFlow<List<String>>(
        try {
            val json = prefs.getString(AppSettings.KEY_WHITELISTED_FOLDERS, null)
            if (json != null) {
                val type = object : com.google.gson.reflect.TypeToken<List<String>>() {}.type
                val rawList: List<String> = Gson().fromJson(json, type) ?: emptyList()
                val sanitized = rawList.map { StoragePathUtils.sanitizeAndNormalizePath(it) }.filter { it.isNotBlank() }.distinct()
                if (sanitized != rawList) {
                    prefs.edit { putString(AppSettings.KEY_WHITELISTED_FOLDERS, Gson().toJson(sanitized)) }
                }
                sanitized
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            emptyList()
        }
    )

    val whitelistedFolders: StateFlow<List<String>> get() = _whitelistedFolders.asStateFlow()
    internal val _pinnedFolders = MutableStateFlow<List<String>>(
        try {
            val json = prefs.getString(AppSettings.KEY_PINNED_FOLDERS, null)
            if (json != null) {
                Gson().fromJson(json, object : TypeToken<List<String>>() {}.type)
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            emptyList()
        }
    )

    val pinnedFolders: StateFlow<List<String>> get() = _pinnedFolders.asStateFlow()
    internal val _blacklistedSongs = MutableStateFlow<List<String>>(
        try {
            val json = prefs.getString(AppSettings.KEY_BLACKLISTED_SONGS, null)
            if (json != null) {
                Gson().fromJson(json, object : TypeToken<List<String>>() {}.type)
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            emptyList()
        }
    )

    val blacklistedSongs: StateFlow<List<String>> get() = _blacklistedSongs.asStateFlow()
    internal val _blacklistedFolders = MutableStateFlow<List<String>>(
        try {
            val json = prefs.getString(AppSettings.KEY_BLACKLISTED_FOLDERS, null)
            if (json != null) {
                val rawList: List<String> = Gson().fromJson(json, object : TypeToken<List<String>>() {}.type) ?: emptyList()
                val sanitized = rawList.map { StoragePathUtils.sanitizeAndNormalizePath(it) }.filter { it.isNotBlank() }.distinct()
                if (sanitized != rawList) {
                    prefs.edit { putString(AppSettings.KEY_BLACKLISTED_FOLDERS, Gson().toJson(sanitized)) }
                }
                sanitized
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            emptyList()
        }
    )

    val blacklistedFolders: StateFlow<List<String>> get() = _blacklistedFolders.asStateFlow()
    internal val _genreDetectionCompleted = MutableStateFlow(prefs.getBoolean(AppSettings.KEY_GENRE_DETECTION_COMPLETED, false))

    val genreDetectionCompleted: StateFlow<Boolean> get() = _genreDetectionCompleted.asStateFlow()
    fun setGenreDetectionCompleted(completed: Boolean) {
        prefs.edit { putBoolean(AppSettings.KEY_GENRE_DETECTION_COMPLETED, completed) }
        _genreDetectionCompleted.value = completed
    }

    private fun safeLong(key: String, defaultValue: Long): Long {
        val stored = prefs.getLong(key, Long.MIN_VALUE)
        return if (stored == Long.MIN_VALUE) defaultValue else stored
    }

    internal val _audioMetadataExtractionCompleted = MutableStateFlow(prefs.getBoolean(AppSettings.KEY_AUDIO_METADATA_EXTRACTION_COMPLETED, false))
    val audioMetadataExtractionCompleted: StateFlow<Boolean> get() = _audioMetadataExtractionCompleted.asStateFlow()
    fun setAudioMetadataExtractionCompleted(completed: Boolean) {
        prefs.edit { putBoolean(AppSettings.KEY_AUDIO_METADATA_EXTRACTION_COMPLETED, completed) }
        _audioMetadataExtractionCompleted.value = completed
    }
}
