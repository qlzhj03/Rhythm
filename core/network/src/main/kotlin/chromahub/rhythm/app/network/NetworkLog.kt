package chromahub.rhythm.app.network

import android.util.Log
import com.qlzhj.rhythm.core.common.SENSITIVE_HEADERS
import com.qlzhj.rhythm.core.common.sanitizeLogMessage
import okhttp3.logging.HttpLoggingInterceptor
import com.qlzhj.rhythm.core.common.SENSITIVE_HEADERS
import com.qlzhj.rhythm.core.common.sanitizeLogMessage

/**
 * Single entry point for network-related logging.
 *
 * Policy:
 * - Every message is passed through [sanitizeLogMessage], so URLs, headers, credentials
 *   and exception messages are redacted regardless of which client produced the line.
 * - Verbose `d` logs are debug-only; `i`/`w`/`e` stay available in release but carry no
 *   raw URLs, headers or stack traces.
 * - All HTTP clients share one interceptor configuration: `BASIC` in debug, `NONE` in
 *   release, with the shared sensitive-header list redacted. `BODY` is never used.
 */
object NetworkLog {
    const val TAG = "RhythmNetwork"

    fun d(tag: String, message: String) {
        if (NetworkBuildInfo.isDebug) {
            Log.d(tag, sanitizeLogMessage(message))
        }
    }

    fun i(tag: String, message: String) {
        Log.i(tag, sanitizeLogMessage(message))
    }

    fun w(tag: String, message: String, throwable: Throwable? = null) {
        val safe = sanitizeLogMessage(message)
        if (throwable != null && NetworkBuildInfo.isDebug) {
            Log.w(tag, safe, throwable)
        } else {
            Log.w(tag, safe)
        }
    }

    fun e(tag: String, message: String, throwable: Throwable? = null) {
        val safe = sanitizeLogMessage(message)
        if (throwable != null && NetworkBuildInfo.isDebug) {
            Log.e(tag, safe, throwable)
        } else {
            Log.e(tag, safe)
        }
    }

    fun createLoggingInterceptor(): HttpLoggingInterceptor =
        HttpLoggingInterceptor { message ->
            try {
                Log.d(TAG, sanitizeLogMessage(message))
            } catch (e: Exception) {
                Log.w(TAG, "Error logging HTTP message: ${sanitizeLogMessage(e.message)}")
            }
        }.apply {
            level = if (NetworkBuildInfo.isDebug) {
                HttpLoggingInterceptor.Level.BASIC
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
            SENSITIVE_HEADERS.forEach(::redactHeader)
        }
}
