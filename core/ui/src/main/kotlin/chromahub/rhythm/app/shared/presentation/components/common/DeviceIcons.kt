package chromahub.rhythm.app.shared.presentation.components.common

import com.qlzhj.rhythm.core.model.UserAudioDevice
import com.qlzhj.rhythm.core.designsystem.icons.MaterialSymbolIcon
import com.qlzhj.rhythm.core.designsystem.icons.RhythmIcons

/**
 * Returns the appropriate MaterialSymbolIcon based on the detected or selected DeviceType.
 */
fun getDeviceIcon(type: UserAudioDevice.DeviceType): MaterialSymbolIcon {
    return when (type) {
        UserAudioDevice.DeviceType.HEADPHONES -> RhythmIcons.Headphones
        UserAudioDevice.DeviceType.EARBUDS -> MaterialSymbolIcon("earbuds")
        UserAudioDevice.DeviceType.IEM -> MaterialSymbolIcon("earbuds")
        UserAudioDevice.DeviceType.SPEAKERS -> RhythmIcons.Speaker
        UserAudioDevice.DeviceType.BLUETOOTH_SPEAKER -> RhythmIcons.BluetoothFilled
        UserAudioDevice.DeviceType.CAR_AUDIO -> MaterialSymbolIcon("directions_car")
        UserAudioDevice.DeviceType.STUDIO_MONITORS -> MaterialSymbolIcon("speaker_group")
        UserAudioDevice.DeviceType.OTHER -> RhythmIcons.Headphones
    }
}
