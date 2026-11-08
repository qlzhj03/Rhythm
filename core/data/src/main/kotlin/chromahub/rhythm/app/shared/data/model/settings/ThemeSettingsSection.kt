package chromahub.rhythm.app.shared.data.model.settings

import android.content.SharedPreferences
import androidx.core.content.edit
import chromahub.rhythm.app.shared.data.model.AppSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
Theme appearance settings state (dark/AMOLED/dynamic colors, fonts, color source, expressive colors & intensity, festive themes), carved out of AppSettings (B-phase B2).
 */
internal class ThemeSettingsSection(private val prefs: SharedPreferences) {
    internal val _useSystemTheme = MutableStateFlow(prefs.getBoolean(AppSettings.KEY_USE_SYSTEM_THEME, true))

    val useSystemTheme: StateFlow<Boolean> get() = _useSystemTheme.asStateFlow()
    fun setUseSystemTheme(use: Boolean) {
        prefs.edit { putBoolean(AppSettings.KEY_USE_SYSTEM_THEME, use) }
        _useSystemTheme.value = use
    }
    internal val _darkMode = MutableStateFlow(prefs.getBoolean(AppSettings.KEY_DARK_MODE, true))

    val darkMode: StateFlow<Boolean> get() = _darkMode.asStateFlow()
    fun setDarkMode(dark: Boolean) {
        prefs.edit { putBoolean(AppSettings.KEY_DARK_MODE, dark) }
        _darkMode.value = dark
    }
    internal val _amoledTheme = MutableStateFlow(prefs.getBoolean(AppSettings.KEY_AMOLED_THEME, false))

    val amoledTheme: StateFlow<Boolean> get() = _amoledTheme.asStateFlow()
    fun setAmoledTheme(amoled: Boolean) {
        prefs.edit { putBoolean(AppSettings.KEY_AMOLED_THEME, amoled) }
        _amoledTheme.value = amoled
    }
    internal val _floatingNavigationBar = MutableStateFlow(prefs.getBoolean(AppSettings.KEY_FLOATING_NAVIGATION_BAR, true))

    val floatingNavigationBar: StateFlow<Boolean> get() = _floatingNavigationBar.asStateFlow()
    fun setFloatingNavigationBar(enabled: Boolean) {
        prefs.edit { putBoolean(AppSettings.KEY_FLOATING_NAVIGATION_BAR, enabled) }
        _floatingNavigationBar.value = enabled
    }
    internal val _useDynamicColors = MutableStateFlow(prefs.getBoolean(AppSettings.KEY_USE_DYNAMIC_COLORS, true))

    val useDynamicColors: StateFlow<Boolean> get() = _useDynamicColors.asStateFlow()
    fun setUseDynamicColors(use: Boolean) {
        prefs.edit { putBoolean(AppSettings.KEY_USE_DYNAMIC_COLORS, use) }
        _useDynamicColors.value = use
    }
    internal val _customColorScheme = MutableStateFlow(prefs.getString(AppSettings.KEY_CUSTOM_COLOR_SCHEME, "Default") ?: "Default")

    val customColorScheme: StateFlow<String> get() = _customColorScheme.asStateFlow()
    fun setCustomColorScheme(scheme: String) {
        prefs.edit { putString(AppSettings.KEY_CUSTOM_COLOR_SCHEME, scheme) }
        _customColorScheme.value = scheme
    }
    internal val _customFont = MutableStateFlow(prefs.getString(AppSettings.KEY_CUSTOM_FONT, "Geom") ?: "Geom")

    val customFont: StateFlow<String> get() = _customFont.asStateFlow()
    fun setCustomFont(font: String) {
        prefs.edit { putString(AppSettings.KEY_CUSTOM_FONT, font) }
        _customFont.value = font
    }
    internal val _colorSource = MutableStateFlow(prefs.getString(AppSettings.KEY_COLOR_SOURCE, "MONET") ?: "MONET")

    val colorSource: StateFlow<String> get() = _colorSource.asStateFlow()
    fun setColorSource(source: String) {
        prefs.edit { putString(AppSettings.KEY_COLOR_SOURCE, source) }
        _colorSource.value = source
    }
    internal val _fontSource = MutableStateFlow(prefs.getString(AppSettings.KEY_FONT_SOURCE, "SYSTEM") ?: "SYSTEM")

