package chromahub.rhythm.app.core.model

data class StreamingServiceSession(
    val serviceId: String,
    val isConnected: Boolean,
    val serverUrl: String = "",
    val username: String = "",
    val lastConnectedAtEpochMillis: Long = 0L
)
