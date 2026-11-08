package com.qlzhj.rhythm.core.data.shared.data.model.settings

import android.content.SharedPreferences
import androidx.core.content.edit
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.qlzhj.rhythm.core.data.shared.data.model.AppSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
Playback queue / sleep-timer settings state (queue persistence, shuffle/repeat, speed & pitch, sleep timer, playback history), carved out of AppSettings (B-phase B2). Recently-played song cache (private snapshot helpers) and cache-size stay in the facade.
 */
internal class PlaybackQueueSettingsSection(private val prefs: SharedPreferences) {
    internal val initialHidePlayedQueueValue = prefs.getBoolean(
        AppSettings.KEY_HIDE_PLAYED_QUEUE_SONGS,
        prefs.getBoolean(AppSettings.KEY_HIDE_PLAYED_SONGS_IN_QUEUE, false)
    )

    internal val _sleepTimerActive = MutableStateFlow(prefs.getBoolean(AppSettings.KEY_SLEEP_TIMER_ACTIVE, false))

    val sleepTimerActive: StateFlow<Boolean> get() = _sleepTimerActive.asStateFlow()
    fun setSleepTimerActive(active: Boolean) {
        prefs.edit { putBoolean(AppSettings.KEY_SLEEP_TIMER_ACTIVE, active) }
        _sleepTimerActive.value = active
    }
    internal val _sleepTimerRemainingSeconds = MutableStateFlow(prefs.getLong(AppSettings.KEY_SLEEP_TIMER_REMAINING_SECONDS, 0L))

    val sleepTimerRemainingSeconds: StateFlow<Long> get() = _sleepTimerRemainingSeconds.asStateFlow()
    fun setSleepTimerRemainingSeconds(seconds: Long) {
        prefs.edit { putLong(AppSettings.KEY_SLEEP_TIMER_REMAINING_SECONDS, seconds) }
        _sleepTimerRemainingSeconds.value = seconds
    }
    internal val _sleepTimerAction = MutableStateFlow(prefs.getString(AppSettings.KEY_SLEEP_TIMER_ACTION, "FADE_OUT") ?: "FADE_OUT")

    val sleepTimerAction: StateFlow<String> get() = _sleepTimerAction.asStateFlow()
    fun setSleepTimerAction(action: String) {
        prefs.edit { putString(AppSettings.KEY_SLEEP_TIMER_ACTION, action) }
        _sleepTimerAction.value = action
    }
    internal val _shuffleUsesExoplayer = MutableStateFlow(prefs.getBoolean(AppSettings.KEY_SHUFFLE_USES_EXOPLAYER, false))

    val shuffleUsesExoplayer: StateFlow<Boolean> get() = _shuffleUsesExoplayer.asStateFlow()
    fun setShuffleUsesExoplayer(useExoplayer: Boolean) {
        prefs.edit { putBoolean(AppSettings.KEY_SHUFFLE_USES_EXOPLAYER, useExoplayer) }
        _shuffleUsesExoplayer.value = useExoplayer
    }
    internal val _autoAddToQueue = MutableStateFlow(prefs.getBoolean(AppSettings.KEY_AUTO_ADD_TO_QUEUE, true))

    val autoAddToQueue: StateFlow<Boolean> get() = _autoAddToQueue.asStateFlow()
    fun setAutoAddToQueue(autoAdd: Boolean) {
        prefs.edit { putBoolean(AppSettings.KEY_AUTO_ADD_TO_QUEUE, autoAdd) }
        _autoAddToQueue.value = autoAdd
    }
    internal val _respectAlbumOnPlay = MutableStateFlow(prefs.getBoolean(AppSettings.KEY_RESPECT_ALBUM_ON_PLAY, true))

    val respectAlbumOnPlay: StateFlow<Boolean> get() = _respectAlbumOnPlay.asStateFlow()
    fun setRespectAlbumOnPlay(respect: Boolean) {
        prefs.edit { putBoolean(AppSettings.KEY_RESPECT_ALBUM_ON_PLAY, respect) }
        _respectAlbumOnPlay.value = respect
    }
    internal val _clearQueueOnNewSong = MutableStateFlow(prefs.getBoolean(AppSettings.KEY_CLEAR_QUEUE_ON_NEW_SONG, false))

