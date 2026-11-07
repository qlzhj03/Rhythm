package chromahub.rhythm.app.core.common

import android.content.Context
import android.content.Intent
import android.util.Log

object ServiceStartUtils {

    /**
     * Starts a service command defensively across foreground/background contexts.
     */
    fun startServiceSafely(
        context: Context,
        intent: Intent,
        logTag: String,
        reason: String
    ): Boolean {
        return try {
            context.startService(intent)
            true
        } catch (startException: Exception) {
            Log.w(
                logTag,
                "startService failed for $reason: ${startException.javaClass.simpleName}: ${startException.message}"
            )
            false
        }
    }
}
