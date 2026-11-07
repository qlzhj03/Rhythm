package chromahub.rhythm.app.core.model

/**
 * Stages of library synchronization for streaming services.
 */
enum class StreamingSyncStage {
    Idle,
    Syncing,
    Complete,
    Error
}

/**
 * Live library sync progress state.
 */
data class StreamingSyncProgress(
    val isSyncing: Boolean = false,
    val current: Int = 0,
    val total: Int = 0,
    val songsCount: Int = 0,
    val stage: StreamingSyncStage = StreamingSyncStage.Idle
)