    val clearQueueOnNewSong: StateFlow<Boolean> get() = _clearQueueOnNewSong.asStateFlow()
    fun setClearQueueOnNewSong(clearQueue: Boolean) {
        prefs.edit { putBoolean(AppSettings.KEY_CLEAR_QUEUE_ON_NEW_SONG, clearQueue) }
        _clearQueueOnNewSong.value = clearQueue
    }
    internal val _showQueueDialog = MutableStateFlow(prefs.getBoolean(AppSettings.KEY_SHOW_QUEUE_DIALOG, true))

    val showQueueDialog: StateFlow<Boolean> get() = _showQueueDialog.asStateFlow()
    fun setShowQueueDialog(show: Boolean) {
        prefs.edit { putBoolean(AppSettings.KEY_SHOW_QUEUE_DIALOG, show) }
        _showQueueDialog.value = show
    }
    internal val _listQueueActionBehavior = MutableStateFlow(
        prefs.getString(AppSettings.KEY_LIST_QUEUE_ACTION_BEHAVIOR, "replace") ?: "replace"
    )

    val listQueueActionBehavior: StateFlow<String> get() = _listQueueActionBehavior.asStateFlow()
    fun setListQueueActionBehavior(behavior: String) {
        if (behavior in listOf("replace", "ask", "play_next", "add_to_end")) {
            prefs.edit { putString(AppSettings.KEY_LIST_QUEUE_ACTION_BEHAVIOR, behavior) }
            _listQueueActionBehavior.value = behavior
        }
    }
    internal val _hidePlayedQueueSongs = MutableStateFlow(initialHidePlayedQueueValue)

    val hidePlayedQueueSongs: StateFlow<Boolean> get() = _hidePlayedQueueSongs.asStateFlow()
    fun setHidePlayedQueueSongs(hide: Boolean) {
        prefs.edit {
    putBoolean(AppSettings.KEY_HIDE_PLAYED_QUEUE_SONGS, hide)
    putBoolean(AppSettings.KEY_HIDE_PLAYED_SONGS_IN_QUEUE, hide)
}
        _hidePlayedQueueSongs.value = hide
    }
    internal val _repeatModePersistence = MutableStateFlow(prefs.getBoolean(AppSettings.KEY_REPEAT_MODE_PERSISTENCE, true))

    val repeatModePersistence: StateFlow<Boolean> get() = _repeatModePersistence.asStateFlow()
    fun setRepeatModePersistence(persist: Boolean) {
        prefs.edit { putBoolean(AppSettings.KEY_REPEAT_MODE_PERSISTENCE, persist) }
        _repeatModePersistence.value = persist
    }
    internal val _shuffleModePersistence = MutableStateFlow(prefs.getBoolean(AppSettings.KEY_SHUFFLE_MODE_PERSISTENCE, true))

    val shuffleModePersistence: StateFlow<Boolean> get() = _shuffleModePersistence.asStateFlow()
    fun setShuffleModePersistence(persist: Boolean) {
        prefs.edit { putBoolean(AppSettings.KEY_SHUFFLE_MODE_PERSISTENCE, persist) }
        _shuffleModePersistence.value = persist
    }
    internal val _keepShuffleOnSelection = MutableStateFlow(prefs.getBoolean(AppSettings.KEY_KEEP_SHUFFLE_ON_SELECTION, false))

    val keepShuffleOnSelection: StateFlow<Boolean> get() = _keepShuffleOnSelection.asStateFlow()
    fun setKeepShuffleOnSelection(keep: Boolean) {
        prefs.edit { putBoolean(AppSettings.KEY_KEEP_SHUFFLE_ON_SELECTION, keep) }
        _keepShuffleOnSelection.value = keep
    }
    internal val _savedShuffleState = MutableStateFlow(prefs.getBoolean(AppSettings.KEY_SAVED_SHUFFLE_STATE, false))

