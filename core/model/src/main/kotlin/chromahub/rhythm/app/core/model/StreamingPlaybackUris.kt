package chromahub.rhythm.app.core.model

import android.net.Uri
import androidx.core.net.toUri

/**
 * 流媒体歌曲的播放 URI。
 *
 * NETEASE / SUBSONIC / JELLYFIN 的播放地址都由服务端按需签发，且带时效：网易云 CDN 的签名地址
 * 几十分钟后即回 403。把这种地址固化进 Song.uri、播放队列、最近播放缓存或歌单，下一次播放必然失败，
 * 播放器侧只会看到一句「Source error」（Media3 的 TYPE_SOURCE 通用文案）。
 *
 * 因此这些服务的歌曲统一用 `streaming://track/<service>::<providerId>` 作为播放 URI，
 * 由 RhythmPlayerEngine / PreloadController 里的 ResolvingDataSource 在每次打开音源时现问一个
 * 新鲜地址（加载失败重试时也会重新解析）。
 */
object StreamingPlaybackUris {

    private const val TRACK_URI_PREFIX = "streaming://track/"

    /**
     * 可重新解析播放地址的流媒体歌曲 id（形如 `NETEASE::362937`）。
     * 本地歌曲 id、以及本身就是 URI 的 id 返回 null。
     */
    fun resolvableSongId(songId: String): String? {
        val separator = songId.indexOf("::")
        if (separator <= 0 || separator >= songId.length - 2) return null
        val serviceId = songId.substring(0, separator)
        return songId.takeIf { serviceId in StreamingServiceId.all }
    }

    /** 该歌曲 id 对应的「播放时现解析」URI；不可解析时返回 null。 */
    fun of(songId: String): Uri? =
        resolvableSongId(songId)?.let { (TRACK_URI_PREFIX + it).toUri() }
}