    val fontSource: StateFlow<String> get() = _fontSource.asStateFlow()
    fun setFontSource(source: String) {
        prefs.edit { putString(AppSettings.KEY_FONT_SOURCE, source) }
        _fontSource.value = source
    }
    internal val _customFontPath = MutableStateFlow(prefs.getString(AppSettings.KEY_CUSTOM_FONT_PATH, null))

    val customFontPath: StateFlow<String?> get() = _customFontPath.asStateFlow()
    fun setCustomFontPath(path: String?) {
        prefs.edit { putString(AppSettings.KEY_CUSTOM_FONT_PATH, path) }
        _customFontPath.value = path
    }
    internal val _customFontFamily = MutableStateFlow(prefs.getString(AppSettings.KEY_CUSTOM_FONT_FAMILY, "System") ?: "System")

    val customFontFamily: StateFlow<String> get() = _customFontFamily.asStateFlow()
    fun setCustomFontFamily(family: String) {
        prefs.edit { putString(AppSettings.KEY_CUSTOM_FONT_FAMILY, family) }
        _customFontFamily.value = family
    }
    internal val _expressiveColors = MutableStateFlow(prefs.getBoolean(AppSettings.KEY_EXPRESSIVE_COLORS, true))

    val expressiveColors: StateFlow<Boolean> get() = _expressiveColors.asStateFlow()
    fun setExpressiveColors(enabled: Boolean) {
        prefs.edit { putBoolean(AppSettings.KEY_EXPRESSIVE_COLORS, enabled) }
        _expressiveColors.value = enabled
    }
    internal val _themeIntensity = MutableStateFlow(prefs.getString(AppSettings.KEY_THEME_INTENSITY, "STANDARD") ?: "STANDARD")

    val themeIntensity: StateFlow<String> get() = _themeIntensity.asStateFlow()
    fun setThemeIntensity(intensity: String) {
        prefs.edit { putString(AppSettings.KEY_THEME_INTENSITY, intensity) }
        _themeIntensity.value = intensity
    }
    internal val _festiveThemeEnabled = MutableStateFlow(prefs.getBoolean(AppSettings.KEY_FESTIVE_THEME_ENABLED, true))

    val festiveThemeEnabled: StateFlow<Boolean> get() = _festiveThemeEnabled.asStateFlow()
    fun setFestiveThemeEnabled(enabled: Boolean) {
        prefs.edit { putBoolean(AppSettings.KEY_FESTIVE_THEME_ENABLED, enabled) }
        _festiveThemeEnabled.value = enabled
    }
    internal val _festiveThemeType = MutableStateFlow(prefs.getString(AppSettings.KEY_FESTIVE_THEME_TYPE, "CHRISTMAS") ?: "CHRISTMAS")

    val festiveThemeType: StateFlow<String> get() = _festiveThemeType.asStateFlow()
    fun setFestiveThemeType(type: String) {
        prefs.edit { putString(AppSettings.KEY_FESTIVE_THEME_TYPE, type) }
        _festiveThemeType.value = type
    }
    internal val _festiveThemeIntensity = MutableStateFlow(prefs.getFloat(AppSettings.KEY_FESTIVE_THEME_INTENSITY, 0.5f))

    val festiveThemeIntensity: StateFlow<Float> get() = _festiveThemeIntensity.asStateFlow()
    fun setFestiveThemeIntensity(intensity: Float) {
        prefs.edit { putFloat(AppSettings.KEY_FESTIVE_THEME_INTENSITY, intensity) }
        _festiveThemeIntensity.value = intensity
    }
    internal val _festiveThemeAutoDetect = MutableStateFlow(prefs.getBoolean(AppSettings.KEY_FESTIVE_THEME_AUTO_DETECT, true))

    val festiveThemeAutoDetect: StateFlow<Boolean> get() = _festiveThemeAutoDetect.asStateFlow()
    fun setFestiveThemeAutoDetect(autoDetect: Boolean) {
        prefs.edit { putBoolean(AppSettings.KEY_FESTIVE_THEME_AUTO_DETECT, autoDetect) }
        _festiveThemeAutoDetect.value = autoDetect
    }
}