    val savedShuffleState: StateFlow<Boolean> get() = _savedShuffleState.asStateFlow()
    fun setSavedShuffleState(enabled: Boolean) {
        prefs.edit { putBoolean(AppSettings.KEY_SAVED_SHUFFLE_STATE, enabled) }
        _savedShuffleState.value = enabled
    }
    internal val _savedRepeatMode = MutableStateFlow(prefs.getInt(AppSettings.KEY_SAVED_REPEAT_MODE, 0))

    val savedRepeatMode: StateFlow<Int> get() = _savedRepeatMode.asStateFlow()
    fun setSavedRepeatMode(mode: Int) {
        prefs.edit { putInt(AppSettings.KEY_SAVED_REPEAT_MODE, mode) }
        _savedRepeatMode.value = mode
    }
    internal val _playbackSpeed = MutableStateFlow(prefs.getFloat(AppSettings.KEY_PLAYBACK_SPEED, 1.0f))

    val playbackSpeed: StateFlow<Float> get() = _playbackSpeed.asStateFlow()
    fun setPlaybackSpeed(speed: Float) {
        prefs.edit { putFloat(AppSettings.KEY_PLAYBACK_SPEED, speed) }
        _playbackSpeed.value = speed
    }
    internal val _defaultPlaybackSpeed = MutableStateFlow(prefs.getFloat(AppSettings.KEY_DEFAULT_PLAYBACK_SPEED, 1.0f))

    val defaultPlaybackSpeed: StateFlow<Float> get() = _defaultPlaybackSpeed.asStateFlow()
    fun setDefaultPlaybackSpeed(speed: Float) {
        prefs.edit { putFloat(AppSettings.KEY_DEFAULT_PLAYBACK_SPEED, speed) }
        _defaultPlaybackSpeed.value = speed
    }
    internal val _useDefaultPlaybackSpeed = MutableStateFlow(prefs.getBoolean(AppSettings.KEY_USE_DEFAULT_PLAYBACK_SPEED, false))

    val useDefaultPlaybackSpeed: StateFlow<Boolean> get() = _useDefaultPlaybackSpeed.asStateFlow()
    fun setUseDefaultPlaybackSpeed(enabled: Boolean) {
        prefs.edit { putBoolean(AppSettings.KEY_USE_DEFAULT_PLAYBACK_SPEED, enabled) }
        _useDefaultPlaybackSpeed.value = enabled
    }
    internal val _playbackPitch = MutableStateFlow(prefs.getFloat(AppSettings.KEY_PLAYBACK_PITCH, 1.0f))

    val playbackPitch: StateFlow<Float> get() = _playbackPitch.asStateFlow()
    fun setPlaybackPitch(pitch: Float) {
        prefs.edit { putFloat(AppSettings.KEY_PLAYBACK_PITCH, pitch) }
        _playbackPitch.value = pitch
    }
    internal val _syncSpeedAndPitch = MutableStateFlow(prefs.getBoolean(AppSettings.KEY_SYNC_SPEED_AND_PITCH, false))

    val syncSpeedAndPitch: StateFlow<Boolean> get() = _syncSpeedAndPitch.asStateFlow()
    fun setSyncSpeedAndPitch(sync: Boolean) {
        prefs.edit { putBoolean(AppSettings.KEY_SYNC_SPEED_AND_PITCH, sync) }
        _syncSpeedAndPitch.value = sync
    }
    internal val _useHoursInTimeFormat = MutableStateFlow(prefs.getBoolean(AppSettings.KEY_USE_HOURS_IN_TIME_FORMAT, true))

    val useHoursInTimeFormat: StateFlow<Boolean> get() = _useHoursInTimeFormat.asStateFlow()
    fun setUseHoursInTimeFormat(enabled: Boolean) {
        prefs.edit { putBoolean(AppSettings.KEY_USE_HOURS_IN_TIME_FORMAT, enabled) }
        _useHoursInTimeFormat.value = enabled
    }
    internal val _showRemainingTime = MutableStateFlow(prefs.getBoolean(AppSettings.KEY_SHOW_REMAINING_TIME, false))

