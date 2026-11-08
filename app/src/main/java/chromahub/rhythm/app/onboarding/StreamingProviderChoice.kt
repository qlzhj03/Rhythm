package chromahub.rhythm.app.onboarding

import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import chromahub.rhythm.app.R
import chromahub.rhythm.app.shared.presentation.components.common.StreamingServiceOptions
import chromahub.rhythm.app.shared.presentation.components.Material3SettingsGroup
import chromahub.rhythm.app.shared.presentation.components.Material3SettingsItem
import chromahub.rhythm.app.core.designsystem.utils.HapticType
import chromahub.rhythm.app.core.designsystem.utils.HapticUtils

/**
 * 引导页「选择串流服务商」列表。选项由 [StreamingServiceOptions.defaults] 单一来源驱动，
 * 未来接入新的在线播放平台时只需在 defaults 中增加一项（并在 [com.qlzhj.rhythm.core.model.StreamingServiceId]
 * 登记规则），本列表与后续配置步骤会自动跟随，无需改动引导页代码。
 */
@Composable
fun StreamingProviderSelectionList(
    selectedProvider: String,
    onProviderSelected: (String) -> Unit
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current

    Material3SettingsGroup(
        items = StreamingServiceOptions.defaults.map { option ->
            Material3SettingsItem(
                leadingContent = {
                    Icon(
                        painter = painterResource(id = option.iconRes),
                        contentDescription = null,
                        tint = if (selectedProvider == option.id) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        modifier = Modifier.size(28.dp)
                    )
                },
                title = {
                    Text(
                        text = stringResource(option.nameRes),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                    )
                },
                description = {
                    Text(
                        text = stringResource(option.descriptionRes),
                        style = MaterialTheme.typography.bodyMedium
                    )
                },
                trailingContent = {
                    RadioButton(
                        selected = selectedProvider == option.id,
                        onClick = null,
                        colors = RadioButtonDefaults.colors(
                            selectedColor = MaterialTheme.colorScheme.primary,
                            unselectedColor = MaterialTheme.colorScheme.outline
                        )
                    )
                },
                onClick = {
                    HapticUtils.performHapticFeedback(context, haptic, HapticType.LIGHT)
                    onProviderSelected(option.id)
                }
            )
        },
        containerColor = MaterialTheme.colorScheme.surface
    )
}
