package com.qlzhj.rhythm.core.ui.shared.presentation.viewmodel

import android.app.Activity
import android.content.ContextWrapper
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModelStoreOwner
import androidx.hilt.navigation.compose.hiltViewModel

/**
 * Composable helper to get the AppUpdaterViewModel scoped to the Activity,
 * ensuring only a single instance exists across the app lifecycle.
 */
@Composable
fun rememberAppUpdaterViewModel(): AppUpdaterViewModel {
    val context = LocalContext.current
    val activity = remember(context) {
        var ctx = context
        while (ctx is ContextWrapper) {
            if (ctx is Activity) {
                break
            }
            ctx = ctx.baseContext
        }
        ctx as? Activity
    }
    return if (activity is ViewModelStoreOwner) {
        hiltViewModel(viewModelStoreOwner = activity)
    } else {
        hiltViewModel()
    }
}
