package chromahub.rhythm.app.features.streaming.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import chromahub.rhythm.app.core.model.StreamingPlaybackUris

/**
 * 流媒体歌曲的播放 URI 约定：只有 `<service>::<providerId>` 形式的 id 才允许在播放时
 * 重新解析地址，其余（本地歌曲、已是 URI 的 id、未知服务）必须原样使用已有 uri。
 */
class StreamingPlaybackUrisTest {

    @Test
    fun testResolvableSongId_knownProviders() {
        assertEquals("NETEASE::362937", StreamingPlaybackUris.resolvableSongId("NETEASE::362937"))
        assertEquals("SUBSONIC::1234", StreamingPlaybackUris.resolvableSongId("SUBSONIC::1234"))
        assertEquals("JELLYFIN::abc-def", StreamingPlaybackUris.resolvableSongId("JELLYFIN::abc-def"))
    }

    @Test
    fun testResolvableSongId_localMediaStoreId() {
        assertNull(StreamingPlaybackUris.resolvableSongId("2875"))
    }

    @Test
    fun testResolvableSongId_uriShapedIdsAreNotResolvable() {
        assertNull(StreamingPlaybackUris.resolvableSongId("streaming://track/NETEASE::362937"))
        assertNull(StreamingPlaybackUris.resolvableSongId("http://m7.music.126.net/x.flac"))
        assertNull(StreamingPlaybackUris.resolvableSongId("content://media/external/audio/media/2875"))
    }

    @Test
    fun testResolvableSongId_unknownProvider() {
        assertNull(StreamingPlaybackUris.resolvableSongId("SPOTIFY::abc"))
        assertNull(StreamingPlaybackUris.resolvableSongId("netease::362937"))
    }

    @Test
    fun testResolvableSongId_malformedIds() {
        assertNull(StreamingPlaybackUris.resolvableSongId(""))
        assertNull(StreamingPlaybackUris.resolvableSongId("::362937"))
        assertNull(StreamingPlaybackUris.resolvableSongId("NETEASE::"))
        assertNull(StreamingPlaybackUris.resolvableSongId("NETEASE:362937"))
    }
}
