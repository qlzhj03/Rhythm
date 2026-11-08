package com.qlzhj.rhythm.core.data.core.domain.settings

import android.content.Context
import android.net.Uri
import android.util.Log

/**
 * CSV codec for per-song lyrics preferences and custom LRC file names,
 * carved out of AppSettings (B-phase B2): pure file I/O and parsing — the
 * settings store keeps ownership of state, keys and persistence.
 */
internal object LyricsPreferenceIo {
    fun export(
        currentPrefs: Map<String, String>,
        customFiles: Map<String, String>,
        context: Context,
        uri: Uri
    ): Boolean {
        return try {
            val contentResolver = context.contentResolver
            contentResolver.openOutputStream(uri)?.use { outputStream ->
                val writer = java.io.BufferedWriter(java.io.OutputStreamWriter(outputStream))
                writer.write("Song ID,Title,Artist,Source Preference,Custom LRC Filename\n")

                val allSongIds = currentPrefs.keys + customFiles.keys

                val songMeta = mutableMapOf<String, Pair<String, String>>()
                try {
                    val projection = arrayOf(android.provider.MediaStore.Audio.Media._ID, android.provider.MediaStore.Audio.Media.TITLE, android.provider.MediaStore.Audio.Media.ARTIST)
                    context.contentResolver.query(
                        android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                        projection,
                        null,
                        null,
                        null
                    )?.use { cursor ->
                        val idCol = cursor.getColumnIndex(android.provider.MediaStore.Audio.Media._ID)
                        val titleCol = cursor.getColumnIndex(android.provider.MediaStore.Audio.Media.TITLE)
                        val artistCol = cursor.getColumnIndex(android.provider.MediaStore.Audio.Media.ARTIST)
                        while (cursor.moveToNext()) {
                            val id = cursor.getString(idCol)
                            val title = cursor.getString(titleCol).orEmpty()
                            val artist = cursor.getString(artistCol).orEmpty()
                            songMeta[id] = Pair(title, artist)
                        }
                    }
                } catch (e: Exception) {
                    Log.e("AppSettings", "Error querying song metadata for CSV export", e)
                }

                for (id in allSongIds) {
                    val meta = songMeta[id]
                    val title = meta?.first?.replace("\"", "\"\"") ?: ""
                    val artist = meta?.second?.replace("\"", "\"\"") ?: ""
                    val pref = currentPrefs[id] ?: ""
                    val file = customFiles[id] ?: ""

                    writer.write("\"$id\",\"$title\",\"$artist\",\"$pref\",\"$file\"\n")
                }
                writer.flush()
            }
            true
        } catch (e: Exception) {
            Log.e("AppSettings", "Failed to export lyrics preferences to CSV", e)
            false
        }
    }

    /**
     * Merges the CSV rows at [uri] into copies of [currentPrefs]/[currentFiles].
     * Returns the merged maps, or null on failure.
     */
    fun import(
        currentPrefs: Map<String, String>,
        currentFiles: Map<String, String>,
        context: Context,
        uri: Uri
    ): Pair<Map<String, String>, Map<String, String>>? {
        return try {
            val contentResolver = context.contentResolver
            val prefs = currentPrefs.toMutableMap()
            val files = currentFiles.toMutableMap()
            contentResolver.openInputStream(uri)?.use { inputStream ->
                val reader = java.io.BufferedReader(java.io.InputStreamReader(inputStream))
                val header = reader.readLine() // Read header

                var line = reader.readLine()
                while (line != null) {
                    val tokens = parseCsvLine(line)
                    if (tokens.size >= 5) {
                        val id = tokens[0].trim()
                        val pref = tokens[3].trim()
                        val file = tokens[4].trim()

                        if (id.isNotEmpty()) {
                            if (pref.isNotEmpty()) {
                                prefs[id] = pref
                            } else {
                                prefs.remove(id)
                            }

                            if (file.isNotEmpty()) {
                                files[id] = file
                            } else {
                                files.remove(id)
                            }
                        }
                    }
                    line = reader.readLine()
                }
            }
            Pair(prefs, files)
        } catch (e: Exception) {
            Log.e("AppSettings", "Failed to import lyrics preferences from CSV", e)
            null
        }
    }

    private fun parseCsvLine(line: String): List<String> {
        val result = mutableListOf<String>()
        var inQuotes = false
        var currentToken = StringBuilder()
        var i = 0
        while (i < line.length) {
            val c = line[i]
            if (c == '"') {
                if (inQuotes && i + 1 < line.length && line[i + 1] == '"') {
                    currentToken.append('"')
                    i++ // Skip next quote
                } else {
                    inQuotes = !inQuotes
                }
            } else if (c == ',' && !inQuotes) {
                result.add(currentToken.toString())
                currentToken = StringBuilder()
            } else {
                currentToken.append(c)
            }
            i++
        }
        result.add(currentToken.toString())
        return result
    }
}
