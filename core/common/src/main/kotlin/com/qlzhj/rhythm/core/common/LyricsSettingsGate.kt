package com.qlzhj.rhythm.core.common

/**
 * Settings access for lyric parsers in :core:media (LyricsParser). The
 * Application module installs resource-backed providers at startup so parser
 * utilities stay decoupled from RhythmApplication (multi-module split PR-5),
 * mirroring the AppStrings / HapticsGate seam pattern.
 */
object LyricsSettingsGate {
    var trimLyrics: () -> Boolean = { true }
    var translationAutoWord: () -> Boolean = { false }
}
