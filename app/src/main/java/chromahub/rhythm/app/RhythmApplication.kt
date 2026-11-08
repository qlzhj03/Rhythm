package chromahub.rhythm.app

import android.app.Application
import android.content.ComponentCallbacks2
import android.os.Build
import android.util.Log
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.edit
import coil.Coil
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import okhttp3.Dispatcher
import chromahub.rhythm.app.infrastructure.widget.glance.GlanceShapeBitmaps
import chromahub.rhythm.app.infrastructure.widget.glance.RhythmCookieWidget
import chromahub.rhythm.app.infrastructure.widget.glance.RhythmMusicWidget
import chromahub.rhythm.app.di.NetworkClientInitializer
import com.qlzhj.rhythm.core.data.core.domain.repository.StreamingMusicRepository
import com.qlzhj.rhythm.core.data.shared.data.model.AppSettings
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject
import com.qlzhj.rhythm.core.common.ANRWatchdog
import com.qlzhj.rhythm.core.common.HapticsGate
import com.qlzhj.rhythm.core.media.util.CacheManager
import com.qlzhj.rhythm.core.ui.util.CrashReporter
import com.qlzhj.rhythm.core.media.util.MediaUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Custom Application class for Rhythm Music Player.
 * Handles initialization of:
 * - AppSettings
 * - CrashReporter
 * - NetworkClient
 * - ANR Watchdog (debug builds)
 */
@HiltAndroidApp
class RhythmApplication : Application(), ImageLoaderFactory {
    
    companion object {
        private const val TAG = "RhythmApplication"

        
        // Static reference to the application instance
        // Using a static reference is safe for Application class
        lateinit var instance: RhythmApplication
            private set
    }
    
    // Hilt-injected composition-root wiring (multi-module split PR-6).
    @Inject
    lateinit var streamingRepository: StreamingMusicRepository
    @Inject
    lateinit var networkClientInitializer: NetworkClientInitializer

    private var anrWatchdog: ANRWatchdog? = null
    
