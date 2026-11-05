package chromahub.rhythm.app.features.streaming.data.repository

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Starts [load] asynchronously in [scope] and allows callers to suspend until completion.
 */
internal class BackgroundLoad(scope: CoroutineScope, load: suspend () -> Unit) {

    private val job = scope.launch { load() }

    /** Suspends (without blocking) until the load has finished, successfully or not. */
    suspend fun await() {
        job.join()
    }
}
