package chromahub.rhythm.app.shared.data.model.settings

import android.content.SharedPreferences
import androidx.core.content.edit
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import chromahub.rhythm.app.shared.data.model.AppSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
Guard / listening-stats settings state (rhythm guard, listening stats, timeout), carved out of AppSettings (B-phase B2). The store keeps ownership of keys, persistence and cross-domain orchestration (setters that drive other sections stay in the facade).
 */
internal class GuardStatsSettingsSection(private val prefs: SharedPreferences) {
    internal val _listeningTime = MutableStateFlow(safeLong(AppSettings.KEY_LISTENING_TIME, 0L))

    val listeningTime: StateFlow<Long> get() = _listeningTime.asStateFlow()
    fun setListeningTime(time: Long) {
        prefs.edit { putLong(AppSettings.KEY_LISTENING_TIME, time) }
        _listeningTime.value = time
    }
    internal val _songsPlayed = MutableStateFlow(prefs.getInt(AppSettings.KEY_SONGS_PLAYED, 0))

    val songsPlayed: StateFlow<Int> get() = _songsPlayed.asStateFlow()
    fun setSongsPlayed(count: Int) {
        prefs.edit { putInt(AppSettings.KEY_SONGS_PLAYED, count) }
        _songsPlayed.value = count
    }
    internal val _rhythmAuraMode = MutableStateFlow(
        sanitizeRhythmGuardMode(
            prefs.getString(
                AppSettings.KEY_RHYTHM_GUARD_MODE,
                prefs.getString(AppSettings.KEY_RHYTHM_AURA_MODE, AppSettings.RHYTHM_GUARD_MODE_OFF)
            )
        )
    )

    val rhythmGuardMode: StateFlow<String> get() = _rhythmAuraMode.asStateFlow()
    internal val _rhythmAuraAge = MutableStateFlow(
        prefs.getInt(AppSettings.KEY_RHYTHM_GUARD_AGE, prefs.getInt(AppSettings.KEY_RHYTHM_AURA_AGE, 18)).coerceIn(8, 80)
    )

    val rhythmGuardAge: StateFlow<Int> get() = _rhythmAuraAge.asStateFlow()
    internal val _rhythmAuraManualWarningsEnabled = MutableStateFlow(
        prefs.getBoolean(
            AppSettings.KEY_RHYTHM_GUARD_MANUAL_WARNINGS_ENABLED,
            prefs.getBoolean(AppSettings.KEY_RHYTHM_AURA_MANUAL_WARNINGS_ENABLED, true)
        )
    )

    val rhythmGuardManualWarningsEnabled: StateFlow<Boolean> get() = _rhythmAuraManualWarningsEnabled.asStateFlow()
    fun setRhythmGuardManualWarningsEnabled(enabled: Boolean) {
        prefs.edit {
    putBoolean(AppSettings.KEY_RHYTHM_GUARD_MANUAL_WARNINGS_ENABLED, enabled)
    putBoolean(AppSettings.KEY_RHYTHM_AURA_MANUAL_WARNINGS_ENABLED, enabled)
}
        _rhythmAuraManualWarningsEnabled.value = enabled
    }
    internal val _rhythmAuraManualVolumeThreshold = MutableStateFlow(
        prefs.getFloat(
            AppSettings.KEY_RHYTHM_GUARD_MANUAL_VOLUME_THRESHOLD,
            prefs.getFloat(AppSettings.KEY_RHYTHM_AURA_MANUAL_VOLUME_THRESHOLD, 0.68f)
        ).coerceIn(0.40f, 0.95f)
    )

    val rhythmGuardManualVolumeThreshold: StateFlow<Float> get() = _rhythmAuraManualVolumeThreshold.asStateFlow()
    fun setRhythmGuardManualVolumeThreshold(threshold: Float) {
        val safeThreshold = threshold.coerceIn(0.40f, 0.95f)
        prefs.edit {
    putFloat(AppSettings.KEY_RHYTHM_GUARD_MANUAL_VOLUME_THRESHOLD, safeThreshold)
    putFloat(AppSettings.KEY_RHYTHM_AURA_MANUAL_VOLUME_THRESHOLD, safeThreshold)
}
        _rhythmAuraManualVolumeThreshold.value = safeThreshold
    }
    internal val _rhythmGuardApplyVolumeLimitOnSpeaker = MutableStateFlow(
        prefs.getBoolean(AppSettings.KEY_RHYTHM_GUARD_APPLY_VOLUME_LIMIT_ON_SPEAKER, false)
    )

