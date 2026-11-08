package com.qlzhj.rhythm.core.common

fun rhythmGuardFormatDurationFromMinutes(minutes: Int, useHoursFormat: Boolean = true): String {
    val safeMinutes = minutes.coerceAtLeast(0)
    if (!useHoursFormat) {
        return "${safeMinutes}m"
    }
    val days = safeMinutes / (24 * 60)
    val hours = (safeMinutes % (24 * 60)) / 60
    val mins = safeMinutes % 60

    return when {
        days > 0 && hours > 0 && mins > 0 -> "${days}d ${hours}h ${mins}m"
        days > 0 && hours > 0 -> "${days}d ${hours}h"
        days > 0 && mins > 0 -> "${days}d ${mins}m"
        days > 0 -> "${days}d"
        hours > 0 && mins > 0 -> "${hours}h ${mins}m"
        hours > 0 -> "${hours}h"
        else -> "${mins}m"
    }
}
