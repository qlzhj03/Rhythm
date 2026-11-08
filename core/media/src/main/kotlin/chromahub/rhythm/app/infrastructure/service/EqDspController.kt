package chromahub.rhythm.app.infrastructure.service

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.util.Log
import com.qlzhj.rhythm.core.model.AutoEQProfile
import com.qlzhj.rhythm.core.model.CustomEqualizerPreset
import com.qlzhj.rhythm.core.model.PlaybackLocation
import com.qlzhj.rhythm.core.model.UserAudioDevice
import chromahub.rhythm.app.util.AudioDeviceManager
import chromahub.rhythm.app.util.AutoEQManager
import com.qlzhj.rhythm.core.data.shared.data.model.AppSettings
import com.qlzhj.rhythm.core.common.ServiceStartUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * EQ/DSP domain store (P3-phase, B1 plan B): owns the equalizer/spatial/
 * AutoEQ/audio-device runtime state and the set-then-intent logic that was
 * previously inlined in MusicViewModel. Implements the B1 PR-9 seams
 * ([AudioEffectsController] / [AudioDeviceController]) so the ViewModel and,
 * after P3-2, the navigation wiring can hand this straight to the EQ screens.
 *
 * Bodies are moved verbatim from MusicViewModel with three adaptations:
 * `getApplication<Application>()` → constructor [context], `viewModelScope` →
 * injected [scope], and the media-session audio session id → injected
 * [audioSessionId] provider. Persistence stays in AppSettings
 * (AudioEqSettingsSection); the engine keeps its Intent + direct-read
 * channels unchanged.
 *
 * [openSystemEqualizerImpl] injects the system-equalizer opener because
 * EqualizerUtils lives in :feature:local (media must not depend on it).
 */
