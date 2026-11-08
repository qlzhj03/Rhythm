package com.qlzhj.rhythm.core.media.infrastructure.service

/**
 * Holds the StreamingPlaybackResolver implementation installed by :app (the
 * streaming repository lives in :feature:streaming from PR-5 — multi-module split).
 */
object PlaybackResolverInstaller {
    @Volatile
    var resolver: StreamingPlaybackResolver? = null

    fun require(): StreamingPlaybackResolver =
        resolver ?: error("StreamingPlaybackResolver not installed by :app")
}
