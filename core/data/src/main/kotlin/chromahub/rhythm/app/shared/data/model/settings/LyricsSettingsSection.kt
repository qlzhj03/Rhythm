package chromahub.rhythm.app.shared.data.model.settings

import android.content.SharedPreferences
import androidx.core.content.edit
import com.google.gson.Gson
import com.qlzhj.rhythm.core.model.LyricsSourcePreference
import chromahub.rhythm.app.shared.data.model.LyricsApiPriority
import com.google.gson.reflect.TypeToken
import chromahub.rhythm.app.shared.data.model.AppSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
Lyrics settings state (source preference, display toggles, per-song preferences and custom LRC files), carved out of AppSettings (B-phase B2). CSV export/import (LyricsPreferenceIo) and lyrically API integration stay in the facade.
 */
internal class LyricsSettingsSection(
    private val prefs: SharedPreferences,
    private val updateDerived: () -> Unit,
) {
    internal val migratedLyricsPreference = run {
        val hasOldSetting = prefs.contains(AppSettings.KEY_ONLINE_ONLY_LYRICS)
        if (hasOldSetting && !prefs.contains(AppSettings.KEY_LYRICS_SOURCE_PREFERENCE)) {
            // Migrate: online_only = true means API_FIRST, false means allow all sources
            val onlineOnly = prefs.getBoolean(AppSettings.KEY_ONLINE_ONLY_LYRICS, true)
            if (onlineOnly) LyricsSourcePreference.API_FIRST.ordinal else LyricsSourcePreference.EMBEDDED_FIRST.ordinal
        } else {
            prefs.getInt(AppSettings.KEY_LYRICS_SOURCE_PREFERENCE, LyricsSourcePreference.LOCAL_FIRST.ordinal)
        }
    }

    internal val _showLyrics = MutableStateFlow(prefs.getBoolean(AppSettings.KEY_SHOW_LYRICS, true))

    val showLyrics: StateFlow<Boolean> get() = _showLyrics.asStateFlow()
    fun setShowLyrics(show: Boolean) {
        prefs.edit { putBoolean(AppSettings.KEY_SHOW_LYRICS, show) }
        _showLyrics.value = show
    }
    internal val _lyricsSourcePreference = MutableStateFlow(LyricsSourcePreference.fromOrdinal(migratedLyricsPreference))

    val lyricsSourcePreference: StateFlow<LyricsSourcePreference> get() = _lyricsSourcePreference.asStateFlow()
    fun setLyricsSourcePreference(preference: LyricsSourcePreference) {
        prefs.edit { putInt(AppSettings.KEY_LYRICS_SOURCE_PREFERENCE, preference.ordinal) }
        _lyricsSourcePreference.value = preference
        // Update the backward compatibility flag
        _onlineOnlyLyrics.value = (preference == LyricsSourcePreference.API_FIRST)
    }
    internal val _onlineOnlyLyrics = MutableStateFlow(_lyricsSourcePreference.value == LyricsSourcePreference.API_FIRST)

    val onlineOnlyLyrics: StateFlow<Boolean> get() = _onlineOnlyLyrics.asStateFlow()
    fun setOnlineOnlyLyrics(onlineOnly: Boolean) {
        // Convert to new preference system
        val preference = if (onlineOnly) LyricsSourcePreference.API_FIRST else LyricsSourcePreference.EMBEDDED_FIRST
        setLyricsSourcePreference(preference)
    }
    internal val _showLyricsBackgroundArtwork = MutableStateFlow(prefs.getBoolean(AppSettings.KEY_SHOW_LYRICS_BACKGROUND_ARTWORK, true))

    val showLyricsBackgroundArtwork: StateFlow<Boolean> get() = _showLyricsBackgroundArtwork.asStateFlow()
    fun setShowLyricsBackgroundArtwork(show: Boolean) {
        prefs.edit { putBoolean(AppSettings.KEY_SHOW_LYRICS_BACKGROUND_ARTWORK, show) }
        _showLyricsBackgroundArtwork.value = show
    }
    internal val _showLyricsTranslation = MutableStateFlow(prefs.getBoolean(AppSettings.KEY_SHOW_LYRICS_TRANSLATION, true))

    val showLyricsTranslation: StateFlow<Boolean> get() = _showLyricsTranslation.asStateFlow()
    fun setShowLyricsTranslation(show: Boolean) {
        prefs.edit { putBoolean(AppSettings.KEY_SHOW_LYRICS_TRANSLATION, show) }
        _showLyricsTranslation.value = show
    }
    internal val _showLyricsRomanization = MutableStateFlow(prefs.getBoolean(AppSettings.KEY_SHOW_LYRICS_ROMANIZATION, true))

    val showLyricsRomanization: StateFlow<Boolean> get() = _showLyricsRomanization.asStateFlow()
    fun setShowLyricsRomanization(show: Boolean) {
        prefs.edit { putBoolean(AppSettings.KEY_SHOW_LYRICS_ROMANIZATION, show) }
        _showLyricsRomanization.value = show
    }
    internal val _keepScreenOnLyrics = MutableStateFlow(prefs.getBoolean(AppSettings.KEY_KEEP_SCREEN_ON_LYRICS, false))

    val keepScreenOnLyrics: StateFlow<Boolean> get() = _keepScreenOnLyrics.asStateFlow()
    fun setKeepScreenOnLyrics(enabled: Boolean) {
        prefs.edit { putBoolean(AppSettings.KEY_KEEP_SCREEN_ON_LYRICS, enabled) }
        _keepScreenOnLyrics.value = enabled
    }
    internal val _tapLyricsToFullScreen = MutableStateFlow(prefs.getBoolean(AppSettings.KEY_TAP_LYRICS_TO_FULL_SCREEN, true))

    val tapLyricsToFullScreen: StateFlow<Boolean> get() = _tapLyricsToFullScreen.asStateFlow()
    fun setTapLyricsToFullScreen(enable: Boolean) {
        prefs.edit { putBoolean(AppSettings.KEY_TAP_LYRICS_TO_FULL_SCREEN, enable) }
        _tapLyricsToFullScreen.value = enable
    }
    internal val _lyricsApiPriority = MutableStateFlow(
        LyricsApiPriority.fromOrdinal(prefs.getInt(AppSettings.KEY_LYRICS_API_PRIORITY, LyricsApiPriority.LYRICALLY_FIRST.ordinal))
    )

    val lyricsApiPriority: StateFlow<LyricsApiPriority> get() = _lyricsApiPriority.asStateFlow()
    fun setLyricsApiPriority(priority: LyricsApiPriority) {
        prefs.edit { putInt(AppSettings.KEY_LYRICS_API_PRIORITY, priority.ordinal) }
        _lyricsApiPriority.value = priority
    }
    internal val _lyricsApiFallbackRetry = MutableStateFlow(prefs.getBoolean(AppSettings.KEY_LYRICS_API_FALLBACK_RETRY, true))

    val lyricsApiFallbackRetry: StateFlow<Boolean> get() = _lyricsApiFallbackRetry.asStateFlow()
    fun setLyricsApiFallbackRetry(enable: Boolean) {
        prefs.edit { putBoolean(AppSettings.KEY_LYRICS_API_FALLBACK_RETRY, enable) }
        _lyricsApiFallbackRetry.value = enable
    }
    internal val _autoHideLyricsControls = MutableStateFlow(prefs.getBoolean(AppSettings.KEY_AUTO_HIDE_LYRICS_CONTROLS, true))

    val autoHideLyricsControls: StateFlow<Boolean> get() = _autoHideLyricsControls.asStateFlow()
    fun setAutoHideLyricsControls(enabled: Boolean) {
        prefs.edit { putBoolean(AppSettings.KEY_AUTO_HIDE_LYRICS_CONTROLS, enabled) }
        _autoHideLyricsControls.value = enabled
    }
    internal val _lyricBold = MutableStateFlow(prefs.getBoolean(AppSettings.KEY_LYRIC_BOLD, false))

    val lyricBold: StateFlow<Boolean> get() = _lyricBold.asStateFlow()
    fun setLyricBold(enable: Boolean) {
        prefs.edit { putBoolean(AppSettings.KEY_LYRIC_BOLD, enable) }
        _lyricBold.value = enable
    }
    internal val _lyricNoAnimation = MutableStateFlow(prefs.getBoolean(AppSettings.KEY_LYRIC_NO_ANIMATION, false))

    val lyricNoAnimation: StateFlow<Boolean> get() = _lyricNoAnimation.asStateFlow()
    fun setLyricNoAnimation(enable: Boolean) {
        prefs.edit { putBoolean(AppSettings.KEY_LYRIC_NO_ANIMATION, enable) }
        _lyricNoAnimation.value = enable
    }
    internal val _translationAutoWord = MutableStateFlow(prefs.getBoolean(AppSettings.KEY_TRANSLATION_AUTO_WORD, false))

    val translationAutoWord: StateFlow<Boolean> get() = _translationAutoWord.asStateFlow()
    fun setTranslationAutoWord(enable: Boolean) {
        prefs.edit { putBoolean(AppSettings.KEY_TRANSLATION_AUTO_WORD, enable) }
        _translationAutoWord.value = enable
    }
    internal val _songLyricsPreferences = MutableStateFlow<Map<String, String>>(
        try {
            val json = prefs.getString(AppSettings.KEY_SONG_LYRICS_PREFERENCES, null)
            if (json != null) {
                Gson().fromJson(json, object : TypeToken<Map<String, String>>() {}.type)
            } else {
                emptyMap()
            }
        } catch (e: Exception) {
            emptyMap()
        }
    )

    val songLyricsPreferences: StateFlow<Map<String, String>> get() = _songLyricsPreferences.asStateFlow()
    internal val _songCustomLrcFiles = MutableStateFlow<Map<String, String>>(
        try {
            val json = prefs.getString(AppSettings.KEY_SONG_CUSTOM_LRC_FILES, null)
            if (json != null) {
                Gson().fromJson(json, object : TypeToken<Map<String, String>>() {}.type)
            } else {
                emptyMap()
            }
        } catch (e: Exception) {
            emptyMap()
        }
    )

    val songCustomLrcFiles: StateFlow<Map<String, String>> get() = _songCustomLrcFiles.asStateFlow()
    internal val _lrcRenameBehavior = MutableStateFlow(prefs.getString(AppSettings.KEY_LRC_RENAME_BEHAVIOR, "ask") ?: "ask")

    val lrcRenameBehavior: StateFlow<String> get() = _lrcRenameBehavior.asStateFlow()
    fun setLrcRenameBehavior(value: String) {
        _lrcRenameBehavior.value = value
        prefs.edit { putString(AppSettings.KEY_LRC_RENAME_BEHAVIOR, value) }
    }
    internal val _bluetoothLyricsEnabled = MutableStateFlow(prefs.getBoolean(AppSettings.KEY_BLUETOOTH_LYRICS_ENABLED, false))

    val bluetoothLyricsEnabled: StateFlow<Boolean> get() = _bluetoothLyricsEnabled.asStateFlow()
    fun setBluetoothLyricsEnabled(enabled: Boolean) {
        prefs.edit { putBoolean(AppSettings.KEY_BLUETOOTH_LYRICS_ENABLED, enabled) }
        _bluetoothLyricsEnabled.value = enabled
    }
    internal val _tapLyricsToSeek = MutableStateFlow(prefs.getBoolean(AppSettings.KEY_TAP_LYRICS_TO_SEEK, true))

    val tapLyricsToSeek: StateFlow<Boolean> get() = _tapLyricsToSeek.asStateFlow()
    fun setTapLyricsToSeek(value: Boolean) {
        _tapLyricsToSeek.value = value
        prefs.edit { putBoolean(AppSettings.KEY_TAP_LYRICS_TO_SEEK, value) }
    }
}
