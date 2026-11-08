package com.qlzhj.rhythm.core.data.core.domain.repository

/**
 * Library cache maintenance seam implemented by MusicViewModel
 * (:feature:local) and consumed by CacheManagementSettingsScreen (B1 PR-8,
 * docs/b1-musicviewmodel-seams-design.md §4.2). The two media-store clears
 * and the Room count delegate one-to-one to MusicRepository; the lyrics
 * cache clear is owned by the ViewModel itself.
 */
interface LibraryCacheManager {
    fun clearInMemoryCaches()
    fun clearSongCacheData()
    suspend fun getRoomSongCount(): Int
    fun clearAllLyricsCache()
}