class EqDspController(
    private val context: Context,
    private val appSettings: AppSettings,
    private val scope: CoroutineScope,
    private val audioSessionId: () -> Int,
    private val openSystemEqualizerImpl: (Context, Int, Activity?, Int) -> Unit,
) : AudioEffectsController, AudioDeviceController {

    private val autoEQManager = AutoEQManager(context)
    override val audioDeviceManager = AudioDeviceManager(context)

    private val _autoEQProfiles = MutableStateFlow<List<AutoEQProfile>>(emptyList())
    override val autoEQProfiles: StateFlow<List<AutoEQProfile>> = _autoEQProfiles.asStateFlow()
    private val _autoEQLoading = MutableStateFlow(false)
    val autoEQLoading: StateFlow<Boolean> = _autoEQLoading.asStateFlow()

    // Equalizer settings (persistence pass-throughs — same flow objects as AppSettings)
    override val equalizerEnabled get() = appSettings.equalizerEnabled
    override val equalizerPreset get() = appSettings.equalizerPreset
    override val equalizerBandLevels get() = appSettings.equalizerBandLevels
    override val bassBoostEnabled get() = appSettings.bassBoostEnabled
    override val bassBoostStrength get() = appSettings.bassBoostStrength
    override val virtualizerEnabled get() = appSettings.virtualizerEnabled
    override val virtualizerStrength get() = appSettings.virtualizerStrength
    override val customEqualizerPresets get() = appSettings.customEqualizerPresets
    override val equalizerPresetOrder get() = appSettings.equalizerPresetOrder
    override val hiddenEqualizerPresets get() = appSettings.hiddenEqualizerPresets
    override val pinnedAutoEQProfiles get() = appSettings.pinnedAutoEQProfiles
    override val customAutoEQProfiles get() = appSettings.customAutoEQProfiles
    private var lastHeadphoneAutoEQProfile: String? = null

    // Spatialization status
    private val _spatializationStatus = MutableStateFlow("Unknown")
    override val spatializationStatus: StateFlow<String> = _spatializationStatus.asStateFlow()

    private val _isSpatializationAvailable = MutableStateFlow(false)
    override val isSpatializationAvailable: StateFlow<Boolean> = _isSpatializationAvailable.asStateFlow()

    // Bass boost availability: direct pass-through — the service writes this
    // key directly (P3-3 divergence fix), so the flow must live in AppSettings.
    override val isBassBoostAvailable: StateFlow<Boolean> get() = appSettings.isBassBoostAvailableFlow

    init {
        autoEQManager.setCustomProfiles(appSettings.customAutoEQProfiles.value)
        scope.launch {
            appSettings.customAutoEQProfiles.collect { profiles ->
                autoEQManager.setCustomProfiles(profiles)
                if (_autoEQProfiles.value.isNotEmpty()) {
                    _autoEQProfiles.value = autoEQManager.getAllProfiles()
                }
            }
        }

        scope.launch {
            var previousDevice: PlaybackLocation? = null
            audioDeviceManager.currentDevice.collect { device ->
                if (device == null) return@collect
                val isSpeaker = device.id == AudioDeviceManager.DEVICE_SPEAKER
                val wasExternal = previousDevice != null && previousDevice?.id != AudioDeviceManager.DEVICE_SPEAKER

                if (isSpeaker && wasExternal && appSettings.speakerAutoEQBypass.value) {
                    val activeAutoEQ = appSettings.autoEQProfile.value
                    if (activeAutoEQ.isNotBlank()) {
                        Log.d(TAG, "Audio routed to speaker: bypassing AutoEQ profile ($activeAutoEQ)")
                        lastHeadphoneAutoEQProfile = activeAutoEQ
                        applyEqualizerPreset("Flat", List(10) { 0f })
                        appSettings.setAutoEQProfile("")
                    }
                } else if (!isSpeaker && previousDevice?.id == AudioDeviceManager.DEVICE_SPEAKER) {
                    val matchedDevice = findMatchingUserDevice(device.name)
                    val profileName = matchedDevice?.autoEQProfileName
                    if (profileName != null) {
                        val profile = autoEQManager.findProfileByName(profileName)
                        if (profile != null) {
                            Log.d(TAG, "Reconnected to configured device: applying ${profile.name}")
                            applyAutoEQProfile(profile)
                            lastHeadphoneAutoEQProfile = null
                        }
                    } else if (lastHeadphoneAutoEQProfile != null) {
                        val profile = autoEQManager.findProfileByName(lastHeadphoneAutoEQProfile!!)
                        if (profile != null) {
                            Log.d(TAG, "Restoring previous headphone AutoEQ profile: ${profile.name}")
                            applyAutoEQProfile(profile)
                        }
                        lastHeadphoneAutoEQProfile = null
                    }
                }
                previousDevice = device
            }
        }
    }

    override fun setEqualizerEnabled(enabled: Boolean) {
        // Save to settings
        appSettings.setEqualizerEnabled(enabled)

        // Send to service
        val intent = Intent(context, MediaPlaybackService::class.java).apply {
            action = MediaPlaybackService.ACTION_SET_EQUALIZER_ENABLED
            putExtra("enabled", enabled)
        }
        val sent = ServiceStartUtils.startServiceSafely(
            context = context,
            intent = intent,
            logTag = TAG,
            reason = "set_equalizer_enabled"
        )
        if (sent) {
            Log.d(TAG, "Set equalizer enabled: $enabled")
        }
    }

    override fun setEqualizerBandLevel(band: Short, level: Short) {
        // Clear AutoEQ profile and set preset to Custom when user manually adjusts bands
        val currentPreset = appSettings.equalizerPreset.value
        if (currentPreset.startsWith("AutoEQ:") || currentPreset != "Custom") {
            appSettings.setEqualizerPreset("Custom")
            appSettings.setAutoEQProfile("")
            Log.d(TAG, "Switched to Custom preset as user manually adjusted EQ band")
        }

        val intent = Intent(context, MediaPlaybackService::class.java).apply {
            action = MediaPlaybackService.ACTION_SET_EQUALIZER_BAND
            putExtra("band", band)
            putExtra("level", level)
        }
        val sent = ServiceStartUtils.startServiceSafely(
            context = context,
            intent = intent,
            logTag = TAG,
            reason = "set_equalizer_band"
        )
        if (sent) {
            Log.d(TAG, "Set equalizer band $band to level $level")
        }
    }

    override fun setBassBoost(enabled: Boolean, strength: Short) {
        // Save to settings
        appSettings.setBassBoostEnabled(enabled)
        appSettings.setBassBoostStrength(strength.toInt())

        val intent = Intent(context, MediaPlaybackService::class.java).apply {
            action = MediaPlaybackService.ACTION_SET_BASS_BOOST
            putExtra("enabled", enabled)
            putExtra("strength", strength)
        }
        val sent = ServiceStartUtils.startServiceSafely(
            context = context,
            intent = intent,
            logTag = TAG,
            reason = "set_bass_boost"
        )
        if (sent) {
            Log.d(TAG, "Set bass boost enabled: $enabled, strength: $strength")
        }
    }

    fun isBassBoostSupported(): Boolean {
        return isBassBoostAvailable.value
    }

    fun setBassBoostAvailable(available: Boolean) {
        appSettings.setBassBoostAvailable(available)
    }

    override fun setVirtualizer(enabled: Boolean, strength: Short) {
        // Save to settings
        appSettings.setVirtualizerEnabled(enabled)
        appSettings.setVirtualizerStrength(strength.toInt())

        val intent = Intent(context, MediaPlaybackService::class.java).apply {
            action = MediaPlaybackService.ACTION_SET_VIRTUALIZER
            putExtra("enabled", enabled)
            putExtra("strength", strength)
        }
        val sent = ServiceStartUtils.startServiceSafely(
            context = context,
            intent = intent,
            logTag = TAG,
            reason = "set_virtualizer"
        )
        if (sent) {
            Log.d(TAG, "Set virtualizer enabled: $enabled, strength: $strength")
        }

        // Update spatialization status after a short delay
        scope.launch {
            delay(200)
            updateSpatializationStatus()
        }
    }

    /**
     * Mono-audio write path shared with [setActiveAudioDevice]'s per-device
     * preference; MusicViewModel's PlaybackSettingsController override
     * delegates here so both entry points share one implementation.
     */
    fun setMonoAudioEnabledInternal(enabled: Boolean) {
        appSettings.setMonoAudioEnabled(enabled)

        val intent = Intent(context, MediaPlaybackService::class.java).apply {
            action = MediaPlaybackService.ACTION_SET_MONO_AUDIO
            putExtra("enabled", enabled)
        }
        val sent = ServiceStartUtils.startServiceSafely(
            context = context,
            intent = intent,
            logTag = TAG,
            reason = "set_mono_audio"
        )
        if (sent) {
            Log.d(TAG, "Set mono audio enabled: $enabled")
        }
    }

    override fun updateSpatializationStatus() {
        scope.launch {
            try {
                // Rhythm's custom spatialization works on ALL Android versions
                // It uses M/S stereo widening, so it's always available for stereo audio
                _isSpatializationAvailable.value = true
                _spatializationStatus.value = when {
                    appSettings.virtualizerEnabled.value -> "Active (Rhythm Spatial Audio)"
                    else -> "Available"
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error updating spatialization status", e)
                _isSpatializationAvailable.value = true // Still available even if check fails
                _spatializationStatus.value = "Available"
            }
        }
    }

    override fun applyEqualizerPreset(preset: String, levels: List<Float>) {
        // Save to settings
        appSettings.setEqualizerPreset(preset)
        appSettings.setEqualizerBandLevels(levels.joinToString(","))

        // Clear AutoEQ profile when applying a different preset (unless it's an AutoEQ preset)
        if (!preset.startsWith("AutoEQ:")) {
            appSettings.setAutoEQProfile("")
            Log.d(TAG, "Cleared AutoEQ profile as user selected preset: $preset")
        }

        val intent = Intent(context, MediaPlaybackService::class.java).apply {
            action = MediaPlaybackService.ACTION_APPLY_EQUALIZER_PRESET
            putExtra("preset", preset)
            putExtra("levels", levels.toFloatArray())
        }
        val sent = ServiceStartUtils.startServiceSafely(
            context = context,
            intent = intent,
            logTag = TAG,
            reason = "apply_equalizer_preset"
        )
        if (sent) {
            Log.d(TAG, "Applied equalizer preset: $preset")
        }
    }

    // AutoEQ functions

    override fun loadAutoEQProfiles() {
        scope.launch {
            _autoEQLoading.value = true
            try {
                val result = autoEQManager.loadProfiles()
                if (result.isSuccess) {
                    _autoEQProfiles.value = autoEQManager.getAllProfiles()
                    Log.d(TAG, "Loaded ${_autoEQProfiles.value.size} AutoEQ profiles (database + custom)")
                } else {
                    Log.e(TAG, "Failed to load AutoEQ profiles", result.exceptionOrNull())
                }
            } finally {
                _autoEQLoading.value = false
            }
        }
    }

    fun searchAutoEQProfiles(query: String): List<AutoEQProfile> {
        return autoEQManager.searchProfiles(query)
    }

    override fun applyAutoEQProfile(profile: AutoEQProfile) {
        if (profile.name.isBlank() || profile.name.equals("None", ignoreCase = true)) {
            Log.d(TAG, "Disabling AutoEQ profile")
            appSettings.setAutoEQProfile("")
            applyEqualizerPreset("Flat", List(10) { 0f })
            return
        }

        Log.d(TAG, "Applying AutoEQ profile: ${profile.name}")

        // Ensure we have 10 bands
        val levels = when {
            profile.bands.size == 10 -> profile.bands
            profile.bands.size > 10 -> profile.bands.take(10)
            else -> {
                val padded = profile.bands.toMutableList()
                while (padded.size < 10) {
                    padded.add(0f)
                }
                padded
            }
        }

        // Save profile name to settings
        appSettings.setAutoEQProfile(profile.name)

        // Enable equalizer first if it's not already enabled
        val wasEnabled = appSettings.equalizerEnabled.value
        if (!wasEnabled) {
            Log.d(TAG, "Enabling equalizer to apply AutoEQ profile")
            setEqualizerEnabled(true)

            // Wait for equalizer to initialize using coroutine
            scope.launch {
                // Increased delay to ensure service has time to initialize equalizer
                // This is critical because the service needs to:
                // 1. Process the enable intent
                // 2. Check if equalizer object exists (may need to reinitialize)
                // 3. Enable the equalizer
                delay(300) // Increased from 150ms to 300ms for reliability

                // Apply the profile as a preset
                applyEqualizerPreset("AutoEQ: ${profile.name}", levels)
                Log.d(TAG, "AutoEQ profile applied successfully (equalizer was enabled automatically)")
            }
        } else {
            // Equalizer already enabled, apply immediately
            applyEqualizerPreset("AutoEQ: ${profile.name}", levels)
            Log.d(TAG, "AutoEQ profile applied successfully (equalizer was already enabled)")
        }
    }

    fun getAutoEQRecommendedProfiles(): List<AutoEQProfile> {
        return autoEQManager.getRecommendedProfiles()
    }

    // User Audio Device Management
    override fun saveUserAudioDevice(device: UserAudioDevice) {
        val currentDevicesJson = appSettings.userAudioDevices.value
        val currentDevices = UserAudioDevice.fromJson(currentDevicesJson).toMutableList()

        // Check if device already exists (update) or is new (add)
        val existingIndex = currentDevices.indexOfFirst { it.id == device.id }
        if (existingIndex >= 0) {
            currentDevices[existingIndex] = device
            Log.d(TAG, "Updated audio device: ${device.name}")
        } else {
            currentDevices.add(device)
            Log.d(TAG, "Added new audio device: ${device.name}")
        }

        appSettings.setUserAudioDevices(UserAudioDevice.toJson(currentDevices))
    }

    override fun deleteUserAudioDevice(deviceId: String) {
        val currentDevicesJson = appSettings.userAudioDevices.value
        val currentDevices = UserAudioDevice.fromJson(currentDevicesJson).toMutableList()

        currentDevices.removeAll { it.id == deviceId }
        appSettings.setUserAudioDevices(UserAudioDevice.toJson(currentDevices))

        // If deleted device was active, clear active device
        if (appSettings.activeAudioDeviceId.value == deviceId) {
            appSettings.setActiveAudioDeviceId(null)
        }

        Log.d(TAG, "Deleted audio device: $deviceId")
    }

    override fun setActiveAudioDevice(device: UserAudioDevice) {
        appSettings.setActiveAudioDeviceId(device.id)
        Log.d(TAG, "Set active audio device: ${device.name}")

        // Apply AutoEQ profile if the device has one configured
        val deviceProfileName = device.autoEQProfileName
        if (deviceProfileName != null) {
            val profile: AutoEQProfile? = autoEQManager.findProfileByName(deviceProfileName)
            if (profile != null) {
                applyAutoEQProfile(profile)
                Log.d(TAG, "Applied AutoEQ profile for device: ${device.autoEQProfileName}")
            }
        }

        // Apply device-specific mono audio preference
        setMonoAudioEnabledInternal(device.monoAudioEnabled)
        Log.d(TAG, "Applied mono audio for device: ${device.monoAudioEnabled}")
    }

    fun getActiveAudioDevice(): UserAudioDevice? {
        val activeId = appSettings.activeAudioDeviceId.value ?: return null
        val devices = UserAudioDevice.fromJson(appSettings.userAudioDevices.value)
        return devices.find { it.id == activeId }
    }

    /**
     * Try to match a connected audio device (from PlaybackLocation) with saved UserAudioDevice
     * Uses fuzzy matching to account for slight name variations
     */
    override fun findMatchingUserDevice(deviceName: String): UserAudioDevice? {
        val devices = UserAudioDevice.fromJson(appSettings.userAudioDevices.value)
        if (devices.isEmpty()) return null

        val normalizedSearchName = deviceName.lowercase().trim()

        // First try exact match
        devices.find { it.name.equals(deviceName, ignoreCase = true) }?.let { return it }

        // Try fuzzy matching - device name contains saved name or vice versa
        devices.find { savedDevice ->
            val normalizedSavedName = savedDevice.name.lowercase().trim()
            normalizedSearchName.contains(normalizedSavedName) ||
            normalizedSavedName.contains(normalizedSearchName)
        }?.let { return it }

        // Try matching by brand if available
        devices.find { savedDevice ->
            if (savedDevice.brand.isNotEmpty()) {
                normalizedSearchName.contains(savedDevice.brand.lowercase())
            } else false
        }?.let { return it }

        return null
    }

    /**
     * Check if device detection should show AutoEQ suggestion
     * Returns true if device hasn't been dismissed before
     */
    fun shouldShowAutoEQSuggestion(deviceId: String): Boolean {
        val dismissedDevices = appSettings.dismissedAutoEQSuggestions.value?.split(",") ?: emptyList()
        return !dismissedDevices.contains(deviceId)
    }

    /**
     * Mark a device as "don't ask again" for AutoEQ suggestions
     */
    override fun dismissAutoEQSuggestion(deviceId: String) {
        val current = appSettings.dismissedAutoEQSuggestions.value ?: ""
        val dismissedList = if (current.isEmpty()) {
            listOf(deviceId)
        } else {
            current.split(",").toMutableList().apply { add(deviceId) }
        }
        appSettings.setDismissedAutoEQSuggestions(dismissedList.joinToString(","))
        Log.d(TAG, "Dismissed AutoEQ suggestion for device: $deviceId")
    }

    // Custom Equalizer Presets & Reordering
    override fun saveCustomEqualizerPreset(name: String, bands: List<Float>): CustomEqualizerPreset {
        val preset = CustomEqualizerPreset(
            name = name,
            bands = bands
        )
        appSettings.saveCustomEqualizerPreset(preset)
        Log.d(TAG, "Saved custom equalizer preset: $name")
        return preset
    }

    override fun deleteCustomEqualizerPreset(id: String) {
        val customPresets = appSettings.customEqualizerPresets.value
        val preset = customPresets.find { it.id == id }
        val currentPreset = appSettings.equalizerPreset.value
        val isActive = preset != null && (currentPreset == preset.name || currentPreset == "Custom")

        appSettings.deleteCustomEqualizerPreset(id)

        if (isActive) {
            val flatBands = List(10) { 0f }
            appSettings.setEqualizerPreset("Flat")
            appSettings.setEqualizerBandLevels(flatBands.joinToString(","))
            applyEqualizerPreset("Flat", flatBands)
        }
        Log.d(TAG, "Deleted custom equalizer preset: $id")
    }

    fun saveCustomAutoEQProfile(profile: AutoEQProfile) {
        val verifiedBands = when {
            profile.bands.size == 10 -> profile.bands
            profile.bands.size > 10 -> profile.bands.take(10)
            else -> profile.bands + List(10 - profile.bands.size) { 0f }
        }
        val normalized = profile.copy(bands = verifiedBands)
        appSettings.saveCustomAutoEQProfile(normalized)
        autoEQManager.setCustomProfiles(appSettings.customAutoEQProfiles.value)
        _autoEQProfiles.value = autoEQManager.getAllProfiles()
        pinAutoEQProfile(normalized.name)
        Log.d(TAG, "Saved custom AutoEQ profile with 10 bands: ${normalized.name}")
    }

    override fun saveCustomAutoEQProfiles(profiles: List<AutoEQProfile>) {
        val normalizedList = profiles.map { profile ->
            val verifiedBands = when {
                profile.bands.size == 10 -> profile.bands
                profile.bands.size > 10 -> profile.bands.take(10)
                else -> profile.bands + List(10 - profile.bands.size) { 0f }
            }
            profile.copy(bands = verifiedBands)
        }
        appSettings.saveCustomAutoEQProfiles(normalizedList)
        autoEQManager.setCustomProfiles(appSettings.customAutoEQProfiles.value)
        _autoEQProfiles.value = autoEQManager.getAllProfiles()
        normalizedList.firstOrNull()?.let { pinAutoEQProfile(it.name) }
        Log.d(TAG, "Saved ${normalizedList.size} custom AutoEQ profiles (10 bands enforced)")
    }

    override fun deleteAutoEQProfile(name: String) {
        // 1. Delete from custom profiles if present
        appSettings.deleteCustomAutoEQProfile(name)
        autoEQManager.setCustomProfiles(appSettings.customAutoEQProfiles.value)
        _autoEQProfiles.value = autoEQManager.getAllProfiles()

        // 2. Unpin from pinned profiles
        appSettings.unpinAutoEQProfile(name)

        // 3. Check if this profile was actively selected or applied
        val currentAutoEQ = appSettings.autoEQProfile.value
        val currentPreset = appSettings.equalizerPreset.value
        val isCurrentAutoEQ = currentAutoEQ.equals(name, ignoreCase = true) ||
                currentPreset == "AutoEQ: $name" ||
                currentPreset.equals(name, ignoreCase = true)

        if (isCurrentAutoEQ) {
            appSettings.setAutoEQProfile("")
            appSettings.setEqualizerPreset("Flat")
            val flatBands = List(10) { 0f }
            appSettings.setEqualizerBandLevels(flatBands.joinToString(","))
            applyEqualizerPreset("Flat", flatBands)
        }

        // 4. Clear this AutoEQ profile from any saved user audio devices
        val currentDevicesJson = appSettings.userAudioDevices.value
        if (currentDevicesJson != null) {
            val devices = UserAudioDevice.fromJson(currentDevicesJson)
            var modified = false
            val updatedDevices = devices.map { device ->
                if (device.autoEQProfileName.equals(name, ignoreCase = true)) {
                    modified = true
                    device.copy(autoEQProfileName = null)
                } else {
                    device
                }
            }
            if (modified) {
                appSettings.setUserAudioDevices(UserAudioDevice.toJson(updatedDevices))
            }
        }

        // 5. Remove from preset order and hidden presets
        val key = "AutoEQ: $name"
        val order = appSettings.equalizerPresetOrder.value.toMutableList()
        if (order.remove(key) || order.remove(name)) {
            appSettings.setEqualizerPresetOrder(order)
        }
        val hidden = appSettings.hiddenEqualizerPresets.value.toMutableSet()
        if (hidden.remove(key) || hidden.remove(name)) {
            appSettings.setHiddenEqualizerPresets(hidden)
        }
        Log.d(TAG, "Deleted AutoEQ profile and cleaned references: $name")
    }

    override fun setEqualizerPresetOrder(order: List<String>) {
        appSettings.setEqualizerPresetOrder(order)
    }

    override fun resetEqualizerPresetOrder() {
        appSettings.resetEqualizerPresetOrder()
    }

    override fun setHiddenEqualizerPresets(hidden: Set<String>) {
        appSettings.setHiddenEqualizerPresets(hidden)
    }

    override fun pinAutoEQProfile(name: String) {
        appSettings.pinAutoEQProfile(name)
        Log.d(TAG, "Pinned AutoEQ profile: $name")
    }

    fun unpinAutoEQProfile(name: String) {
        appSettings.unpinAutoEQProfile(name)
        Log.d(TAG, "Unpinned AutoEQ profile: $name")
    }

    fun assignAutoEQProfileToDevice(device: UserAudioDevice, profileName: String?) {
        val updated = device.copy(autoEQProfileName = profileName)
        saveUserAudioDevice(updated)
        if (profileName != null) {
            val profile = autoEQManager.findProfileByName(profileName)
            if (profile != null) {
                applyAutoEQProfile(profile)
            }
            if (profileName.isNotBlank() && !profileName.equals("None", ignoreCase = true)) {
                pinAutoEQProfile(profileName)
            }
        }
    }

    /**
     * Opens the system equalizer for the current audio session via the
     * injected opener (EqualizerUtils, :feature:local).
     */
    override fun openSystemEqualizer(activity: Activity?, requestCode: Int) {
        openSystemEqualizerImpl(context, audioSessionId(), activity, requestCode)
    }

    companion object {
        private const val TAG = "EqDspController"
    }
}
