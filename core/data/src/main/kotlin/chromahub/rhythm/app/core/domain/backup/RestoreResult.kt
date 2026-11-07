package chromahub.rhythm.app.core.domain.backup

/**
 * Lifecycle state of a backup-restore operation, surfaced to UI as a
 * [StateFlow]. Extracted from its former home as a nested class of
 * MusicViewModel (B1 PR-8, docs/b1-musicviewmodel-seams-design.md) so the
 * BackupRestoreController seam and settings screens can reference it without
 * a feature dependency.
 */
sealed class RestoreResult {
    object Idle : RestoreResult()
    object Queued : RestoreResult()
    data class Success(val wasQueued: Boolean) : RestoreResult()
    data class Failure(val errorMessage: String) : RestoreResult()
}
