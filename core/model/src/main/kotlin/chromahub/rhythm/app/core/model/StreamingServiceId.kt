package chromahub.rhythm.app.core.model

object StreamingServiceId {
    const val SUBSONIC = "SUBSONIC"
    const val JELLYFIN = "JELLYFIN"
    const val NETEASE = "NETEASE"
    val all = listOf(
        SUBSONIC,
        JELLYFIN,
        NETEASE
    )
}

object StreamingServiceRules {
    fun requiresServerUrl(serviceId: String): Boolean {
        return serviceId == StreamingServiceId.SUBSONIC || serviceId == StreamingServiceId.JELLYFIN
    }

    /** NetEase signs in by scanning a QR code in the official app; no server URL or password. */
    fun usesQrLogin(serviceId: String): Boolean {
        return serviceId == StreamingServiceId.NETEASE
    }
}
