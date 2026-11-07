package chromahub.rhythm.app.core.domain.repository

import chromahub.rhythm.app.core.domain.backup.RestoreResult
import chromahub.rhythm.app.shared.data.model.AppSettings
import kotlinx.coroutines.flow.StateFlow

/**
 * Backup/restore orchestration seam implemented by MusicViewModel
 * (:feature:local) and consumed by BackupRestoreSettingsScreen (B1 PR-8,
 * docs/b1-musicviewmodel-seams-design.md §4.2).
 */
interface BackupRestoreController {
    val restoreResult: StateFlow<RestoreResult>
    fun restoreFromBackup(
        backupJson: String,
        sections: AppSettings.BackupRestoreSections = AppSettings.BackupRestoreSections()
    )
    fun clearRestoreResult()
}
