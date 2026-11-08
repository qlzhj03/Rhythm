package chromahub.rhythm.app.util

import android.net.FakeUri
import com.qlzhj.rhythm.core.model.Song
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * detectFormatCached 元数据快速路径的 JVM 单测。
 *
 * 无 mock 框架（勿引入），android.jar stub 也不提供 Uri 工厂，因此：
 *  - 用 [FakeUri] 子类化 abstract Uri 以构造 Song
 *  - 快速路径的判定与计算分别测 [AudioFormatDetector.hasUsableSongMetadata]
 *    与 [AudioFormatDetector.detectRemoteFormat]（快速路径即委托后者）
 *  - detectFormatCached 的路由/缓存层依赖 Context 与 native 解析器，JVM 无法覆盖，
 *    由真机日志验证（MediaMetadataRetriever 不再随 tab 切换反复出现）
 */
class AudioFormatDetectorTest {
    /** 元数据齐全的 24-bit/96kHz 立体声 FLAC（约 55% 压缩率对应 ~2534 kbps） */
    private fun song(
        codec: String? = "FLAC",
        bitrate: Int? = 2_534_400,
        sampleRate: Int? = 96_000,
        channels: Int? = 2,
    ) = Song(
        id = "1",
        title = "Test Song",
        artist = "Artist",
        album = "Album",
        duration = 180_000L,
        uri = FakeUri("file:///music/test.flac"),
        bitrate = bitrate,
        sampleRate = sampleRate,
        channels = channels,
        codec = codec,
    )

    // region 快速路径门槛：codec 可信 + 存在有效码率/采样率

    @Test
    fun completeMetadataPassesGate() {
        assertTrue(AudioFormatDetector.hasUsableSongMetadata(song()))
    }

    @Test
    fun unknownCodecFailsGate() {
        assertFalse(AudioFormatDetector.hasUsableSongMetadata(song(codec = "Unknown")))
    }

    @Test
    fun placeholderCodecFailsGate() {
        // 扫描管线对提取失败的歌把 codec 写为 "-"
        assertFalse(AudioFormatDetector.hasUsableSongMetadata(song(codec = "-")))
    }

    @Test
    fun nullOrBlankCodecFailsGate() {
        assertFalse(AudioFormatDetector.hasUsableSongMetadata(song(codec = null)))
        assertFalse(AudioFormatDetector.hasUsableSongMetadata(song(codec = "")))
    }

    @Test
    fun missingSignalMetadataFailsGate() {
        assertFalse(AudioFormatDetector.hasUsableSongMetadata(song(bitrate = null, sampleRate = null)))
    }

    @Test
    fun negativeMetadataValuesFailGate() {
        // 扫描管线对提取失败的歌把数值写为 -1
        assertFalse(
            AudioFormatDetector.hasUsableSongMetadata(
                song(bitrate = -1, sampleRate = -1, channels = -1),
            ),
        )
    }

    // endregion

    // region 快速路径计算：复用 detectRemoteFormat 的纯启发式

    @Test
    fun hiResLosslessDerivedFromSongMetadata() {
        val info = AudioFormatDetector.detectRemoteFormat(FakeUri("file:///music/test.flac"), song())

        assertEquals("FLAC", info.codec)
        assertTrue(info.isLossless)
        assertTrue("96kHz 无损应判为 Hi-Res", info.isHiRes)
        assertEquals(96_000, info.sampleRateHz)
        assertEquals(2, info.channelCount)
        assertEquals("按 bitrate/(sampleRate*channels) 估算应为 24-bit", 24, info.bitDepth)
    }

    @Test
    fun lossyCodecStaysNonLossless() {
        val info =
            AudioFormatDetector.detectRemoteFormat(
                FakeUri("file:///music/test.mp3"),
                song(codec = "MP3", bitrate = 320_000, sampleRate = 44_100),
            )

        assertEquals("MP3", info.codec)
        assertFalse("高码率 MP3 也不是无损", info.isLossless)
        assertFalse(info.isHiRes)
        assertEquals(320, info.bitrateKbps)
    }

    @Test
    fun dolbyCodecDetected() {
        val info =
            AudioFormatDetector.detectRemoteFormat(
                FakeUri("file:///music/test.eac3"),
                song(codec = "E-AC-3", bitrate = 768_000, sampleRate = 48_000, channels = 6),
            )

        assertTrue("E-AC-3 应识别为 Dolby", info.isDolby)
        assertFalse(info.isLossless)
    }

    @Test
    fun cdQualityLosslessDetected() {
        // 16-bit/44.1kHz 立体声 FLAC：rawBitrate = 16*44100*2 ≈ 1411 kbps，压缩率 0.55 → ~776 kbps
        val info =
            AudioFormatDetector.detectRemoteFormat(
                FakeUri("file:///music/test.flac"),
                song(bitrate = 776_160, sampleRate = 44_100, channels = 2),
            )

        assertTrue(info.isLossless)
        assertFalse("44.1kHz 无损不是 Hi-Res", info.isHiRes)
        assertEquals(16, info.bitDepth)
    }

    // endregion
}
