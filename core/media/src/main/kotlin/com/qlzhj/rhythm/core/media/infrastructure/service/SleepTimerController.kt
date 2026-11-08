package com.qlzhj.rhythm.core.media.infrastructure.service

import kotlinx.coroutines.flow.StateFlow

/**
 * End-of-playback action selected for a sleep timer. Extracted from its
 * former home as a nested enum of MusicViewModel (B1 PR-8,
 * docs/b1-musicviewmodel-seams-design.md §4.2) so the seam can type it.
 */
enum class SleepAction {
    PAUSE, STOP, FADE_OUT
}

/**
 * Sleep timer state and control, implemented by MusicViewModel
 * (:feature:local) and consumed by SleepTimerBottomSheetNew across module
 * boundaries (B1 PR-8, docs/b1-musicviewmodel-seams-design.md §4.2).
 */
interface SleepTimerController {
    val sleepTimerActive: StateFlow<Boolean>
    val sleepTimerRemainingSeconds: StateFlow<Long>
    val sleepTimerTotalSeconds: StateFlow<Long>
    val sleepTimerAction: StateFlow<String>
    val serviceConnected: StateFlow<Boolean>
    fun startSleepTimer(minutes: Int, action: SleepAction)
    fun stopSleepTimer()
}
