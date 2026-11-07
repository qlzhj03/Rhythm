package chromahub.rhythm.app.infrastructure.service

import android.app.Activity
import chromahub.rhythm.app.core.model.AutoEQProfile
import chromahub.rhythm.app.core.model.CustomEqualizerPreset
import kotlinx.coroutines.flow.StateFlow

/**
 * Equalizer / spatial audio / bass boost / AutoEQ seam implemented by
 * MusicViewModel (:feature:local) and consumed by EqualizerScreen and the
 * EQ bottom sheets across module boundaries (B1 PR-9,
 * docs/b1-musicviewmodel-seams-design.md §4.4). Members mirror the
 * ViewModel's declarations verbatim; the ViewModel's default arguments for
 * [setBassBoost], [setVirtualizer] and [openSystemEqualizer] live here
 * because Kotlin forbids defaults on overrides, and every call site
 * inherits them unchanged.
 */
interface AudioEffectsController {
    val equalizerEnabled: StateFlow<Boolean>
    val equalizerPreset: StateFlow<String>
    val equalizerBandLevels: StateFlow<String>
    val bassBoostEnabled: StateFlow<Boolean>
    val bassBoostStrength: StateFlow<Int>
    val virtualizerEnabled: StateFlow<Boolean>
    val virtualizerStrength: StateFlow<Int>
    val customEqualizerPresets: StateFlow<List<CustomEqualizerPreset>>
    val equalizerPresetOrder: StateFlow<List<String>>
    val hiddenEqualizerPresets: StateFlow<Set<String>>
    val pinnedAutoEQProfiles: StateFlow<List<String>>
    val customAutoEQProfiles: StateFlow<List<AutoEQProfile>>
    val autoEQProfiles: StateFlow<List<AutoEQProfile>>
    val spatializationStatus: StateFlow<String>
    val isSpatializationAvailable: StateFlow<Boolean>
    val isBassBoostAvailable: StateFlow<Boolean>

    fun setEqualizerEnabled(enabled: Boolean)
    fun setEqualizerBandLevel(band: Short, level: Short)
    fun applyEqualizerPreset(preset: String, levels: List<Float>)
    fun saveCustomEqualizerPreset(name: String, bands: List<Float>): CustomEqualizerPreset
    fun applyAutoEQProfile(profile: AutoEQProfile)
    fun loadAutoEQProfiles()
    fun pinAutoEQProfile(name: String)
    fun setVirtualizer(enabled: Boolean, strength: Short = 500)
    fun setBassBoost(enabled: Boolean, strength: Short = 500)
    fun openSystemEqualizer(activity: Activity? = null, requestCode: Int = 0)
    fun updateSpatializationStatus()
    fun updateBassBoostAvailability()
    fun setEqualizerPresetOrder(order: List<String>)
    fun resetEqualizerPresetOrder()
    fun deleteAutoEQProfile(name: String)
    fun deleteCustomEqualizerPreset(id: String)
    fun setHiddenEqualizerPresets(hidden: Set<String>)
    fun saveCustomAutoEQProfiles(profiles: List<AutoEQProfile>)
}
