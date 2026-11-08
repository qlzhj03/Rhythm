package com.qlzhj.rhythm.core.data.shared.data.model.settings

import android.content.SharedPreferences
import androidx.core.content.edit
import android.util.Log
import com.google.gson.Gson
import com.qlzhj.rhythm.core.data.shared.data.model.CrashLogEntry
import com.google.gson.reflect.TypeToken
import com.qlzhj.rhythm.core.data.shared.data.model.AppSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
Platform-level settings state (notifications, updates, onboarding, backup, crash logs, battery saver, cache, misc playback-engine toggles), carved out of AppSettings (B-phase B2). Setters that drive WorkManager jobs or other sections stay in the facade; interface override members (integrations/API enables) remain in the facade by design.
 */
internal class PlatformSettingsSection(
    private val prefs: SharedPreferences,
    private val updateDerived: () -> Unit,
) {
    internal val _useCustomNotification = MutableStateFlow(prefs.getBoolean(AppSettings.KEY_USE_CUSTOM_NOTIFICATION, false))

    val useCustomNotification: StateFlow<Boolean> get() = _useCustomNotification.asStateFlow()
    fun setUseCustomNotification(enabled: Boolean) {
        prefs.edit { putBoolean(AppSettings.KEY_USE_CUSTOM_NOTIFICATION, enabled) }
        _useCustomNotification.value = enabled
    }
    internal val _sleepTimerNotificationsEnabled = MutableStateFlow(
        prefs.getBoolean(AppSettings.KEY_SLEEP_TIMER_NOTIFICATIONS_ENABLED, true)
    )

    val sleepTimerNotificationsEnabled: StateFlow<Boolean> get() = _sleepTimerNotificationsEnabled.asStateFlow()
    fun setSleepTimerNotificationsEnabled(enabled: Boolean) {
        prefs.edit { putBoolean(AppSettings.KEY_SLEEP_TIMER_NOTIFICATIONS_ENABLED, enabled) }
        _sleepTimerNotificationsEnabled.value = enabled
    }
    internal val _streamingNotificationsEnabled = MutableStateFlow(
        prefs.getBoolean(AppSettings.KEY_STREAMING_NOTIFICATIONS_ENABLED, true)
    )

    val streamingNotificationsEnabled: StateFlow<Boolean> get() = _streamingNotificationsEnabled.asStateFlow()
    fun setStreamingNotificationsEnabled(enabled: Boolean) {
        prefs.edit { putBoolean(AppSettings.KEY_STREAMING_NOTIFICATIONS_ENABLED, enabled) }
        _streamingNotificationsEnabled.value = enabled
    }
    internal val _rhythmPulseNotificationsEnabled = MutableStateFlow(
        prefs.getBoolean(AppSettings.KEY_RHYTHM_PULSE_NOTIFICATIONS_ENABLED, false)
    )

    val rhythmPulseNotificationsEnabled: StateFlow<Boolean> get() = _rhythmPulseNotificationsEnabled.asStateFlow()
    internal val _rhythmPulseNotificationIntervalHours = MutableStateFlow(
        prefs.getInt(AppSettings.KEY_RHYTHM_PULSE_NOTIFICATION_INTERVAL_HOURS, 24).coerceIn(6, 72)
    )

    val rhythmPulseNotificationIntervalHours: StateFlow<Int> get() = _rhythmPulseNotificationIntervalHours.asStateFlow()
    internal val _updateChannel = MutableStateFlow(prefs.getString(AppSettings.KEY_UPDATE_CHANNEL, "stable") ?: "stable")

    val updateChannel: StateFlow<String> get() = _updateChannel.asStateFlow()
    fun setUpdateChannel(channel: String) {
        prefs.edit { putString(AppSettings.KEY_UPDATE_CHANNEL, channel) }
        _updateChannel.value = channel
    }
    internal val _updateSource = MutableStateFlow(prefs.getString(AppSettings.KEY_UPDATE_SOURCE, "installed") ?: "installed")

    val updateSource: StateFlow<String> get() = _updateSource.asStateFlow()
    fun setUpdateSource(source: String) {
        prefs.edit { putString(AppSettings.KEY_UPDATE_SOURCE, source) }
        _updateSource.value = source
    }
    internal val _updatesEnabled = MutableStateFlow(prefs.getBoolean(AppSettings.KEY_UPDATES_ENABLED, true))

    val updatesEnabled: StateFlow<Boolean> get() = _updatesEnabled.asStateFlow()
    internal val _updateNotificationsEnabled = MutableStateFlow(prefs.getBoolean(AppSettings.KEY_UPDATE_NOTIFICATIONS_ENABLED, true))

    val updateNotificationsEnabled: StateFlow<Boolean> get() = _updateNotificationsEnabled.asStateFlow()
    internal val _updateStatusNotificationsEnabled = MutableStateFlow(prefs.getBoolean(AppSettings.KEY_UPDATE_STATUS_NOTIFICATIONS_ENABLED, false))

    val updateStatusNotificationsEnabled: StateFlow<Boolean> get() = _updateStatusNotificationsEnabled.asStateFlow()
    internal val _updateCheckIntervalHours = MutableStateFlow(prefs.getInt(AppSettings.KEY_UPDATE_CHECK_INTERVAL_HOURS, 6))

    val updateCheckIntervalHours: StateFlow<Int> get() = _updateCheckIntervalHours.asStateFlow()
    internal val _onboardingCompleted = MutableStateFlow(prefs.getBoolean(AppSettings.KEY_ONBOARDING_COMPLETED, false))

    val onboardingCompleted: StateFlow<Boolean> get() = _onboardingCompleted.asStateFlow()
    fun setOnboardingCompleted(completed: Boolean) {
        prefs.edit { putBoolean(AppSettings.KEY_ONBOARDING_COMPLETED, completed) }
        _onboardingCompleted.value = completed
    }
    internal val _hasShownBetaPopup = MutableStateFlow(prefs.getBoolean(AppSettings.KEY_HAS_SHOWN_BETA_POPUP, false))

    val hasShownBetaPopup: StateFlow<Boolean> get() = _hasShownBetaPopup.asStateFlow()
    fun setHasShownBetaPopup(shown: Boolean) {
        prefs.edit { putBoolean(AppSettings.KEY_HAS_SHOWN_BETA_POPUP, shown) }
        _hasShownBetaPopup.value = shown
    }
    internal val _lastBackupTimestamp = MutableStateFlow(safeLong(AppSettings.KEY_LAST_BACKUP_TIMESTAMP, 0L))

    val lastBackupTimestamp: StateFlow<Long> get() = _lastBackupTimestamp.asStateFlow()
    fun setLastBackupTimestamp(timestamp: Long) {
        prefs.edit { putLong(AppSettings.KEY_LAST_BACKUP_TIMESTAMP, timestamp) }
        _lastBackupTimestamp.value = timestamp
    }
    internal val _autoBackupEnabled = MutableStateFlow(prefs.getBoolean(AppSettings.KEY_AUTO_BACKUP_ENABLED, false))

    val autoBackupEnabled: StateFlow<Boolean> get() = _autoBackupEnabled.asStateFlow()
    internal val _backupLocation = MutableStateFlow(prefs.getString(AppSettings.KEY_BACKUP_LOCATION, null))

    val backupLocation: StateFlow<String?> get() = _backupLocation.asStateFlow()
    fun setBackupLocation(location: String?) {
        if (location == null) {
            prefs.edit { remove(AppSettings.KEY_BACKUP_LOCATION) }
        } else {
            prefs.edit { putString(AppSettings.KEY_BACKUP_LOCATION, location) }
        }
        _backupLocation.value = location
    }
    internal val _lastCrashLog = MutableStateFlow<String?>(prefs.getString(AppSettings.KEY_LAST_CRASH_LOG, null))

    val lastCrashLog: StateFlow<String?> get() = _lastCrashLog.asStateFlow()
    fun setLastCrashLog(log: String?) {
        // Use commit() instead of apply() to ensure immediate persistence for crash logs
        if (log == null) {
            prefs.edit { remove(AppSettings.KEY_LAST_CRASH_LOG) }
        } else {
            prefs.edit { putString(AppSettings.KEY_LAST_CRASH_LOG, log) }
        }
        _lastCrashLog.value = log
    }
    internal val _crashLogHistory = MutableStateFlow<List<CrashLogEntry>>(
        try {
            val json = prefs.getString(AppSettings.KEY_CRASH_LOG_HISTORY, null)
            if (json != null) {
                Gson().fromJson(json, object : TypeToken<List<CrashLogEntry>>() {}.type)
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            emptyList()
        }
    )

    val crashLogHistory: StateFlow<List<CrashLogEntry>> get() = _crashLogHistory.asStateFlow()
    internal val _batterySaverEnabled = MutableStateFlow(prefs.getBoolean(AppSettings.KEY_BATTERY_SAVER_ENABLED, false))

    val batterySaverEnabled: StateFlow<Boolean> get() = _batterySaverEnabled.asStateFlow()
    fun setBatterySaverEnabled(enabled: Boolean) {
        prefs.edit { putBoolean(AppSettings.KEY_BATTERY_SAVER_ENABLED, enabled) }
        _batterySaverEnabled.value = enabled
        updateDerived()
    }
    internal val _batterySaverMode = MutableStateFlow(prefs.getString(AppSettings.KEY_BATTERY_SAVER_MODE, "auto") ?: "auto")

    val batterySaverMode: StateFlow<String> get() = _batterySaverMode.asStateFlow()
    fun setBatterySaverMode(mode: String) {
        prefs.edit { putString(AppSettings.KEY_BATTERY_SAVER_MODE, mode) }
        _batterySaverMode.value = mode
        updateDerived()
    }
    internal val _batterySaverDisableHaptics = MutableStateFlow(prefs.getBoolean(AppSettings.KEY_BATTERY_SAVER_DISABLE_HAPTICS, true))

    val batterySaverDisableHaptics: StateFlow<Boolean> get() = _batterySaverDisableHaptics.asStateFlow()
    fun setBatterySaverDisableHaptics(disable: Boolean) {
        prefs.edit { putBoolean(AppSettings.KEY_BATTERY_SAVER_DISABLE_HAPTICS, disable) }
        _batterySaverDisableHaptics.value = disable
        updateDerived()
    }
    internal val _batterySaverEnableOffload = MutableStateFlow(prefs.getBoolean(AppSettings.KEY_BATTERY_SAVER_ENABLE_OFFLOAD, true))

    val batterySaverEnableOffload: StateFlow<Boolean> get() = _batterySaverEnableOffload.asStateFlow()
    fun setBatterySaverEnableOffload(enable: Boolean) {
        prefs.edit { putBoolean(AppSettings.KEY_BATTERY_SAVER_ENABLE_OFFLOAD, enable) }
        _batterySaverEnableOffload.value = enable
        updateDerived()
    }
    internal val _batterySaverDisableMarquee = MutableStateFlow(prefs.getBoolean(AppSettings.KEY_BATTERY_SAVER_DISABLE_MARQUEE, true))

    val batterySaverDisableMarquee: StateFlow<Boolean> get() = _batterySaverDisableMarquee.asStateFlow()
    fun setBatterySaverDisableMarquee(disable: Boolean) {
        prefs.edit { putBoolean(AppSettings.KEY_BATTERY_SAVER_DISABLE_MARQUEE, disable) }
        _batterySaverDisableMarquee.value = disable
        updateDerived()
    }
    internal val _batterySaverDisableLosslessArtwork = MutableStateFlow(prefs.getBoolean(AppSettings.KEY_BATTERY_SAVER_DISABLE_LOSSLESS_ARTWORK, true))

    val batterySaverDisableLosslessArtwork: StateFlow<Boolean> get() = _batterySaverDisableLosslessArtwork.asStateFlow()
    fun setBatterySaverDisableLosslessArtwork(disable: Boolean) {
        prefs.edit { putBoolean(AppSettings.KEY_BATTERY_SAVER_DISABLE_LOSSLESS_ARTWORK, disable) }
        _batterySaverDisableLosslessArtwork.value = disable
        updateDerived()
    }
    internal val _batterySaverDisableAutoFetchArtwork = MutableStateFlow(prefs.getBoolean(AppSettings.KEY_BATTERY_SAVER_DISABLE_AUTO_FETCH_ARTWORK, true))

    val batterySaverDisableAutoFetchArtwork: StateFlow<Boolean> get() = _batterySaverDisableAutoFetchArtwork.asStateFlow()
    fun setBatterySaverDisableAutoFetchArtwork(disable: Boolean) {
        prefs.edit { putBoolean(AppSettings.KEY_BATTERY_SAVER_DISABLE_AUTO_FETCH_ARTWORK, disable) }
        _batterySaverDisableAutoFetchArtwork.value = disable
        updateDerived()
    }
    internal val _maxCacheSize = MutableStateFlow(safeLong(AppSettings.KEY_MAX_CACHE_SIZE, 300L * 1024L * 1024L)) // 300MB default

    val maxCacheSize: StateFlow<Long> get() = _maxCacheSize.asStateFlow()
    fun setMaxCacheSize(size: Long) {
        if (isValidCacheSize(size)) {
            prefs.edit { putLong(AppSettings.KEY_MAX_CACHE_SIZE, size) }
            _maxCacheSize.value = size
        } else {
            Log.w("AppSettings", "Invalid cache size: $size, keeping current value")
        }
    }
    internal val _searchHistory = MutableStateFlow<String?>(prefs.getString(AppSettings.KEY_SEARCH_HISTORY, null))

    val searchHistory: StateFlow<String?> get() = _searchHistory.asStateFlow()
    fun setSearchHistory(history: String?) {
        if (history == null) {
            prefs.edit { remove(AppSettings.KEY_SEARCH_HISTORY) }
        } else {
            prefs.edit { putString(AppSettings.KEY_SEARCH_HISTORY, history) }
        }
        _searchHistory.value = history
    }
    internal val _showKeyboardOnSearchOpen = MutableStateFlow(prefs.getBoolean(AppSettings.KEY_SHOW_KEYBOARD_ON_SEARCH_OPEN, true))

    val showKeyboardOnSearchOpen: StateFlow<Boolean> get() = _showKeyboardOnSearchOpen.asStateFlow()
    fun setShowKeyboardOnSearchOpen(enabled: Boolean) {
        prefs.edit { putBoolean(AppSettings.KEY_SHOW_KEYBOARD_ON_SEARCH_OPEN, enabled) }
        _showKeyboardOnSearchOpen.value = enabled
    }
    internal val _forcePlayerCompactMode = MutableStateFlow(prefs.getBoolean(AppSettings.KEY_FORCE_PLAYER_COMPACT_MODE, false))

    val forcePlayerCompactMode: StateFlow<Boolean> get() = _forcePlayerCompactMode.asStateFlow()
    fun setForcePlayerCompactMode(enabled: Boolean) {
        prefs.edit { putBoolean(AppSettings.KEY_FORCE_PLAYER_COMPACT_MODE, enabled) }
        _forcePlayerCompactMode.value = enabled
    }
    internal val _useExperimentalPlayerUi = MutableStateFlow(prefs.getBoolean(AppSettings.KEY_USE_EXPERIMENTAL_PLAYER_UI, false))

    val useExperimentalPlayerUi: StateFlow<Boolean> get() = _useExperimentalPlayerUi.asStateFlow()
    fun setUseExperimentalPlayerUi(enabled: Boolean) {
        prefs.edit { putBoolean(AppSettings.KEY_USE_EXPERIMENTAL_PLAYER_UI, enabled) }
        _useExperimentalPlayerUi.value = enabled
    }
    internal val _trackErrorCheckerEnabled = MutableStateFlow(prefs.getBoolean(AppSettings.KEY_TRACK_ERROR_CHECKER_ENABLED, true))

    val trackErrorCheckerEnabled: StateFlow<Boolean> get() = _trackErrorCheckerEnabled.asStateFlow()
    fun setTrackErrorCheckerEnabled(enabled: Boolean) {
        prefs.edit { putBoolean(AppSettings.KEY_TRACK_ERROR_CHECKER_ENABLED, enabled) }
        _trackErrorCheckerEnabled.value = enabled
    }
    internal val _codecMonitoringEnabled = MutableStateFlow(prefs.getBoolean("codec_monitoring_enabled", false))

    val codecMonitoringEnabled: StateFlow<Boolean> get() = _codecMonitoringEnabled.asStateFlow()
    fun setCodecMonitoringEnabled(enabled: Boolean) {
        prefs.edit { putBoolean(AppSettings.KEY_CODEC_MONITORING_ENABLED, enabled) }
        _codecMonitoringEnabled.value = enabled
    }
    internal val _showCodecNotifications = MutableStateFlow(prefs.getBoolean(AppSettings.KEY_SHOW_CODEC_NOTIFICATIONS, false))

    val showCodecNotifications: StateFlow<Boolean> get() = _showCodecNotifications.asStateFlow()
    fun setShowCodecNotifications(enabled: Boolean) {
        prefs.edit { putBoolean(AppSettings.KEY_SHOW_CODEC_NOTIFICATIONS, enabled) }
        _showCodecNotifications.value = enabled
    }
    internal val _enhancedSeekingEnabled = MutableStateFlow(prefs.getBoolean(AppSettings.KEY_ENHANCED_SEEKING_ENABLED, true))

    val enhancedSeekingEnabled: StateFlow<Boolean> get() = _enhancedSeekingEnabled.asStateFlow()
    fun setEnhancedSeekingEnabled(enabled: Boolean) {
        prefs.edit { putBoolean(AppSettings.KEY_ENHANCED_SEEKING_ENABLED, enabled) }
        _enhancedSeekingEnabled.value = enabled
    }
    internal val _globalMarqueeEnabled = MutableStateFlow(prefs.getBoolean(AppSettings.KEY_GLOBAL_MARQUEE_ENABLED, true))

    val globalMarqueeEnabled: StateFlow<Boolean> get() = _globalMarqueeEnabled.asStateFlow()
    fun setGlobalMarqueeEnabled(enabled: Boolean) {
        prefs.edit { putBoolean(AppSettings.KEY_GLOBAL_MARQUEE_ENABLED, enabled) }
        _globalMarqueeEnabled.value = enabled
        updateDerived()
    }
    internal val _preloadLimit = MutableStateFlow(prefs.getInt(AppSettings.KEY_PRELOAD_LIMIT, 3))

    val preloadLimit: StateFlow<Int> get() = _preloadLimit.asStateFlow()
    fun setPreloadLimit(limit: Int) {
        prefs.edit { putInt(AppSettings.KEY_PRELOAD_LIMIT, limit) }
        _preloadLimit.value = limit
    }
    internal val _gaplessPlayback = MutableStateFlow(prefs.getBoolean(AppSettings.KEY_GAPLESS_PLAYBACK, true))

    val gaplessPlayback: StateFlow<Boolean> get() = _gaplessPlayback.asStateFlow()
    fun setGaplessPlayback(enable: Boolean) {
        prefs.edit { putBoolean(AppSettings.KEY_GAPLESS_PLAYBACK, enable) }
        _gaplessPlayback.value = enable
    }
    internal val _skipSilenceEnabled = MutableStateFlow(prefs.getBoolean(AppSettings.KEY_SKIP_SILENCE, false))

    val skipSilenceEnabled: StateFlow<Boolean> get() = _skipSilenceEnabled.asStateFlow()
    fun setSkipSilenceEnabled(enable: Boolean) {
        prefs.edit { putBoolean(AppSettings.KEY_SKIP_SILENCE, enable) }
        _skipSilenceEnabled.value = enable
        Log.d("AppSettings", "Skip silence ${if (enable) "enabled" else "disabled"}")
    }
    internal val _minimumBitrate = MutableStateFlow(prefs.getInt(AppSettings.KEY_MINIMUM_BITRATE, 0)) // 0 = no filter

    val minimumBitrate: StateFlow<Int> get() = _minimumBitrate.asStateFlow()
    internal val _trimLyrics = MutableStateFlow(prefs.getBoolean(AppSettings.KEY_TRIM_LYRICS, true))

    val trimLyrics: StateFlow<Boolean> get() = _trimLyrics.asStateFlow()
    fun setTrimLyrics(enable: Boolean) {
        prefs.edit { putBoolean(AppSettings.KEY_TRIM_LYRICS, enable) }
        _trimLyrics.value = enable
    }
    internal fun isValidCacheSize(size: Long): Boolean {
        val minSize = 50L * 1024 * 1024 // 50MB minimum
        val maxSize = 10L * 1024 * 1024 * 1024 // 10GB maximum
        return size in minSize..maxSize
    }

    private fun safeLong(key: String, defaultValue: Long): Long {
        val stored = prefs.getLong(key, Long.MIN_VALUE)
        return if (stored == Long.MIN_VALUE) defaultValue else stored
    }
}
