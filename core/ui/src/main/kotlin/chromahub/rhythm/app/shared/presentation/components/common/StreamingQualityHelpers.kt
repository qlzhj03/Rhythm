package chromahub.rhythm.app.shared.presentation.components.common

import android.content.Context
import androidx.annotation.StringRes
import chromahub.rhythm.app.R

/**
 * Streaming quality picker helpers shared by :feature:streaming's settings
 * screens and :feature:local's PlaybackBottomSheet (D-phase D3) so neither
 * feature needs a compile dependency on the other.
 */
data class StreamingQualityOption(
    val value: String,
    @param:StringRes val titleRes: Int,
    @param:StringRes val descriptionRes: Int
)

val streamingQualityOptions = listOf(
    StreamingQualityOption(
        value = "LOW",
        titleRes = R.string.streaming_quality_low,
        descriptionRes = R.string.streaming_quality_low_desc
    ),
    StreamingQualityOption(
        value = "NORMAL",
        titleRes = R.string.streaming_quality_normal,
        descriptionRes = R.string.streaming_quality_normal_desc
    ),
    StreamingQualityOption(
        value = "HIGH",
        titleRes = R.string.streaming_quality_high,
        descriptionRes = R.string.streaming_quality_high_desc
    ),
    StreamingQualityOption(
        value = "LOSSLESS",
        titleRes = R.string.streaming_quality_lossless,
        descriptionRes = R.string.streaming_quality_lossless_desc
    )
)

fun normalizeStreamingQuality(rawValue: String): String {
    val normalized = rawValue.uppercase()
    return if (streamingQualityOptions.any { it.value == normalized }) normalized else "HIGH"
}

fun streamingQualityLabel(quality: String, context: Context): String {
    return when (normalizeStreamingQuality(quality)) {
        "LOW" -> context.getString(R.string.streaming_quality_low)
        "NORMAL" -> context.getString(R.string.streaming_quality_normal)
        "HIGH" -> context.getString(R.string.streaming_quality_high)
        "LOSSLESS" -> context.getString(R.string.streaming_quality_lossless)
        else -> quality
    }
}