    val rhythmGuardApplyVolumeLimitOnSpeaker: StateFlow<Boolean> get() = _rhythmGuardApplyVolumeLimitOnSpeaker.asStateFlow()
    fun setRhythmGuardApplyVolumeLimitOnSpeaker(enabled: Boolean) {
        prefs.edit { putBoolean(AppSettings.KEY_RHYTHM_GUARD_APPLY_VOLUME_LIMIT_ON_SPEAKER, enabled) }
        _rhythmGuardApplyVolumeLimitOnSpeaker.value = enabled
    }
    internal val _rhythmAuraLastAutoAppliedAt = MutableStateFlow(
        safeLong(AppSettings.KEY_RHYTHM_GUARD_LAST_AUTO_APPLIED_AT, safeLong(AppSettings.KEY_RHYTHM_AURA_LAST_AUTO_APPLIED_AT, 0L))
    )

    val rhythmGuardLastAutoAppliedAt: StateFlow<Long> get() = _rhythmAuraLastAutoAppliedAt.asStateFlow()
    fun setRhythmGuardLastAutoAppliedAt(timestamp: Long) {
        prefs.edit {
    putLong(AppSettings.KEY_RHYTHM_GUARD_LAST_AUTO_APPLIED_AT, timestamp)
    putLong(AppSettings.KEY_RHYTHM_AURA_LAST_AUTO_APPLIED_AT, timestamp)
}
        _rhythmAuraLastAutoAppliedAt.value = timestamp
    }
    internal val _rhythmGuardAlertThresholdMinutes = MutableStateFlow(
        prefs.getInt(AppSettings.KEY_RHYTHM_GUARD_ALERT_THRESHOLD_MINUTES, -1).coerceIn(-1, 24 * 60)
    )

    val rhythmGuardAlertThresholdMinutes: StateFlow<Int> get() = _rhythmGuardAlertThresholdMinutes.asStateFlow()
    fun setRhythmGuardAlertThresholdMinutes(minutes: Int) {
        val safeMinutes = minutes.coerceIn(-1, 24 * 60)
        prefs.edit { putInt(AppSettings.KEY_RHYTHM_GUARD_ALERT_THRESHOLD_MINUTES, safeMinutes) }
        _rhythmGuardAlertThresholdMinutes.value = safeMinutes
    }
    internal val _rhythmGuardWarningTimeoutMinutes = MutableStateFlow(
        prefs.getInt(AppSettings.KEY_RHYTHM_GUARD_WARNING_TIMEOUT_MINUTES, 2).coerceIn(1, 60)
    )

    val rhythmGuardWarningTimeoutMinutes: StateFlow<Int> get() = _rhythmGuardWarningTimeoutMinutes.asStateFlow()
    fun setRhythmGuardWarningTimeoutMinutes(minutes: Int) {
        val safeMinutes = minutes.coerceIn(1, 60)
        prefs.edit { putInt(AppSettings.KEY_RHYTHM_GUARD_WARNING_TIMEOUT_MINUTES, safeMinutes) }
        _rhythmGuardWarningTimeoutMinutes.value = safeMinutes
    }
    internal val _rhythmGuardPostTimeoutCooldownMinutes = MutableStateFlow(
        prefs.getInt(AppSettings.KEY_RHYTHM_GUARD_POST_TIMEOUT_COOLDOWN_MINUTES, 10).coerceIn(1, 60)
    )

    val rhythmGuardPostTimeoutCooldownMinutes: StateFlow<Int> get() = _rhythmGuardPostTimeoutCooldownMinutes.asStateFlow()
    fun setRhythmGuardPostTimeoutCooldownMinutes(minutes: Int) {
        val safeMinutes = minutes.coerceIn(1, 60)
        prefs.edit { putInt(AppSettings.KEY_RHYTHM_GUARD_POST_TIMEOUT_COOLDOWN_MINUTES, safeMinutes) }
        _rhythmGuardPostTimeoutCooldownMinutes.value = safeMinutes
    }
    internal val _rhythmGuardBreakResumeMinutes = MutableStateFlow(
        prefs.getInt(AppSettings.KEY_RHYTHM_GUARD_BREAK_RESUME_MINUTES, 15).coerceIn(1, 180)
    )

