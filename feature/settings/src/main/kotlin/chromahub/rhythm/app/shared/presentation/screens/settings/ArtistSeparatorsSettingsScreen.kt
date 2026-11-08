@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)

package chromahub.rhythm.app.shared.presentation.screens.settings

import com.qlzhj.rhythm.core.designsystem.LocalMiniPlayerPadding
import androidx.compose.foundation.layout.PaddingValues
import com.qlzhj.rhythm.core.designsystem.icons.RhythmIcons
import com.qlzhj.rhythm.core.designsystem.icons.MaterialSymbolIcon
import com.qlzhj.rhythm.core.designsystem.icons.Icon

import android.content.Context
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.animation.core.Spring
import androidx.compose.foundation.BorderStroke
import chromahub.rhythm.app.R
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import com.qlzhj.rhythm.core.data.shared.data.model.AppSettings
import com.qlzhj.rhythm.core.common.ArtistSeparator
import com.qlzhj.rhythm.core.designsystem.utils.HapticUtils
import com.qlzhj.rhythm.core.designsystem.utils.HapticType
import com.qlzhj.rhythm.core.ui.shared.presentation.components.common.CollapsibleHeaderScreen
import com.qlzhj.rhythm.core.designsystem.components.ExpressiveFilterChip
import com.qlzhj.rhythm.core.ui.shared.presentation.components.common.RhythmButtonSize
import com.qlzhj.rhythm.core.ui.shared.presentation.components.common.RhythmButtonWeighted
import com.qlzhj.rhythm.core.ui.shared.presentation.components.common.RhythmGroupedButton
import com.qlzhj.rhythm.core.ui.shared.presentation.components.bottomsheets.ArtistDelimitersBottomSheet
import com.qlzhj.rhythm.core.designsystem.utils.LazyListStateSaver
import com.qlzhj.rhythm.core.ui.shared.presentation.components.Material3SettingsGroup
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArtistSeparatorsSettingsScreen(onBackClick: () -> Unit) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val appSettings = AppSettings.getInstance(context)
    val scope = rememberCoroutineScope()

    val artistSeparatorEnabled by appSettings.artistSeparatorEnabled.collectAsState()
    val artistSeparatorDelimiters by appSettings.artistSeparatorDelimiters.collectAsState()

    var showDelimiterBottomSheet by remember { mutableStateOf(false) }

    val currentDelimitersList = remember(artistSeparatorDelimiters) {
        ArtistSeparator.parseDelimiters(artistSeparatorDelimiters)
    }

    CollapsibleHeaderScreen(
        title = context.getString(R.string.artists_title),
        showBackButton = true,
        onBackClick = {
            HapticUtils.performHapticFeedback(context, haptic, HapticType.HEAVY)
            onBackClick()
        }
    ) { modifier ->
        val settingGroups = listOf(
            SettingGroup(
                title = context.getString(R.string.artist_multi_parsing),
                items = listOf(
                    SettingItem(
                        RhythmIcons.Artist,
                        context.getString(R.string.artist_enable_separation),
                        context.getString(R.string.artist_enable_separation_desc),
                        toggleState = artistSeparatorEnabled,
                        onToggleChange = {
                            HapticUtils.performHapticFeedback(context, haptic, HapticType.LIGHT)
                            appSettings.setArtistSeparatorEnabled(it)
                        }
                    ),
                    SettingItem(
                        RhythmIcons.Settings,
                        context.getString(R.string.artist_configure_delimiters),
                        context.getString(R.string.artist_current_delimiters, currentDelimitersList.joinToString(", ")),
                        onClick = {
                            HapticUtils.performHapticFeedback(context, haptic, HapticType.LIGHT)
                            showDelimiterBottomSheet = true
                        }
                    )
                ),
            )
        )

        val lazyListState = rememberSaveable(
            saver = LazyListStateSaver
        ) {
            androidx.compose.foundation.lazy.LazyListState()
        }

        LazyColumn(
            contentPadding = PaddingValues(bottom = 24.dp + LocalMiniPlayerPadding.current.calculateBottomPadding()),
            state = lazyListState,
            modifier = modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(horizontal = 24.dp)
        ) {
            items(
                items = settingGroups,
                key = { "setting_${it.title}" },
                contentType = { "settingGroup" }
            ) { group ->
                Spacer(modifier = Modifier.height(24.dp))

                val materialItems = if (group.title == context.getString(R.string.artist_multi_parsing)) {
                    buildList {
                        if (group.items.isNotEmpty()) {
                            add(toMaterial3SettingsItem(context = context, item = group.items[0], hapticFeedback = haptic))
                        }
                        if (artistSeparatorEnabled && group.items.size > 1) {
                            add(toMaterial3SettingsItem(context = context, item = group.items[1], hapticFeedback = haptic))
                        }
                    }
                } else {
                    group.items.map { item ->
                        toMaterial3SettingsItem(context = context, item = item, hapticFeedback = haptic)
                    }
                }

                Material3SettingsGroup(
                    title = group.title,
                    items = materialItems,
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                )
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = RhythmIcons.MusicNote,
                                contentDescription = null,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = context.getString(R.string.settings_about_multi_artist),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = context.getString(R.string.settings_multi_artist_parsing_info),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }

    if (showDelimiterBottomSheet) {
        ArtistDelimitersBottomSheet(
            onDismiss = { showDelimiterBottomSheet = false },
            appSettings = appSettings
        )
    }
}