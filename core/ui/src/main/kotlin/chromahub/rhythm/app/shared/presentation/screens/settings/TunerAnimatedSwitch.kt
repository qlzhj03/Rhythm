package chromahub.rhythm.app.shared.presentation.screens.settings

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.tween
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import com.qlzhj.rhythm.core.designsystem.icons.Icon
import com.qlzhj.rhythm.core.designsystem.icons.RhythmIcons
import com.qlzhj.rhythm.core.designsystem.utils.HapticUtils
import com.qlzhj.rhythm.core.designsystem.utils.HapticType

/**
 * Expressive animated switch used across settings rows, local device sheets and
 * streaming quality pickers. Extracted from TunerComponents (PR-5) so
 * :core:ui / :feature:local / :feature:streaming share one switch primitive.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TunerAnimatedSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val context = LocalContext.current
    val hapticFeedback = LocalHapticFeedback.current
    Switch(
        checked = checked,
        onCheckedChange = {
            HapticUtils.performHapticFeedback(context, hapticFeedback, HapticType.LIGHT)
            onCheckedChange(it)
        },
        enabled = enabled,
        modifier = modifier,
        thumbContent = {
            AnimatedContent(
                targetState = checked,
                transitionSpec = {
                    fadeIn(animationSpec = tween(220)) togetherWith fadeOut(animationSpec = tween(110))
                },
                label = "switch_icon_animation"
            ) { isChecked ->
                Icon(
                    imageVector = if (isChecked) RhythmIcons.Check else RhythmIcons.Close,
                    contentDescription = null,
                    modifier = Modifier.size(SwitchDefaults.IconSize)
                )
            }
        }
    )
}
