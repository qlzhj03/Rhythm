package com.qlzhj.rhythm.core.data.shared.data.model.settings

import android.content.SharedPreferences
import androidx.core.content.edit
import com.qlzhj.rhythm.core.data.shared.data.model.AppSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
Streaming mode settings state (app mode local/streaming, service, quality, cellular, offline, remembered passwords), carved out of AppSettings (B-phase B2). Mode-switch orchestration stays with callers.
 */
internal class StreamingModeSettingsSection(private val prefs: SharedPreferences) {
    internal val _appMode = MutableStateFlow(prefs.getString(AppSettings.KEY_APP_MODE, "LOCAL") ?: "LOCAL")

    val appMode: StateFlow<String> get() = _appMode.asStateFlow()
    fun setAppMode(mode: String) {
        prefs.edit { putString(AppSettings.KEY_APP_MODE, mode) }
        _appMode.value = mode
    }
    internal val _streamingService = MutableStateFlow(prefs.getString(AppSettings.KEY_STREAMING_SERVICE, "SUBSONIC") ?: "SUBSONIC")

    val streamingService: StateFlow<String> get() = _streamingService.asStateFlow()
    fun setStreamingService(service: String) {
        prefs.edit { putString(AppSettings.KEY_STREAMING_SERVICE, service) }
        _streamingService.value = service
    }
    internal val _streamingQuality = MutableStateFlow(prefs.getString(AppSettings.KEY_STREAMING_QUALITY, "LOSSLESS") ?: "LOSSLESS")

    val streamingQuality: StateFlow<String> get() = _streamingQuality.asStateFlow()
    fun setStreamingQuality(quality: String) {
        prefs.edit { putString(AppSettings.KEY_STREAMING_QUALITY, quality) }
        _streamingQuality.value = quality
    }
    internal val _allowCellularStreaming = MutableStateFlow(prefs.getBoolean(AppSettings.KEY_ALLOW_CELLULAR_STREAMING, true))

    val allowCellularStreaming: StateFlow<Boolean> get() = _allowCellularStreaming.asStateFlow()
    fun setAllowCellularStreaming(allow: Boolean) {
        prefs.edit { putBoolean(AppSettings.KEY_ALLOW_CELLULAR_STREAMING, allow) }
        _allowCellularStreaming.value = allow
    }
    internal val _offlineMode = MutableStateFlow(prefs.getBoolean(AppSettings.KEY_OFFLINE_MODE, false))

    val offlineMode: StateFlow<Boolean> get() = _offlineMode.asStateFlow()
    fun setOfflineMode(enabled: Boolean) {
        prefs.edit { putBoolean(AppSettings.KEY_OFFLINE_MODE, enabled) }
        _offlineMode.value = enabled
    }
    internal val _rememberStreamingPasswords = MutableStateFlow(prefs.getBoolean(AppSettings.KEY_REMEMBER_STREAMING_PASSWORDS, true))

    val rememberStreamingPasswords: StateFlow<Boolean> get() = _rememberStreamingPasswords.asStateFlow()
    fun setRememberStreamingPasswords(enabled: Boolean) {
        prefs.edit { putBoolean(AppSettings.KEY_REMEMBER_STREAMING_PASSWORDS, enabled) }
        _rememberStreamingPasswords.value = enabled
    }
}
