package chromahub.rhythm.app.infrastructure.service

import chromahub.rhythm.app.core.model.Song

/**
 * Queue-mutation seam implemented by MusicViewModel (:feature:local) and
 * consumed by StreamingMusicViewModel (:feature:streaming) so the streaming
 * feature can delegate "play next / add to queue" without a feature-to-feature
 * dependency (multi-module split PR-5, per docs/multi-module-refactor-plan.md
 * §二.4).
 */
interface LocalQueueController {
    fun playNext(song: Song)
    fun addSongToQueue(song: Song)
}
