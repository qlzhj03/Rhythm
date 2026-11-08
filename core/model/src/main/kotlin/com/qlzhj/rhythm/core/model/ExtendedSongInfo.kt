package com.qlzhj.rhythm.core.model

/**
 * Detailed technical metadata for a song, assembled by MediaUtils
 * (getExtendedSongInfo) and surfaced in SongInfoBottomSheet. Extracted from the
 * bottom sheet (PR-5) so the media layer can build it without a UI dependency.
 */
data class ExtendedSongInfo(
    val fileSize: Long = 0,
    val bitrate: String = "Unknown",
    val sampleRate: String = "Unknown",
    val format: String = "Unknown",
    val composer: String = "",
    val discNumber: Int = 0,
    val dateAdded: Long = 0,
    val dateModified: Long = 0,
    val filePath: String = "",
    val albumArtist: String = "",
    val year: Int = 0,
    val mimeType: String = "",
    val channels: String = "Unknown",
    val hasLyrics: Boolean = false,
    val genre: String = "",
    val isLossless: Boolean = false,
    val isDolby: Boolean = false,
    val isDTS: Boolean = false,
    val isHiRes: Boolean = false,
    val audioCodec: String = "Unknown",
    val formatName: String = "Unknown",
    val qualityType: String = "Unknown",
    val qualityLabel: String = "Unknown",
    val qualityDescription: String = "",
    val bitDepth: Int = 0,
    val qualityCategory: String = "Unknown"
)
