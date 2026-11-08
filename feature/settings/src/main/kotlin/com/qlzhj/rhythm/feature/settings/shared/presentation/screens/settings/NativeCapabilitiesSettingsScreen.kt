package com.qlzhj.rhythm.feature.settings.shared.presentation.screens.settings

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.qlzhj.rhythm.feature.settings.R
import com.qlzhj.rhythm.core.media.core.device.AudioOutputCategory
import com.qlzhj.rhythm.core.media.core.device.DeviceCapabilitiesProvider
import com.qlzhj.rhythm.core.media.core.device.HiFiCapabilityChecker
import com.qlzhj.rhythm.core.ui.shared.presentation.components.common.CollapsibleHeaderScreen
import com.qlzhj.rhythm.core.ui.shared.presentation.components.common.ExpressiveCookieEmptyState
import com.qlzhj.rhythm.core.designsystem.icons.MaterialSymbolIcon
import com.qlzhj.rhythm.core.designsystem.icons.RhythmIcons
import com.qlzhj.rhythm.core.designsystem.LocalMiniPlayerPadding
import com.qlzhj.rhythm.core.designsystem.utils.HapticType
import com.qlzhj.rhythm.core.designsystem.utils.HapticUtils
import java.util.Locale
import kotlin.math.roundToInt

data class NativeGridItem(
    val label: String,
    val value: String,
    val subtitle: String? = null,
    val badge: String? = null,
    val isHighlighted: Boolean = false,
    val onClick: (() -> Unit)? = null
)

