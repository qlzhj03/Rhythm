package chromahub.rhythm.app.network

import androidx.work.ListenableWorker

/**
 * Worker class references owned by :app (workers live with the app manifest for
 * now). AppSettings schedules background work through these classes so it can
 * live in :core:data without depending on :app (multi-module split PR-3).
 * :app installs them once at startup (RhythmApplication).
 */
object AppWorkerRegistry {
    var backupWorkerClass: Class<out ListenableWorker>? = null
    var updateNotificationWorkerClass: Class<out ListenableWorker>? = null
    var rhythmPulseWorkerClass: Class<out ListenableWorker>? = null

    // Unique work names live with the worker implementations (:app) and must not drift.
    var backupWorkName: String = ""
    var updateNotificationWorkName: String = ""
    var rhythmPulseWorkName: String = ""
}
