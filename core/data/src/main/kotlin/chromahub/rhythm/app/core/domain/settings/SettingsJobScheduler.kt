package chromahub.rhythm.app.core.domain.settings

import android.content.Context
import android.util.Log
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.OneTimeWorkRequest
import androidx.work.PeriodicWorkRequest
import androidx.work.WorkManager
import chromahub.rhythm.app.network.AppWorkerRegistry
import kotlinx.coroutines.flow.StateFlow
import java.util.concurrent.TimeUnit

/**
 * WorkManager scheduling for settings-driven background jobs (auto-backup,
 * update-notification checks, rhythm pulse), carved out of AppSettings
 * (B-phase B2) so the settings store stays pure storage. Bodies are moved
 * verbatim; reads go through the settings StateFlows passed at construction.
 */
internal class SettingsJobScheduler(
    private val context: Context,
    private val updatesEnabled: StateFlow<Boolean>,
    private val autoCheckForUpdates: StateFlow<Boolean>,
    private val updateNotificationsEnabled: StateFlow<Boolean>,
    private val updateStatusNotificationsEnabled: StateFlow<Boolean>,
    private val updateCheckIntervalHours: StateFlow<Int>,
    private val rhythmPulseNotificationIntervalHours: StateFlow<Int>,
) {
    fun shouldRunUpdateNotificationWorker(): Boolean {
        return updatesEnabled.value &&
            autoCheckForUpdates.value &&
            (updateNotificationsEnabled.value || updateStatusNotificationsEnabled.value)
    }

    fun scheduleAutoBackup() {
        try {
            val workRequest = PeriodicWorkRequest.Builder(requireNotNull(AppWorkerRegistry.backupWorkerClass) { "worker classes not installed" },
                7, TimeUnit.DAYS, // Repeat every 7 days
                1, TimeUnit.HOURS  // Flex interval of 1 hour
            ).build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                AppWorkerRegistry.backupWorkName,
                ExistingPeriodicWorkPolicy.KEEP, // Keep existing schedule if already running
                workRequest
            )

            Log.d("AppSettings", "Auto-backup scheduled: weekly backups enabled")
            Log.d("AppSettings", "Next backup will occur within the next 7 days")
        } catch (e: Exception) {
            Log.e("AppSettings", "Failed to schedule auto-backup", e)
        }
    }

    fun triggerImmediateBackup() {
        try {
            val workRequest = OneTimeWorkRequest.Builder(requireNotNull(AppWorkerRegistry.backupWorkerClass) { "worker classes not installed" })
                .build()

            WorkManager.getInstance(context).enqueue(workRequest)
            Log.d("AppSettings", "Immediate backup triggered")
        } catch (e: Exception) {
            Log.e("AppSettings", "Failed to trigger immediate backup", e)
        }
    }

    fun cancelAutoBackup() {
        try {
            WorkManager.getInstance(context).cancelUniqueWork(AppWorkerRegistry.backupWorkName)
            Log.d("AppSettings", "Auto-backup cancelled")
        } catch (e: Exception) {
            Log.e("AppSettings", "Failed to cancel auto-backup", e)
        }
    }

    fun scheduleUpdateNotificationWorker() {
        try {
            // Get the check interval from settings (default 6 hours)
            val intervalHours = updateCheckIntervalHours.value.toLong()

            val workRequest = PeriodicWorkRequest.Builder(requireNotNull(AppWorkerRegistry.updateNotificationWorkerClass) { "worker classes not installed" },
                intervalHours, TimeUnit.HOURS,
                30, TimeUnit.MINUTES // Flex interval
            ).build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                AppWorkerRegistry.updateNotificationWorkName,
                ExistingPeriodicWorkPolicy.UPDATE, // Update if interval changes
                workRequest
            )

            Log.d("AppSettings", "Update notification worker scheduled: checks every $intervalHours hours")
        } catch (e: Exception) {
            Log.e("AppSettings", "Failed to schedule update notification worker", e)
        }
    }

    fun scheduleRhythmPulseNotificationWorker() {
        try {
            val intervalHours = rhythmPulseNotificationIntervalHours.value.toLong().coerceIn(6L, 72L)

            val workRequest = PeriodicWorkRequest.Builder(requireNotNull(AppWorkerRegistry.rhythmPulseWorkerClass) { "worker classes not installed" },
                intervalHours, TimeUnit.HOURS,
                1, TimeUnit.HOURS
            ).build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                AppWorkerRegistry.rhythmPulseWorkName,
                ExistingPeriodicWorkPolicy.UPDATE,
                workRequest
            )

            Log.d("AppSettings", "Rhythm tips worker scheduled: every $intervalHours hours")
        } catch (e: Exception) {
            Log.e("AppSettings", "Failed to schedule Rhythm tips worker", e)
        }
    }

    fun cancelRhythmPulseNotificationWorker() {
        try {
            WorkManager.getInstance(context).cancelUniqueWork(AppWorkerRegistry.rhythmPulseWorkName)
            Log.d("AppSettings", "Rhythm tips worker cancelled")
        } catch (e: Exception) {
            Log.e("AppSettings", "Failed to cancel Rhythm tips worker", e)
        }
    }

    fun cancelUpdateNotificationWorker() {
        try {
            WorkManager.getInstance(context).cancelUniqueWork(
                AppWorkerRegistry.updateNotificationWorkName
            )
            Log.d("AppSettings", "Update notification worker cancelled")
        } catch (e: Exception) {
            Log.e("AppSettings", "Failed to cancel update notification worker", e)
        }
    }
}