    override fun onCreate() {
        super.onCreate()

        // Preheat the settings prefs file off the main thread before AppSettings
        // init blocks on first read (B-phase B2 startup preheat).
        AppSettings.preheat(this)
        
        instance = this
        
        Log.d(TAG, "═══════════════════════════════════════════════════")
        Log.d(TAG, "RhythmApplication onCreate")
        Log.d(TAG, "Build Type: ${BuildConfig.BUILD_TYPE}")
        Log.d(TAG, "Version: ${BuildConfig.VERSION_NAME}")
        Log.d(TAG, "═══════════════════════════════════════════════════")
        
        // Initialize AppSettings early (singleton, uses application context)
        val settings = AppSettings.getInstance(applicationContext)
        Log.d(TAG, "✓ AppSettings initialized")

        // Localized label seam for :core:data (resource-free module — PR-3).
        com.qlzhj.rhythm.core.common.AppStrings.apply {
            unresolvedTrack = { getString(chromahub.rhythm.app.R.string.unresolved_track) }
            unknownArtist = { getString(chromahub.rhythm.app.R.string.unknown_artist_name) }
            unknownAlbum = { getString(chromahub.rhythm.app.R.string.unknown_album_name) }
        }

        // Media service seams for :core:media (PR-4).
        com.qlzhj.rhythm.core.common.AppBuildInfo.apply {
            applicationId = packageName
            versionName = BuildConfig.VERSION_NAME
            versionCode = BuildConfig.VERSION_CODE
            releaseDate = BuildConfig.RELEASE_DATE
            flavor = BuildConfig.FLAVOR
            isDebugBuild = BuildConfig.DEBUG
            isNightly = BuildConfig.IS_NIGHTLY
            enableYouTubeMusic = BuildConfig.ENABLE_YOUTUBE_MUSIC
            enableLyricallyApi = BuildConfig.ENABLE_LYRICALLY_API
            enableDeezer = BuildConfig.ENABLE_DEEZER
            enableLrclib = BuildConfig.ENABLE_LRCLIB
            enableBetterLyrics = BuildConfig.ENABLE_BETTERLYRICS
            enableSpotifySearch = BuildConfig.ENABLE_SPOTIFY_SEARCH
            enableWikipedia = BuildConfig.ENABLE_WIKIPEDIA
        }
        com.qlzhj.rhythm.core.media.infrastructure.service.MediaServiceHooks.qualityIcons =
            com.qlzhj.rhythm.core.model.PlaybackQualityInfo.QualityIcons(
                hd = chromahub.rhythm.app.R.drawable.ic_hd,
                highRes = chromahub.rhythm.app.R.drawable.ic_high_res,
                dolby = chromahub.rhythm.app.R.drawable.ic_dolby,
                dts = chromahub.rhythm.app.R.drawable.ic_dts,
                surround = chromahub.rhythm.app.R.drawable.ic_surround_sound,
                hq = chromahub.rhythm.app.R.drawable.ic_hq,
                sq = chromahub.rhythm.app.R.drawable.ic_sq,
            )
        com.qlzhj.rhythm.core.media.infrastructure.service.MediaServiceHooks.guardTimeoutStarter = { context, reason, untilMs, startedAtMs ->
            chromahub.rhythm.app.activities.RhythmGuardTimeoutActivity.start(
                context = context, reason = reason, timeoutUntilMs = untilMs, timeoutStartedAtMs = startedAtMs)
        }
        com.qlzhj.rhythm.core.media.infrastructure.service.MediaServiceHooks.mainActivityClass =
            chromahub.rhythm.app.activities.MainActivity::class.java
        com.qlzhj.rhythm.core.media.infrastructure.service.PlaybackWidgetBridgeInstaller.bridge =
            object : com.qlzhj.rhythm.core.media.infrastructure.service.PlaybackWidgetBridge {
                // NOTE: must call the Glance updater directly — WidgetUpdater now
                // forwards through this very bridge (PR-5 seam inversion).
                override fun updateWidget(
                    context: android.content.Context,
                    song: com.qlzhj.rhythm.core.model.Song?,
                    isPlaying: Boolean,
                    hasPrevious: Boolean,
                    hasNext: Boolean,
                    isFavorite: Boolean,
                    isShuffleEnabled: Boolean,
                    repeatMode: Int,
                ) = chromahub.rhythm.app.infrastructure.widget.glance.GlanceWidgetUpdater.updateWidget(
                    context = context,
                    song = song,
                    isPlaying = isPlaying,
                    hasPrevious = hasPrevious,
                    hasNext = hasNext,
                    isFavorite = isFavorite,
                    isShuffleEnabled = isShuffleEnabled,
                    repeatMode = repeatMode)

                override fun updateLyrics(context: android.content.Context, lyricTexts: List<String>, activeIndex: Int) =
                    chromahub.rhythm.app.infrastructure.widget.glance.GlanceWidgetUpdater.updateLyrics(context, lyricTexts, activeIndex)

                override fun updateAllWidgets(context: android.content.Context) =
                    chromahub.rhythm.app.infrastructure.widget.glance.GlanceWidgetUpdater.updateAllWidgets(context)
            }

        // Streaming playback seam for :core:media (repository lives in :feature:streaming).
        com.qlzhj.rhythm.core.media.infrastructure.service.PlaybackResolverInstaller.resolver =
            object : com.qlzhj.rhythm.core.media.infrastructure.service.StreamingPlaybackResolver {
                override suspend fun getStreamingUrl(songId: String): String? = streamingRepository.getStreamingUrl(songId)
                override fun buildUserTrustingHttpClientBuilder() =
                    chromahub.rhythm.app.features.streaming.data.provider.UserTrustManager.buildUserTrustingHttpClientBuilder()
            }

        // Haptics seam: :core:designsystem reads haptics setting through this gate
        // so its helpers stay decoupled from AppSettings (multi-module split PR-2).
        HapticsGate.isEnabled = { settings.isHapticEnabled.value }

        // Lyrics parsing seam for :core:media's LyricsParser (PR-5).
        com.qlzhj.rhythm.core.common.LyricsSettingsGate.apply {
            trimLyrics = { settings.trimLyrics.value }
            translationAutoWord = { settings.translationAutoWord.value }
        }

        // Apply the user's theme preference to the system configuration so the
        // system splash screen and window background match the app theme
        // (light in light mode, dark in dark mode) instead of always being dark.
        val nightMode = if (settings.useSystemTheme.value) {
            AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        } else if (settings.darkMode.value) {
            AppCompatDelegate.MODE_NIGHT_YES
        } else {
            AppCompatDelegate.MODE_NIGHT_NO
        }
        AppCompatDelegate.setDefaultNightMode(nightMode)
        Log.d(TAG, "✓ Applied night mode: $nightMode")
        
        // Initialize CrashReporter + its :app-side crash screen starter
        CrashReporter.crashScreenStarter = { context, crashLog ->
            chromahub.rhythm.app.activities.CrashActivity.start(context, crashLog)
        }
        CrashReporter.init(this)
        Log.d(TAG, "✓ CrashReporter initialized")
        
        // Install worker class references for AppSettings scheduling (:core:data
        // cannot see :app workers directly — multi-module split PR-3).
        com.qlzhj.rhythm.core.network.network.AppWorkerRegistry.apply {
            backupWorkerClass = com.qlzhj.rhythm.core.media.worker.BackupWorker::class.java
            updateNotificationWorkerClass = chromahub.rhythm.app.worker.UpdateNotificationWorker::class.java
            rhythmPulseWorkerClass = chromahub.rhythm.app.worker.RhythmPulseNotificationWorker::class.java
            backupWorkName = com.qlzhj.rhythm.core.media.worker.BackupWorker.WORK_NAME
            updateNotificationWorkName = chromahub.rhythm.app.worker.UpdateNotificationWorker.WORK_NAME
            rhythmPulseWorkName = chromahub.rhythm.app.worker.RhythmPulseNotificationWorker.WORK_NAME
        }
        
        // Initialize NetworkClient (settings/flags flow through Hilt + the
        // AppBuildInfo seam; version/token stay :app BuildConfig references).
        networkClientInitializer.initialize(
            BuildConfig.VERSION_NAME,
            BuildConfig.APPLE_MUSIC_FALLBACK_TOKEN,
        )
        Log.d(TAG, "✓ NetworkClient initialized")
        
        // Start the ANR watchdog for debug builds
        if (BuildConfig.DEBUG) {
            startANRWatchdog()
        }
        
        Log.d(TAG, "RhythmApplication initialization complete")
        
        // Trim caches on startup so storage stays within the user's chosen limit
        // even if the app was force-stopped (onDestroy trim never ran). Gated to
        // at most once per day so low-end devices don't walk the cache directories
        // on every single launch.
        val trimPrefs = getSharedPreferences("cache_trim", android.content.Context.MODE_PRIVATE)
        val lastTrimMs = trimPrefs.getLong("last_startup_trim_ms", 0L)
        val dayMs = 24L * 60 * 60 * 1000
        if (System.currentTimeMillis() - lastTrimMs >= dayMs) {
            trimPrefs.edit { putLong("last_startup_trim_ms", System.currentTimeMillis()) }
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    CacheManager.autoTrimCache(applicationContext, currentMaxCacheSize())
                } catch (e: Exception) {
                    Log.e(TAG, "Error during startup cache trim", e)
                }
            }
        }
    }

    /**
     * Returns the user-configured max cache size or a sane fallback if
     * settings are unavailable.
     */
    private fun currentMaxCacheSize(): Long {
        return try {
            AppSettings.getInstance(this).maxCacheSize.value
        } catch (e: Exception) {
            300L * 1024 * 1024
        }
    }

    /**
     * Bounded Coil ImageLoader used app-wide with on-demand audio artwork decoding.
     * The factory defaults keep the disk cache at ~2% of total disk space and the memory
     * cache at 25% of heap, which is far too large for an offline music app — bound both explicitly.
     */
    override fun newImageLoader(): ImageLoader {
        val imageOkHttpClient = chromahub.rhythm.app.features.streaming.data.provider.UserTrustManager
            .buildUserTrustingHttpClientBuilder()
            .dispatcher(Dispatcher().apply {
                maxRequests = 16
                maxRequestsPerHost = 4
            })
            .build()

        return ImageLoader.Builder(this)
            .okHttpClient(imageOkHttpClient)
            .components {
                add(com.qlzhj.rhythm.core.media.util.coil.AudioArtworkKeyer())
                add(com.qlzhj.rhythm.core.media.util.coil.StreamingArtworkKeyer())
                add(com.qlzhj.rhythm.core.media.util.coil.StreamingArtworkStringKeyer())
                add(com.qlzhj.rhythm.core.media.util.coil.AudioArtworkFetcher.Factory(applicationContext))
            }
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(0.15) // 15% of app heap (default is 25%)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("image_cache"))
                    .maxSizeBytes(128L * 1024 * 1024) // 128 MB (default is ~2% of disk)
                    .build()
            }
            .crossfade(true)
            .build()
    }

    /**
     * Start ANR watchdog to monitor UI thread responsiveness
     */
    private fun startANRWatchdog() {
        try {
            // Start with 5 second timeout (standard ANR threshold)
            anrWatchdog = ANRWatchdog(timeoutMs = 5000).apply {
                start()
            }
            Log.d(TAG, "✓ ANR Watchdog started")
        } catch (e: Exception) {
            Log.e(TAG, "Error starting ANR Watchdog", e)
        }
    }
    
    override fun onTerminate() {
        Log.d(TAG, "RhythmApplication onTerminate")
        
        // Stop ANR watchdog
        anrWatchdog?.stopWatching()
        anrWatchdog = null
        
        super.onTerminate()
    }
    
    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        
        val levelName = when {
            level == ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN -> "UI_HIDDEN"
            level == ComponentCallbacks2.TRIM_MEMORY_BACKGROUND -> "BACKGROUND"
            level == 5 -> "RUNNING_MODERATE"
            level == 10 -> "RUNNING_LOW"
            level == 15 -> "RUNNING_CRITICAL"
            level == 60 -> "MODERATE"
            level == 80 -> "COMPLETE"
            else -> "UNKNOWN($level)"
        }
        
        Log.w(TAG, "onTrimMemory: $levelName")
        
        // Trim levels are inverted: lower numbers mean MORE pressure (5 = critical,
        // 10 = low, 15 = moderate). Release image caches whenever the app is under
        // real pressure OR fully backgrounded — this is the main OOM fix when many
        // apps are open.
        val isBackgroundOrCritical = level >= ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN
        if (isBackgroundOrCritical) {
            CoroutineScope(Dispatchers.Main).launch {
                try {
                    Coil.imageLoader(applicationContext).memoryCache?.clear()
                } catch (e: Exception) {
                    Log.w(TAG, "Error clearing Coil memory cache", e)
                }
                MediaUtils.clearRawArtworkCache()
                GlanceShapeBitmaps.clearCache()
                RhythmMusicWidget.clearArtCache()
                RhythmCookieWidget.clearArtCache()
            }
        }
        
        // Light cleanup when the app moves to the background
        if (level == ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN || level == ComponentCallbacks2.TRIM_MEMORY_BACKGROUND) {
            Log.d(TAG, "App backgrounded - trimming caches")
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    CacheManager.autoTrimCache(applicationContext, currentMaxCacheSize())
                } catch (e: Exception) {
                    Log.e(TAG, "Error during background cache trim", e)
                }
            }
        }
    }
}
