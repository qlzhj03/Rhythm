package chromahub.rhythm.app.core.common

/**
 * Application-level build constants injected by :app — :core:* / :feature:*
 * modules have no BuildConfig of their own (multi-module split). Feature-flag
 * fields mirror the BuildConfig.ENABLE_* constants; do not repurpose them.
 */
object AppBuildInfo {
    var applicationId: String = "chromahub.rhythm.app"

    /** Mirrors BuildConfig.VERSION_NAME (installed by :app at startup, PR-5). */
    var versionName: String = "0.0.0"

    /** Mirrors BuildConfig.VERSION_CODE. */
    var versionCode: Int = 0

    /** Mirrors BuildConfig.RELEASE_DATE. */
    var releaseDate: String = ""

    /** Mirrors BuildConfig.FLAVOR ("github"). */
    var flavor: String = "github"

    /** Mirrors BuildConfig.DEBUG. */
    var isDebugBuild: Boolean = false

    /** Mirrors BuildConfig.IS_NIGHTLY. */
    var isNightly: Boolean = false

    /** Mirrors BuildConfig.ENABLE_YOUTUBE_MUSIC. */
    var enableYouTubeMusic: Boolean = true

    /** Mirrors BuildConfig.ENABLE_LYRICALLY_API. */
    var enableLyricallyApi: Boolean = true

    /** Mirrors BuildConfig.ENABLE_DEEZER. */
    var enableDeezer: Boolean = true

    /** Mirrors BuildConfig.ENABLE_LRCLIB. */
    var enableLrclib: Boolean = true

    /** Mirrors BuildConfig.ENABLE_BETTERLYRICS. */
    var enableBetterLyrics: Boolean = true

    /** Mirrors BuildConfig.ENABLE_SPOTIFY_SEARCH. */
    var enableSpotifySearch: Boolean = true

    /** Mirrors BuildConfig.ENABLE_WIKIPEDIA. */
    var enableWikipedia: Boolean = true
}
