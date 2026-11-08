package com.qlzhj.rhythm.core.media.infrastructure.service

import com.qlzhj.rhythm.core.model.BrowseCategory
import com.qlzhj.rhythm.core.model.StreamingSearchResults
import com.qlzhj.rhythm.core.model.StreamingSong
import kotlinx.coroutines.flow.StateFlow

/**
 * Narrow seam for streaming search consumed by :feature:local's
 * UniversalSearchScreen (D-phase D3): lets the unified search drive
 * streaming search and queue/like actions without a feature-to-feature
 * dependency. Members mirror the screen's verified consumption set.
 * Lives beside [LocalQueueController] — the two seams together bridge
 * the streaming and local features. Implemented by
 * StreamingMusicViewModel; :app passes the root instance.
 */
interface StreamingSearchController {
    fun search(query: String)

    fun likeSong(song: StreamingSong)

    fun unlikeSong(song: StreamingSong)

    fun playNext(song: StreamingSong, localQueueController: LocalQueueController)

    fun addSongToQueue(song: StreamingSong, localQueueController: LocalQueueController)

    val searchQuery: StateFlow<String>

    val searchResults: StateFlow<StreamingSearchResults>

    val isLoading: StateFlow<Boolean>

    val likedSongs: StateFlow<List<StreamingSong>>

    val browseCategories: StateFlow<List<BrowseCategory>>
}
