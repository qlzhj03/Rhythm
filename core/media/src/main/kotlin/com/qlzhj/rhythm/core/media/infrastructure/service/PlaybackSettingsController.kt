package com.qlzhj.rhythm.core.media.infrastructure.service

/**
 * Playback preference toggles owned by MusicViewModel (:feature:local),
 * consumed by PlaybackSettingsScreen through this seam instead of the
 * concrete ViewModel (B1 PR-8, docs/b1-musicviewmodel-seams-design.md §4.2).
 */
interface PlaybackSettingsController {
    fun setMonoAudioEnabled(enabled: Boolean)
    fun setUseSystemVolumeMode(enable: Boolean)
}
