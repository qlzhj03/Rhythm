package chromahub.rhythm.app.infrastructure.service

import com.qlzhj.rhythm.core.model.UserAudioDevice
import chromahub.rhythm.app.util.AudioDeviceManager

/**
 * Audio device management seam implemented by MusicViewModel
 * (:feature:local) and consumed by DeviceConfigurationBottomSheet (B1 PR-9,
 * docs/b1-musicviewmodel-seams-design.md §4.4).
 */
interface AudioDeviceController {
    val audioDeviceManager: AudioDeviceManager
    fun saveUserAudioDevice(device: UserAudioDevice)
    fun deleteUserAudioDevice(deviceId: String)
    fun setActiveAudioDevice(device: UserAudioDevice)
    fun findMatchingUserDevice(deviceName: String): UserAudioDevice?
    fun dismissAutoEQSuggestion(deviceId: String)
}
