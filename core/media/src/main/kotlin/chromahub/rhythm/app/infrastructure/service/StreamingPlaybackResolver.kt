package chromahub.rhythm.app.infrastructure.service

import okhttp3.OkHttpClient

/**
 * Seam between the playback engine (:core:media) and the streaming backend
 * (StreamingMusicRepositoryImpl / UserTrustManager, :feature:streaming from PR-5).
 * :app installs the implementation at startup until Hilt wiring lands.
 */
interface StreamingPlaybackResolver {
    /** Resolves a playable URL for a streaming track id (self-hosted or provider). */
    suspend fun getStreamingUrl(songId: String): String?

    /** HttpClient builder trusting user-installed CAs (self-hosted servers). */
    fun buildUserTrustingHttpClientBuilder(): OkHttpClient.Builder
}
