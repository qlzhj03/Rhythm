package chromahub.rhythm.app.features.streaming.presentation.components

import android.graphics.Bitmap
import android.graphics.Color
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import chromahub.rhythm.app.R
import chromahub.rhythm.app.features.streaming.presentation.viewmodel.NetEaseQrLoginPhase
import chromahub.rhythm.app.features.streaming.presentation.viewmodel.NetEaseQrLoginUiState
import com.qlzhj.rhythm.core.designsystem.components.M3FourColorCircularLoader
import com.qlzhj.rhythm.core.designsystem.icons.Icon
import com.qlzhj.rhythm.core.designsystem.icons.MaterialSymbolIcon
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 网易云扫码登录卡片：展示 unikey 对应的二维码（内容为官方网页登录页的 codekey 链接，
 * 与 NeteaseCloudMusicApi 的 login_qr_create 生成的一致），并随轮询状态提示扫码进度。
 */
@Composable
fun NetEaseQrLoginCard(
    state: NetEaseQrLoginUiState,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = colorScheme.surfaceContainer
        ),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = stringResource(id = R.string.netease_qr_title),
                style = typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = colorScheme.onSurface
            )
            Text(
                text = stringResource(id = R.string.netease_qr_instruction),
                style = typography.bodySmall,
                color = colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            val qrBitmap by produceState<Bitmap?>(initialValue = null, key1 = state.qrContent) {
                value = state.qrContent?.let { content ->
                    withContext(Dispatchers.Default) { generateQrBitmap(content) }
                }
            }

            Box(
                modifier = Modifier.size(220.dp),
                contentAlignment = Alignment.Center
            ) {
                when (state.phase) {
                    NetEaseQrLoginPhase.GENERATING -> {
                        M3FourColorCircularLoader(modifier = Modifier.size(56.dp))
                    }
                    NetEaseQrLoginPhase.EXPIRED, NetEaseQrLoginPhase.FAILED, NetEaseQrLoginPhase.IDLE -> {
                        Icon(
                            imageVector = MaterialSymbolIcon("qr_code_2"),
                            contentDescription = null,
                            tint = colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(72.dp)
                        )
                    }
                    else -> {
                        val bitmap = qrBitmap
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = androidx.compose.ui.graphics.Color.White,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            if (bitmap != null) {
                                Image(
                                    bitmap = bitmap.asImageBitmap(),
                                    contentDescription = stringResource(id = R.string.netease_qr_qr_content_description),
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(12.dp)
                                )
                            }
                        }
                    }
                }
            }

            val statusText = when (state.phase) {
                NetEaseQrLoginPhase.GENERATING -> stringResource(id = R.string.netease_qr_state_generating)
                NetEaseQrLoginPhase.SHOWING, NetEaseQrLoginPhase.WAITING ->
                    stringResource(id = R.string.netease_qr_state_waiting)
                NetEaseQrLoginPhase.SCANNED -> stringResource(id = R.string.netease_qr_state_scanned)
                NetEaseQrLoginPhase.EXPIRED -> stringResource(id = R.string.netease_qr_state_expired)
                NetEaseQrLoginPhase.FAILED -> stringResource(
                    id = R.string.netease_qr_state_failed,
                    state.failureMessage.orEmpty()
                )
                NetEaseQrLoginPhase.CONFIRMED -> stringResource(id = R.string.streaming_status_connected)
                NetEaseQrLoginPhase.IDLE -> null
            }
            if (statusText != null) {
                Text(
                    text = statusText,
                    style = typography.bodyMedium,
                    color = if (state.phase == NetEaseQrLoginPhase.FAILED) {
                        colorScheme.error
                    } else {
                        colorScheme.onSurfaceVariant
                    },
                    textAlign = TextAlign.Center
                )
            }

            if (state.phase == NetEaseQrLoginPhase.EXPIRED || state.phase == NetEaseQrLoginPhase.FAILED) {
                FilledTonalButton(
                    onClick = onRefresh,
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    Icon(
                        imageVector = MaterialSymbolIcon("refresh"),
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = stringResource(id = R.string.netease_qr_refresh),
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            }
        }
    }
}

/**
 * 本地生成二维码位图（官方客户端扫码识别的正是这份 codekey 内容）。留白 MARGIN=2 与
 * 上游 qrimg 的默认留白一致。
 */
private fun generateQrBitmap(content: String, sizePx: Int = 512): Bitmap {
    val hints = mapOf(
        EncodeHintType.CHARACTER_SET to "UTF-8",
        EncodeHintType.MARGIN to 2
    )
    val matrix = QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, sizePx, sizePx, hints)
    val pixels = IntArray(sizePx * sizePx)
    val onColor = Color.BLACK
    val offColor = Color.WHITE
    for (y in 0 until sizePx) {
        val rowOffset = y * sizePx
        for (x in 0 until sizePx) {
            pixels[rowOffset + x] = if (matrix[x, y]) onColor else offColor
        }
    }
    return Bitmap.createBitmap(pixels, sizePx, sizePx, Bitmap.Config.ARGB_8888)
}

@Preview(name = "Waiting for scan", showBackground = true)
@Composable
private fun NetEaseQrLoginCardWaitingPreview() {
    MaterialTheme {
        NetEaseQrLoginCard(
            state = NetEaseQrLoginUiState(
                phase = NetEaseQrLoginPhase.WAITING,
                qrContent = "https://music.163.com/login?codekey=preview-unikey"
            ),
            onRefresh = {}
        )
    }
}

@Preview(name = "Expired", showBackground = true)
@Composable
private fun NetEaseQrLoginCardExpiredPreview() {
    MaterialTheme {
        Surface(color = androidx.compose.ui.graphics.Color.White) {
            NetEaseQrLoginCard(
                state = NetEaseQrLoginUiState(phase = NetEaseQrLoginPhase.EXPIRED),
                onRefresh = {}
            )
        }
    }
}