    val rhythmGuardBreakResumeMinutes: StateFlow<Int> get() = _rhythmGuardBreakResumeMinutes.asStateFlow()
    fun setRhythmGuardBreakResumeMinutes(minutes: Int) {
        val safeMinutes = minutes.coerceIn(1, 180)
        prefs.edit { putInt(AppSettings.KEY_RHYTHM_GUARD_BREAK_RESUME_MINUTES, safeMinutes) }
        _rhythmGuardBreakResumeMinutes.value = safeMinutes
    }
    internal val _rhythmGuardTimeoutUntilMs = MutableStateFlow(
        safeLong(AppSettings.KEY_RHYTHM_GUARD_TIMEOUT_UNTIL_MS, 0L).coerceAtLeast(0L)
    )

    val rhythmGuardTimeoutUntilMs: StateFlow<Long> get() = _rhythmGuardTimeoutUntilMs.asStateFlow()
    internal val _rhythmGuardTimeoutReason = MutableStateFlow(
        prefs.getString(AppSettings.KEY_RHYTHM_GUARD_TIMEOUT_REASON, null).orEmpty()
    )

    val rhythmGuardTimeoutReason: StateFlow<String> get() = _rhythmGuardTimeoutReason.asStateFlow()
    internal val _rhythmGuardTimeoutStartedAtMs = MutableStateFlow(
        safeLong(AppSettings.KEY_RHYTHM_GUARD_TIMEOUT_STARTED_AT_MS, 0L).coerceAtLeast(0L)
    )

    val rhythmGuardTimeoutStartedAtMs: StateFlow<Long> get() = _rhythmGuardTimeoutStartedAtMs.asStateFlow()
    internal val _rhythmGuardTimeoutCooldownUntilMs = MutableStateFlow(
        safeLong(AppSettings.KEY_RHYTHM_GUARD_TIMEOUT_COOLDOWN_UNTIL_MS, 0L).coerceAtLeast(0L)
    )

    val rhythmGuardTimeoutCooldownUntilMs: StateFlow<Long> get() = _rhythmGuardTimeoutCooldownUntilMs.asStateFlow()
    fun setRhythmGuardTimeoutCooldownUntilMs(untilEpochMs: Long) {
        val safeUntil = untilEpochMs.coerceAtLeast(0L)
        prefs.edit { putLong(AppSettings.KEY_RHYTHM_GUARD_TIMEOUT_COOLDOWN_UNTIL_MS, safeUntil) }
        _rhythmGuardTimeoutCooldownUntilMs.value = safeUntil
    }
    internal val _rhythmGuardNextAllowedLimitMinutes = MutableStateFlow(
        prefs.getInt(AppSettings.KEY_RHYTHM_GUARD_NEXT_ALLOWED_LIMIT_MINUTES, 0)
    )

    val rhythmGuardNextAllowedLimitMinutes: StateFlow<Int> get() = _rhythmGuardNextAllowedLimitMinutes.asStateFlow()
    fun setRhythmGuardNextAllowedLimitMinutes(value: Int) {
        _rhythmGuardNextAllowedLimitMinutes.value = value
        prefs.edit { putInt(AppSettings.KEY_RHYTHM_GUARD_NEXT_ALLOWED_LIMIT_MINUTES, value) }
    }
    internal val _rhythmGuardFirstBreakSeen = MutableStateFlow(
        prefs.getBoolean(AppSettings.KEY_RHYTHM_GUARD_FIRST_BREAK_SEEN, false)
    )

    val rhythmGuardFirstBreakSeen: StateFlow<Boolean> get() = _rhythmGuardFirstBreakSeen.asStateFlow()
    fun setRhythmGuardFirstBreakSeen(seen: Boolean) {
        _rhythmGuardFirstBreakSeen.value = seen
        prefs.edit { putBoolean(AppSettings.KEY_RHYTHM_GUARD_FIRST_BREAK_SEEN, seen) }
    }
    internal val _rhythmGuardAlertNotificationsEnabled = MutableStateFlow(
        prefs.getBoolean(AppSettings.KEY_RHYTHM_GUARD_ALERT_NOTIFICATIONS_ENABLED, true)
    )

