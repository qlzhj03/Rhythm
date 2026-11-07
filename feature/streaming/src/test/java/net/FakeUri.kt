package android.net

/**
 * android.jar stub 不提供可用的 Uri 工厂（Uri.parse 在 JVM 单测中返回 null），
 * 构造器又是 package-private，因此把 fake 放在 android.net 包内直接子类化，
 * 供 JVM 单测构造携带 Uri 的模型类（如 Song）。
 *
 * 仅保证实例合法；不模拟 Uri 语义，快速路径在 codec 可信时不读取 uri 内容。
 */
class FakeUri(
    private val uriString: String,
) : Uri() {
    override fun getScheme(): String? = uriString.substringBefore(":").takeIf { ":" in uriString }

    override fun getSchemeSpecificPart(): String = uriString.substringAfter(":", "")

    override fun getEncodedSchemeSpecificPart(): String = getSchemeSpecificPart()

    override fun getAuthority(): String? = null

    override fun getEncodedAuthority(): String? = null

    override fun getUserInfo(): String? = null

    override fun getEncodedUserInfo(): String? = null

    override fun getHost(): String? = null

    override fun getPort(): Int = -1

    override fun getPath(): String? = uriString.substringAfter("://", "")

    override fun getEncodedPath(): String? = getPath()

    override fun getPathSegments(): MutableList<String> = mutableListOf()

    override fun getLastPathSegment(): String? = getPath()?.substringAfterLast('/')

    override fun getQuery(): String? = null

    override fun getEncodedQuery(): String? = null

    override fun getFragment(): String? = null

    override fun getEncodedFragment(): String? = null

    override fun getBooleanQueryParameter(
        key: String?,
        defaultValue: Boolean,
    ): Boolean = defaultValue

    override fun isOpaque(): Boolean = false

    override fun isAbsolute(): Boolean = getScheme() != null

    override fun isRelative(): Boolean = getScheme() == null

    override fun isHierarchical(): Boolean = true

    override fun normalizeScheme(): Uri = this

    override fun buildUpon(): Uri.Builder = throw UnsupportedOperationException("not needed in tests")

    override fun toString(): String = uriString

    override fun hashCode(): Int = uriString.hashCode()

    override fun equals(other: Any?): Boolean = other is FakeUri && other.uriString == uriString

    override fun compareTo(other: Uri): Int = 0

    override fun describeContents(): Int = 0

    override fun writeToParcel(
        dest: android.os.Parcel,
        flags: Int,
    ) {}
}