    val showRemainingTime: StateFlow<Boolean> get() = _showRemainingTime.asStateFlow()
    fun setShowRemainingTime(enabled: Boolean) {
        prefs.edit { putBoolean(AppSettings.KEY_SHOW_REMAINING_TIME, enabled) }
        _showRemainingTime.value = enabled
    }
    internal val _stopPlaybackOnAppClose = MutableStateFlow(prefs.getBoolean(AppSettings.KEY_STOP_PLAYBACK_ON_APP_CLOSE, false))

    val stopPlaybackOnAppClose: StateFlow<Boolean> get() = _stopPlaybackOnAppClose.asStateFlow()
    fun setStopPlaybackOnAppClose(enabled: Boolean) {
        prefs.edit { putBoolean(AppSettings.KEY_STOP_PLAYBACK_ON_APP_CLOSE, enabled) }
        _stopPlaybackOnAppClose.value = enabled
    }
    internal val _queuePersistenceEnabled = MutableStateFlow(prefs.getBoolean(AppSettings.KEY_QUEUE_PERSISTENCE_ENABLED, true))

    val queuePersistenceEnabled: StateFlow<Boolean> get() = _queuePersistenceEnabled.asStateFlow()
    fun setQueuePersistenceEnabled(enabled: Boolean) {
        prefs.edit { putBoolean(AppSettings.KEY_QUEUE_PERSISTENCE_ENABLED, enabled) }
        _queuePersistenceEnabled.value = enabled
        
        // Clear saved queue if persistence is disabled
        if (!enabled) {
            clearSavedQueue()
        }
    }
    internal val _contextQueuePreference = MutableStateFlow(prefs.getString(AppSettings.KEY_CONTEXT_QUEUE_PREFERENCE, "ARTIST_THEN_GENRE")!!)

    val contextQueuePreference: StateFlow<String> get() = _contextQueuePreference.asStateFlow()
    fun setContextQueuePreference(pref: String) {
        prefs.edit { putString(AppSettings.KEY_CONTEXT_QUEUE_PREFERENCE, pref) }
        _contextQueuePreference.value = pref
    }
    internal val _contextQueuePersistence = MutableStateFlow(prefs.getString(AppSettings.KEY_CONTEXT_QUEUE_PERSISTENCE, "EPHEMERAL")!!)

    val contextQueuePersistence: StateFlow<String> get() = _contextQueuePersistence.asStateFlow()
    fun setContextQueuePersistence(value: String) {
        prefs.edit { putString(AppSettings.KEY_CONTEXT_QUEUE_PERSISTENCE, value) }
        _contextQueuePersistence.value = value
    }
    internal val _contextQueueSize = MutableStateFlow(prefs.getInt(AppSettings.KEY_CONTEXT_QUEUE_SIZE, 50))

