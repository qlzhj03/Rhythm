package com.qlzhj.rhythm.app.shared.presentation.theme.festive

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.material3.SliderState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.qlzhj.rhythm.core.designsystem.theme.festive.*
import com.qlzhj.rhythm.app.R
import androidx.compose.ui.res.stringResource

/**
 * Example implementations of the Festive Theme Engine
 * These examples demonstrate various ways to use festive decorations
 */

// Example 1: Basic Christmas snowfall with default settings
@Composable
fun ExampleBasicChristmasSnowfall() {
    ChristmasSnowfallOverlay(
        enabled = true,
        intensity = 0.5f
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = stringResource(R.string.festivesplashgreeting_merry_christmas),
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onBackground
            )
        }
    }
}

// Example 2: Manual festive configuration
@Composable
fun ExampleManualConfiguration() {
    val config = FestiveConfig(
        type = FestiveThemeType.CHRISTMAS,
        intensity = 0.7f,
        enabled = true,
        autoDetect = false
    )
    
    FestiveOverlay(config = config) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = stringResource(R.string.festivethemeexamples_custom_festive_theme),
                style = MaterialTheme.typography.headlineMedium
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.ui_intensity_percent, (config.intensity * 100).toInt()),
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}

// Example 3: With auto-detection
@Composable
fun ExampleAutoDetection() {
    val config = FestiveConfig(
        type = FestiveThemeType.NONE, // Will be overridden by auto-detect
        intensity = 0.6f,
        enabled = true,
        autoDetect = true
    )
    
    val activeTheme = FestiveThemeEngine.getActiveFestiveTheme(config)
    
    FestiveOverlay(config = config) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = stringResource(R.string.festivethemeexamples_autodetected_theme),
                style = MaterialTheme.typography.headlineMedium
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = when (activeTheme) {
                    FestiveThemeType.CHRISTMAS -> "🎄 " + stringResource(R.string.festive_christmas)
                    FestiveThemeType.NEW_YEAR -> "🎉 " + stringResource(R.string.festive_new_year)
                    FestiveThemeType.HALLOWEEN -> "🎃 " + stringResource(R.string.festive_halloween)
                    FestiveThemeType.VALENTINES -> "❤️ " + stringResource(R.string.festive_valentines)
                    FestiveThemeType.NONE -> stringResource(R.string.festive_no_theme)
                    else -> stringResource(R.string.festive_custom_theme)
                },
                style = MaterialTheme.typography.headlineSmall
            )
        }
    }
}

// Example 4: Interactive intensity control
@Composable
fun ExampleInteractiveIntensity() {
    var intensity by remember { mutableFloatStateOf(0.5f) }
    
    ChristmasSnowfallOverlay(
        enabled = true,
        intensity = intensity
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = stringResource(R.string.festivethemeexamples_adjust_snowfall),
                style = MaterialTheme.typography.headlineMedium
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            Text(
                text = stringResource(R.string.ui_intensity_percent, (intensity * 100).toInt()),
                style = MaterialTheme.typography.bodyLarge
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            val intensitySliderState = remember(intensity) {
                SliderState(
                    value = intensity,
                    trackRange = 0.1f..1f
                )
            }
            intensitySliderState.value = intensity
            Slider(
                state = intensitySliderState,
                onValueChange = { intensity = it },
                modifier = Modifier.fillMaxWidth(0.8f)
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Button(onClick = { intensity = 0.3f }) {
                    Text(stringResource(R.string.settings_theme_light))
                }
                Button(onClick = { intensity = 0.5f }) {
                    Text(stringResource(R.string.festivethemeexamples_medium))
                }
                Button(onClick = { intensity = 0.8f }) {
                    Text(stringResource(R.string.festivethemeexamples_heavy))
                }
            }
        }
    }
}

// Example 5: Lightweight performance mode
@Composable
fun ExampleLightweightMode() {
    val config = FestiveConfig(
        type = FestiveThemeType.CHRISTMAS,
        intensity = 0.4f, // Reduced for performance
        enabled = true,
        autoDetect = false
    )
    
    LightweightFestiveOverlay(config = config) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stringResource(R.string.festivethemeexamples_performance_mode),
                    style = MaterialTheme.typography.headlineMedium
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.festivethemeexamples_optimized_for_lowerend_devices),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// Example 6: Toggle festive decorations
@Composable
fun ExampleToggleable() {
    var enabled by remember { mutableStateOf(true) }
    
    ChristmasSnowfallOverlay(
        enabled = enabled,
        intensity = 0.6f
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = if (enabled) stringResource(R.string.ui_snowfall_active) else stringResource(R.string.ui_no_decorations),
                style = MaterialTheme.typography.headlineMedium
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            Switch(
                checked = enabled,
                onCheckedChange = { enabled = it }
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = if (enabled) stringResource(R.string.festive_decorations_on) else stringResource(R.string.festive_decorations_off),
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

// Example 7: Multiple theme types
@Composable
fun ExampleThemeSelector() {
    var selectedTheme by remember { mutableStateOf(FestiveThemeType.CHRISTMAS) }
    
    val config = FestiveConfig(
        type = selectedTheme,
        intensity = 0.6f,
        enabled = true,
        autoDetect = false
    )
    
    FestiveOverlay(config = config) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = stringResource(R.string.festivethemeexamples_select_theme),
                style = MaterialTheme.typography.headlineMedium
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { selectedTheme = FestiveThemeType.CHRISTMAS },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.festivethemeexamples_christmas))
                }
                
                Button(
                    onClick = { selectedTheme = FestiveThemeType.NEW_YEAR },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.festivethemeexamples_new_year))
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = stringResource(R.string.ui_current_theme, selectedTheme.name),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

// Example 8: Using with AppSettings
@Composable
fun ExampleWithAppSettings() {
    // This automatically reads from and respects AppSettings
    FestiveOverlayFromSettings {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = stringResource(R.string.festivethemeexamples_using_app_settings),
                style = MaterialTheme.typography.headlineMedium
            )
        }
    }
}

/**
 * Preview composable showing all examples
 * Note: This is for demonstration purposes
 */
@Composable
fun FestiveThemeExamples() {
    var selectedExample by remember { mutableIntStateOf(0) }
    
    when (selectedExample) {
        0 -> ExampleBasicChristmasSnowfall()
        1 -> ExampleManualConfiguration()
        2 -> ExampleAutoDetection()
        3 -> ExampleInteractiveIntensity()
        4 -> ExampleLightweightMode()
        5 -> ExampleToggleable()
        6 -> ExampleThemeSelector()
        7 -> ExampleWithAppSettings()
    }
}
