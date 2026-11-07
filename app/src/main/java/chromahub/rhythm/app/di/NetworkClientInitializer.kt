package chromahub.rhythm.app.di

import android.content.Context
import chromahub.rhythm.app.core.common.AppBuildInfo
import chromahub.rhythm.app.network.NetworkClient
import chromahub.rhythm.app.network.NetworkFeatureFlags
import chromahub.rhythm.app.network.NetworkSettingsProvider
import chromahub.rhythm.app.shared.data.model.AppSettings
import dagger.Module
import dagger.Provides
import javax.inject.Inject
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt wiring for the network stack (multi-module split PR-6, previously a
 * manual NetworkClient.initialize block inside RhythmApplication). Version and
 * Apple-Music fallback token stay :app-only references (BuildConfig), while
 * flags/debug come through the AppBuildInfo seam.
 */
@Singleton
class NetworkClientInitializer @Inject constructor(
    private val settings: NetworkSettingsProvider,
) {

    fun initialize(appVersion: String, appleMusicFallbackToken: String) {
        NetworkClient.initialize(
            settings,
            NetworkFeatureFlags(
                youtubeMusic = AppBuildInfo.enableYouTubeMusic,
                lyrically = AppBuildInfo.enableLyricallyApi,
                deezer = AppBuildInfo.enableDeezer,
                lrclib = AppBuildInfo.enableLrclib,
                betterLyrics = AppBuildInfo.enableBetterLyrics,
                spotifySearch = AppBuildInfo.enableSpotifySearch,
                wikipedia = AppBuildInfo.enableWikipedia,
            ),
            appVersion,
            appleMusicFallbackToken,
            AppBuildInfo.isDebugBuild,
        )
    }
}

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideNetworkSettingsProvider(@ApplicationContext context: Context): NetworkSettingsProvider =
        AppSettings.getInstance(context)
}