    val rhythmGuardAlertNotificationsEnabled: StateFlow<Boolean> get() = _rhythmGuardAlertNotificationsEnabled.asStateFlow()
    fun setRhythmGuardAlertNotificationsEnabled(enabled: Boolean) {
        prefs.edit { putBoolean(AppSettings.KEY_RHYTHM_GUARD_ALERT_NOTIFICATIONS_ENABLED, enabled) }
        _rhythmGuardAlertNotificationsEnabled.value = enabled
    }
    internal val _rhythmGuardTimerNotificationsEnabled = MutableStateFlow(
        prefs.getBoolean(AppSettings.KEY_RHYTHM_GUARD_TIMER_NOTIFICATIONS_ENABLED, true)
    )

    val rhythmGuardTimerNotificationsEnabled: StateFlow<Boolean> get() = _rhythmGuardTimerNotificationsEnabled.asStateFlow()
    fun setRhythmGuardTimerNotificationsEnabled(enabled: Boolean) {
        prefs.edit { putBoolean(AppSettings.KEY_RHYTHM_GUARD_TIMER_NOTIFICATIONS_ENABLED, enabled) }
        _rhythmGuardTimerNotificationsEnabled.value = enabled
    }
    internal val _dailyListeningStats = MutableStateFlow<Map<String, Long>>(
        try {
            val json = prefs.getString(AppSettings.KEY_DAILY_LISTENING_STATS, null)
            if (json != null) {
                Gson().fromJson(json, object : TypeToken<Map<String, Long>>() {}.type)
            } else {
                emptyMap()
            }
        } catch (e: Exception) {
            emptyMap()
        }
    )

    val dailyListeningStats: StateFlow<Map<String, Long>> get() = _dailyListeningStats.asStateFlow()
    internal val _weeklyTopArtists = MutableStateFlow<Map<String, Int>>(
        try {
            val json = prefs.getString(AppSettings.KEY_WEEKLY_TOP_ARTISTS, null)
            if (json != null) {
                Gson().fromJson(json, object : TypeToken<Map<String, Int>>() {}.type)
            } else {
                emptyMap()
            }
        } catch (e: Exception) {
            emptyMap()
        }
    )

    val weeklyTopArtists: StateFlow<Map<String, Int>> get() = _weeklyTopArtists.asStateFlow()
    internal val _moodPreferences = MutableStateFlow<Map<String, List<String>>>(
        try {
            val json = prefs.getString(AppSettings.KEY_MOOD_PREFERENCES, null)
            if (json != null) {
                Gson().fromJson(json, object : TypeToken<Map<String, List<String>>>() {}.type)
            } else {
                emptyMap()
            }
        } catch (e: Exception) {
            emptyMap()
        }
    )

    val moodPreferences: StateFlow<Map<String, List<String>>> get() = _moodPreferences.asStateFlow()
    internal val _timeBasedPreferences = MutableStateFlow<Map<Int, List<String>>>(
        try {
            val json = prefs.getString(AppSettings.KEY_TIME_BASED_PREFERENCES, null)
            if (json != null) {
                Gson().fromJson(json, object : TypeToken<Map<Int, List<String>>>() {}.type)
            } else {
                emptyMap()
            }
        } catch (e: Exception) {
            emptyMap()
        }
    )

