package chromahub.rhythm.app.infrastructure.service

import chromahub.rhythm.app.core.model.Song
import android.content.Context

/** Main-activity class + widget bridge, installed by :app (multi-module split PR-4). */
object MediaServiceHooks {
    /** Audio-effects panel request code (openSystemEqualizer contract). */
    const val DISPLAY_AUDIO_EFFECT_CONTROL_PANEL_REQUEST = 1002

    /** Extra key for opening the player directly (owned by MainActivity). */
    @Volatile
    var openPlayerExtraKey: String = ""

    @Volatile
    var mainActivityClass: Class<out android.app.Activity>? = null

    /** Artwork bitmap loader for the media session (lives in :app until PR-5). */
    /** Quality-tier icon drawables owned by :app (:core:media is resource-free). */
    @Volatile
    var qualityIcons: chromahub.rhythm.app.core.model.PlaybackQualityInfo.QualityIcons =
        chromahub.rhythm.app.core.model.PlaybackQualityInfo.QualityIcons()

    @Volatile
    var artworkBitmapLoader: androidx.media3.common.util.BitmapLoader? = null

    @Volatile
    var externalMetadataExtractor: ExternalMetadataExtractor? = null

    /** Launches the guard-timeout activity (lives in :app). */
    @Volatile
    var guardTimeoutStarter: ((Context, String, Long, Long) -> Unit)? = null

    fun requireMainActivity(): Class<out android.app.Activity> =
        mainActivityClass ?: error("MainActivity class not installed by :app")
}

/** Metadata extraction for externally-opened audio files (MediaUtils, :app). */
interface ExternalMetadataExtractor {
    fun getMediaMimeType(context: Context, uri: android.net.Uri): String?
    fun extractMetadataFromUri(context: Context, uri: android.net.Uri): chromahub.rhythm.app.core.model.Song?
}

/** Widget update bridge; implementation lives with the Glance widgets (:feature:widget, PR-5). */
interface PlaybackWidgetBridge {
    fun updateWidget(
        context: Context,
        song: Song?,
        isPlaying: Boolean,
        hasPrevious: Boolean = false,
        hasNext: Boolean = false,
        isFavorite: Boolean = false,
        isShuffleEnabled: Boolean = false,
        repeatMode: Int = 0,
    )

    fun updateLyrics(context: Context, lyricTexts: List<String>, activeIndex: Int)
}

object PlaybackWidgetBridgeInstaller {
    @Volatile
    var bridge: PlaybackWidgetBridge? = null

    fun require(): PlaybackWidgetBridge =
        bridge ?: error("PlaybackWidgetBridge not installed by :app")
}
