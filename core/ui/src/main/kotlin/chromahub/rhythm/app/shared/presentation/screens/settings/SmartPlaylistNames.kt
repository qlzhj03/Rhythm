package chromahub.rhythm.app.shared.presentation.screens.settings

import android.content.Context
import chromahub.rhythm.app.R

/**
 * Display name for the built-in smart playlists. Extracted from
 * PlaylistsSettingsScreen (PR-5) so :core:ui consumers (SongPickerBottomSheet)
 * and :feature:local screens can resolve it without depending on the settings
 * module.
 */
fun smartPlaylistDisplayName(id: String, dbName: String, context: Context): String = when (id) {
    "1" -> context.getString(R.string.smart_playlist_liked)
    "2" -> context.getString(R.string.settings_smart_playlist_recently_added)
    "3" -> context.getString(R.string.settings_smart_playlist_most_played)
    "4" -> context.getString(R.string.settings_smart_playlist_on_repeat)
    "5" -> context.getString(R.string.settings_smart_playlist_forgotten_favorites)
    "6" -> context.getString(R.string.settings_smart_playlist_recently_played)
    else -> dbName
}