    val contextQueueSize: StateFlow<Int> get() = _contextQueueSize.asStateFlow()
    fun setContextQueueSize(size: Int) {
        val safeSize = size.coerceAtLeast(1)
        prefs.edit { putInt(AppSettings.KEY_CONTEXT_QUEUE_SIZE, safeSize) }
        _contextQueueSize.value = safeSize
    }
    internal val _savedQueue = MutableStateFlow<List<String>>(
        try {
            val json = prefs.getString(AppSettings.KEY_SAVED_QUEUE, null)
            if (json != null) {
                Gson().fromJson(json, object : TypeToken<List<String>>() {}.type)
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            emptyList()
        }
    )

    val savedQueue: StateFlow<List<String>> get() = _savedQueue.asStateFlow()
    fun setSavedQueue(songIds: List<String>) {
        val json = Gson().toJson(songIds)
        prefs.edit { putString(AppSettings.KEY_SAVED_QUEUE, json) }
        _savedQueue.value = songIds
    }
    internal val _savedQueueIndex = MutableStateFlow(prefs.getInt(AppSettings.KEY_SAVED_QUEUE_INDEX, -1))

    val savedQueueIndex: StateFlow<Int> get() = _savedQueueIndex.asStateFlow()
    fun setSavedQueueIndex(index: Int) {
        prefs.edit { putInt(AppSettings.KEY_SAVED_QUEUE_INDEX, index) }
        _savedQueueIndex.value = index
    }
    internal val _savedPlaybackPosition = MutableStateFlow(prefs.getLong(AppSettings.KEY_SAVED_PLAYBACK_POSITION, 0L))

    val savedPlaybackPosition: StateFlow<Long> get() = _savedPlaybackPosition.asStateFlow()
    fun setSavedPlaybackPosition(position: Long) {
        prefs.edit { putLong(AppSettings.KEY_SAVED_PLAYBACK_POSITION, position) }
        _savedPlaybackPosition.value = position
    }
    internal val _savedOriginalQueue = MutableStateFlow<List<String>>(
        try {
            val json = prefs.getString(AppSettings.KEY_SAVED_ORIGINAL_QUEUE, null)
            if (json != null) {
                Gson().fromJson(json, object : TypeToken<List<String>>() {}.type)
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            emptyList()
        }
    )

    val savedOriginalQueue: StateFlow<List<String>> get() = _savedOriginalQueue.asStateFlow()
    fun setSavedOriginalQueue(songIds: List<String>) {
        val json = Gson().toJson(songIds)
        prefs.edit { putString(AppSettings.KEY_SAVED_ORIGINAL_QUEUE, json) }
        _savedOriginalQueue.value = songIds
    }
    internal val _savedOriginalQueueSource = MutableStateFlow<String?>(prefs.getString(AppSettings.KEY_SAVED_ORIGINAL_QUEUE_SOURCE, null))

    val savedOriginalQueueSource: StateFlow<String?> get() = _savedOriginalQueueSource.asStateFlow()
    fun setSavedOriginalQueueSource(sourceName: String?) {
        if (sourceName == null) {
            prefs.edit { remove(AppSettings.KEY_SAVED_ORIGINAL_QUEUE_SOURCE) }
        } else {
            prefs.edit { putString(AppSettings.KEY_SAVED_ORIGINAL_QUEUE_SOURCE, sourceName) }
        }
        _savedOriginalQueueSource.value = sourceName
    }
    internal val _recentlyPlayed = MutableStateFlow<List<String>>(
        try {
            val json = prefs.getString(AppSettings.KEY_RECENTLY_PLAYED, null)
            if (json != null) {
                Gson().fromJson(json, object : TypeToken<List<String>>() {}.type)
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            emptyList()
        }
    )

    val recentlyPlayed: StateFlow<List<String>> get() = _recentlyPlayed.asStateFlow()
    internal val _lastPlayedTimestamp = MutableStateFlow(safeLong(AppSettings.KEY_LAST_PLAYED_TIMESTAMP, 0L))

    val lastPlayedTimestamp: StateFlow<Long> get() = _lastPlayedTimestamp.asStateFlow()
    fun clearSavedQueue() {
        prefs.edit { remove(AppSettings.KEY_SAVED_QUEUE) }
        prefs.edit { remove(AppSettings.KEY_SAVED_QUEUE_INDEX) }
        prefs.edit { remove(AppSettings.KEY_SAVED_PLAYBACK_POSITION) }
        clearSavedOriginalQueue()
        _savedQueue.value = emptyList()
        _savedQueueIndex.value = -1
        _savedPlaybackPosition.value = 0L
    }
    fun clearSavedOriginalQueue() {
        prefs.edit { remove(AppSettings.KEY_SAVED_ORIGINAL_QUEUE) }
        prefs.edit { remove(AppSettings.KEY_SAVED_ORIGINAL_QUEUE_SOURCE) }
        _savedOriginalQueue.value = emptyList()
        _savedOriginalQueueSource.value = null
    }

    private fun safeLong(key: String, defaultValue: Long): Long {
        val stored = prefs.getLong(key, Long.MIN_VALUE)
        return if (stored == Long.MIN_VALUE) defaultValue else stored
    }
}
