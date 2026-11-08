package com.qlzhj.rhythm.core.model

import com.qlzhj.rhythm.core.model.PlayableItem
import com.qlzhj.rhythm.core.model.PlaylistItem
import com.qlzhj.rhythm.core.model.SourceType

/**
 * Represents a playlist from a streaming service.
 */
data class StreamingPlaylist(
    override val id: String,
    override val name: String,
    override val description: String?,
    override val artworkUri: String?,
    override val songCount: Int,
    override val isEditable: Boolean,
    override val sourceType: SourceType,
    val externalId: String? = null,
    val owner: PlaylistOwner? = null,
    val isPublic: Boolean = true,
    val isCollaborative: Boolean = false,
    val followers: Long? = null,
    val snapshotId: String? = null, // For Spotify change tracking
    private val tracks: List<StreamingSong> = emptyList()
) : PlaylistItem {
    
    override suspend fun getSongs(): List<PlayableItem> = tracks
    
    /**
     * Get tracks if already loaded.
     */
    fun getTracks(): List<StreamingSong> = tracks
}

/**
 * Represents the owner of a playlist.
 */
data class PlaylistOwner(
    val id: String,
    val displayName: String,
    val imageUrl: String? = null,
    val isCurrentUser: Boolean = false
)