    val timeBasedPreferences: StateFlow<Map<Int, List<String>>> get() = _timeBasedPreferences.asStateFlow()
    fun setTimeBasedPreferences(preferences: Map<Int, List<String>>) {
        val json = Gson().toJson(preferences)
        prefs.edit { putString(AppSettings.KEY_TIME_BASED_PREFERENCES, json) }
        _timeBasedPreferences.value = preferences
    }
    fun setRhythmGuardListeningTimeout(
        untilEpochMs: Long,
        reason: String = "",
        startedAtEpochMs: Long = System.currentTimeMillis()
    ) {
        val safeUntil = untilEpochMs.coerceAtLeast(0L)
        val safeReason = reason.trim()
        val safeStartedAt = when {
            safeUntil <= 0L -> 0L
            startedAtEpochMs <= 0L -> System.currentTimeMillis().coerceAtMost(safeUntil)
            else -> startedAtEpochMs.coerceAtMost(safeUntil)
        }.coerceAtLeast(0L)
        prefs.edit {
            putLong(AppSettings.KEY_RHYTHM_GUARD_TIMEOUT_UNTIL_MS, safeUntil)
            putString(AppSettings.KEY_RHYTHM_GUARD_TIMEOUT_REASON, safeReason)
            putLong(AppSettings.KEY_RHYTHM_GUARD_TIMEOUT_STARTED_AT_MS, safeStartedAt)
            putInt(AppSettings.KEY_RHYTHM_GUARD_NEXT_ALLOWED_LIMIT_MINUTES, 0)

            if (safeUntil > 0L) {
                putLong(AppSettings.KEY_RHYTHM_GUARD_TIMEOUT_COOLDOWN_UNTIL_MS, 0L)
            }
        }
        _rhythmGuardTimeoutUntilMs.value = safeUntil
        _rhythmGuardTimeoutReason.value = safeReason
        _rhythmGuardTimeoutStartedAtMs.value = safeStartedAt
        _rhythmGuardNextAllowedLimitMinutes.value = 0
        if (safeUntil > 0L) {
            _rhythmGuardTimeoutCooldownUntilMs.value = 0L
        }
    }
    fun clearRhythmGuardListeningTimeout() {
        prefs.edit {
    putLong(AppSettings.KEY_RHYTHM_GUARD_TIMEOUT_UNTIL_MS, 0L)
    putString(AppSettings.KEY_RHYTHM_GUARD_TIMEOUT_REASON, "")
    putLong(AppSettings.KEY_RHYTHM_GUARD_TIMEOUT_STARTED_AT_MS, 0L)
}
        _rhythmGuardTimeoutUntilMs.value = 0L
        _rhythmGuardTimeoutReason.value = ""
        _rhythmGuardTimeoutStartedAtMs.value = 0L
    }
    fun setRhythmGuardTimeoutCooldownWithLimit(cooldownUntilMs: Long, nextLimitMinutes: Int) {
        val safeUntil = cooldownUntilMs.coerceAtLeast(0L)
        prefs.edit {
            putLong(AppSettings.KEY_RHYTHM_GUARD_TIMEOUT_COOLDOWN_UNTIL_MS, safeUntil)
            if (nextLimitMinutes > 0) {
                putInt(AppSettings.KEY_RHYTHM_GUARD_NEXT_ALLOWED_LIMIT_MINUTES, nextLimitMinutes)
            }
        }
        if (nextLimitMinutes > 0) {
            _rhythmGuardNextAllowedLimitMinutes.value = nextLimitMinutes
        }
        _rhythmGuardTimeoutCooldownUntilMs.value = safeUntil
    }
    fun clearRhythmGuardTimeoutCooldown() {
        setRhythmGuardTimeoutCooldownUntilMs(0L)
    }
    fun isRhythmGuardTimeoutActive(nowMs: Long = System.currentTimeMillis()): Boolean {
        val timeoutUntil = _rhythmGuardTimeoutUntilMs.value
        return timeoutUntil > nowMs
    }
    fun estimateRhythmGuardTodayListeningMinutes(
        dailyListeningStats: Map<String, Long>,
        songsPlayed: Int,
        listeningTimeMs: Long
    ): Int {
        val today = java.time.LocalDate.now().toString()
        val songsToday = (dailyListeningStats[today] ?: 0L).coerceAtLeast(0L)
        if (songsToday == 0L) return 0

        val safeSongsPlayed = songsPlayed.coerceAtLeast(1)
        val averageSongMinutes = ((listeningTimeMs / (1000f * 60f)) / safeSongsPlayed)
            .coerceIn(2.0f, 6.0f)

        return (songsToday * averageSongMinutes).toInt().coerceAtLeast(0)
    }
    fun estimateRhythmAuraTodayListeningMinutes(
        dailyListeningStats: Map<String, Long>,
        songsPlayed: Int,
        listeningTimeMs: Long
    ): Int = estimateRhythmGuardTodayListeningMinutes(dailyListeningStats, songsPlayed, listeningTimeMs)
    internal fun sanitizeRhythmGuardMode(mode: String?): String {
        return when (mode) {
            AppSettings.RHYTHM_GUARD_MODE_AUTO,
            AppSettings.RHYTHM_GUARD_MODE_MANUAL,
            AppSettings.RHYTHM_GUARD_MODE_OFF -> mode
            else -> AppSettings.RHYTHM_GUARD_MODE_OFF
        }
    }
    internal fun sanitizeRhythmAuraMode(mode: String?): String = sanitizeRhythmGuardMode(mode)

    private fun safeLong(key: String, defaultValue: Long): Long {
        val stored = prefs.getLong(key, Long.MIN_VALUE)
        return if (stored == Long.MIN_VALUE) defaultValue else stored
    }
}
