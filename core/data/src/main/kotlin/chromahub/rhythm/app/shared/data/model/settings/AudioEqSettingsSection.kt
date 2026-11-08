package chromahub.rhythm.app.shared.data.model.settings

import android.content.SharedPreferences
import androidx.core.content.edit
import android.util.Log
import com.qlzhj.rhythm.core.model.AutoEQProfile
import com.qlzhj.rhythm.core.model.CustomEqualizerPreset
import chromahub.rhythm.app.shared.data.model.AppSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
/**
 * Equalizer / audio effects / audio device settings state and write
 * operations (B-phase B2 domain split).
 */
 */
internal class AudioEqSettingsSection(
    private val prefs: SharedPreferences,
    private val updateDerived: () -> Unit,
) {

    val defaultEqualizerPresetOrder = listOf(
        "Flat", "Rock", "Pop", "Jazz", "Classical", "Electronic",
        "Hip Hop", "Vocal", "Bass Boost", "Treble Boost", "V-Shape", "Harman"
    )
    internal val _audioDeviceLoggingEnabled = MutableStateFlow(prefs.getBoolean("audio_device_logging_enabled", false))

    val audioDeviceLoggingEnabled: StateFlow<Boolean> get() = _audioDeviceLoggingEnabled.asStateFlow()
    fun setAudioDeviceLoggingEnabled(enabled: Boolean) {
        prefs.edit { putBoolean("audio_device_logging_enabled", enabled) }
        _audioDeviceLoggingEnabled.value = enabled
    }
    internal val _audioEngineDisabledDecoders = MutableStateFlow(prefs.getStringSet(AppSettings.KEY_AUDIO_ENGINE_DISABLED_DECODERS, emptySet())?.toSet() ?: emptySet())

    val audioEngineDisabledDecoders: StateFlow<Set<String>> get() = _audioEngineDisabledDecoders.asStateFlow()
    fun setAudioEngineDisabledDecoders(disabledSet: Set<String>) {
        prefs.edit { putStringSet(AppSettings.KEY_AUDIO_ENGINE_DISABLED_DECODERS, disabledSet) }
        _audioEngineDisabledDecoders.value = disabledSet
    }
    internal val _audioEngineFloatOutput = MutableStateFlow(
        run {
            if (!prefs.getBoolean(AppSettings.KEY_AUDIO_ENGINE_FLOAT_OUTPUT_MIGRATED, false)) {
                prefs.edit {
                    putBoolean(AppSettings.KEY_AUDIO_ENGINE_FLOAT_OUTPUT, false)
                    putBoolean(AppSettings.KEY_AUDIO_ENGINE_FLOAT_OUTPUT_MIGRATED, true)
                }
                false
            } else {
                prefs.getBoolean(AppSettings.KEY_AUDIO_ENGINE_FLOAT_OUTPUT, false)
            }
        }
    )

    val audioEngineFloatOutput: StateFlow<Boolean> get() = _audioEngineFloatOutput.asStateFlow()
    fun setAudioEngineFloatOutput(enable: Boolean) {
        prefs.edit {
            putBoolean(AppSettings.KEY_AUDIO_ENGINE_FLOAT_OUTPUT, enable)
            putBoolean(AppSettings.KEY_AUDIO_ENGINE_FLOAT_OUTPUT_MIGRATED, true)
        }
        _audioEngineFloatOutput.value = enable
    }
    internal val _audioEnginePreferHardware = MutableStateFlow(prefs.getBoolean(AppSettings.KEY_AUDIO_ENGINE_PREFER_HARDWARE, true))

    val audioEnginePreferHardware: StateFlow<Boolean> get() = _audioEnginePreferHardware.asStateFlow()
    fun setAudioEnginePreferHardware(prefer: Boolean) {
        prefs.edit { putBoolean(AppSettings.KEY_AUDIO_ENGINE_PREFER_HARDWARE, prefer) }
        _audioEnginePreferHardware.value = prefer
    }
    internal val _audioEngineSoftwareEnabled = MutableStateFlow(prefs.getBoolean(AppSettings.KEY_AUDIO_ENGINE_SOFTWARE_ENABLED, true))

    val audioEngineSoftwareEnabled: StateFlow<Boolean> get() = _audioEngineSoftwareEnabled.asStateFlow()
    fun setAudioEngineSoftwareEnabled(enabled: Boolean) {
        prefs.edit { putBoolean(AppSettings.KEY_AUDIO_ENGINE_SOFTWARE_ENABLED, enabled) }
        _audioEngineSoftwareEnabled.value = enabled
    }

    internal val _audioNormalization = MutableStateFlow(prefs.getBoolean(AppSettings.KEY_AUDIO_NORMALIZATION, true))

    val audioNormalization: StateFlow<Boolean> get() = _audioNormalization.asStateFlow()
    fun setAudioNormalization(enable: Boolean) {
        prefs.edit { putBoolean(AppSettings.KEY_AUDIO_NORMALIZATION, enable) }
        _audioNormalization.value = enable
    }
    internal val _audioOffloadEnabled = MutableStateFlow(prefs.getBoolean(AppSettings.KEY_AUDIO_OFFLOAD_ENABLED, false))

    val audioOffloadEnabled: StateFlow<Boolean> get() = _audioOffloadEnabled.asStateFlow()
    fun setAudioOffloadEnabled(enable: Boolean) {
        prefs.edit { putBoolean(AppSettings.KEY_AUDIO_OFFLOAD_ENABLED, enable) }
        _audioOffloadEnabled.value = enable
        updateDerived()
    }
    internal val _audioRoutingMode = MutableStateFlow(prefs.getString(AppSettings.KEY_AUDIO_ROUTING_MODE, "default") ?: "default")

    val audioRoutingMode: StateFlow<String> get() = _audioRoutingMode.asStateFlow()
    fun setAudioRoutingMode(mode: String) {
        require(mode in listOf("default", "app", "system")) { "Invalid audio routing mode: $mode" }
        prefs.edit { putString(AppSettings.KEY_AUDIO_ROUTING_MODE, mode) }
        _audioRoutingMode.value = mode
        Log.d("AppSettings", "Audio routing mode set to: $mode")
    }
    internal val _autoEQProfile = MutableStateFlow(prefs.getString(AppSettings.KEY_AUTOEQ_PROFILE, "") ?: "")

    val autoEQProfile: StateFlow<String> get() = _autoEQProfile.asStateFlow()
    fun setAutoEQProfile(profileName: String) {
        prefs.edit { putString(AppSettings.KEY_AUTOEQ_PROFILE, profileName) }
        _autoEQProfile.value = profileName
    }
    internal val _isBassBoostAvailable = MutableStateFlow(prefs.getBoolean(AppSettings.KEY_BASS_BOOST_AVAILABLE, true))

    val isBassBoostAvailable: StateFlow<Boolean> get() = _isBassBoostAvailable.asStateFlow()
    fun setBassBoostAvailable(available: Boolean) {
        prefs.edit { putBoolean(AppSettings.KEY_BASS_BOOST_AVAILABLE, available) }
        _isBassBoostAvailable.value = available
    }
    internal val _bassBoostEnabled = MutableStateFlow(prefs.getBoolean(AppSettings.KEY_BASS_BOOST_ENABLED, false))

    val bassBoostEnabled: StateFlow<Boolean> get() = _bassBoostEnabled.asStateFlow()
    fun setBassBoostEnabled(enable: Boolean) {
        prefs.edit { putBoolean(AppSettings.KEY_BASS_BOOST_ENABLED, enable) }
        _bassBoostEnabled.value = enable
    }
    internal val _bassBoostStrength = MutableStateFlow(prefs.getInt(AppSettings.KEY_BASS_BOOST_STRENGTH, 0))

    val bassBoostStrength: StateFlow<Int> get() = _bassBoostStrength.asStateFlow()
    fun setBassBoostStrength(strength: Int) {
        prefs.edit { putInt(AppSettings.KEY_BASS_BOOST_STRENGTH, strength) }
        _bassBoostStrength.value = strength
    }
    internal val _crossfade = MutableStateFlow(prefs.getBoolean(AppSettings.KEY_CROSSFADE, true))

    val crossfade: StateFlow<Boolean> get() = _crossfade.asStateFlow()
    fun setCrossfade(enable: Boolean) {
        prefs.edit { putBoolean(AppSettings.KEY_CROSSFADE, enable) }
        _crossfade.value = enable
    }
    internal val _crossfadeDuration = MutableStateFlow(prefs.getFloat(AppSettings.KEY_CROSSFADE_DURATION, 4f))

    val crossfadeDuration: StateFlow<Float> get() = _crossfadeDuration.asStateFlow()
    fun setCrossfadeDuration(duration: Float) {
        if (isValidCrossfadeDuration(duration)) {
            prefs.edit { putFloat(AppSettings.KEY_CROSSFADE_DURATION, duration) }
            _crossfadeDuration.value = duration
        } else {
            Log.w("AppSettings", "Invalid crossfade duration: $duration, keeping current value")
        }
    }
    internal val _crossfadeOnSkip = MutableStateFlow(prefs.getBoolean(AppSettings.KEY_CROSSFADE_ON_SKIP, true))

    val crossfadeOnSkip: StateFlow<Boolean> get() = _crossfadeOnSkip.asStateFlow()
    fun setCrossfadeOnSkip(enable: Boolean) {
        prefs.edit { putBoolean(AppSettings.KEY_CROSSFADE_ON_SKIP, enable) }
        _crossfadeOnSkip.value = enable
    }
    internal val _crossfadeRepeatOne = MutableStateFlow(prefs.getBoolean(AppSettings.KEY_CROSSFADE_REPEAT_ONE, true))

    val crossfadeRepeatOne: StateFlow<Boolean> get() = _crossfadeRepeatOne.asStateFlow()
    fun setCrossfadeRepeatOne(enabled: Boolean) {
        prefs.edit { putBoolean(AppSettings.KEY_CROSSFADE_REPEAT_ONE, enabled) }
        _crossfadeRepeatOne.value = enabled
    }
    internal val _decoderExtensionsEnabled = MutableStateFlow(prefs.getBoolean(AppSettings.KEY_DECODER_EXTENSIONS_ENABLED, true))

    val decoderExtensionsEnabled: StateFlow<Boolean> get() = _decoderExtensionsEnabled.asStateFlow()
    fun setDecoderExtensionsEnabled(enabled: Boolean) {
        prefs.edit { putBoolean(AppSettings.KEY_DECODER_EXTENSIONS_ENABLED, enabled) }
        _decoderExtensionsEnabled.value = enabled
    }
    internal val _dismissedAutoEQSuggestions = MutableStateFlow(prefs.getString(AppSettings.KEY_DISMISSED_AUTOEQ_SUGGESTIONS, null))

    val dismissedAutoEQSuggestions: StateFlow<String?> get() = _dismissedAutoEQSuggestions.asStateFlow()
    fun setDismissedAutoEQSuggestions(dismissedDevices: String?) {
        prefs.edit { putString(AppSettings.KEY_DISMISSED_AUTOEQ_SUGGESTIONS, dismissedDevices) }
        _dismissedAutoEQSuggestions.value = dismissedDevices
    }
    internal val _equalizerBandLevels = MutableStateFlow(prefs.getString(AppSettings.KEY_EQUALIZER_BAND_LEVELS, "0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0") ?: "0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0")

    val equalizerBandLevels: StateFlow<String> get() = _equalizerBandLevels.asStateFlow()
    fun setEqualizerBandLevels(levels: String) {
        prefs.edit { putString(AppSettings.KEY_EQUALIZER_BAND_LEVELS, levels) }
        _equalizerBandLevels.value = levels
    }
    internal val _equalizerEnabled = MutableStateFlow(prefs.getBoolean(AppSettings.KEY_EQUALIZER_ENABLED, false))

    val equalizerEnabled: StateFlow<Boolean> get() = _equalizerEnabled.asStateFlow()
    fun setEqualizerEnabled(enable: Boolean) {
        prefs.edit { putBoolean(AppSettings.KEY_EQUALIZER_ENABLED, enable) }
        _equalizerEnabled.value = enable
    }
    internal val _equalizerPreset = MutableStateFlow(prefs.getString(AppSettings.KEY_EQUALIZER_PRESET, "Custom") ?: "Custom")

    val equalizerPreset: StateFlow<String> get() = _equalizerPreset.asStateFlow()
    fun setEqualizerPreset(preset: String) {
        prefs.edit { putString(AppSettings.KEY_EQUALIZER_PRESET, preset) }
        _equalizerPreset.value = preset
    }
    internal val _equalizerPresetOrder = MutableStateFlow(
        prefs.getString(AppSettings.KEY_EQUALIZER_PRESET_ORDER, null)
            ?.split(",")
            ?.filter { it.isNotBlank() }
            ?.takeIf { it.isNotEmpty() }
            ?: defaultEqualizerPresetOrder
    )

    val equalizerPresetOrder: StateFlow<List<String>> get() = _equalizerPresetOrder.asStateFlow()
    fun setEqualizerPresetOrder(order: List<String>) {
        val orderStr = order.joinToString(",")
        prefs.edit { putString(AppSettings.KEY_EQUALIZER_PRESET_ORDER, orderStr) }
        _equalizerPresetOrder.value = order
    }
    internal val _hiddenEqualizerPresets = MutableStateFlow(
        prefs.getString(AppSettings.KEY_HIDDEN_EQUALIZER_PRESETS, null)
            ?.split(",")
            ?.filter { it.isNotBlank() }
            ?.toSet()
            ?: emptySet()
    )

    val hiddenEqualizerPresets: StateFlow<Set<String>> get() = _hiddenEqualizerPresets.asStateFlow()
    fun setHiddenEqualizerPresets(hidden: Set<String>) {
        val hiddenStr = hidden.joinToString(",")
        prefs.edit { putString(AppSettings.KEY_HIDDEN_EQUALIZER_PRESETS, hiddenStr) }
        _hiddenEqualizerPresets.value = hidden
    }
    internal val _monoAudioEnabled = MutableStateFlow(prefs.getBoolean(AppSettings.KEY_MONO_AUDIO_ENABLED, false))

    val monoAudioEnabled: StateFlow<Boolean> get() = _monoAudioEnabled.asStateFlow()
    fun setMonoAudioEnabled(enable: Boolean) {
        prefs.edit { putBoolean(AppSettings.KEY_MONO_AUDIO_ENABLED, enable) }
        _monoAudioEnabled.value = enable
    }
    internal val _replayGain = MutableStateFlow(prefs.getBoolean(AppSettings.KEY_REPLAY_GAIN, false))

    val replayGain: StateFlow<Boolean> get() = _replayGain.asStateFlow()
    fun setReplayGain(enable: Boolean) {
        prefs.edit { putBoolean(AppSettings.KEY_REPLAY_GAIN, enable) }
        _replayGain.value = enable
    }
    internal val _replayGainDrc = MutableStateFlow(prefs.getBoolean(AppSettings.KEY_REPLAY_GAIN_DRC, true))

    val replayGainDrc: StateFlow<Boolean> get() = _replayGainDrc.asStateFlow()
    fun setReplayGainDrc(enable: Boolean) {
        prefs.edit { putBoolean(AppSettings.KEY_REPLAY_GAIN_DRC, enable) }
        _replayGainDrc.value = enable
    }
    internal val _replayGainMode = MutableStateFlow(prefs.getInt(AppSettings.KEY_REPLAY_GAIN_MODE, 1)) // 1 = Track, 2 = Album, 0 = None

    val replayGainMode: StateFlow<Int> get() = _replayGainMode.asStateFlow()
    fun setReplayGainMode(mode: Int) {
        prefs.edit { putInt(AppSettings.KEY_REPLAY_GAIN_MODE, mode) }
        _replayGainMode.value = mode
    }
    internal val _replayGainPreamp = MutableStateFlow(prefs.getFloat(AppSettings.KEY_REPLAY_GAIN_PREAMP, 0f))

    val replayGainPreamp: StateFlow<Float> get() = _replayGainPreamp.asStateFlow()
    fun setReplayGainPreamp(preamp: Float) {
        prefs.edit { putFloat(AppSettings.KEY_REPLAY_GAIN_PREAMP, preamp) }
        _replayGainPreamp.value = preamp
    }
    internal val _replayGainPreampUntagged = MutableStateFlow(prefs.getFloat(AppSettings.KEY_REPLAY_GAIN_PREAMP_UNTAGGED, 0f))

    val replayGainPreampUntagged: StateFlow<Float> get() = _replayGainPreampUntagged.asStateFlow()
    fun setReplayGainPreampUntagged(preamp: Float) {
        prefs.edit { putFloat(AppSettings.KEY_REPLAY_GAIN_PREAMP_UNTAGGED, preamp) }
        _replayGainPreampUntagged.value = preamp
    }
    internal val _resumeOnDeviceReconnect = MutableStateFlow(prefs.getBoolean(AppSettings.KEY_RESUME_ON_DEVICE_RECONNECT, false))

    val resumeOnDeviceReconnect: StateFlow<Boolean> get() = _resumeOnDeviceReconnect.asStateFlow()
    fun setResumeOnDeviceReconnect(enable: Boolean) {
        prefs.edit { putBoolean(AppSettings.KEY_RESUME_ON_DEVICE_RECONNECT, enable) }
        _resumeOnDeviceReconnect.value = enable
    }
    internal val _speakerAutoEQBypass = MutableStateFlow(prefs.getBoolean(AppSettings.KEY_SPEAKER_AUTOEQ_BYPASS, true))

    val speakerAutoEQBypass: StateFlow<Boolean> get() = _speakerAutoEQBypass.asStateFlow()
    fun setSpeakerAutoEQBypass(bypass: Boolean) {
        prefs.edit { putBoolean(AppSettings.KEY_SPEAKER_AUTOEQ_BYPASS, bypass) }
        _speakerAutoEQBypass.value = bypass
    }
    internal val _virtualizerEnabled = MutableStateFlow(prefs.getBoolean(AppSettings.KEY_VIRTUALIZER_ENABLED, false))

    val virtualizerEnabled: StateFlow<Boolean> get() = _virtualizerEnabled.asStateFlow()
    fun setVirtualizerEnabled(enable: Boolean) {
        prefs.edit { putBoolean(AppSettings.KEY_VIRTUALIZER_ENABLED, enable) }
        _virtualizerEnabled.value = enable
    }
    internal val _virtualizerStrength = MutableStateFlow(prefs.getInt(AppSettings.KEY_VIRTUALIZER_STRENGTH, 0))

    val virtualizerStrength: StateFlow<Int> get() = _virtualizerStrength.asStateFlow()
    fun setVirtualizerStrength(strength: Int) {
        prefs.edit { putInt(AppSettings.KEY_VIRTUALIZER_STRENGTH, strength) }
        _virtualizerStrength.value = strength
    }
    internal val _appVolume = MutableStateFlow(prefs.getFloat(AppSettings.KEY_APP_VOLUME, 1.0f))

    val appVolume: StateFlow<Float> get() = _appVolume.asStateFlow()
    fun setAppVolume(volume: Float) {
        val clamped = volume.coerceIn(0f, 1f)
        prefs.edit { putFloat(AppSettings.KEY_APP_VOLUME, clamped) }
        _appVolume.value = clamped
    }
    internal val _useSystemVolume = MutableStateFlow(prefs.getBoolean(AppSettings.KEY_USE_SYSTEM_VOLUME, true))

    val useSystemVolume: StateFlow<Boolean> get() = _useSystemVolume.asStateFlow()
    fun setUseSystemVolume(enable: Boolean) {
        prefs.edit { putBoolean(AppSettings.KEY_USE_SYSTEM_VOLUME, enable) }
        _useSystemVolume.value = enable
    }
    internal val _stopPlaybackOnZeroVolume = MutableStateFlow(prefs.getBoolean(AppSettings.KEY_STOP_PLAYBACK_ON_ZERO_VOLUME, false))

    val stopPlaybackOnZeroVolume: StateFlow<Boolean> get() = _stopPlaybackOnZeroVolume.asStateFlow()
    fun setStopPlaybackOnZeroVolume(enable: Boolean) {
        prefs.edit { putBoolean(AppSettings.KEY_STOP_PLAYBACK_ON_ZERO_VOLUME, enable) }
        _stopPlaybackOnZeroVolume.value = enable
    }
    internal val _autoConnectDevice = MutableStateFlow(prefs.getBoolean(AppSettings.KEY_AUTO_CONNECT_DEVICE, true))

    val autoConnectDevice: StateFlow<Boolean> get() = _autoConnectDevice.asStateFlow()
    fun setAutoConnectDevice(enable: Boolean) {
        prefs.edit { putBoolean(AppSettings.KEY_AUTO_CONNECT_DEVICE, enable) }
        _autoConnectDevice.value = enable
    }
    internal val _activeAudioDeviceId = MutableStateFlow(prefs.getString(AppSettings.KEY_ACTIVE_AUDIO_DEVICE_ID, null))

    val activeAudioDeviceId: StateFlow<String?> get() = _activeAudioDeviceId.asStateFlow()
    fun setActiveAudioDeviceId(deviceId: String?) {
        prefs.edit { putString(AppSettings.KEY_ACTIVE_AUDIO_DEVICE_ID, deviceId) }
        _activeAudioDeviceId.value = deviceId
    }
    internal val _customEqualizerPresets = MutableStateFlow(
        CustomEqualizerPreset.fromJson(prefs.getString(AppSettings.KEY_CUSTOM_EQUALIZER_PRESETS, null))
    )

    val customEqualizerPresets: StateFlow<List<CustomEqualizerPreset>> get() = _customEqualizerPresets.asStateFlow()
    internal val _pinnedAutoEQProfiles = MutableStateFlow(
        prefs.getString(AppSettings.KEY_PINNED_AUTOEQ_PROFILES, null)
            ?.split(";;")
            ?.filter { it.isNotBlank() }
            ?: emptyList()
    )

    val pinnedAutoEQProfiles: StateFlow<List<String>> get() = _pinnedAutoEQProfiles.asStateFlow()
    fun saveCustomEqualizerPreset(preset: CustomEqualizerPreset) {
        val current = _customEqualizerPresets.value.toMutableList()
        val index = current.indexOfFirst { it.id == preset.id || it.name.equals(preset.name, ignoreCase = true) }
        if (index >= 0) {
            current[index] = preset
        } else {
            current.add(preset)
        }
        val json = CustomEqualizerPreset.toJson(current)
        prefs.edit { putString(AppSettings.KEY_CUSTOM_EQUALIZER_PRESETS, json) }
        _customEqualizerPresets.value = current

        // Also ensure preset is in the preset order
        val order = _equalizerPresetOrder.value.toMutableList()
        if (preset.name !in order) {
            val firstBuiltInIndex = order.indexOfFirst { it in defaultEqualizerPresetOrder }
            if (firstBuiltInIndex >= 0) {
                order.add(firstBuiltInIndex, preset.name)
            } else {
                order.add(preset.name)
            }
            setEqualizerPresetOrder(order)
        }
    }
    fun deleteCustomEqualizerPreset(id: String) {
        val current = _customEqualizerPresets.value.toMutableList()
        val preset = current.find { it.id == id }
        current.removeAll { it.id == id }
        val json = CustomEqualizerPreset.toJson(current)
        prefs.edit { putString(AppSettings.KEY_CUSTOM_EQUALIZER_PRESETS, json) }
        _customEqualizerPresets.value = current

        if (preset != null) {
            val order = _equalizerPresetOrder.value.toMutableList()
            order.remove(preset.name)
            setEqualizerPresetOrder(order)
            val hidden = _hiddenEqualizerPresets.value.toMutableSet()
            hidden.remove(preset.name)
            setHiddenEqualizerPresets(hidden)

            if (_equalizerPreset.value == preset.name) {
                setEqualizerPreset("Flat")
                setEqualizerBandLevels(List(10) { 0f }.joinToString(","))
            }
        }
    }
    fun resetEqualizerPresetOrder() {
        val customNames = _customEqualizerPresets.value.map { it.name }
        val pinnedNames = _pinnedAutoEQProfiles.value.map { "AutoEQ: $it" }
        val resetOrder = pinnedNames + customNames + defaultEqualizerPresetOrder
        prefs.edit { remove(AppSettings.KEY_EQUALIZER_PRESET_ORDER) }
        _equalizerPresetOrder.value = resetOrder
        setHiddenEqualizerPresets(emptySet())
    }
    fun pinAutoEQProfile(name: String) {
        val current = _pinnedAutoEQProfiles.value.toMutableList()
        if (!current.contains(name)) {
            current.add(name)
            val str = current.joinToString(";;")
            prefs.edit { putString(AppSettings.KEY_PINNED_AUTOEQ_PROFILES, str) }
            _pinnedAutoEQProfiles.value = current

            val order = _equalizerPresetOrder.value.toMutableList()
            val presetKey = "AutoEQ: $name"
            if (presetKey !in order) {
                val lastAutoEQIndex = order.indexOfLast { it.startsWith("AutoEQ: ") }
                val insertIndex = if (lastAutoEQIndex >= 0) lastAutoEQIndex + 1 else 0
                order.add(insertIndex, presetKey)
                setEqualizerPresetOrder(order)
            }
        }
    }
    fun unpinAutoEQProfile(name: String) {
        val current = _pinnedAutoEQProfiles.value.toMutableList()
        current.remove(name)
        val str = current.joinToString(";;")
        prefs.edit { putString(AppSettings.KEY_PINNED_AUTOEQ_PROFILES, str) }
        _pinnedAutoEQProfiles.value = current

        val order = _equalizerPresetOrder.value.toMutableList()
        val presetKey = "AutoEQ: $name"
        order.remove(presetKey)
        setEqualizerPresetOrder(order)
        val hidden = _hiddenEqualizerPresets.value.toMutableSet()
        hidden.remove(presetKey)
        setHiddenEqualizerPresets(hidden)
    }
    fun saveCustomAutoEQProfile(profile: AutoEQProfile) {
        val current = _customAutoEQProfiles.value.toMutableList()
        val index = current.indexOfFirst { it.name.equals(profile.name, ignoreCase = true) }
        if (index >= 0) {
            current[index] = profile
        } else {
            current.add(profile)
        }
        val json = AutoEQProfile.listToJson(current)
        prefs.edit { putString(AppSettings.KEY_CUSTOM_AUTOEQ_PROFILES, json) }
        _customAutoEQProfiles.value = current
    }
    fun saveCustomAutoEQProfiles(profiles: List<AutoEQProfile>) {
        val current = _customAutoEQProfiles.value.toMutableList()
        for (profile in profiles) {
            val index = current.indexOfFirst { it.name.equals(profile.name, ignoreCase = true) }
            if (index >= 0) {
                current[index] = profile
            } else {
                current.add(profile)
            }
        }
        val json = AutoEQProfile.listToJson(current)
        prefs.edit { putString(AppSettings.KEY_CUSTOM_AUTOEQ_PROFILES, json) }
        _customAutoEQProfiles.value = current
    }
    fun deleteCustomAutoEQProfile(name: String) {
        val current = _customAutoEQProfiles.value.toMutableList()
        current.removeAll { it.name.equals(name, ignoreCase = true) }
        val json = AutoEQProfile.listToJson(current)
        prefs.edit { putString(AppSettings.KEY_CUSTOM_AUTOEQ_PROFILES, json) }
        _customAutoEQProfiles.value = current
        unpinAutoEQProfile(name)
        if (_autoEQProfile.value.equals(name, ignoreCase = true)) {
            setAutoEQProfile("")
        }
    }
    fun toggleAudioEngineDecoder(decoderName: String, enabled: Boolean) {
        val current = _audioEngineDisabledDecoders.value.toMutableSet()
        if (enabled) {
            current.remove(decoderName)
        } else {
            current.add(decoderName)
        }
        setAudioEngineDisabledDecoders(current)
    }
    fun resetAudioEngineDecoders() {
        setAudioEngineDisabledDecoders(emptySet())
    }
    fun toggleDecoderExtension(extensionId: String, enabled: Boolean) {
        val current = _disabledDecoderExtensions.value.toMutableSet()
        if (enabled) {
            current.remove(extensionId)
        } else {
            current.add(extensionId)
        }
        setDisabledDecoderExtensions(current)
    }
    fun resetDecoderExtensions() {
        setDisabledDecoderExtensions(emptySet())
    }
    fun isBassBoostAvailable(): Boolean {
        return _isBassBoostAvailable.value
    }
    internal val _customAutoEQProfiles = MutableStateFlow(
        AutoEQProfile.listFromJson(prefs.getString(AppSettings.KEY_CUSTOM_AUTOEQ_PROFILES, null))
    )

    val customAutoEQProfiles: StateFlow<List<AutoEQProfile>> get() = _customAutoEQProfiles.asStateFlow()
    internal val _disabledDecoderExtensions = MutableStateFlow(prefs.getStringSet(AppSettings.KEY_DISABLED_DECODER_EXTENSIONS, emptySet())?.toSet() ?: emptySet())

    val disabledDecoderExtensions: StateFlow<Set<String>> get() = _disabledDecoderExtensions.asStateFlow()
    fun setDisabledDecoderExtensions(disabledSet: Set<String>) {
        prefs.edit { putStringSet(AppSettings.KEY_DISABLED_DECODER_EXTENSIONS, disabledSet) }
        _disabledDecoderExtensions.value = disabledSet
    }

    /**
     * Validates audioEqSection._crossfade duration
     */
    private fun isValidCrossfadeDuration(duration: Float): Boolean {
        return duration in 0.1f..12.0f
    }
}
