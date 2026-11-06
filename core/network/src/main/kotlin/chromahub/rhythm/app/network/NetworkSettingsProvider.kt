package chromahub.rhythm.app.network

import kotlinx.coroutines.flow.StateFlow

/**
 * Read-only view of the API-gating settings NetworkClient needs. Implemented by
 * AppSettings (:core:data) so the network stack never depends on the settings
 * implementation directly (multi-module split, docs/multi-module-refactor-plan.md).
 */
interface NetworkSettingsProvider {
    val integrationsEnabled: StateFlow<Boolean>
    val deezerApiEnabled: StateFlow<Boolean>
    val lrclibApiEnabled: StateFlow<Boolean>
    val betterLyricsApiEnabled: StateFlow<Boolean>
    val ytMusicApiEnabled: StateFlow<Boolean>
    val spotifyApiEnabled: StateFlow<Boolean>
    val lyricallyApiEnabled: StateFlow<Boolean>
    val wikipediaApiEnabled: StateFlow<Boolean>
    val spotifyClientId: StateFlow<String>
    val spotifyClientSecret: StateFlow<String>
}

/**
 * Compile-time feature flags (BuildConfig.ENABLE_* in :app). Kept explicit so
 * the flag logic stays reviewable and the ENABLE_* fields are not duplicated.
 */
data class NetworkFeatureFlags(
    val youtubeMusic: Boolean,
    val lyrically: Boolean,
    val deezer: Boolean,
    val lrclib: Boolean,
    val betterLyrics: Boolean,
    val spotifySearch: Boolean,
    val wikipedia: Boolean,
)
