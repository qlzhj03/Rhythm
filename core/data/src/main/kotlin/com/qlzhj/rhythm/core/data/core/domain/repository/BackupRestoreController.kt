package com.qlzhj.rhythm.core.data.core.domain.repository

import com.qlzhj.rhythm.core.data.core.domain.backup.RestoreResult
import com.qlzhj.rhythm.core.data.shared.data.model.AppSettings
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
