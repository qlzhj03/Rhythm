package com.qlzhj.rhythm.core.common.shared.presentation.navigation

/**
 * Rhythm Guard risk classification shared between :app's RhythmNavigation
 * (warning host) and :feature:settings' RhythmGuardSettingsScreen. Extracted
 * from RhythmNavigation (PR-5).
 */
enum class RhythmGuardRiskLevel {
    LOW,
    MODERATE,
    HIGH,
    SEVERE
}

fun rhythmGuardResolveRiskLevel(
    currentVolumePercent: Int,
    safeThresholdPercent: Int,
    exposureMinutes: Int,
    exposureLimitMinutes: Int
): RhythmGuardRiskLevel {
    val safeVolume = safeThresholdPercent.coerceAtLeast(1)
    val safeExposure = exposureLimitMinutes.coerceAtLeast(1)

    val volumeRatio = currentVolumePercent.toFloat() / safeVolume.toFloat()
    val exposureRatio = exposureMinutes.toFloat() / safeExposure.toFloat()
    val combinedRatio = maxOf(volumeRatio, exposureRatio, (volumeRatio + exposureRatio) / 2f)

    return when {
        combinedRatio >= 1.50f -> RhythmGuardRiskLevel.SEVERE
        combinedRatio >= 1.25f -> RhythmGuardRiskLevel.HIGH
        combinedRatio >= 1.00f -> RhythmGuardRiskLevel.MODERATE
        else -> RhythmGuardRiskLevel.LOW
    }
}