@Composable
fun NativeCapabilitiesSettingsScreen(
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current

    val copyToClipboard: (String, String) -> Unit = { label, text ->
        HapticUtils.performHapticFeedback(context, haptics, HapticType.LIGHT)
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText(label, text)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(context, context.getString(R.string.about_copied_format, label), Toast.LENGTH_SHORT).show()
    }

    val systemSummary = remember(context) { DeviceCapabilitiesProvider.getSystemSummary(context) }
    val capabilities = remember(context) { DeviceCapabilitiesProvider.getCapabilities(context) }
    val isFloatSupported = remember { HiFiCapabilityChecker.isSupported() }
    val allDecoders = remember(context) { DeviceCapabilitiesProvider.getAudioDecoders() }
    val hardwareDecoders = remember(allDecoders) { allDecoders.filter { it.isHardwareAccelerated } }

    val storageStats = remember {
        runCatching {
            val stat = StatFs(Environment.getDataDirectory().path)
            val total = stat.blockCountLong * stat.blockSizeLong
            val free = stat.availableBlocksLong * stat.blockSizeLong
            total to free
        }.getOrDefault(0L to 0L)
    }
    val totalStorageBytes = storageStats.first
    val freeStorageBytes = storageStats.second

    val displayMetrics = context.resources.displayMetrics
    val refreshRate = remember(context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            runCatching { context.display.refreshRate }.getOrDefault(60f)
        } else {
            @Suppress("DEPRECATION")
            runCatching {
                (context.getSystemService(Context.WINDOW_SERVICE) as? android.view.WindowManager)?.defaultDisplay?.refreshRate ?: 60f
            }.getOrDefault(60f)
        }
    }

    val hardwareItems = remember(systemSummary, totalStorageBytes, freeStorageBytes, refreshRate) {
        listOf(
            NativeGridItem(
                label = context.getString(R.string.ui_device_model),
                value = "${systemSummary.brand} ${systemSummary.model}",
                subtitle = context.getString(R.string.ui_manufacturer_format, Build.MANUFACTURER),
                onClick = { copyToClipboard(context.getString(R.string.ui_device), "${systemSummary.brand} ${systemSummary.model}") }
            ),
            NativeGridItem(
                label = context.getString(R.string.ui_soc_hardware),
                value = Build.HARDWARE.ifBlank { context.getString(R.string.common_unknown) },
                subtitle = context.getString(R.string.ui_board_format, Build.BOARD),
                onClick = { copyToClipboard(context.getString(R.string.ui_hardware), "${Build.HARDWARE} (${Build.BOARD})") }
            ),
            NativeGridItem(
                label = context.getString(R.string.ui_cpu_architecture),
                value = Build.SUPPORTED_ABIS.firstOrNull() ?: context.getString(R.string.common_unknown),
                subtitle = Build.SUPPORTED_ABIS.drop(1).take(2).joinToString(", ").ifBlank { null },
                onClick = { copyToClipboard(context.getString(R.string.ui_architecture), Build.SUPPORTED_ABIS.joinToString(", ")) }
            ),
            NativeGridItem(
                label = context.getString(R.string.ui_platform_release),
                value = context.getString(R.string.ui_android_version_format, Build.VERSION.RELEASE),
                subtitle = context.getString(R.string.ui_api_level_format, Build.VERSION.SDK_INT),
                onClick = { copyToClipboard(context.getString(R.string.ui_android_version), "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})") }
            ),
            NativeGridItem(
                label = context.getString(R.string.ui_system_ram),
                value = formatNativeSize(systemSummary.totalRamBytes),
                subtitle = context.getString(R.string.ui_available_ram_format, formatNativeSize(systemSummary.availableRamBytes)),
                onClick = { copyToClipboard(context.getString(R.string.ui_ram), "${formatNativeSize(systemSummary.totalRamBytes)} (${formatNativeSize(systemSummary.availableRamBytes)} Available)") }
            ),
            NativeGridItem(
                label = context.getString(R.string.ui_internal_storage),
                value = formatNativeSize(totalStorageBytes),
                subtitle = context.getString(R.string.ui_free_storage_format, formatNativeSize(freeStorageBytes)),
                onClick = { copyToClipboard(context.getString(R.string.ui_storage), "${formatNativeSize(totalStorageBytes)} (${formatNativeSize(freeStorageBytes)} Free)") }
            ),
            NativeGridItem(
                label = context.getString(R.string.ui_display_screen),
                value = "${displayMetrics.widthPixels} × ${displayMetrics.heightPixels}",
                subtitle = context.getString(R.string.ui_display_metrics_format, refreshRate.roundToInt(), displayMetrics.densityDpi),
                onClick = { copyToClipboard(context.getString(R.string.ui_display), "${displayMetrics.widthPixels}x${displayMetrics.heightPixels} @ ${refreshRate.roundToInt()}Hz") }
            ),
            NativeGridItem(
                label = context.getString(R.string.ui_security_patch),
                value = Build.VERSION.SECURITY_PATCH.ifBlank { context.getString(R.string.ui_current) },
                subtitle = context.getString(R.string.ui_build_id_format, Build.ID),
                onClick = { copyToClipboard(context.getString(R.string.ui_security_patch), Build.VERSION.SECURITY_PATCH) }
            )
        )
    }

    val pipelineItems = remember(capabilities, isFloatSupported) {
        val isOffloadSupported = capabilities.offloadSupportedFormats.isNotEmpty()
        listOf(
            NativeGridItem(
                label = context.getString(R.string.ui_native_hal_rate),
                value = context.getString(R.string.ui_hz_format, capabilities.nativeSampleRate),
                badge = "HAL",
                isHighlighted = true,
                subtitle = context.getString(R.string.nativecaps_hardware_clock),
                onClick = { copyToClipboard(context.getString(R.string.ui_native_hal_rate), "${capabilities.nativeSampleRate} Hz") }
            ),
            NativeGridItem(
                label = context.getString(R.string.ui_buffer_frame_burst),
                value = context.getString(R.string.ui_frames_format, capabilities.framesPerBuffer),
                badge = context.getString(R.string.ui_low_latency),
                subtitle = context.getString(R.string.nativecaps_burst_quantum),
                onClick = { copyToClipboard(context.getString(R.string.ui_native_buffer_size), "${capabilities.framesPerBuffer} frames") }
            ),
            NativeGridItem(
                label = context.getString(R.string.ui_dsp_audio_offload),
                value = if (isOffloadSupported) context.getString(R.string.ui_supported_formats_format, capabilities.offloadSupportedFormats.size) else context.getString(R.string.ui_unsupported),
                badge = if (isOffloadSupported) context.getString(R.string.ui_hw_dsp) else context.getString(R.string.ui_disabled),
                isHighlighted = isOffloadSupported,
                subtitle = if (isOffloadSupported) capabilities.offloadSupportedFormats.joinToString(", ") else context.getString(R.string.ui_direct_dac_power_saving),
                onClick = { copyToClipboard(context.getString(R.string.ui_dsp_audio_offload), if (isOffloadSupported) capabilities.offloadSupportedFormats.joinToString(", ") else context.getString(R.string.ui_unsupported)) }
            ),
            NativeGridItem(
                label = context.getString(R.string.ui_pro_audio_stack),
                value = if (capabilities.isProAudioSupported) context.getString(R.string.ui_supported) else context.getString(R.string.ui_unsupported),
                badge = context.getString(R.string.ui_feature),
                isHighlighted = capabilities.isProAudioSupported,
                subtitle = context.getString(R.string.ui_pro_audio_routing),
                onClick = { copyToClipboard(context.getString(R.string.ui_pro_audio_stack), if (capabilities.isProAudioSupported) context.getString(R.string.ui_supported) else context.getString(R.string.ui_unsupported)) }
            ),
            NativeGridItem(
                label = context.getString(R.string.ui_fast_track_audio),
                value = if (capabilities.isLowLatencySupported) context.getString(R.string.ui_supported) else context.getString(R.string.ui_unsupported),
                badge = context.getString(R.string.ui_fast_track),
                isHighlighted = capabilities.isLowLatencySupported,
                subtitle = context.getString(R.string.nativecaps_mixer_bypass),
                onClick = { copyToClipboard(context.getString(R.string.ui_low_latency), if (capabilities.isLowLatencySupported) context.getString(R.string.ui_supported) else context.getString(R.string.ui_unsupported)) }
            ),
            NativeGridItem(
                label = context.getString(R.string.ui_float_pcm),
                value = if (isFloatSupported) context.getString(R.string.ui_supported) else context.getString(R.string.ui_unsupported),
                badge = "IEEE 754",
                isHighlighted = isFloatSupported,
                subtitle = context.getString(R.string.nativecaps_full_dynamic),
                onClick = { copyToClipboard(context.getString(R.string.ui_float_pcm), if (isFloatSupported) context.getString(R.string.ui_supported) else context.getString(R.string.ui_unsupported)) }
            )
        )
    }

    val endpointItems = remember(capabilities.outputRoutes) {
        if (capabilities.outputRoutes.isNotEmpty()) {
            capabilities.outputRoutes.map { route ->
                NativeGridItem(
                    label = route.name,
                    value = route.category.name,
                    badge = route.category.name.uppercase(Locale.US),
                    isHighlighted = route.category == AudioOutputCategory.BuiltIn || route.category == AudioOutputCategory.Bluetooth || route.category == AudioOutputCategory.Usb,
                    subtitle = context.getString(R.string.ui_route_id_format, route.id.toString()),
                    onClick = { copyToClipboard(context.getString(R.string.ui_device), "${route.name} (${route.category})") }
                )
            }
        } else {
            listOf(
                NativeGridItem(
                    label = context.getString(R.string.playbacksheet_builtin_speaker),
                    value = context.getString(R.string.ui_available),
                    badge = context.getString(R.string.ui_speaker),
                    isHighlighted = true,
                    subtitle = context.getString(R.string.nativecaps_internal_speaker)
                ),
                NativeGridItem(
                    label = context.getString(R.string.ui_audio_headset),
                    value = context.getString(R.string.ui_ready),
                    badge = context.getString(R.string.ui_wired),
                    subtitle = context.getString(R.string.ui_wired_jack)
                )
            )
        }
    }

    val hwDecoderItems = remember(hardwareDecoders) {
        hardwareDecoders.map { dec ->
            NativeGridItem(
                label = dec.name,
                value = getNativeMimeDisplayName(dec.mimeType),
                badge = context.getString(R.string.ui_hardware_badge),
                isHighlighted = true,
                subtitle = context.getString(R.string.nativecaps_max_instances_format, dec.maxInstances),
                onClick = { copyToClipboard(context.getString(R.string.ui_hardware_badge), "${dec.name} (${dec.mimeType})") }
            )
        }
    }

    CollapsibleHeaderScreen(
        title = stringResource(R.string.settings_category_native_capabilities_title),
        showBackButton = true,
        onBackClick = onBackClick
    ) { modifier ->
        LazyColumn(
            contentPadding = PaddingValues(
                top = 8.dp,
                bottom = 24.dp + LocalMiniPlayerPadding.current.calculateBottomPadding()
            ),
            modifier = modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            item(key = "header_soc") {
                NativeSectionHeader(title = stringResource(R.string.settings_native_caps_soc))
            }
            item(key = "grid_soc") {
                NativeConnectedGrid(items = hardwareItems)
            }

            item(key = "header_pipeline") {
                NativeSectionHeader(title = stringResource(R.string.settings_native_caps_pipeline))
            }
            item(key = "grid_pipeline") {
                NativeConnectedGrid(items = pipelineItems)
            }

            item(key = "header_endpoints") {
                NativeSectionHeader(title = stringResource(R.string.settings_native_caps_endpoints))
            }
            item(key = "grid_endpoints") {
                NativeConnectedGrid(items = endpointItems)
            }

            item(key = "header_hw_decoders") {
                NativeSectionHeader(title = stringResource(R.string.settings_native_caps_hw_decoders))
            }
            if (hwDecoderItems.isEmpty()) {
                item(key = "empty_hw_decoders") {
                    ExpressiveCookieEmptyState(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 36.dp),
                        title = stringResource(R.string.settings_native_caps_no_decoders_title),
                        subtitle = stringResource(R.string.settings_native_caps_no_decoders_desc),
                        mainIcon = MaterialSymbolIcon("memory"),
                        accentIcon = MaterialSymbolIcon("audio_file"),
                        cornerIcon = RhythmIcons.Info,
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            } else {
                item(key = "grid_hw_decoders") {
                    NativeConnectedGrid(items = hwDecoderItems)
                }
            }
        }
    }
}

