package chromahub.rhythm.app.infrastructure.service.player

import chromahub.rhythm.app.infrastructure.service.StreamingPlaybackResolver
import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.source.preload.DefaultPreloadManager
import androidx.media3.exoplayer.source.preload.TargetPreloadStatusControl
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.ResolvingDataSource
import androidx.media3.datasource.DataSpec
import com.qlzhj.rhythm.core.data.shared.data.model.AppSettings
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import java.util.concurrent.ConcurrentHashMap
import androidx.core.net.toUri

@OptIn(UnstableApi::class)
class PreloadController(
    private val context: Context,
    private val appSettings: AppSettings,
    private val streamingResolver: StreamingPlaybackResolver
) {
    companion object {
        private const val TAG = "PreloadController"

        /** 与 RhythmPlayerEngine 一致的解析时限：预加载卡住不能拖累正在播的曲目。 */
        private const val STREAMING_URL_RESOLVE_TIMEOUT_MS = 15_000L
    }

    private var preloadManager: DefaultPreloadManager? = null
    private val targetPreloadStatusControl = PlaylistTargetPreloadStatusControl()

    private var queue: List<MediaItem> = emptyList()
    private var currentIndex: Int = C.INDEX_UNSET

    /** 已注册进预加载器的条目 → 注册时用的 ranking（队列下标）。 */
    private val registeredItems = LinkedHashMap<MediaItem, Int>()

    init {
        initialize()
    }

    fun initialize() {
        try {
            val limit = appSettings.preloadLimit.value
            targetPreloadStatusControl.preloadLimit = limit

            val resolvingDataSourceFactory = ResolvingDataSource.Factory(
                DefaultDataSource.Factory(context, DefaultHttpDataSource.Factory()),
                object : ResolvingDataSource.Resolver {
                    override fun resolveDataSpec(dataSpec: DataSpec): DataSpec {
                        if (dataSpec.uri.scheme == "streaming") {
                            val trackId = dataSpec.uri.lastPathSegment
                            if (!trackId.isNullOrBlank()) {
                                val repository = streamingResolver
                                // 与 RhythmPlayerEngine 相同的解析限时；超时按「本次预载失败」处理，
                                // 返回原 DataSpec 由预加载流程跳过，不影响正在播的曲目。
                                val freshUrl = try {
                                    runBlocking {
                                        withTimeout(STREAMING_URL_RESOLVE_TIMEOUT_MS) {
                                            repository.getStreamingUrl(trackId)
                                        }
                                    }
                                } catch (e: TimeoutCancellationException) {
                                    Log.w(TAG, "Preload: resolving $trackId timed out after ${STREAMING_URL_RESOLVE_TIMEOUT_MS / 1000}ms")
                                    null
                                }
                                if (!freshUrl.isNullOrBlank()) {
                                    return dataSpec.withUri((freshUrl).toUri())
                                }
                            }
                        }
                        return dataSpec
                    }
                }
            )

            val mediaSourceFactory = DefaultMediaSourceFactory(context)
                .setDataSourceFactory(resolvingDataSourceFactory)

            @Suppress("DEPRECATION")
            val builder = DefaultPreloadManager.Builder(context, targetPreloadStatusControl as TargetPreloadStatusControl<Int, DefaultPreloadManager.PreloadStatus>)
                .setMediaSourceFactory(mediaSourceFactory)

            preloadManager = builder.build()
            registeredItems.clear()
            syncPreloadWindow()
            Log.d(TAG, "PreloadController: DefaultPreloadManager initialized with limit: $limit")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize DefaultPreloadManager, using fallback precaching", e)
        }
    }

    fun setPlayingIndex(index: Int) {
        currentIndex = index
        targetPreloadStatusControl.currentPlayingIndex = index
        preloadManager?.setCurrentPlayingIndex(index)
        syncPreloadWindow()
    }

    fun addOrUpdateQueue(mediaItems: List<MediaItem>) {
        queue = mediaItems
        syncPreloadWindow()
    }

    fun remove(mediaItem: MediaItem) {
        queue = queue.filterNot { it == mediaItem }
        registeredItems.remove(mediaItem)
        preloadManager?.remove(mediaItem)
        syncPreloadWindow()
    }

    fun release() {
        registeredItems.clear()
        preloadManager?.release()
        preloadManager = null
    }

    /**
     * 只把「接下来真的要预载的那几首」注册给 [DefaultPreloadManager]。
     *
     * Media3 的 BasePreloadManager 是按 holder 同步推进的：目标状态为 PRELOAD_STATUS_NOT_PRELOADED
     * 的 holder 会走 onSkipped → maybeAdvanceToNextMediaSourceHolder，而 postOrRun 在同一 looper 上
     * 是内联执行的，于是每跳过一首就多压一层调用栈。以前把整条队列都 add 进去，播放到后面时跳过链
     * 长达剩余曲目数，直接把栈撑爆（StackOverflowError in BasePreloadManager.onSkipped）。
     *
     * 现在窗口外的条目根本不注册，跳过链最长只有窗口大小。
     */
    private fun syncPreloadWindow() {
        val manager = preloadManager ?: return
        val window = preloadWindow()

        val registered = registeredItems.entries.iterator()
        while (registered.hasNext()) {
            val entry = registered.next()
            if (window[entry.key] != entry.value) {
                manager.remove(entry.key)
                registered.remove()
            }
        }

        window.forEach { (mediaItem, index) ->
            if (registeredItems[mediaItem] != index) {
                manager.add(mediaItem, index)
                registeredItems[mediaItem] = index
            }
        }

        manager.invalidate()
    }

    /** 当前曲目之后的预载窗口（队列下标 → MediaItem）；未开始播放或窗口为空时返回空表。 */
    private fun preloadWindow(): Map<MediaItem, Int> {
        if (currentIndex == C.INDEX_UNSET) return emptyMap()
        val limit = targetPreloadStatusControl.preloadLimit
        if (limit <= 0) return emptyMap()

        val lastIndex = minOf(currentIndex + limit, queue.lastIndex)
        if (currentIndex + 1 > lastIndex) return emptyMap()

        val window = LinkedHashMap<MediaItem, Int>()
        for (index in (currentIndex + 1)..lastIndex) {
            window[queue[index]] = index
        }
        return window
    }

    private class PlaylistTargetPreloadStatusControl : TargetPreloadStatusControl<Int, DefaultPreloadManager.PreloadStatus> {
        var currentPlayingIndex: Int = C.INDEX_UNSET
        var preloadLimit: Int = 3

        override fun getTargetPreloadStatus(rankingData: Int): DefaultPreloadManager.PreloadStatus {
            if (currentPlayingIndex == C.INDEX_UNSET) {
                return DefaultPreloadManager.PreloadStatus.PRELOAD_STATUS_NOT_PRELOADED
            }
            
            val distance = rankingData - currentPlayingIndex
            return when {
                // Preload the next few songs
                distance in 1..preloadLimit -> {
                    // Stage: source prepared is excellent for warm caching and rapid starts
                    DefaultPreloadManager.PreloadStatus.PRELOAD_STATUS_SOURCE_PREPARED
                }
                // Everything else: still return a valid status but at minimum level
                else -> DefaultPreloadManager.PreloadStatus.PRELOAD_STATUS_NOT_PRELOADED
            }
        }
    }
}
