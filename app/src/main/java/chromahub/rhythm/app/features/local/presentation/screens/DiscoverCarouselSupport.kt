package chromahub.rhythm.app.features.local.presentation.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import chromahub.rhythm.app.shared.data.model.Album
import kotlinx.coroutines.delay
import kotlin.random.Random

private const val DISCOVER_LOADING_APPEARANCE_MS = 2_000L

/**
 * Picks the discover carousel albums without copying or shuffling the whole library.
 *
 * Previously shown album ids survive library / metadata refreshes so the carousel does not
 * reshuffle on every emission. Random index sampling keeps the work proportional to the
 * requested item count instead of allocating a shuffled copy of a possibly multi-thousand
 * item library on the main thread during the cold-start entrance animation.
 */
internal fun pickFeaturedAlbums(
    source: List<Album>,
    targetCount: Int,
    previousIds: List<String>,
    random: Random = Random.Default
): List<Album> {
    if (source.isEmpty() || targetCount <= 0) return emptyList()
    val wanted = minOf(targetCount, source.size)
    val selected = ArrayList<Album>(wanted)
    val selectedIds = HashSet<String>(wanted * 2)

    for (id in previousIds) {
        if (selected.size >= wanted) break
        val match = source.firstOrNull { it.id == id }
        if (match != null && selectedIds.add(match.id)) {
            selected.add(match)
        }
    }

    var attempts = 0
    val maxAttempts = source.size * 2
    while (selected.size < wanted && attempts < maxAttempts) {
        attempts++
        val candidate = source[random.nextInt(source.size)]
        if (selectedIds.add(candidate.id)) {
            selected.add(candidate)
        }
    }

    // Deterministic fallback for small libraries or unlucky sampling runs, and for
    // sources containing duplicate ids.
    if (selected.size < wanted) {
        for (candidate in source) {
            if (selected.size >= wanted) break
            if (selectedIds.add(candidate.id)) {
                selected.add(candidate)
            }
        }
    }
    return selected
}

/**
 * Whether the discover placeholder should still look like it is loading.
 *
 * The placeholder stays mounted at the carousel's exact height until real album data
 * arrives, regardless of this flag, so switching it never changes the layout. Its only
 * purpose is to stop the pulse and reveal the empty-state text when the derived album
 * flows (`combine` + `flowOn` + `stateIn` seeded with `emptyList()`) turn out to have no
 * data at all. This is a display timer, not a wait for an async result.
 */
@Composable
internal fun rememberDiscoverLoading(): Boolean {
    var loading by remember { mutableStateOf(true) }
    LaunchedEffect(Unit) {
        delay(DISCOVER_LOADING_APPEARANCE_MS)
        loading = false
    }
    return loading
}