@Composable
private fun NativeSectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 2.dp, top = 16.dp, bottom = 4.dp)
    )
}

@Composable
private fun NativeConnectedGrid(
    items: List<NativeGridItem>,
    columns: Int = 2
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        items.chunked(columns).forEachIndexed { rowIndex, rowItems ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Max)
            ) {
                rowItems.forEachIndexed { colIndex, item ->
                    val index = rowIndex * columns + colIndex
                    NativeConnectedCard(
                        item = item,
                        shape = getNativeConnectedShape(index, items.size, columns),
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    )
                }
                if (rowItems.size < columns) {
                    Spacer(modifier = Modifier.weight((columns - rowItems.size).toFloat()))
                }
            }
        }
    }
}

@Composable
private fun NativeConnectedCard(
    item: NativeGridItem,
    shape: RoundedCornerShape,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current

    Card(
        modifier = modifier,
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .then(
                    if (item.onClick != null) {
                        Modifier.clickable {
                            HapticUtils.performHapticFeedback(context, haptics, HapticType.LIGHT)
                            item.onClick.invoke()
                        }
                    } else {
                        Modifier
                    }
                )
                .padding(14.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = item.label,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                if (item.badge != null) {
                    Surface(
                        color = if (item.isHighlighted) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(50),
                        modifier = Modifier.padding(start = 4.dp)
                    ) {
                        Text(
                            text = item.badge,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (item.isHighlighted) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = item.value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            if (item.subtitle != null) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = item.subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

private fun getNativeConnectedShape(index: Int, totalItems: Int, columns: Int = 2): RoundedCornerShape {
    if (totalItems <= 1) {
        return RoundedCornerShape(20.dp)
    }
    val row = index / columns
    val col = index % columns
    val totalRows = (totalItems + columns - 1) / columns
    val isFirstRow = row == 0
    val isLastRow = row == totalRows - 1
    val isLeftEdge = col == 0
    val isRightEdge = (col == columns - 1) || (index == totalItems - 1)

    val topStart = if (isFirstRow && isLeftEdge) 20.dp else 6.dp
    val topEnd = if (isFirstRow && isRightEdge) 20.dp else 6.dp
    val bottomStart = if (isLastRow && isLeftEdge) 20.dp else 6.dp
    val bottomEnd = if (isLastRow && isRightEdge) 20.dp else 6.dp

    return RoundedCornerShape(
        topStart = topStart,
        topEnd = topEnd,
        bottomStart = bottomStart,
        bottomEnd = bottomEnd
    )
}

private fun formatNativeSize(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    val gb = bytes.toDouble() / (1024 * 1024 * 1024)
    return if (gb >= 1.0) {
        String.format(Locale.US, "%.1f GB", gb)
    } else {
        val mb = bytes.toDouble() / (1024 * 1024)
        String.format(Locale.US, "%.0f MB", mb)
    }
}

private fun getNativeMimeDisplayName(mime: String): String {
    return when {
        mime.contains("flac", ignoreCase = true) -> "FLAC"
        mime.contains("opus", ignoreCase = true) -> "Opus"
        mime.contains("mp4a-latm", ignoreCase = true) || mime.contains("aac", ignoreCase = true) -> "AAC"
        mime.contains("mp3", ignoreCase = true) || mime.contains("mpeg", ignoreCase = true) -> "MP3"
        mime.contains("vorbis", ignoreCase = true) -> "Vorbis"
        mime.contains("raw", ignoreCase = true) -> "PCM RAW"
        else -> mime.substringAfterLast("/").substringAfterLast(".").uppercase()
    }
}

@Composable
fun DeviceCapabilitiesSettingsScreen(
    onBackClick: () -> Unit
) {
    NativeCapabilitiesSettingsScreen(onBackClick = onBackClick)
}
