package chromahub.rhythm.app.shared.presentation.components.common

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import chromahub.rhythm.app.R
import com.qlzhj.rhythm.core.model.StreamingServiceId

/**
 * 流媒体服务选项单一事实来源：新增平台时在 [StreamingServiceOptions.defaults] 里加一条
 * （id 进 [StreamingServiceId]），引导页/设置页的选择列表与配置页头部会自动跟随渲染。
 */
data class StreamingServiceOption(
    val id: String,
    @param:StringRes val nameRes: Int,
    @param:StringRes val descriptionRes: Int,
    @param:DrawableRes val iconRes: Int
)

object StreamingServiceOptions {
    const val SUBSONIC = StreamingServiceId.SUBSONIC
    const val JELLYFIN = StreamingServiceId.JELLYFIN
    const val NETEASE = StreamingServiceId.NETEASE
    val defaults: List<StreamingServiceOption> = listOf(
        StreamingServiceOption(
            id = NETEASE,
            nameRes = R.string.streaming_service_netease,
            descriptionRes = R.string.streaming_service_netease_desc,
            iconRes = R.drawable.ic_netease
        ),
        StreamingServiceOption(
            id = SUBSONIC,
            nameRes = R.string.streaming_service_subsonic,
            descriptionRes = R.string.streaming_service_subsonic_desc,
            iconRes = R.drawable.ic_subsonic
        ),
        StreamingServiceOption(
            id = JELLYFIN,
            nameRes = R.string.streaming_service_jellyfin,
            descriptionRes = R.string.streaming_service_jellyfin_desc,
            iconRes = R.drawable.ic_jellyfin
        ),
    )
}
