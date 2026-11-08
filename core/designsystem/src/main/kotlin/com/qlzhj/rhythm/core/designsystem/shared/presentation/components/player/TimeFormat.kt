package com.qlzhj.rhythm.core.designsystem.shared.presentation.components.player

import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * Formats a duration in milliseconds as m:ss / h:mm:ss. Extracted from
 * PlayerComponents (PR-5) so :core:ui bottom sheets can share it with the
 * player feature without a feature dependency.
 */
fun formatDuration(durationMs: Long, useHoursFormat: Boolean = false): String {
    val totalMinutes = TimeUnit.MILLISECONDS.toMinutes(durationMs)
    val seconds = TimeUnit.MILLISECONDS.toSeconds(durationMs) -
            TimeUnit.MINUTES.toSeconds(totalMinutes)
    
    return if (useHoursFormat && totalMinutes >= 60) {
        val hours = totalMinutes / 60
        val minutes = totalMinutes % 60
        String.format(Locale.ROOT, "%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format(Locale.ROOT, "%d:%02d", totalMinutes, seconds)
    }
}
