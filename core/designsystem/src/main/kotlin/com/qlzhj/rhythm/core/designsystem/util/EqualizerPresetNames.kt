package com.qlzhj.rhythm.core.designsystem.util

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.qlzhj.rhythm.core.designsystem.R

/**
 * Localizes built-in equalizer preset names for display. Extracted from
 * EqualizerUtils.kt (:feature:local) in B1 PR-9 so :feature:settings can use
 * it without a feature dependency; lives in :core:designsystem because it
 * needs Compose stringResource and R — :core:media has neither.
 */
@Composable
fun getLocalizedPresetName(name: String): String {
    return when (name) {
        "Flat" -> stringResource(R.string.eq_preset_flat)
        "Rock" -> stringResource(R.string.eq_preset_rock)
        "Pop" -> stringResource(R.string.eq_preset_pop)
        "Jazz" -> stringResource(R.string.eq_preset_jazz)
        "Classical" -> stringResource(R.string.eq_preset_classical)
        "Electronic" -> stringResource(R.string.eq_preset_electronic)
        "Hip Hop" -> stringResource(R.string.eq_preset_hiphop)
        "Vocal" -> stringResource(R.string.eq_preset_vocal)
        "Bass Boost" -> stringResource(R.string.eq_preset_bass_boost)
        "Treble Boost" -> stringResource(R.string.eq_preset_treble_boost)
        "V-Shape" -> stringResource(R.string.eq_preset_v_shape)
        "Harman" -> stringResource(R.string.eq_preset_harman)
        "Custom" -> stringResource(R.string.eq_preset_custom)
        else -> name
    }
}
