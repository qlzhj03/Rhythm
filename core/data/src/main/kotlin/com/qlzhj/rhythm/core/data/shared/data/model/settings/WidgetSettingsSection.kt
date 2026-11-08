package com.qlzhj.rhythm.core.data.shared.data.model.settings

import android.content.SharedPreferences
import androidx.core.content.edit
import com.qlzhj.rhythm.core.data.shared.data.model.AppSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
/**
 * Widget settings state + write operations (B-phase B2 domain split).
 * Groups moved verbatim from AppSettings; keys referenced through
 * AppSettings's internal companion constants.
 */
 */
internal class WidgetSettingsSection(private val prefs: SharedPreferences) {
    internal val _widgetShowAlbumArt = MutableStateFlow(prefs.getBoolean(AppSettings.KEY_WIDGET_SHOW_ALBUM_ART, true))
    val widgetShowAlbumArt: StateFlow<Boolean> get() = _widgetShowAlbumArt.asStateFlow()
    fun setWidgetShowAlbumArt(value: Boolean) {
        _widgetShowAlbumArt.value = value
        prefs.edit { putBoolean(AppSettings.KEY_WIDGET_SHOW_ALBUM_ART, value) }
    }
    internal val _widgetShowArtist = MutableStateFlow(prefs.getBoolean(AppSettings.KEY_WIDGET_SHOW_ARTIST, true))
    val widgetShowArtist: StateFlow<Boolean> get() = _widgetShowArtist.asStateFlow()
    fun setWidgetShowArtist(value: Boolean) {
        _widgetShowArtist.value = value
        prefs.edit { putBoolean(AppSettings.KEY_WIDGET_SHOW_ARTIST, value) }
    }
    internal val _widgetShowAlbum = MutableStateFlow(prefs.getBoolean(AppSettings.KEY_WIDGET_SHOW_ALBUM, false))
    val widgetShowAlbum: StateFlow<Boolean> get() = _widgetShowAlbum.asStateFlow()
    fun setWidgetShowAlbum(value: Boolean) {
        _widgetShowAlbum.value = value
        prefs.edit { putBoolean(AppSettings.KEY_WIDGET_SHOW_ALBUM, value) }
    }
    internal val _widgetCornerRadius = MutableStateFlow(prefs.getInt(AppSettings.KEY_WIDGET_CORNER_RADIUS, 28))
    val widgetCornerRadius: StateFlow<Int> get() = _widgetCornerRadius.asStateFlow()
    fun setWidgetCornerRadius(value: Int) {
        if (value in 0..60) {
            _widgetCornerRadius.value = value
            prefs.edit { putInt(AppSettings.KEY_WIDGET_CORNER_RADIUS, value) }
        }
    }
    internal val _widgetAutoUpdate = MutableStateFlow(prefs.getBoolean(AppSettings.KEY_WIDGET_AUTO_UPDATE, true))
    val widgetAutoUpdate: StateFlow<Boolean> get() = _widgetAutoUpdate.asStateFlow()
    fun setWidgetAutoUpdate(value: Boolean) {
        _widgetAutoUpdate.value = value
        prefs.edit { putBoolean(AppSettings.KEY_WIDGET_AUTO_UPDATE, value) }
    }
    internal val _widgetShowFavoriteButton = MutableStateFlow(prefs.getBoolean(AppSettings.KEY_WIDGET_SHOW_FAVORITE_BUTTON, true))
    val widgetShowFavoriteButton: StateFlow<Boolean> get() = _widgetShowFavoriteButton.asStateFlow()
    fun setWidgetShowFavoriteButton(value: Boolean) {
        _widgetShowFavoriteButton.value = value
        prefs.edit { putBoolean(AppSettings.KEY_WIDGET_SHOW_FAVORITE_BUTTON, value) }
    }
    internal val _widgetTheme = MutableStateFlow(prefs.getInt(AppSettings.KEY_WIDGET_THEME, 0))
    val widgetTheme: StateFlow<Int> get() = _widgetTheme.asStateFlow()
    fun setWidgetTheme(value: Int) {
        _widgetTheme.value = value
        prefs.edit { putInt(AppSettings.KEY_WIDGET_THEME, value) }
    }
    internal val _widgetCookieBottomLeft = MutableStateFlow(prefs.getInt(AppSettings.KEY_WIDGET_COOKIE_BOTTOM_LEFT, 3))
    val widgetCookieBottomLeft: StateFlow<Int> get() = _widgetCookieBottomLeft.asStateFlow()
    fun setWidgetCookieBottomLeft(value: Int) {
        _widgetCookieBottomLeft.value = value
        prefs.edit { putInt(AppSettings.KEY_WIDGET_COOKIE_BOTTOM_LEFT, value) }
    }
    internal val _widgetCookieBottomRight = MutableStateFlow(prefs.getInt(AppSettings.KEY_WIDGET_COOKIE_BOTTOM_RIGHT, 4))
    val widgetCookieBottomRight: StateFlow<Int> get() = _widgetCookieBottomRight.asStateFlow()
    fun setWidgetCookieBottomRight(value: Int) {
        _widgetCookieBottomRight.value = value
        prefs.edit { putInt(AppSettings.KEY_WIDGET_COOKIE_BOTTOM_RIGHT, value) }
    }
    internal val _widgetStatsRange = MutableStateFlow(prefs.getInt(AppSettings.KEY_WIDGET_STATS_RANGE, 0))
    val widgetStatsRange: StateFlow<Int> get() = _widgetStatsRange.asStateFlow()
    fun setWidgetStatsRange(value: Int) {
        _widgetStatsRange.value = value
        prefs.edit { putInt(AppSettings.KEY_WIDGET_STATS_RANGE, value) }
    }
    internal val _widgetStatsGem = MutableStateFlow(prefs.getInt(AppSettings.KEY_WIDGET_STATS_GEM, 0))
    val widgetStatsGem: StateFlow<Int> get() = _widgetStatsGem.asStateFlow()
    fun setWidgetStatsGem(value: Int) {
        _widgetStatsGem.value = value
        prefs.edit { putInt(AppSettings.KEY_WIDGET_STATS_GEM, value) }
    }
}
