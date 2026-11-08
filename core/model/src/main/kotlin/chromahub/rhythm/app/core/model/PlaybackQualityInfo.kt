package chromahub.rhythm.app.core.model

import android.os.Parcelable
import com.qlzhj.rhythm.core.common.AudioQualityDetector
import kotlinx.parcelize.Parcelize
import java.util.Locale

/**
 * Real-time audio playback telemetry data model.
 * Captures the actual format processed by the decoder engine
 * and the output transmission pipeline (Bluetooth / USB DAC / Speaker / Wired).
 */
@Parcelize
data class PlaybackQualityInfo(
    val sourceCodec: String = "",
    val sourceBitrateKbps: Int = 0,
    val sourceSampleRateHz: Int = 0,
    val sourceBitDepth: Int = 0,
    val sourceChannels: Int = 2,

    val decoderName: String = "",
    val decodedSampleRateHz: Int = 0,
    val decodedBitDepth: Int = 0,
    val decodedChannels: Int = 2,
    val decodedMimeType: String = "",
    val decodedBitrateKbps: Int = 0,

    val outputDeviceName: String = "",
    val outputDeviceType: String = "",
    val isBluetooth: Boolean = false,
    val bluetoothCodec: String? = null,
    val bluetoothBitrate: String? = null,
    val bluetoothSampleRateHz: Int? = null,
    val bluetoothBitDepth: Int? = null,
    val isResampled: Boolean = false,
    val resamplingDescription: String? = null,
    val isBitPerfect: Boolean = false,

    val isLossless: Boolean = false,
    val isHiRes: Boolean = false,
    val isDolby: Boolean = false,
    val isDTS: Boolean = false,
    val qualityCategory: String = "",

    val qualityType: AudioQualityDetector.QualityType = AudioQualityDetector.QualityType.UNKNOWN,
    val qualityLabel: String = "",
    val qualityDescription: String = "",
    val outputQualityStatus: String = "",
    val iconResId: Int = 0
) : Parcelable {

    val displaySubheading: String
        get() = outputQualityStatus.ifBlank { qualityDescription }

    /** Icon mapping supplied by the UI layer (:app) — the model module is resource-free. */
    data class QualityIcons(
        val hd: Int = 0,
        val highRes: Int = 0,
        val dolby: Int = 0,
        val dts: Int = 0,
        val surround: Int = 0,
        val hq: Int = 0,
        val sq: Int = 0,
    )

    companion object {
        fun formatSampleRate(sampleRateHz: Int): String {
            return when {
                sampleRateHz <= 0 -> "Unknown"
                sampleRateHz % 1000 == 0 -> "${sampleRateHz / 1000} kHz"
                else -> String.format(Locale.US, "%.1f kHz", sampleRateHz / 1000.0)
            }
        }

        fun formatBitrate(bitrateKbps: Int): String {
            return if (bitrateKbps > 0) "$bitrateKbps kbps" else "Variable"
        }

        fun formatBitDepth(bitDepth: Int): String {
            return if (bitDepth > 0) "$bitDepth-bit" else "16-bit (est.)"
        }

        fun formatChannels(channels: Int): String {
            return when (channels) {
                1 -> "Mono (1.0)"
                2 -> "Stereo (2.0)"
                6 -> "5.1 Surround"
                8 -> "7.1 Surround"
                else -> if (channels > 0) "$channels Channels" else "Stereo"
            }
        }

        /**
         * Resolves the appropriate vector icon resource for a quality tier.
         */

        fun resolveIconRes(
            qualityType: AudioQualityDetector.QualityType,
            qualityLabel: String = "",
            icons: QualityIcons = QualityIcons(),
        ): Int {
            return when (qualityType) {
                AudioQualityDetector.QualityType.DSD_HIGH_RES -> icons.highRes
                AudioQualityDetector.QualityType.HI_RES_STUDIO_MASTER -> icons.highRes
                AudioQualityDetector.QualityType.HI_RES_LOSSLESS -> icons.highRes
                AudioQualityDetector.QualityType.CD_QUALITY_LOSSLESS -> icons.hd
                AudioQualityDetector.QualityType.DOLBY_ATMOS -> icons.dolby
                AudioQualityDetector.QualityType.DOLBY_LOSSLESS -> icons.dolby
                AudioQualityDetector.QualityType.DOLBY_LOSSY_SURROUND -> icons.dolby
                AudioQualityDetector.QualityType.DTS_SURROUND -> icons.dts
                AudioQualityDetector.QualityType.MPEG_H_3D -> icons.surround
                AudioQualityDetector.QualityType.LOSSLESS_SURROUND -> icons.surround
                AudioQualityDetector.QualityType.LOSSY_COMPRESSED -> {
                    if (qualityLabel.contains("320", ignoreCase = true)) {
                        icons.hq
                    } else {
                        icons.sq
                    }
                }
                AudioQualityDetector.QualityType.UNKNOWN -> icons.highRes
            }
        }
    }
}
