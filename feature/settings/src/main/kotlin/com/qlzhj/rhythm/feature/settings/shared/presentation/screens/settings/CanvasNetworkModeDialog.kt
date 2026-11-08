@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)

package com.qlzhj.rhythm.feature.settings.shared.presentation.screens.settings

import com.qlzhj.rhythm.core.ui.shared.presentation.components.bottomsheets.AdaptiveSheetScrollContainer
import com.qlzhj.rhythm.core.ui.shared.presentation.components.bottomsheets.RhythmAdaptiveModalSheet
import com.qlzhj.rhythm.core.ui.shared.presentation.components.bottomsheets.SheetAdaptiveType
import com.qlzhj.rhythm.core.ui.shared.presentation.components.bottomsheets.StandardBottomSheetHeader
import com.qlzhj.rhythm.core.ui.shared.presentation.components.bottomsheets.groupedBottomSheetItemShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.qlzhj.rhythm.feature.settings.R
import com.qlzhj.rhythm.core.data.shared.data.model.AppSettings
import com.qlzhj.rhythm.core.model.CanvasNetworkMode
import com.qlzhj.rhythm.core.designsystem.icons.Icon
import com.qlzhj.rhythm.core.designsystem.icons.MaterialSymbolIcon
import com.qlzhj.rhythm.core.designsystem.icons.RhythmIcons
import com.qlzhj.rhythm.core.designsystem.utils.HapticType
import com.qlzhj.rhythm.core.designsystem.utils.HapticUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CanvasNetworkModeDialog(
    onDismiss: () -> Unit,
    appSettings: AppSettings,
    context: Context,
    haptic: HapticFeedback
) {
    val canvasNetworkMode by appSettings.appleCanvasNetworkMode.collectAsState()
    val sheetState = rememberBottomSheetState(initialValue = SheetValue.Hidden, enabledValues = setOf(SheetValue.Hidden, SheetValue.Expanded))

    RhythmAdaptiveModalSheet(
        adaptiveType = SheetAdaptiveType.COMPACT_DIALOG,
        modifier = Modifier.widthIn(max = 640.dp).fillMaxWidth(),
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = {
            BottomSheetDefaults.DragHandle(
                color = MaterialTheme.colorScheme.primary
            )
        },
        containerColor = MaterialTheme.colorScheme.surfaceContainer
    ) {
        StandardBottomSheetHeader(
            title = context.getString(R.string.canvas_connectivity),
            subtitle = context.getString(R.string.canvas_connectivity_desc),
            visible = true
        )

        val scrollState = rememberScrollState()

        AdaptiveSheetScrollContainer(
            scrollState = scrollState,
            modifier = Modifier.fillMaxWidth()
        ) { endPadding ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(scrollState)
                    .padding(start = 24.dp, end = 24.dp + endPadding, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Options
                val sourceOptions = listOf(
                    CanvasNetworkMode.WIFI_ONLY to Triple(
                        stringResource(R.string.api_apple_canvas_wifi),
                        stringResource(R.string.canvas_mode_wifi_desc),
                        RhythmIcons.Connectivity.Wifi
                    ),
                    CanvasNetworkMode.BOTH to Triple(
                        stringResource(R.string.api_apple_canvas_both),
                        stringResource(R.string.canvas_mode_both_desc),
                        RhythmIcons.Public
                    )
                )

                sourceOptions.forEachIndexed { index, (preference, info) ->
                    val (title, description, icon) = info
                    val isSelected = canvasNetworkMode == preference

                    Card(
                        onClick = {
                            HapticUtils.performHapticFeedback(context, haptic, HapticType.LIGHT)
                            appSettings.setAppleCanvasNetworkMode(preference)
                        },
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected)
                                MaterialTheme.colorScheme.primaryContainer
                            else
                                MaterialTheme.colorScheme.surfaceContainerHigh
                        ),
                        shape = groupedBottomSheetItemShape(index, sourceOptions.size),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = if (isSelected)
                                    MaterialTheme.colorScheme.onPrimaryContainer
                                else
                                    MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(if (isSelected) 30.dp else 26.dp)
                            )

                            Spacer(modifier = Modifier.width(16.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = title,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (isSelected)
                                        MaterialTheme.colorScheme.onPrimaryContainer
                                    else
                                        MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (isSelected)
                                        MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                                    else
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            if (isSelected) {
                                Icon(
                                    imageVector = RhythmIcons.CheckCircle,
                                    contentDescription = context.getString(R.string.cd_selected),
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Info card
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            imageVector = MaterialSymbolIcon("lightbulb", filled = true),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = context.getString(R.string.canvas_network_note),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
            }
        }
    }
}
