package com.qlzhj.rhythm.core.common

/**
 * Localized-fallback labels needed by :core:data (AppSettings builds placeholder
 * Song entries). :app installs resource-backed providers at startup so the data
 * module stays resource-free (multi-module split PR-3).
 */
object AppStrings {
    var unresolvedTrack: () -> String = { "Unresolved track" }
    var unknownArtist: () -> String = { "Unknown Artist" }
    var unknownAlbum: () -> String = { "Unknown Album" }
}
