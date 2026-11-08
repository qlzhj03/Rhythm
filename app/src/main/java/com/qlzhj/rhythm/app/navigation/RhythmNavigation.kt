package com.qlzhj.rhythm.app.navigation
import androidx.hilt.navigation.compose.hiltViewModel
import com.qlzhj.rhythm.app.rhythmguard.RhythmGuardWarningHost
import androidx.compose.animation.AnimatedContent
import com.qlzhj.rhythm.feature.streaming.presentation.viewmodel.StreamingMusicViewModel
import androidx.compose.animation.core.EaseInOutQuart
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.lifecycle.viewmodel.compose.viewModel
import com.qlzhj.rhythm.feature.local.presentation.viewmodel.MusicViewModel
import com.qlzhj.rhythm.core.ui.shared.presentation.viewmodel.ThemeViewModel
import com.qlzhj.rhythm.core.data.shared.data.model.AppSettings
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.WindowInsetsSides
import com.qlzhj.rhythm.core.designsystem.LocalMiniPlayerPadding




/**
 * Main navigation composable that handles switching between Local and Streaming modes.
 * Acts as the root navigation for the entire app.
 */
@Composable
fun RhythmNavigation(
    modifier: Modifier = Modifier,
    musicViewModel: MusicViewModel = hiltViewModel(),
    themeViewModel: ThemeViewModel = hiltViewModel(),
    streamingMusicViewModel: StreamingMusicViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val appSettings = remember { AppSettings.getInstance(context) }
    val appMode by appSettings.appMode.collectAsState()
    
    // Create a NavHostController that can be passed to both local and streaming navigation
    val rootNavController = rememberNavController()
    
    // Settings navigation callback that works for both modes
    val navigateToSettings: () -> Unit = {
        rootNavController.navigate("settings") {
            launchSingleTop = true
        }
    }
    
    val defaultMiniPlayerPadding = WindowInsets.navigationBars
        .only(WindowInsetsSides.Bottom)
        .asPaddingValues()

    // Switch between Local and Streaming navigation based on app mode with animated transitions.
    // RhythmGuardWarningHost is mounted at root so warnings appear above all screens and bottom sheets.
    CompositionLocalProvider(LocalMiniPlayerPadding provides defaultMiniPlayerPadding) {
        Box(modifier = modifier.fillMaxSize()) {
        NavHost(
            navController = rootNavController,
            startDestination = "main",
            modifier = Modifier.fillMaxSize(),
            predictivePopEnterTransition = {
                fadeIn(animationSpec = tween(300))
            },
            predictivePopExitTransition = {
                fadeOut(animationSpec = tween(300)) +
                    slideOutVertically(
                        targetOffsetY = { it / 4 },
                        animationSpec = tween(350, easing = EaseInOutQuart)
                    )
            }
        ) {
            composable("main") {
                AnimatedContent(
                    targetState = appMode,
                    modifier = Modifier.fillMaxSize(),
                    transitionSpec = {
                        fadeIn(
                            animationSpec = tween(400)
                        ) + slideInHorizontally(
                            initialOffsetX = { if (targetState == "STREAMING") it else -it },
                            animationSpec = tween(500, easing = EaseInOutQuart)
                        ) togetherWith fadeOut(
                            animationSpec = tween(400)
                        ) + slideOutHorizontally(
                            targetOffsetX = { if (targetState == "STREAMING") -it else it },
                            animationSpec = tween(500, easing = EaseInOutQuart)
                        )
                    },
                    label = "appModeTransition"
                ) { mode ->
                    when (mode) {
                        "STREAMING" -> {
                            // Streaming (Go) content is hosted on the shared Home/Library screens
                            LocalNavigation(
                                viewModel = musicViewModel,
                                themeViewModel = themeViewModel,
                                appSettings = appSettings,
                                streamingMusicViewModel = streamingMusicViewModel
                            )
                        }

                        else -> {
                            // Use LocalNavigation for local mode (default)
                            LocalNavigation(
                                viewModel = musicViewModel,
                                themeViewModel = themeViewModel,
                                appSettings = appSettings,
                                streamingMusicViewModel = streamingMusicViewModel
                            )
                        }
                    }
                }
            }

            // Settings screen (shared between both modes)
            composable(
                route = "settings",
                enterTransition = {
                    fadeIn(animationSpec = tween(300)) +
                        slideInVertically(
                            initialOffsetY = { it / 4 },
                            animationSpec = tween(350, easing = EaseInOutQuart)
                        )
                },
                exitTransition = {
                    fadeOut(animationSpec = tween(300))
                },
                popExitTransition = {
                    fadeOut(animationSpec = tween(300)) +
                        slideOutVertically(
                            targetOffsetY = { it / 4 },
                            animationSpec = tween(350, easing = EaseInOutQuart)
                        )
                }
            ) {
                com.qlzhj.rhythm.feature.settings.shared.presentation.screens.settings.SettingsScreenWrapper(
                    onBack = {
                        val popped = rootNavController.popBackStack()
                        if (!popped) {
                            rootNavController.navigate("main") {
                                launchSingleTop = true
                            }
                        }
                    },
                    appSettings = appSettings,
                    navController = rootNavController,
                    playbackState = musicViewModel,
                    playbackSettings = musicViewModel,
                    playlistAdmin = musicViewModel,
                    libraryAdmin = musicViewModel,
                    cacheManager = musicViewModel,
                    backupRestore = musicViewModel,
                    sleepTimer = musicViewModel
                )
            }
        }

        RhythmGuardWarningHost(
            appSettings = appSettings,
            musicViewModel = musicViewModel
        )
        }
    }
}


