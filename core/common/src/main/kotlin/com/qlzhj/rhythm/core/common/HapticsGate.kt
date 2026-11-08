package com.qlzhj.rhythm.core.common

/**
 * Seam between UI-level haptics helpers (which live in :core:designsystem) and
 * the user's haptics setting (AppSettings, :core:data / :app). :app installs the
 * real provider once at startup; before installation — e.g. in JVM tests or
 * early process start — haptics default to enabled.
 */
object HapticsGate {
    var isEnabled: () -> Boolean = { true }
}
